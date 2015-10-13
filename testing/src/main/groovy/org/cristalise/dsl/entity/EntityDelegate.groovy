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

import org.cristalise.dsl.lifecycle.instance.WorkflowBuilder
import org.cristalise.kernel.lifecycle.instance.Workflow


/**
 *
 */
@CompileStatic
class EntityDelegate extends EntityPropertyDelegate {
    
    String    name
    String    type
    Workflow  wf

    public EntityDelegate(String n, String t) {
        name = n
        type = t
    }

    public void processClosure(Closure cl) {
        assert cl
        assert name

        //Add the name and type of the Item to the Properties
        Property(Name: name)
        if(type) Property(Type: type)

        cl.delegate = this
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()
    }

    def Workflow(Closure cl) {
        wf = new WorkflowBuilder().build(cl)
    }
}
