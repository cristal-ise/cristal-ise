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

import groovy.transform.CompileStatic

import org.cristalise.kernel.entity.imports.ImportRole


/**
 *
 */
@CompileStatic
class RoleDelegate {

    ArrayList<ImportRole> roles = new ArrayList<ImportRole>()

    public void processClosure(Closure cl) {
        cl.delegate = this
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()
    }

    public void Role(Map<String, Object> attrs, Closure cl = null) {
        assert attrs && attrs.name

        if(!attrs.jobList) attrs.jobList = false

        def role = new ImportRole()
        role.name = attrs.name
        role.jobList = attrs.jobList

        roles.add(role)

        if (cl) cl()
    }

    public void Permission(String p) {
        roles[roles.size-1].permissions.add(p)
    }

    public void Permission(Map<String, String>  args) {
        assert args.domain && args.actions && args.targets, 'domain:actions:targets triplet must be set'

        Permission("${args.domain}:${args.actions}:${args.targets}".toString())
    }
}
