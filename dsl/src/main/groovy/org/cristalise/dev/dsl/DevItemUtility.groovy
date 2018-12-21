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
package org.cristalise.dev.dsl

import static org.cristalise.kernel.collection.BuiltInCollections.*
import static org.cristalise.kernel.process.resource.BuiltInResources.*

import org.cristalise.kernel.entity.agent.Job
import org.cristalise.kernel.entity.proxy.AgentProxy
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.ActivityDef
import org.cristalise.kernel.lifecycle.CompositeActivityDef
import org.cristalise.kernel.lookup.AgentPath
import org.cristalise.kernel.persistency.outcome.Outcome
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.process.resource.BuiltInResources
import org.cristalise.kernel.process.resource.DefaultResourceImportHandler
import org.cristalise.kernel.property.PropertyDescriptionList
import org.cristalise.kernel.test.utils.KernelXMLUtility
import org.cristalise.kernel.utils.Logger

import groovy.transform.CompileStatic

/**
 * Utility class to implement ALL methods required to manage (create/edit)
 * CRISTAL-iSE Resources and Items defined in the dev module:  https://github.com/cristal-ise/dev
 */
@CompileStatic
class DevItemUtility {

    AgentProxy agent = null

    public String elemActDefFactoryName   = "/domain/desc/dev/ElementaryActivityDefFactory"
    public String compActDefFactoryName   = "/domain/desc/dev/CompositeActivityDefFactory"
    public String schemaFactoryName       = "/domain/desc/dev/SchemaFactory"
    public String scriptFactoryName       = "/domain/desc/dev/ScriptFactory"
    public String queryFactoryName        = "/domain/desc/dev/QueryFactory"
    public String stateMachineFactoryName = "/domain/desc/dev/StateMachineFactory"
    public String descItemFactoryName     = "/domain/desc/dev/DescriptionFactory"
    public String moduleFactoryName       = "/domain/desc/dev/ModuleFactory"

    /**
     * 
     * @param item
     * @param agent
     * @param expectedJobs
     */
    public static void checkJobs(ItemProxy item, AgentPath agent, List<Map<String, Object>> expectedJobs) {
        def jobs = item.getJobList(agent)
        //println jobs

        assert jobs.size() == expectedJobs.size()

        expectedJobs.each { Map expectedJob ->
            assert expectedJob && expectedJob.stepName && /*expectedJob.agentRole != null &&*/ expectedJob.transitionName

            assert jobs.find { Job j ->
                j.stepName == expectedJob.stepName &&
                        //j.agentRole == expectedJob.agentRole &&
                        j.transition.name == expectedJob.transitionName
            }, "Cannot find Job: '${expectedJob.stepName}' , '${expectedJob.agentRole}' , '${expectedJob.transitionName}'"
        }
    }

    /**
     * 
     * @param item
     * @param expectedJobs
     */
    public void checkJobs(ItemProxy item, List<Map<String, Object>> expectedJobs) {
        checkJobs(item, agent.getPath(), expectedJobs)
    }

    /**
     *
     * @param proxy
     * @param actName
     * @return
     */
    public Job getDoneJob(ItemProxy proxy, String actName) {
        Logger.msg("DevItemUtility.getDoneJob() - proxy:$proxy.name actName:$actName")
        Job j = proxy.getJobByName(actName, agent)
        assert j && j.getStepName() == actName && j.transition.name == "Done"
        return j
    }

    /**
     *
     * @param proxy
     * @param actName
     * @param outcomeXML
     * @return
     */
    public Job executeDoneJob(ItemProxy proxy, String actName, String outcomeXML) {
        def job = getDoneJob(proxy, actName)
        job.setOutcome(outcomeXML)
        agent.execute(job)
        return job
    }

    /**
     *
     * @param proxy
     * @param actName
     * @param outcome
     */
    public Job executeDoneJob(ItemProxy proxy, String actName, Outcome outcome = null) {
        def job = getDoneJob(proxy, actName)

        if(outcome)               job.outcome = outcome
        else if(job.hasOutcome()) job.outcome = job.getOutcome() //this calls outcome initiator if defined

        agent.execute(job)
        return job
    }

    /**
     *
     * @param factoryPath
     * @param factoryActName
     * @param name
     * @param folder
     * @return
     */
    public void createNewItemByFactory(String factoryPath, String factoryActName, String name, String folder) {
        ItemProxy factory = agent.getItem(factoryPath)
        assert factory && factory.getName() == factoryPath.substring(factoryPath.lastIndexOf('/')+1)

        createNewItemByFactory(factory, factoryActName, name, folder)
    }

    /**
     *
     * @param factory
     * @param factoryActName
     * @param name
     * @param folder
     * @return
     */
    public void createNewItemByFactory(ItemProxy factory, String factoryActName, String name, String folder) {
        executeDoneJob(factory, factoryActName, DevXMLUtility.getNewDevObjectDefXML(name: name, folder: folder) )
    }

    /**
     *
     * @param type
     * @return
     */
    public String getFactoryPath(BuiltInResources type) {
        switch(type) {
            case ELEM_ACT_DESC_RESOURCE: return elemActDefFactoryName
            case COMP_ACT_DESC_RESOURCE: return compActDefFactoryName
            case SCHEMA_RESOURCE:        return schemaFactoryName
            case SCRIPT_RESOURCE:        return scriptFactoryName
            case QUERY_RESOURCE:         return queryFactoryName
            case STATE_MACHINE_RESOURCE: return stateMachineFactoryName

            default: return descItemFactoryName
        }
    }

    /**
     *
     * @param type type of the resource @see DefaultResourceImportHandler
     * @param factoryActName
     * @param name
     * @param folder
     * @return
     */
    public ItemProxy createNewDevItem(BuiltInResources type, String factoryActName, String name, String folder) {
        createNewItemByFactory(getFactoryPath(type), factoryActName, name, folder)

        if(type == null) {
            return agent.getItem("$folder/$name")
        }
        else {
            def resHandler = new DefaultResourceImportHandler(type)
            return agent.getItem("${resHandler.typeRoot}/$folder/$name")
        }
    }

    /**
     *
     * @param type
     * @param editActiName
     * @param newVersionActName
     * @param name
     * @param folder
     * @param xml
     * @return
     */
    public ItemProxy editDevItem(BuiltInResources type, String editActName, String newVersionActName, String name, String folder, String xml) {
        def resHandler = new DefaultResourceImportHandler(type)

        ItemProxy devItem = agent.getItem("${resHandler.typeRoot}/$folder/$name")
        assert devItem && devItem.getName() == name

        executeDoneJob(devItem, editActName, xml)
        executeDoneJob(devItem, newVersionActName)

        assert devItem.getViewpoint(resHandler.name, "0")

        return devItem
    }

    /**
     *
     * @param name
     * @param folder
     */
    public ItemProxy createNewElemActDesc(String name, String folder) {
        return createNewDevItem(ELEM_ACT_DESC_RESOURCE, "CreateNewElementaryActivityDef", name, folder)
    }

    /**
     *
     * @param name
     * @param folder
     */
    public ItemProxy createNewSchema(String name, String folder) {
        return createNewDevItem(SCHEMA_RESOURCE, "CreateNewSchema", name, folder)
    }

    /**
     *
     * @param name
     * @param folder
     */
    public ItemProxy createNewScript(String name, String folder) {
        return createNewDevItem(SCRIPT_RESOURCE, "CreateNewScript", name, folder)
    }

    /**
     *
     * @param name
     * @param folder
     */
    public ItemProxy createNewQuery(String name, String folder) {
        return createNewDevItem(QUERY_RESOURCE, "CreateNewQuery", name, folder)
    }

    /**
     *
     * @param name
     * @param folder
     */
    public ItemProxy createNewCompActDesc(String name, String folder) {
        return createNewDevItem(COMP_ACT_DESC_RESOURCE, "CreateNewCompositeActivityDef", name, folder)
    }

    /**
     *
     * @param name
     * @param folder
     * @param role
     * @param schemaName
     * @param schemaVersion
     */
    public void editElemActDesc(String name, String folder, String role, String schemaName, Integer schemaVersion) {
        def resHandler = new DefaultResourceImportHandler("EA")

        ItemProxy eaDescItem = agent.getItem("${resHandler.typeRoot}/$folder/$name")
        assert eaDescItem && eaDescItem.getName() == name

        executeDoneJob(eaDescItem, "EditDefinition", KernelXMLUtility.getActivityDefXML(Name: name, AgentRole: role))

        //it is possible there was no Schema specified for this Activity
        if(schemaName && !schemaName.startsWith("-")) {
            executeDoneJob(eaDescItem, "SetSchema", KernelXMLUtility.getDescObjectDetailsXML(id: schemaName, version: schemaVersion))
        }

        executeDoneJob(eaDescItem, "AssignNewActivityVersionFromLast")

        if(schemaName && !schemaName.startsWith("-")) {
            assert eaDescItem.getViewpoint(resHandler.name, "0")
            assert eaDescItem.getCollection(SCHEMA, (Integer)0).size() == 1
        }
    }

    /**
     *
     * @param name
     * @param folder
     * @param eaDef
     */
    public void editElemActDesc(String name, String folder, ActivityDef eaDef) {
        def resHandler = new DefaultResourceImportHandler("EA")

        ItemProxy eaDescItem = agent.getItem("${resHandler.typeRoot}/$folder/$name")
        assert eaDescItem && eaDescItem.getName() == name

        eaDef.setItemPath(eaDescItem.getPath())

        executeDoneJob(eaDescItem, "EditDefinition", Gateway.getMarshaller().marshall(eaDef) )

        if(eaDef.schema) {
            executeDoneJob(eaDescItem, "SetSchema", KernelXMLUtility.getDescObjectDetailsXML(id: eaDef.schema.name, version: eaDef.schema.version) )
        }

        if(eaDef.stateMachine) {
            executeDoneJob(eaDescItem, "OverrideStateMachine", KernelXMLUtility.getDescObjectDetailsXML(id: eaDef.stateMachine.name, version: eaDef.stateMachine.version) )
        }

        if(eaDef.script) {
            executeDoneJob(eaDescItem, "AssignScript", KernelXMLUtility.getDescObjectDetailsXML(id: eaDef.script.name, version: eaDef.script.version) )
        }

        if(eaDef.query) {
            executeDoneJob(eaDescItem, "AssignQuery", KernelXMLUtility.getDescObjectDetailsXML(id: eaDef.query.name, version: eaDef.query.version) )
        }

        executeDoneJob(eaDescItem, "AssignNewActivityVersionFromLast")

        assert eaDescItem.getViewpoint(resHandler.name, "0")

        if(eaDef.schema)       assert eaDescItem.getCollection(SCHEMA,        (Integer)0).size() == 1
        if(eaDef.script)       assert eaDescItem.getCollection(SCRIPT,        (Integer)0).size() == 1
        if(eaDef.query)        assert eaDescItem.getCollection(QUERY,         (Integer)0).size() == 1
        if(eaDef.stateMachine) assert eaDescItem.getCollection(STATE_MACHINE, (Integer)0).size() == 1
    }

    /**
     *
     * @param name
     * @param folder
     * @param xsd
     */
    public ItemProxy editSchema(String name, String folder, String xsd) {
        return editDevItem(SCHEMA_RESOURCE, "EditDefinition", "AssignNewSchemaVersionFromLast", name, folder, xsd)
    }

    /**
     *
     * @param name
     * @param folder
     * @param xsd
     */
    public ItemProxy editScript(String name, String folder, String scriptXML) {
        return editDevItem(SCRIPT_RESOURCE, "EditDefinition", "AssignNewScriptVersionFromLast", name, folder, scriptXML)
    }

    /**
     *
     * @param name
     * @param folder
     * @param xml
     */
    public ItemProxy editQuery(String name, String folder, String queryXML) {
        return editDevItem(QUERY_RESOURCE, "EditDefinition", "AssignNewQueryVersionFromLast", name, folder, queryXML)
    }

    /**
     *
     * @param name
     * @param folder
     * @param caXML
     * @param actCollSize
     */
    public ItemProxy editCompActDesc(String name, String folder, String caXML, int actCollSize = 0) {
        def caDescItem = editDevItem(COMP_ACT_DESC_RESOURCE, "EditDefinition", "AssignNewActivityVersionFromLast", name, folder, caXML)

        assert caDescItem.getCollection(ACTIVITY, (Integer)0).size()
        if(actCollSize) assert caDescItem.getCollection(ACTIVITY, (Integer)0).size() == actCollSize

        return caDescItem
    }

    /**
     *
     * @param name
     * @param folder
     * @param activityName
     * @param activityVersion
     */
    public ItemProxy editCompActDesc(String name, String folder, String activityName, Integer activityVersion) {
        String caXML = KernelXMLUtility.getCompositeActivityDefXML(Name: name, ActivityName: activityName, ActivityVersion: activityVersion)
        return editCompActDesc(name, folder, caXML)
    }

    /**
     *
     *
     * @param name
     * @param folder
     * @param caDef
     */
    public ItemProxy editCompActDesc(String name, String folder, CompositeActivityDef caDef) {
        String caXML = Gateway.getMarshaller().marshall(caDef)
        return editCompActDesc(name, folder, caXML)
    }

    /**
     *
     * @param name
     * @param folder
     * @return ItemProxy of newly created DescriptionItem
     */
    public ItemProxy createNewDescriptionItem(String name, String folder) {
        return createNewDevItem( null, "CreateNewDescription", name, folder)
    }

    /**
     *
     * @param name
     * @param folder
     * @param propDesc
     * @param chooseWorkflowXML
     * @return
     */
    public ItemProxy editDescriptionItem(String name, String folder, PropertyDescriptionList propDesc, String chooseWorkflowXML) {
        ItemProxy descriptionItem = agent.getItem("/$folder/$name")
        assert descriptionItem && descriptionItem.getName() == name
        return editDescriptionItem(descriptionItem, propDesc, chooseWorkflowXML)
    }

    /**
     *
     * @param descriptionItem
     * @param propDesc
     * @param chooseWorkflowXML
     * @return
     */
    public ItemProxy editDescriptionItem(ItemProxy descriptionItem, PropertyDescriptionList propDesc, String chooseWorkflowXML) {
        executeDoneJob(descriptionItem, "SetPropertyDescription", Gateway.getMarshaller().marshall(propDesc) )
        executeDoneJob(descriptionItem, "SetInstanceWorkflow",    chooseWorkflowXML)

        return descriptionItem
    }

    /**
     *
     * @param name
     * @param folder
     * @param devObjectDefXML
     * @return
     */
    public ItemProxy createItemFromDescription(String name, String folder, String devObjectDefXML) {
        ItemProxy descriptionItem = agent.getItem("/$folder/$name")
        assert descriptionItem && descriptionItem.getName() == name
        return createItemFromDescription(descriptionItem, devObjectDefXML)
    }

    /**
     *
     * @param descriptionItem
     * @param devObjectDefXML
     * @return
     */
    public ItemProxy createItemFromDescription(ItemProxy descriptionItem, String devObjectDefXML) {
        def doneJob = executeDoneJob(descriptionItem, "CreateNewInstance", devObjectDefXML )

        return agent.getItem(doneJob.getOutcome().getField("SubFolder") + "/" + doneJob.getOutcome().getField("ObjectName"))
    }

    /**
     *
     * @param descriptionItem
     * @param propDesc
     * @param chooseWorkflowXML
     * @param devObjectDefXML
     * @return
     */
    public ItemProxy editDescriptionAndCreateItem( ItemProxy descriptionItem, PropertyDescriptionList propDesc, String chooseWorkflowXML, String devObjectDefXML) {
        editDescriptionItem(descriptionItem, propDesc, chooseWorkflowXML)
        return createItemFromDescription(descriptionItem, devObjectDefXML)
    }
}
