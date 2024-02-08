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
package org.cristalise.kernel.lifecycle.instance.predefined;


import static org.cristalise.kernel.collection.Collection.Type.Bidirectional;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.DEPENDENCY_TO;
import static org.cristalise.kernel.SystemProperties.Erase_force;

import java.util.Collections;
import java.util.ListIterator;

import org.cristalise.kernel.collection.Dependency;
import org.cristalise.kernel.collection.DependencyMember;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.CriseVertxException;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Lookup.PagedResult;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.CastorHashMap;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Erase extends PredefinedStep {
    public static final String description =  "Deletes all domain paths (aliases), roles (if agent) and clusters for this item or agent.";
    private static final boolean FORCE_FLAG = Erase_force.getBoolean();

    public Erase(String schema, String desc) {
        super(schema, desc);
    }

    public Erase(String desc) {
        this(null, desc);
    }

    public Erase() {
        this(null, description);
    }

    /**
     * {@value #description}}
     * 
     * @param requestData is empty
     */
    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath item, int transitionID, String requestData, TransactionKey transactionKey)
            throws InvalidDataException, InvalidCollectionModification, ObjectNotFoundException, ObjectCannotBeUpdated, CannotManageException, PersistencyException, ObjectAlreadyExistsException
    {
        //read name before it is erased, so it can be logged
        String itemName = item.getItemName(transactionKey);

        eraseOneItem(agent, item, FORCE_FLAG, transactionKey);

        log.info("runActivityLogic() - DONE agent:{} item:{}", agent.getAgentName(transactionKey), itemName);

        return requestData;
    }

    protected void eraseOneItem(AgentPath agentP, ItemPath itemP, boolean forceFlag, TransactionKey transactionKey) 
            throws ObjectNotFoundException, ObjectCannotBeUpdated, CannotManageException, InvalidDataException, PersistencyException, InvalidCollectionModification, ObjectAlreadyExistsException
    {
        //read name before it is erased, so it can be logged
        String itemName = itemP.getItemName(transactionKey);

        removeBidirectionalReferences(agentP, itemP, forceFlag, transactionKey);
        removeAliases(itemP, transactionKey);
        removeRolesIfAgent(itemP, transactionKey);
        Gateway.getStorage().removeCluster(itemP, transactionKey);

        log.trace("eraseOneItem() - DONE uuid:{} name:{}", itemP, itemName);
    }

    protected void removeBidirectionalReferences(AgentPath agentP, ItemPath itemP, boolean forceFlag, TransactionKey transactionKey) 
            throws InvalidDataException, ObjectNotFoundException, InvalidCollectionModification, ObjectAlreadyExistsException
    {
        ItemProxy item = Gateway.getProxy(itemP, transactionKey);

        String[] collNames = item.getContents(ClusterType.COLLECTION, transactionKey);

        for (String collName : collNames) {
            Dependency myDep = (Dependency) item.getCollection(collName, transactionKey);
            
            if (myDep.getType() == Bidirectional) {
                triggerRemoveMembersFromCollection(agentP, itemP, myDep, forceFlag, transactionKey);
            }
        }
    }

    private void triggerRemoveMembersFromCollection(AgentPath agentP, ItemPath itemP, Dependency myDep, boolean forceFlag, TransactionKey transactionKey) 
            throws InvalidDataException, InvalidCollectionModification, ObjectAlreadyExistsException
    {
        for (DependencyMember member: myDep.getMembers().list) {
            String toDependencyName = myDep.getToDependencyName(member.getItemPath(), transactionKey);
            Dependency toDep = new Dependency(toDependencyName);

            String dependencyString;

            CastorHashMap memberProps = member.getProperties();
            memberProps.setBuiltInProperty(DEPENDENCY_TO, myDep.getName());
            DependencyMember newMember = toDep.addMember(itemP, memberProps, member.getClassProps(), null);
            newMember.setID(-1); // forces the RemoveMembersFromCollection to use ItemPath instead of slotID

            dependencyString = Gateway.getMarshaller().marshall(toDep);

            // Special error handling due to the forceFalg 
            try {
                new RemoveMembersFromCollection().request(agentP, member.getItemPath(), dependencyString, transactionKey);
            }
            catch (InvalidDataException | InvalidCollectionModification e) {
                if (forceFlag) {
                    log.info("triggerRemoveMembersFromCollection()", e);
                }
                else {
                    throw e;
                }
            }
            catch (CriseVertxException e) {
                if (forceFlag) {
                    log.info("triggerRemoveMembersFromCollection()", e);
                }
                else {
                    throw new InvalidDataException(e);
                }
            }
        }
    }

    /**
     * 
     * @param item
     * @throws ObjectNotFoundException
     * @throws ObjectCannotBeUpdated
     * @throws CannotManageException
     */
    protected void removeAliases(ItemPath item, TransactionKey transactionKey) throws ObjectCannotBeUpdated, CannotManageException {
        PagedResult domPaths = Gateway.getLookup().searchAliases(item, 0, 0, transactionKey);

        // sort DomainPathes alphabetically
        Collections.sort(domPaths.rows, (o1, o2) -> (o1.getStringPath().compareTo(o2.getStringPath())));

        ListIterator<Path> listIter = domPaths.rows.listIterator(domPaths.rows.size());

        // Delete the DomainPathes in reverse alphabetical order to avoid 'Path is not a leaf error'
        while (listIter.hasPrevious()) {
            DomainPath path = (DomainPath) listIter.previous();
            log.debug("removeAliases() - path:{}", path);
            Gateway.getLookupManager().delete(path, transactionKey);
        }
    }

    /**
     * 
     * @param item
     * @throws ObjectCannotBeUpdated
     * @throws ObjectNotFoundException
     * @throws CannotManageException
     */
    protected void removeRolesIfAgent(ItemPath item, TransactionKey transactionKey) throws ObjectCannotBeUpdated, ObjectNotFoundException, CannotManageException {
        try {
            AgentPath targetAgent = new AgentPath(item);

            //This check if the item is an agent or not
            if (targetAgent.getAgentName(transactionKey) != null) {
                for (RolePath role : targetAgent.getRoles(transactionKey)) {
                    Gateway.getLookupManager().removeRole(targetAgent, role, transactionKey);
                }
            }
        }
        catch (InvalidItemPathException e) {
            //this is actually never happens, new AgentPath(item) does not throw InvalidAgentPathException
            //but the exception is needed for 'backward compatibility'
        }
    }
}
