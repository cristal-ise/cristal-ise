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
package org.cristalise.dsl.lifecycle.definition

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.AGENT_NAME
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.AGENT_ROLE
import groovy.transform.CompileStatic

import org.cristalise.dsl.csv.TabularGroovyParser
import org.cristalise.dsl.property.PropertyDelegate
import org.cristalise.kernel.lifecycle.ActivityDef
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine
import org.cristalise.kernel.persistency.outcome.Schema
import org.cristalise.kernel.scripting.Script
import org.cristalise.kernel.utils.LocalObjectLoader

/**
 * Wrapper/Delegate class of Elementary Activity definition
 *
 */
@CompileStatic
class ElemActDefDelegate extends PropertyDelegate {

    //can be instance of CompositeActivityDef as well
    ActivityDef activityDef

    public ElemActDefDelegate() {}

    public ElemActDefDelegate(String n, Integer v) {
        activityDef = new ActivityDef(n, v)
    }

    public void processClosure(Closure cl) {
        if (cl) {
            cl.delegate = this
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl()

            props.each { k, v ->
                activityDef.properties.put(k, v, props.getAbstract().contains(k))
            }
        }
    }

    public List<ActivityDef> processTabularData(TabularGroovyParser parser) {
        def twb = new TabularActivityDefBuilder()
        def actDefs = twb.build(parser)

        if (activityDef.version != null) {
            actDefs.each { it.version = activityDef.version }
        }

        return actDefs
    }

    def Schema(Schema s) {
        activityDef.setSchema(s)
    }

    def Schema(String name, int ver = 0) {
        activityDef.setSchema(LocalObjectLoader.getSchema(name, ver))
    }

    def Script(Script s) {
        activityDef.setScript(s)
    }

    def Script(String name, int ver = 0) {
        activityDef.setScript(LocalObjectLoader.getScript(name, ver))
    }

    def Query(org.cristalise.kernel.querying.Query q) {
        activityDef.setQuery(q)
    }

    def Query(String name, int ver = 0) {
        activityDef.setQuery(LocalObjectLoader.getQuery(name, ver))
    }

    def StateMachine(StateMachine s) {
        activityDef.setStateMachine(s)
    }

    def StateMachine(String name, int ver = 0) {
        activityDef.setStateMachine(LocalObjectLoader.getStateMachine(name, ver))
    }

    def Agent(String a) {
        activityDef.setBuiltInProperty(AGENT_NAME, a)
    }

    def Role(String r) {
        activityDef.setBuiltInProperty(AGENT_ROLE, r)
    }
}
