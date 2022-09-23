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

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.resource.BuiltInResources;
import org.cristalise.kernel.process.resource.ResourceImportHandler;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter @Setter @Slf4j
public class ModuleResource extends ModuleImport {

    public Integer          version;
    public BuiltInResources type;
    public String           resourceLocation;

    public ModuleResource() {
        // if not given, version defaults to 0
        version = 0;
    }

    /**
     * Get the string code of the ResourceType
     * @return typeCode of the Resource
     */
    public String getResourceType() {
        return type.getTypeCode();
    }

    /**
     * Set the type uing the string code
     * @param typeCode the string code of the Resource
     */
    public void setResourceType(String typeCode) {
        type = BuiltInResources.getValue(typeCode);
    }

    public String getResourceDir() {
        return "boot/" + type.getTypeCode();
    }

    public String getResourceExt() {
        return (type == BuiltInResources.SCHEMA_RESOURCE ? "xsd": "xml");
    }

    public String getResourceFileName() {
        if (StringUtils.isBlank(resourceLocation) && ns != null) {
            String[] vals = Gateway.getProperties().getString("Resource.moduleUseFileNameWithVersion", "").split(",");
            log.debug("getResourceFileName(ns:{}) - moduleUseFileNameWithVersion:{}", ns, Arrays.toString(vals));

            if (Arrays.asList(vals).contains(ns)) {
                resourceLocation = getResourceDir() + "/" + name + "_" + version + "." + getResourceExt();
                log.debug("getResourceFileName(WithVersion) - {}", resourceLocation );
            }
            else {
                resourceLocation = getResourceDir() + "/" + name + "." + getResourceExt();
                log.debug("getResourceFileName(NoVersion) - {}", resourceLocation );
            }
        }

        return resourceLocation;
    }

    @Override
    public Path create(AgentPath agentPath, boolean reset, TransactionKey transactionKey) 
            throws ObjectNotFoundException, ObjectCannotBeUpdated, CannotManageException, ObjectAlreadyExistsException, InvalidDataException
    {
        try {
            ResourceImportHandler importHandler = Gateway.getResourceImportHandler(type);

            domainPath = importHandler.importResource(ns, name, version, itemPath, getResourceFileName(), reset, transactionKey);
            resourceChangeStatus = importHandler.getResourceChangeStatus();
            resourceChangeDetails = importHandler.getResourceChangeDetails();

            if (itemPath == null) itemPath = domainPath.getItemPath(transactionKey);

            return domainPath;
        }
        catch (Exception e) {
            log.error("Exception verifying module resource {}/{}", ns, name, e);
            throw new CannotManageException("Exception verifying module resource " + ns + "/" + name);
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode() + type.getTypeCode().hashCode() + version;
    }
}