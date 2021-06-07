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

import static org.cristalise.kernel.process.resource.BuiltInResources.ACTIVITY_DESC_RESOURCE;
import static org.cristalise.kernel.process.resource.BuiltInResources.AGENT_DESC_RESOURCE;
import static org.cristalise.kernel.process.resource.BuiltInResources.ELEM_ACT_DESC_RESOURCE;
import static org.cristalise.kernel.process.resource.BuiltInResources.COMP_ACT_DESC_RESOURCE;
import static org.cristalise.kernel.process.resource.BuiltInResources.ITEM_DESC_RESOURCE;
import static org.cristalise.kernel.process.resource.BuiltInResources.ROLE_DESC_RESOURCE;

import java.util.ArrayList;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.imports.ImportAgent;
import org.cristalise.kernel.entity.imports.ImportItem;
import org.cristalise.kernel.entity.imports.ImportRole;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.AbstractMain;
import org.cristalise.kernel.process.resource.BuiltInResources;
import org.cristalise.kernel.utils.CastorArrayList;
import org.cristalise.kernel.utils.LocalObjectLoader;

import lombok.extern.slf4j.Slf4j;

/**
 * Wrapper class to handle the resources by type declared in the Module
 *
 */
@Slf4j
public class ModuleImports extends CastorArrayList<ModuleImport> {

    public ModuleImports() {
        super();
    }

    public ModuleImports(ArrayList<ModuleImport> aList) {
        super(aList);
    }

    /**
     * Returns all pure Resources defined in Module
     * @return all pure Resources defined in Module
     */
    public ArrayList<ModuleResource> getResources() {
        ArrayList<ModuleResource> subset = new ArrayList<ModuleResource>();

        for (ModuleImport imp : list) {
            if (imp instanceof ModuleResource) subset.add((ModuleResource)imp);
        }
        return subset;
    }

    /**
     * Returns all Items defined in Module
     * @return all Items defined in Module
     */
    public ArrayList<ImportItem> getItems(TransactionKey transactionKey) {
        ArrayList<ImportItem> subset = new ArrayList<ImportItem>();

        for (ModuleImport moduleImport : list) {
            if (moduleImport instanceof ImportItem) {
                subset.add((ImportItem)moduleImport);
            }
            else if (moduleImport instanceof ModuleItem) {
                ModuleItem moduleItem = (ModuleItem) moduleImport;

                try {
                    ImportItem importItem = LocalObjectLoader.getItemDesc(moduleImport.getName(), moduleItem.getVersion(), transactionKey);
                    importItem.setItemPath(null);
                    importItem.setResourceChangeStatus(moduleItem.getResourceChangeStatus());
                    subset.add(importItem);
                }
                catch (ObjectNotFoundException | InvalidDataException e) {
                    log.error("Could not load itemDesc:{} version:{}", moduleImport.getName(), moduleItem.getVersion(), e);
                    AbstractMain.shutdown(1);
                }
            }
        }
        return subset;
    }

    /**
     * Returns all Agents defined in Module
     * @return all Agents defined in Module
     */
    public ArrayList<ImportAgent> getAgents(TransactionKey transactionKey) {
        ArrayList<ImportAgent> subset = new ArrayList<ImportAgent>();

        for (ModuleImport imp : list) {
            if (imp instanceof ImportAgent) {
                subset.add((ImportAgent)imp);
            }
            else if (imp instanceof ModuleAgent) {
                int version = ((ModuleAgent) imp).getVersion();
                try {
                    ImportAgent importAgent= LocalObjectLoader.getAgentDesc(imp.getName(), version, transactionKey);
                    importAgent.setItemPath(null);
                    importAgent.setResourceChangeStatus(imp.getResourceChangeStatus());
                    subset.add(importAgent);
                }
                catch (ObjectNotFoundException | InvalidDataException e) {
                    log.error("Could not load agentDesc:{} version:{}", imp.getName(), version, e);
                    AbstractMain.shutdown(1);
                }
            }
        }
        return subset;
    }

    /**
     * Returns all Roles defined in Module
     * @return all Roles defined in Module
     */
    public ArrayList<ImportRole> getRoles(TransactionKey transactionKey) {
        ArrayList<ImportRole> subset = new ArrayList<ImportRole>();

        for (ModuleImport imp : list) {
            if (imp instanceof ImportRole) {
                subset.add((ImportRole)imp);
            }
            else if (imp instanceof ModuleRole) {
                int version = ((ModuleRole) imp).getVersion();
                try {
                    ImportRole importRole = LocalObjectLoader.getRoleDesc(imp.getName(), version, transactionKey);
                    importRole.setItemPath(null);
                    importRole.setResourceChangeStatus(imp.getResourceChangeStatus());
                    subset.add(importRole);
                }
                catch (ObjectNotFoundException | InvalidDataException e) {
                    log.error("Could not load roleDesc:{} version:{}", imp.getName(), version, e);
                    AbstractMain.shutdown(1);
                }
            }
        }
        return subset;
    }

    /**
     * Finds the name in the imports
     * 
     * @param name hte name of the import
     * @return the ModuleImport if found null otherwise
     */
    public ModuleImport findImport(String name, String typeCode) {
        BuiltInResources type = BuiltInResources.getValue(typeCode);

        for (ModuleImport thisImport : list) {
            BuiltInResources thisType = null;
            //ImportItem/ImportAgent/ImportRole does not have a getTypeCode()
            if (thisImport instanceof ModuleResource) {
                thisType = ((ModuleResource) thisImport).type;
            }
            else if(thisImport instanceof ImportAgent) {
                thisType =  AGENT_DESC_RESOURCE;
            }
            else if(thisImport instanceof ImportItem) {
                thisType =  ITEM_DESC_RESOURCE;
            }
            else if(thisImport instanceof ImportRole) {
                thisType =  ROLE_DESC_RESOURCE;
            }
            else {
                // this case should never happen?!?!
                log.warn("findImport() -  No typeCode is available for ModuleImport:{}", thisImport.getName());
            }

            if (thisType != null && thisImport.getName().equals(name)) {
                // AC is an abstract type, it can be either EA or CA
                if (type == ACTIVITY_DESC_RESOURCE) {
                    if (thisType == ELEM_ACT_DESC_RESOURCE || thisType == COMP_ACT_DESC_RESOURCE) {
                        return thisImport;
                    }
                }
                else if (type == thisType) {
                    return thisImport;
                }
            }
        }
        return null;
    }
}
