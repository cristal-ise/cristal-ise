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
package org.cristalise.kernel.process.module;

import static org.cristalise.kernel.collection.BuiltInCollections.QUERY;
import static org.cristalise.kernel.collection.BuiltInCollections.SCHEMA;
import static org.cristalise.kernel.collection.BuiltInCollections.SCRIPT;
import static org.cristalise.kernel.collection.BuiltInCollections.STATE_MACHINE;

import org.cristalise.kernel.collection.BuiltInCollections;
import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.collection.CollectionMember;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lifecycle.ActivityDef;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.resource.BuiltInResources;
import org.cristalise.kernel.process.resource.ResourceImportHandler;
import org.cristalise.kernel.utils.LocalObjectLoader;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter @Setter @Slf4j
public class ModuleActivity extends ModuleResource {

    ModuleDescRef script, schema, query, stateMachine;
    ActivityDef   actDef;

    public ModuleActivity() {
        super();
        type = BuiltInResources.ELEM_ACT_DESC_RESOURCE;
    }

    public ModuleActivity(ItemProxy child, Integer version) throws ObjectNotFoundException, InvalidDataException {
        this();
        this.version = version;

        script       = getDescRef(child, SCRIPT);
        schema       = getDescRef(child, SCHEMA);
        query        = getDescRef(child, QUERY);
        stateMachine = getDescRef(child, STATE_MACHINE);
    }

    public ModuleDescRef getDescRef(ItemProxy child, BuiltInCollections collection) throws ObjectNotFoundException, InvalidDataException {
        Collection<?> coll = child.getCollection(collection.getName(), version);

        if (coll.size() == 1) throw new InvalidDataException("Too many members in " + collection + " collection in " + name);

        CollectionMember collMem = coll.getMembers().list.get(0);
        return new ModuleDescRef(null, collMem.getChildUUID(), Integer.valueOf(collMem.getProperties().get("Version").toString()));
    }

    @Override
    public Path create(AgentPath agentPath, boolean reset, TransactionKey transactionKey) 
            throws ObjectNotFoundException, ObjectCannotBeUpdated, CannotManageException, ObjectAlreadyExistsException, InvalidDataException
    {
        try {
            ResourceImportHandler importHandler = Gateway.getResourceImportHandler(type);
            domainPath = importHandler.importResource(ns, name, version, itemPath, getResourceFileName(), reset, transactionKey);
            resourceChangeStatus  = importHandler.getResourceChangeStatus();
            resourceChangeDetails = importHandler.getResourceChangeDetails();
            itemPath = domainPath.getItemPath(transactionKey);
        }
        catch (Exception e) {
            log.error("Exception verifying module resource " + ns + "/" + name, e);
            throw new CannotManageException("Exception verifying module resource " + ns + "/" + name);
        }

        switch (resourceChangeStatus) {
            case NEW:
            case UPDATED:
            case OVERWRITTEN:
                actDef = LocalObjectLoader.getActDef(name, version, transactionKey);
                populateActivityDef(transactionKey);

                CollectionArrayList colls = actDef.makeDescCollections(transactionKey);

                for (Collection<?> coll : colls.list) {
                    try {
                        Gateway.getStorage().put(itemPath, coll, transactionKey);
                        // create last collection
                        coll.setVersion(null);
                        Gateway.getStorage().put(itemPath, coll, transactionKey);
                    }
                    catch (PersistencyException e) {
                        log.error("Storing description collections for " + getName() + ".", e);
                        throw new CannotManageException("Persistency exception storing description collections for " + getName() + ".");
                    }
                }
                break;

            case IDENTICAL:
            case SKIPPED:
                //nothing to do
                break;

            case REMOVED:
                // TODO implement
                throw new CannotManageException("Unimplemented resourceChangeStatus '"+resourceChangeStatus+ "' for "+ns+"/"+name);

            default:
                throw new CannotManageException("Unknown resourceChangeStatus '"+resourceChangeStatus+ "' for "+ns+"/"+name);
        }

        return domainPath;
    }

    public void populateActivityDef(TransactionKey transactionKey) throws ObjectNotFoundException, CannotManageException {
        try {
            if (schema != null) {
                actDef.setSchema(LocalObjectLoader.getSchema(
                        schema.id == null ? schema.name : schema.id, 
                        Integer.valueOf(schema.version), transactionKey));
            }
        }
        catch (NumberFormatException | InvalidDataException e) {
            String msg = "Invalid Schema definition for " +ns+"/"+name;
            log.error(msg, e);
            throw new CannotManageException(msg + " exception:" + e.getMessage());
        }

        try {
            if (script != null) {
                actDef.setScript(LocalObjectLoader.getScript(
                        script.id == null ? script.name : script.id,
                        Integer.valueOf(script.version), transactionKey));
            }
        }
        catch (NumberFormatException | InvalidDataException e) {
            String msg = "Invalid Script definition for " +ns+"/"+name;
            log.error(msg, e);
            throw new CannotManageException(msg + " exception:" + e.getMessage());
        }

        try {
            if (query != null) {
                actDef.setQuery(LocalObjectLoader.getQuery(
                        query.id == null ? query.name : query.id, 
                        Integer.valueOf(query.version), transactionKey));
            }
        }
        catch (NumberFormatException | InvalidDataException e) {
            String msg = "Invalid Query definition for " +ns+"/"+name;
            log.error(msg, e);
            throw new CannotManageException(msg + " exception:" + e.getMessage());
        }

        try {
            if (stateMachine != null) {
                actDef.setStateMachine(LocalObjectLoader.getStateMachine(
                        stateMachine.id == null ? stateMachine.name : stateMachine.id,
                        Integer.valueOf(stateMachine.version), transactionKey));
            }
        }
        catch (NumberFormatException | InvalidDataException e) {
            String msg = "Invalid State Machine definition for " +ns+"/"+name;
            log.error(msg, e);
            throw new CannotManageException(msg + " exception:" + e.getMessage());
        }
    }
}
