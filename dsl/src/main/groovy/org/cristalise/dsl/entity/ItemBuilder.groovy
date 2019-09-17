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

import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.entity.imports.ImportItem
import org.cristalise.kernel.lifecycle.CompositeActivityDef
import org.cristalise.kernel.lookup.AgentPath
import org.cristalise.kernel.lookup.DomainPath;

import groovy.transform.CompileStatic


/**
 *
 */
@CompileStatic
class ItemBuilder {

    public ItemBuilder() {}

    public static ImportItem build(Map<String, Object> attrs, Closure cl) {
        assert attrs, "ItemBuilder build() cannot work with empty attributes (Map)"
        return build((String)attrs.name, (String)attrs.folder, attrs.workflow as String, attrs?.workflowVer as Integer, cl)
    }

    public static ImportItem build(String name, String folder, Object workflow, Integer workflowVer, Closure cl) {
        if(!name || !folder) throw new InvalidDataException("")

        def itemD = (workflow == null || workflow instanceof String) ? 
            new ItemDelegate(name, folder, (String)workflow, workflowVer) : new ItemDelegate(name, folder, (CompositeActivityDef)workflow)

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
    }
}
