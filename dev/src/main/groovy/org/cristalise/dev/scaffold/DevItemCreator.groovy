/**
 * This file is part of the CRISTAL-iSE Development Module.
 * Copyright (c) 2001-2017 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.dev.scaffold

import org.apache.commons.lang3.NotImplementedException
import org.cristalise.kernel.entity.Job
import org.cristalise.kernel.entity.proxy.AgentProxy
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.ActivityDef
import org.cristalise.kernel.lifecycle.CompositeActivityDef
import org.cristalise.kernel.test.utils.KernelXMLUtility
import org.cristalise.kernel.utils.DescriptionObject
import groovy.transform.CompileStatic

@CompileStatic
class DevItemCreator extends CRUDItemCreator {

    public DevItemCreator(String ns, UpdateMode mode) {
        super(ns, mode)
    }

    /**
     * Use this constructor when agent cannot be initialised using the inherited login() method.
     * @param a the authenticated agent to be used to create the agents
     */
    public DevItemCreator(String ns, UpdateMode mode, AgentProxy a) {
        super(ns, mode, a)
    }

    private Job executeDoneJob(ItemProxy proxy, String actName) {
        return executeDoneJob(proxy, actName, null)
    }

    private Job executeDoneJob(ItemProxy proxy, String actName, String outcomeXML) {
        def job = proxy.getJobByName(actName, agent)
        
        assert job, "No job found for $actName"

        if (outcomeXML)            job.outcome = outcomeXML
        else if (job.hasOutcome()) job.outcome = job.getOutcome() //this calls outcome initiator if defined

        String resultOutcome = agent.execute(job)
        if (job.hasOutcome()) assert resultOutcome

        return job
    }

    public ItemProxy createItemWithUpdateAndAssignNewVersion(DescriptionObject descObj) {
        def descItem = createItemWithUpdate(
            Name: descObj.name,
            SubFolder: descObj.namespace,
            outcome: descObj.toOutcome(),
            descObj.resourceType.getFactoryPath()
        )

        //assert ! item.checkViewpoint(descObj.resourceType.schemaName, '0')
        //assert item.checkViewpoint(descObj.resourceType.schemaName, 'last')

        switch (descObj.class) {
            case CompositeActivityDef: break; // this case shall be before ActivityDef case
            case ActivityDef: editElemActDef(descItem, (ActivityDef)descObj); break;
        }

        executeDoneJob(descItem, descObj.resourceType.getAssignVersionActivityName())

        return descItem
    }

    public ItemProxy updateItemAndMoveLatestVersion(DescriptionObject descObj) {
        throw new NotImplementedException("descObj:${descObj.name} type:{$descObj.resourceType}")
    }
        
    private void editElemActDef(ItemProxy eaDescItem, ActivityDef eaDef) {
        if (eaDef.schema) {
            def xml = KernelXMLUtility.getDescObjectDetailsXML(id: eaDef.schema.name, version: eaDef.schema.version)
            executeDoneJob(eaDescItem, "SetSchema", xml)
        }

        if (eaDef.stateMachine) {
            def xml = KernelXMLUtility.getDescObjectDetailsXML(id: eaDef.stateMachine.name, version: eaDef.stateMachine.version)
            executeDoneJob(eaDescItem, "OverrideStateMachine", xml)
        }

        if (eaDef.script) {
            def xml = KernelXMLUtility.getDescObjectDetailsXML(id: eaDef.script.name, version: eaDef.script.version)
            executeDoneJob(eaDescItem, "AssignScript", xml)
        }

        if (eaDef.query) {
            def xml = KernelXMLUtility.getDescObjectDetailsXML(id: eaDef.query.name, version: eaDef.query.version)
            executeDoneJob(eaDescItem, "AssignQuery", xml)
        }
    }
}
