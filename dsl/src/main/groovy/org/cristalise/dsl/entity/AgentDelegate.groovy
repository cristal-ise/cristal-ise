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

import groovy.lang.Closure;
import groovy.transform.CompileStatic

import java.util.List;

import org.cristalise.kernel.entity.imports.ImportAgent;
import org.cristalise.kernel.entity.imports.ImportRole


/**
 *
 */
@CompileStatic
class AgentDelegate extends PropertyDelegate {

    ImportAgent newAgent = new ImportAgent()

    public AgentDelegate(String name, String pwd) {
        newAgent.name = name
        newAgent.password = pwd
    }

    public void processClosure(Closure cl) {
        assert cl, "Delegate only works with a valid Closure"

        Property(Name: newAgent.name)
        Property(Type: "Agent")

        cl.delegate = this
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()

        if (itemProps) newAgent.properties = itemProps.list
    }


    def Roles(Closure cl) {
        newAgent.roles = RoleBuilder.build(cl)
    }
}
