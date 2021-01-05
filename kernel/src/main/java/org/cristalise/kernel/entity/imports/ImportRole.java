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

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.module.ModuleImport;
import org.cristalise.kernel.process.resource.BuiltInResources;
import org.cristalise.kernel.utils.DescriptionObject;
import org.cristalise.kernel.utils.FileStringUtility;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j @Getter @Setter
public class ImportRole extends ModuleImport implements DescriptionObject {

    public Integer version;

    public Boolean jobList = null;
    public ArrayList<String> permissions = new ArrayList<>();

    public ImportRole() {}

    public RolePath getRolePath() {
        return new RolePath(name.split("/"), (jobList == null) ? false : jobList, permissions);
    }

    public boolean exists(TransactionKey transactionKey) {
        return getRolePath().exists(transactionKey);
    }

    @Override
    public Path create(AgentPath agentPath, boolean reset, TransactionKey transactionKey)
            throws ObjectAlreadyExistsException, ObjectCannotBeUpdated, CannotManageException, ObjectNotFoundException
    {
        RolePath newRolePath = getRolePath();

        if (newRolePath.exists(transactionKey)) {
            //If jobList is null it means it was NOT set in the module.xml, therefore existing Role cannot be updated
            if (jobList != null) update(agentPath, transactionKey);
        }
        else {
            log.info("create() - Creating Role:"+name+" joblist:"+jobList);

            //Checks if parent exists and throw ObjectNotFoundException
            newRolePath.getParent(transactionKey);

            Gateway.getLookupManager().createRole(newRolePath, transactionKey);
            Gateway.getLookupManager().setPermissions(newRolePath, newRolePath.getPermissionsList(), transactionKey);
        }
        return newRolePath;
    }

    /**
     * 
     * @param agentPath
     * @throws ObjectAlreadyExistsException
     * @throws ObjectCannotBeUpdated
     * @throws CannotManageException
     * @throws ObjectNotFoundException
     */
    public void update(AgentPath agentPath, TransactionKey transactionKey) 
            throws ObjectAlreadyExistsException, ObjectCannotBeUpdated, CannotManageException, ObjectNotFoundException
    {
        log.info("update() - Updating Role:"+name+" joblist:"+jobList);
        RolePath rolePath = getRolePath();

        if (!rolePath.exists(transactionKey)) 
            throw new ObjectNotFoundException("Role '" + rolePath.getName() + "' does NOT exists.");

        Gateway.getLookupManager().setHasJobList(rolePath, (jobList == null) ? false : jobList, transactionKey);
        Gateway.getLookupManager().setPermissions(rolePath, rolePath.getPermissionsList(), transactionKey);
    }

    /**
     * 
     * @param rp
     * @return
     */
    public static ImportRole getImportRole(RolePath rp) {
        ImportRole ir = new ImportRole();

        ir.setName(rp.getName());
        ir.jobList = rp.hasJobList();
        ir.permissions = (ArrayList<String>) rp.getPermissionsList();

        return ir;
    }

    @Override
    public ItemPath getItemPath(TransactionKey transactionKey) {
        return getItemPath();
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
    public void export(Writer imports, File dir, boolean shallow) throws InvalidDataException, ObjectNotFoundException, IOException {
        String xml;
        String typeCode = BuiltInResources.ROLE_DESC_RESOURCE.getTypeCode();
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
            imports.write("<RoleResource "
                    + "name='" + getName() + "' "
                    + (getItemPath() == null ? "" : "id='"      + getItemID()  + "' ")
                    + (getVersion()  == null ? "" : "version='" + getVersion() + "'")
                    + "/>\n");
        }
    }

    @Override
    public String toString() {
        return "ImportRole(name:"+name+" version:"+version+" status:"+resourceChangeStatus+")";
    }
}
