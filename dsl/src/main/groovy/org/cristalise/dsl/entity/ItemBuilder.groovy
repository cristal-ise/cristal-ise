/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
package org.cristalise.dsl.entity

import groovy.transform.CompileStatic

import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.entity.imports.ImportItem
import org.cristalise.kernel.lookup.AgentPath
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath


/**
 *
 */
@CompileStatic
class ItemBuilder {

    public ItemBuilder() {}

    public static ImportItem build(Map<String, Object> attrs, Closure cl) {
        assert attrs, "ItemBuilder build() cannot work with empty attributes (Map)"
        return build((String)attrs.name, (String)attrs.folder, cl)
    }

    public static ImportItem build(String name, String folder, Closure cl) {
        if(!name || !folder) throw new InvalidDataException("")

        def itemD = new ItemDelegate(name, folder)

        itemD.processClosure(cl)

        return itemD.newItem
    }

    public static DomainPath create(Map<String, Object> attrs, Closure cl) {
        assert attrs, "ItemBuilder create() cannot work with empty attributes (Map)"
        assert attrs.agent && (attrs.agent instanceof AgentPath)

        return create((AgentPath)attrs.agent, build(attrs, cl))
    }

    public static DomainPath create(AgentPath agent, ImportItem item) {
        assert agent

        return (DomainPath)item.create(agent, true)
        /*
        DomainPath context = new DomainPath(new DomainPath(folder), name);

        if (context.exists()) throw new ObjectAlreadyExistsException("The path $context exists already.");

        Logger.msg(3, "ItemBuilder.create() - Creating CORBA Object");
        CorbaServer factory = Gateway.getCorbaServer();
        if (factory == null) throw new CannotManageException("This process cannot create new Items");
        ItemPath newItemPath = new ItemPath();
        TraceableEntity newItem = factory.createItem(newItemPath);

        Logger.msg(3, "ItemBuilder.create() - Adding entity '$newItemPath' to lookup");
        Gateway.getLookupManager().add(newItemPath);

        Logger.msg(3, "ItemBuilder.create() - Initializing entity");
        try {
            newItem.initialise(
                agent.getSystemKey(),
                Gateway.getMarshaller().marshall(props),
                wf == null ? null : Gateway.getMarshaller().marshall(wf.search("workflow/domain")),
                null);
        }
        catch (Exception ex) {
            Logger.error(ex)
            Gateway.getLookupManager().delete(newItemPath);
            throw new InvalidDataException("Problem initializing new Item '"+name+"'. See log: "+ex.getMessage());
        }

        // add its domain path
        Logger.msg(3, "ItemBuilder.create() - Creating context '$context'");
        context.setItemPath(newItemPath);
        Gateway.getLookupManager().add(context);
        
        return newItemPath
        */
    }
}
