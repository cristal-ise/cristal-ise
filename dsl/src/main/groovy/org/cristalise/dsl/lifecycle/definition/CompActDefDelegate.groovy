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

import org.apache.tools.ant.types.resources.selectors.InstanceOf

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
class CompActDefDelegate extends ElemActDefDelegate {

    public CompActDefDelegate(String n, Integer v) {
        activityDef = new CompositeActivityDef(n, v)
    }

    @Override
    public List<ActivityDef> processTabularData(TabularGroovyParser parser) {
        def layoutBuilder = new TabularLayoutDefBuilder((CompositeActivityDef)activityDef)
        //buildCompActDef(layoutBuilder.build(parser))
        return null
    }

    def ElemActDef(String actName, int actVer, Closure cl = null) {
        ActivityDef eaDef = ElemActDefBuilder.build('name': (Object)actName, 'version': actVer, cl)
        return ElemActDef(actName, eaDef)
    }

    def ElemActDef(ActivityDef actDef) {
        return ElemActDef(actDef.actName, actDef)
    }

    def ElemActDef(String actName, ActivityDef actDef) {
        return ((CompositeActivityDef)activityDef).addExistingActivityDef(actName, actDef, new GraphPoint())
    }

    def CompActDef(String actName, int actVer) {
        return CompActDef(LocalObjectLoader.getCompActDef(actName, actVer))
    }

    def CompActDef(CompositeActivityDef actDef) {
        return CompActDef(actDef.actName, actDef)
    }

    def CompActDef(String actName, CompositeActivityDef actDef) {
        return ((CompositeActivityDef)activityDef).addExistingActivityDef(actName, actDef, new GraphPoint())
    }

    def Layout(@DelegatesTo(CompActDefLayoutDelegate) Closure cl) {
        def delegate = new CompActDefLayoutDelegate((CompositeActivityDef)activityDef)
        delegate.processClosure(cl)
    }
}
