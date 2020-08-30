package org.cristalise.dsl.lifecycle.definition
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

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.AGENT_NAME
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.AGENT_ROLE
import groovy.transform.CompileStatic

import org.cristalise.dsl.csv.TabularGroovyParser
import org.cristalise.dsl.property.PropertyDelegate
import org.cristalise.kernel.graph.layout.DefaultGraphLayoutGenerator
import org.cristalise.kernel.graph.model.GraphPoint
import org.cristalise.kernel.lifecycle.ActivityDef
import org.cristalise.kernel.lifecycle.ActivitySlotDef
import org.cristalise.kernel.lifecycle.CompositeActivityDef
import org.cristalise.kernel.lifecycle.WfVertexDef;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine
import org.cristalise.kernel.utils.LocalObjectLoader;


/**
 * Wrapper/Delegate class of CompositeActivityDef
 */
@CompileStatic
class CompActDefDelegate extends PropertyDelegate {

    ActivitySlotDef prevActSlotDef = null

    public CompositeActivityDef compActDef

    public Map<String, WfVertexDef> processClosure(String name, int version, Closure cl) {
        compActDef = new CompositeActivityDef()
        compActDef.name = name
        compActDef.version = version

        return processClosure(compActDef, cl)
    }

    public void processClosure(CompositeActivityDef caDef, Closure cl) {
        assert caDef

        compActDef = caDef

        if (cl) {
            cl.delegate = this
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl()

            props.each { k, v ->
                compActDef.properties.put(k, v, props.getAbstract().contains(k))
            }
        }
    }

    public void processTabularData(TabularGroovyParser parser) {
        def twb = new TabularWorkflowDefBuilder()
        buildCompActDef(twb.build(parser))
    }

    public String buildCompActDef(Layout s) {

    }

    def StateMachine(StateMachine s) {
        compActDef.setStateMachine(s)
    }

    def StateMachine(String name, int version = 0) {
        compActDef.setStateMachine(LocalObjectLoader.getStateMachine(name, version))
    }

    @Deprecated
    private int addActDefToSequence(String actName, ActivityDef actDef) {
        def newActSlotDef = compActDef.addExistingActivityDef(actName, actDef, new GraphPoint())

        //Simple logic only to add sequential activities
        if(prevActSlotDef) compActDef.addNextDef(prevActSlotDef, newActSlotDef)
        else               compActDef.getChildrenGraphModel().setStartVertexId(newActSlotDef.ID)

        prevActSlotDef = newActSlotDef;

        return newActSlotDef.ID
    }

    def ElemActDef(String actName, int actVer, Closure cl = null) {
        ActivityDef eaDef = ElemActDefBuilder.build('name': (Object)actName, 'version': actVer, cl)
        return ElemActDef(actName, eaDef)
    }

    def ElemActDef(ActivityDef actDef) {
        return addActDefToSequence(actDef.actName, actDef)
    }

    def ElemActDef(String actName, ActivityDef actDef) {
        return addActDefToSequence(actName, actDef)
    }

    def CompActDef(String actName, int actVer, Closure cl = null) {
        CompositeActivityDef caDef = CompActDefBuilder.build('name': (Object)actName, 'version': actVer, cl)
        return CompActDef(actName, caDef)
    }

    def CompActDef(CompositeActivityDef actDef) {
        return addActDefToSequence(actDef.actName, actDef)
    }

    def CompActDef(String actName, CompositeActivityDef actDef) {
        return addActDefToSequence(actName, actDef)
    }
}
