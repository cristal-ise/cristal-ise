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

import static org.cristalise.kernel.security.BuiltInAuthc.ADMIN_ROLE;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.CollectionUtils;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStep;
import org.cristalise.kernel.lifecycle.instance.predefined.item.CreateItemFromDescription;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.module.ModuleImport;
import org.cristalise.kernel.utils.Logger;
import org.reflections.Reflections;

public class ImportRole extends ModuleImport {

    public Boolean jobList;
    public ArrayList<String> permissions = new ArrayList<>();
    private final String predefinedStepsPackage = "org.cristalise.kernel.lifecycle.instance.predefined";

    public ImportRole() {}

    @Override
    public Path create(AgentPath agentPath, boolean reset)
            throws ObjectAlreadyExistsException, ObjectCannotBeUpdated, CannotManageException, ObjectNotFoundException
    {
        addPredefinedStepsPermission();

        RolePath newRolePath = new RolePath(name.split("/"), (jobList == null) ? false : jobList, permissions);

        if (Gateway.getLookup().exists(newRolePath)) {
            //If jobList is null it means it was NOT set in the module.xml, therefore existing Role cannot be updated
            if (jobList != null) update(agentPath);
        }
        else {
            Logger.msg("ImportRole.create() - Creating Role:"+name+" joblist:"+jobList);

            //Checks if parent exists and throw ObjectNotFoundException
            newRolePath.getParent();

            Gateway.getLookupManager().createRole(newRolePath);
            Gateway.getLookupManager().setPermissions(newRolePath, newRolePath.getPermissionsList());
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
    public void update(AgentPath agentPath)
            throws ObjectAlreadyExistsException, ObjectCannotBeUpdated, CannotManageException, ObjectNotFoundException
    {
        addPredefinedStepsPermission();

        RolePath rolePath = new RolePath(name.split("/"), (jobList == null) ? false : jobList, permissions);

        if (!Gateway.getLookup().exists(rolePath))
            throw new ObjectNotFoundException("Role '" + rolePath.getName() + "' does NOT exists.");

        Gateway.getLookupManager().setHasJobList(rolePath, (jobList == null) ? false : jobList);
        Gateway.getLookupManager().setPermissions(rolePath, rolePath.getPermissionsList());
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

    /**
     * Adds the permission string for all predefined steps if it does not exists already
     */
    public void addPredefinedStepsPermission() {
        boolean addFlag = true;

        for (String actualPermission: permissions) {
            if (actualPermission.contains(CreateItemFromDescription.class.getSimpleName())) addFlag = false;
        }

        if (addFlag && permissions.size() != 0) {
            String steps = getPredefinedStepsPermissionString();

            if (StringUtils.isNotBlank(steps)) {
                Logger.msg(8, "ImportRole.addPredefinedStepsPermission() - '%s' has permissions %s", name, steps);
                permissions.add(steps);
            }
        }
    }

    /**
     * Scan the Cristal's predefined steps packages to get names of all the predefined steps.
     * 
     * @return list of strings of the predefined steps class Simple name
     */
    public List<String> getPredefinedStepNames() {
        ArrayList<String> stepNames = new ArrayList<String>();

        Reflections reflections = new Reflections(predefinedStepsPackage);
        Set<Class<? extends PredefinedStep>> classes = reflections.getSubTypesOf(PredefinedStep.class);

        for (Class<? extends PredefinedStep> clazz : classes) {
            String clazzName = clazz.getSimpleName();

            if (!"PredefinedStepCollectionBase".equals(clazzName)) stepNames.add(clazz.getSimpleName());
        }

        //The list is sorted for easier maintenance and testing
        Collections.sort(stepNames);

        return stepNames;
    }

    /**
     * Gets the predefined steps and convert to comma separated permission string.
     * @return
     */
    public String getPredefinedStepsPermissionString() {
        if (!ADMIN_ROLE.getName().equals(name)) {
            List<String> steps = getPredefinedStepNames();

            if (!CollectionUtils.isEmpty(steps)) return "*:" + String.join(",", steps) + ":*";
        }
        else {
            Logger.msg(5, "ImportRole.getPredefinedStepsPermissionString() - '%s' role does not require permissions for predefined steps", name);
        }

        return StringUtils.EMPTY;
    }
}
