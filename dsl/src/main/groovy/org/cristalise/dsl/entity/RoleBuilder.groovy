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

import groovy.lang.Closure
import groovy.transform.CompileStatic

import java.util.Map

import org.cristalise.kernel.entity.imports.ImportAgent
import org.cristalise.kernel.entity.imports.ImportRole
import org.cristalise.kernel.lookup.AgentPath
import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.lookup.RolePath
import org.cristalise.kernel.utils.Logger


/**
 *
 */
@CompileStatic
class RoleBuilder {

    public static ArrayList<ImportRole> build(Closure cl) {
        def roleDelegate = new RoleDelegate()

        roleDelegate.processClosure(cl)

        Logger.msg 5, "RoleBuilder.build() - Done"

        return roleDelegate.roles
    }

    public static List<RolePath> create(Closure cl) {
        return createRoles(build(cl))
    }

    public static RolePath create(AgentPath builderAgent, ImportRole newRole) {
        assert builderAgent && newRole
        return (RolePath)newRole.create(builderAgent, true)
    }

    public static List<RolePath> createRoles(List<ImportRole> roles) {
        List<RolePath> rolePathes = []

        //Creating roles does not require an Agent during bootstrap and import
        roles.each { ImportRole newRole -> rolePathes.add((RolePath)newRole.create(null, false)) }

        return rolePathes
    }
}
