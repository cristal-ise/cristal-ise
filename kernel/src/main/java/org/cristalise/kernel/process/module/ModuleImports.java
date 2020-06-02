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

import java.util.ArrayList;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.imports.ImportAgent;
import org.cristalise.kernel.entity.imports.ImportItem;
import org.cristalise.kernel.entity.imports.ImportRole;
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
    public ArrayList<ImportItem> getItems() {
        ArrayList<ImportItem> subset = new ArrayList<ImportItem>();

        for (ModuleImport imp : list) {
            if (imp instanceof ImportItem) {
                subset.add((ImportItem)imp);
            }
            else if (imp instanceof ModuleItem) {
                int version = ((ModuleItem) imp).getVersion();
                try {
                    ImportItem importItem = LocalObjectLoader.getItemDesc(imp.getName(), version);
                    importItem.setItemPath(null);
                    subset.add(importItem);
                }
                catch (ObjectNotFoundException | InvalidDataException e) {
                    log.error("Could not load itemDesc:{} version:{}", imp.getName(), version, e);
                }
            }
        }
        return subset;
    }

    /**
     * Returns all Agents defined in Module
     * @return all Agents defined in Module
     */
    public ArrayList<ImportAgent> getAgents() {
        ArrayList<ImportAgent> subset = new ArrayList<ImportAgent>();

        for (ModuleImport imp : list) {
            if (imp instanceof ImportAgent) {
                subset.add((ImportAgent)imp);
            }
            else if (imp instanceof ModuleAgent) {
                int version = ((ModuleAgent) imp).getVersion();
                try {
                    ImportAgent importAgent= LocalObjectLoader.getAgentDesc(imp.getName(), version);
                    importAgent.setItemPath(null);
                    subset.add(importAgent);
                }
                catch (ObjectNotFoundException | InvalidDataException e) {
                    log.error("Could not load agentDesc:{} version:{}", imp.getName(), version, e);
                }
            }
        }
        return subset;
    }

    /**
     * Returns all Roles defined in Module
     * @return all Roles defined in Module
     */
    public ArrayList<ImportRole> getRoles() {
        ArrayList<ImportRole> subset = new ArrayList<ImportRole>();

        for (ModuleImport imp : list) {
            if (imp instanceof ImportRole) {
                subset.add((ImportRole)imp);
            }
            else if (imp instanceof ModuleRole) {
                int version = ((ModuleRole) imp).getVersion();
                try {
                    ImportRole importRole = LocalObjectLoader.getRoleDesc(imp.getName(), version);
                    importRole.setItemPath(null);
                    subset.add(importRole);
                }
                catch (ObjectNotFoundException | InvalidDataException e) {
                    log.error("Could not load agentDesc:{} version:{}", imp.getName(), version, e);
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
    public ModuleImport findImport(String name) {
        for (ModuleImport imp : list) {
            if (imp.getName().equals(name)) return imp;
        }
        return null;
    }
}
