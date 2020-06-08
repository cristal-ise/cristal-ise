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

import static org.cristalise.kernel.property.BuiltInItemProperties.NAME;
import static org.cristalise.kernel.property.BuiltInItemProperties.TYPE;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.agent.ActiveEntity;
import org.cristalise.kernel.lifecycle.CompositeActivityDef;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidAgentPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.module.ModuleImport;
import org.cristalise.kernel.process.resource.BuiltInResources;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyArrayList;
import org.cristalise.kernel.utils.DescriptionObject;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.LocalObjectLoader;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter @Setter @Slf4j
public class ImportAgent extends ModuleImport implements DescriptionObject {

    protected Integer version; //optional

    private String                initialPath; //optional
    private String                password;
    private ArrayList<Property>   properties = new ArrayList<Property>();
    private ArrayList<ImportRole> roles      = new ArrayList<ImportRole>();

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
    public Path create(AgentPath agentPath, boolean reset)
            throws ObjectNotFoundException, ObjectCannotBeUpdated, CannotManageException, ObjectAlreadyExistsException
    {
        if (roles.isEmpty()) throw new ObjectNotFoundException("Agent '"+name+"' must declare at least one Role ");

        if (StringUtils.isNotBlank(initialPath)) {
            domainPath = new DomainPath(new DomainPath(initialPath), name);

            if (domainPath.exists()) {
                ItemPath domItem = domainPath.getItemPath();
                if (!getItemPath().equals(domItem)) {
                    throw new CannotManageException("'"+domainPath+"' was found with the different itemPath ("+domainPath.getItemPath()+" vs "+getItemPath()+")");
                }
            }
            else {
                isDOMPathExists = false;
            }
        }

        ActiveEntity newAgentEnt = getActiveEntity();

        // assemble properties
        properties.add(new Property(NAME, name, true));
        properties.add(new Property(TYPE, "Agent", false));

        try {
            if (StringUtils.isNotBlank(password)) Gateway.getLookupManager().setAgentPassword(getAgentPath(), password);

            newAgentEnt.initialise(
                    agentPath.getSystemKey(), 
                    Gateway.getMarshaller().marshall(new PropertyArrayList(properties)), 
                    Gateway.getMarshaller().marshall(((CompositeActivityDef)LocalObjectLoader.getCompActDef("NoWorkflow", 0)).instantiate()), 
                    null, "", "");
        }
        catch (Exception ex) {
            log.error("Error initialising new agent name:{}", name, ex);
            Gateway.getLookupManager().delete(getAgentPath());
            throw new CannotManageException("Error initialising new agent name:"+name);
        }

        for (ImportRole role : roles) {
            if (role.exists()) {
                RolePath rp = role.getRolePath();
                role.update(agentPath);

                if (!getAgentPath().hasRole(rp)) {
                    Gateway.getLookupManager().addRole(getAgentPath(), rp);
                }
            }
            else {
                RolePath thisRole = (RolePath)role.create(agentPath, reset);
                Gateway.getLookupManager().addRole(getAgentPath(), thisRole);
            }
        }

        if (domainPath != null && !isDOMPathExists) {
            domainPath.setItemPath(getItemPath());
            Gateway.getLookupManager().add(domainPath);
        }

        return getAgentPath();
    }

    private ActiveEntity getActiveEntity()
            throws ObjectNotFoundException, CannotManageException, ObjectAlreadyExistsException, ObjectCannotBeUpdated
    {
        ActiveEntity activeEntity;
        AgentPath ap = getAgentPath();

        if (ap.exists()) {
            log.info("getActiveEntity() - Existing agent:{}", name);
            try {
                activeEntity = Gateway.getCorbaServer().getAgent(ap);
                isNewItem = false;
            }
            catch (InvalidAgentPathException  e) {
                throw new ObjectAlreadyExistsException(e.getMessage());
            }
        }
        else {
            log.info("getActiveEntity() - Creating agent:{}", name);
            activeEntity = Gateway.getCorbaServer().createAgent(ap);
            Gateway.getLookupManager().add(ap);
        }
        return activeEntity;
    }

    public AgentPath getAgentPath() {
        return (AgentPath)getItemPath();
    }

    /**
     * Sets the ItemPath representing the Agent. Tries to find Agent if it already exists, 
     * otherwise creates  new ItemPath, i.e. it creates new UUID.
     */
    @Override
    public ItemPath getItemPath() {
        if (itemPath == null) {
            try {
                itemPath = Gateway.getLookup().getAgentPath(name);
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

    @Override
    public String getItemID() {
        return getID();
    }

    @Override
    public CollectionArrayList makeDescCollections() throws InvalidDataException, ObjectNotFoundException {
        return new CollectionArrayList();
    }

    @Override
    public void export(Writer imports, File dir, boolean shallow) throws InvalidDataException, ObjectNotFoundException, IOException {
        String xml;
        String typeCode = BuiltInResources.AGENT_DESC_RESOURCE.getTypeCode();
        String fileName = getName() + (getVersion() == null ? "" : "_" + getVersion()) + ".xml";

        try {
            xml = Gateway.getMarshaller().marshall(this);
        }
        catch (Exception e) {
            log.error("Couldn't marshall name:" + getName(), e);
            throw new InvalidDataException("Couldn't marshall name:" + getName());
        }

        FileStringUtility.string2File(new File(new File(dir, typeCode), fileName), xml);

        if (imports == null) return;

        if (Gateway.getProperties().getBoolean("Resource.useOldImportFormat", false)) {
            imports.write("<Resource "
                    + "name='" + getName() + "' "
                    + (getItemPath() == null ? "" : "id='"      + getItemID()  + "' ")
                    + (getVersion()  == null ? "" : "version='" + getVersion() + "' ")
                    + "type='" + typeCode + "'>boot/" + typeCode + "/" + fileName
                    + "</Resource>\n");
        }
        else {
            imports.write("<AgentResource "
                    + "name='" + getName() + "' "
                    + (getItemPath() == null ? "" : "id='"      + getItemID()  + "' ")
                    + (getVersion()  == null ? "" : "version='" + getVersion() + "'")
                    + "/>\n");
        }
    }

    @Override
    public String toString() {
        return "ImportAgent(name:"+name+" version:"+version+" status:"+resourceChangeStatus+")";
    }
}
