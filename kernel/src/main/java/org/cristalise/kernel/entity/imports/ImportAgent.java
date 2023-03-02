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
package org.cristalise.kernel.entity.imports;

import static org.cristalise.kernel.SystemProperties.Module_ImportAgent_enableRoleCreation;
import static org.cristalise.kernel.property.BuiltInItemProperties.NAME;
import static org.cristalise.kernel.property.BuiltInItemProperties.TYPE;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.entity.proxy.ProxyManager;
import org.cristalise.kernel.lifecycle.instance.CompositeActivity;
import org.cristalise.kernel.lifecycle.instance.predefined.item.CreateItemFromDescription;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.LookupManager;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.module.ModuleImport;
import org.cristalise.kernel.process.resource.BuiltInResources;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyArrayList;
import org.cristalise.kernel.security.BuiltInAuthc;
import org.cristalise.kernel.utils.DescriptionObject;
import org.cristalise.kernel.utils.LocalObjectLoader;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter @Setter @Slf4j
public class ImportAgent extends ModuleImport implements DescriptionObject {

    protected Integer version; //optional

    private String           initialPath; //optional
    private String           password;
    private List<Property>   properties = new ArrayList<Property>();
    private List<ImportRole> roles      = new ArrayList<ImportRole>();

    public ImportAgent() {}

    public ImportAgent(String folder, String aName, Integer version, String pwd) {
        setInitialPath(folder);
        setName(aName);
        setVersion(version);
        setPassword(pwd);
    }

    public ImportAgent(String folder, String aName, String pwd) {
        this(folder, aName, null, pwd);
    }

    /**
     * Constructor with mandatory fields
     * 
     * @param aName name of the agent
     * @param pwd the password of the agent
     */
    public ImportAgent(String aName, String pwd) {
        this(null, aName, null, pwd);
    }

    @Override
    public void setID(String uuid) throws InvalidItemPathException {
        if (StringUtils.isNotBlank(uuid)) itemPath = new AgentPath(new ItemPath(uuid), name);
    }

    /**
     * 
     */
    @Override
    public DomainPath getDomainPath() {
        if (domainPath == null && StringUtils.isNotBlank(initialPath)) {
            domainPath = new DomainPath(new DomainPath(initialPath), name);
        }
        return domainPath;
    }

    public boolean exists(TransactionKey transactionKey) {
        return getAgentPath(transactionKey).exists(transactionKey);
    }

    @Override
    public Path create(AgentPath agentPath, boolean reset, TransactionKey transactionKey)
            throws ObjectNotFoundException, ObjectCannotBeUpdated, CannotManageException, ObjectAlreadyExistsException
    {
        if (roles.isEmpty()) throw new ObjectNotFoundException("Agent '"+name+"' must declare at least one Role ");

        if (StringUtils.isNotBlank(initialPath)) {
            getDomainPath();

            if (domainPath.exists(transactionKey)) {
                ItemPath domItem = domainPath.getItemPath(transactionKey);
                if (!getItemPath(transactionKey).equals(domItem)) {
                    throw new CannotManageException("'"+domainPath+"' was found with the different itemPath ("+domainPath.getItemPath(transactionKey)+" vs "+getItemPath(transactionKey)+")");
                }
            }
            else {
                isDOMPathExists = false;
            }
        }

        // assemble properties
        properties.add(new Property(NAME, name, true));
        properties.add(new Property(TYPE, "Agent", false));

        LookupManager lookupManager = Gateway.getLookupManager();

        try {
            CreateItemFromDescription.storeItem(
                    agentPath, 
                    getOrCreateAgentPath(transactionKey),
                    new PropertyArrayList(properties),
                    null, //colls
                    (CompositeActivity)LocalObjectLoader.getCompActDef("NoWorkflow", 0, transactionKey).instantiate(transactionKey),
                    null, //initViewpoint
                    null, //initOutcomeString
                    transactionKey);

            if (StringUtils.isNotBlank(password)) {
                lookupManager.setAgentPassword(getAgentPath(transactionKey), password, false, transactionKey);
            }
        }
        catch (Exception ex) {
            log.error("Error initialising new agent name:{}", name, ex);
            lookupManager.delete(getAgentPath(transactionKey), transactionKey);
            throw new CannotManageException("Error initialising new agent name:"+name);
        }

        for (ImportRole role : roles) {
            RolePath rp = role.getRolePath();
            if (rp.exists(transactionKey)) {
                if (!getAgentPath().hasRole(rp, transactionKey)) {
                    lookupManager.addRole(getAgentPath(), rp, transactionKey);
                }
                // no update to the role is done, because role might not be fully specified (i.e. it only contains the name as a reference)
            }
            else {
                if (Module_ImportAgent_enableRoleCreation.getBoolean()) {
                    RolePath thisRole = (RolePath)role.create(agentPath, reset, transactionKey);
                    lookupManager.addRole(getAgentPath(), thisRole, transactionKey);
                }
                else {
                    throw new CannotManageException("Role '"+rp+ "' does not exists");
                } 
            }
        }

        if (domainPath != null && !isDOMPathExists) {
            domainPath.setItemPath(getItemPath(transactionKey));
            lookupManager.add(domainPath, transactionKey);
        }

        return getAgentPath();
    }

    private AgentPath getOrCreateAgentPath(TransactionKey transactionKey)
            throws ObjectNotFoundException, CannotManageException, ObjectAlreadyExistsException, ObjectCannotBeUpdated
    {
        AgentPath ap = getAgentPath(transactionKey);

        if (ap.exists(transactionKey)) {
            log.info("getActiveEntity() - Existing agent:{}", name);
            isNewItem = false;
        }
        else {
            log.info("getActiveEntity() - Creating agent:{}", name);
            Gateway.getLookupManager().add(ap, transactionKey);
        }
        
        return ap;
    }

    public AgentPath getAgentPath() {
        return getAgentPath(null);
    }

    public AgentProxy getProxy() {
        try {
            return ProxyManager.getAgentProxy(getAgentPath());
        } catch (ObjectNotFoundException e) {
            return null;
        }
    }

    public AgentPath getAgentPath(TransactionKey transactionKey) {
        return (AgentPath)getItemPath(transactionKey);
    }

    /**
     * Sets the ItemPath representing the Agent. Tries to find Agent if it already exists, 
     * otherwise creates  new ItemPath, i.e. it creates new UUID.
     */
    @Override
    public ItemPath getItemPath(TransactionKey transactionKey) {
        if (itemPath == null) {
            try {
                itemPath = Gateway.getLookup().getAgentPath(name, transactionKey);
            }
            catch (ObjectNotFoundException ex) {
                itemPath = new AgentPath(new ItemPath(), name);
            }
        }
        return itemPath;
    }

    public void addRoles(List<RolePath> newRoles) {
        for (RolePath rp: newRoles) roles.add(ImportRole.getImportRole(rp));
    }

    public void addRole(BuiltInAuthc role) {
        addRole(role.getName());
    }

    public void addRole(String roleName) {
        addRole(new ImportRole(roleName));
    }

    public void addRole(ImportRole ir) {
        roles.add(ir);
    }

    public void addRole(RolePath rp) {
        addRole(ImportRole.getImportRole(rp));
    }

    @Override
    public String getItemID() {
        return getID();
    }

    @Override
    public CollectionArrayList makeDescCollections(TransactionKey transactionKey) throws InvalidDataException, ObjectNotFoundException {
        return new CollectionArrayList();
    }

    @Override
    public BuiltInResources getResourceType() {
        return BuiltInResources.AGENT_DESC_RESOURCE;
    }

    @Override
    public String toString() {
        return "ImportAgent(name:"+name+" version:"+version+" status:"+resourceChangeStatus+")";
    }
}
