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

package org.cristalise.dev.test.utils

import groovy.transform.CompileStatic

import org.cristalise.dsl.persistency.outcome.SchemaBuilder
import org.cristalise.kernel.collection.BuiltInCollections
import org.cristalise.kernel.entity.agent.Job
import org.cristalise.kernel.entity.proxy.AgentProxy
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.process.resource.DefaultResourceImportHandler
import org.cristalise.kernel.property.PropertyDescriptionList
import org.cristalise.kernel.test.utils.KernelXMLUtility

/**
 * 
 */
@CompileStatic
class DevItemUtility {

    AgentProxy agent = null

    /**
     *
     * @param eaFactory
     * @param actName
     * @return
     */
    public Job getDoneJob(ItemProxy proxy, String actName) {
        Job j = proxy.getJobByName(actName, agent)
        assert j && j.getStepName() == actName && j.transition.name == "Done"
        return j
    }

    /**
     * 
     * @param name
     * @param folder
     */
    public void createNewElemActDesc(String name, String folder) {
        ItemProxy eaDescFactory = agent.getItem("/domain/desc/dev/ElementaryActivityDefFactory")
        assert eaDescFactory && eaDescFactory.getName() == "ElementaryActivityDefFactory"

        Job doneJob = getDoneJob(eaDescFactory, "CreateNewElementaryActivityDef")
        doneJob.setOutcome( DevXMLUtility.getNewDevObjectDefXML(name: name, folder: folder) )

        agent.execute(doneJob)
    }

    /**
     * 
     * @param name
     * @param folder
     */
    public void createNewSchema(String name, String folder) {
        ItemProxy schemaFactory = agent.getItem("/domain/desc/dev/SchemaFactory")
        assert schemaFactory && schemaFactory.getName() == "SchemaFactory"

        Job doneJob = getDoneJob(schemaFactory, "CreateNewSchema")
        doneJob.setOutcome( DevXMLUtility.getNewDevObjectDefXML(name: name, folder: folder) )

        agent.execute(doneJob)
    }

    /**
     * 
     * @param name
     * @param folder
     */
    public void createNewScript(String name, String folder) {
        ItemProxy schemaFactory = agent.getItem("/domain/desc/dev/ScriptFactory")
        assert schemaFactory && schemaFactory.getName() == "ScriptFactory"

        Job doneJob = getDoneJob(schemaFactory, "CreateNewScript")
        doneJob.setOutcome( DevXMLUtility.getNewDevObjectDefXML(name: name, folder: folder) )

        agent.execute(doneJob)
    }

    /**
     * 
     * @param name
     * @param folder
     */
    public void createNewCompActDesc(String name, String folder) {
        ItemProxy caDescFactory = agent.getItem("/domain/desc/dev/CompositeActivityDefFactory")
        assert caDescFactory && caDescFactory.getName() == "CompositeActivityDefFactory"

        Job doneJob = getDoneJob(caDescFactory, "CreateNewCompositeActivityDef")
        doneJob.setOutcome( DevXMLUtility.getNewDevObjectDefXML(name: name, folder: folder) )

        agent.execute(doneJob)
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

        Job doneJob = getDoneJob(eaDescItem, "EditDefinition")
        doneJob.setOutcome( KernelXMLUtility.getActivityDefXML(Name: name, AgentRole: role) )
        agent.execute(doneJob)

        //it is possible there was no Schema specified for this Activity
        if(schemaName != null && !schemaName.startsWith("-")) {
            doneJob = getDoneJob(eaDescItem, "SetSchema")
            doneJob.setOutcome( KernelXMLUtility.getDescObjectDetailsXML(id: schemaName, version: schemaVersion) )
            agent.execute(doneJob)
        }

        doneJob = getDoneJob(eaDescItem, "AssignNewActivityVersionFromLast")
        agent.execute(doneJob)

        if(schemaName != null && !schemaName.startsWith("-")) {
            assert eaDescItem.getViewpoint(resHandler.name, "0")
            assert eaDescItem.getCollection(BuiltInCollections.SCHEMA, 0).size() == 1
        }
    }

    /**
     * 
     * @param name
     * @param folder
     * @param xsd
     */
    public void editSchema(String name, String folder, String xsd) {
        def resHandler = new DefaultResourceImportHandler("OD")

        ItemProxy schemaItem = agent.getItem("${resHandler.typeRoot}/$folder/$name")
        assert schemaItem && schemaItem.getName() == name

        Job doneJob = getDoneJob(schemaItem, "EditDefinition")
        doneJob.setOutcome( xsd )
        agent.execute(doneJob)

        doneJob = getDoneJob(schemaItem, "AssignNewSchemaVersionFromLast")
        agent.execute(doneJob)

        assert schemaItem.getViewpoint(resHandler.name, "0")
    }

    /**
     * 
     * @param name
     * @param folder
     * @param xsd
     */
    public void editScript(String name, String folder, String scriptXML) {
        def resHandler = new DefaultResourceImportHandler("SC")

        ItemProxy schemaItem = agent.getItem("${resHandler.typeRoot}/$folder/$name")
        assert schemaItem && schemaItem.getName() == name

        Job doneJob = getDoneJob(schemaItem, "EditDefinition")
        doneJob.setOutcome( scriptXML )
        agent.execute(doneJob)

        doneJob = getDoneJob(schemaItem, "AssignNewScriptVersionFromLast")
        agent.execute(doneJob)

        assert schemaItem.getViewpoint(resHandler.name, "0")
    }

    /**
     * 
     * @param name
     * @param folder
     * @param activityName
     * @param activityVersion
     */
    public void editCompActDesc(String name, String folder, String activityName, Integer activityVersion) {
        def resHandler = new DefaultResourceImportHandler("CA")

        ItemProxy caDescItem = agent.getItem("${resHandler.typeRoot}/$folder/$name")
        assert caDescItem && caDescItem.getName() == name

        Job doneJob = getDoneJob(caDescItem, "EditDefinition")
        doneJob.setOutcome( KernelXMLUtility.getCompositeActivityDefXML(Name: name, ActivityName: activityName, ActivityVersion: activityVersion) )
        agent.execute(doneJob)

        doneJob = getDoneJob(caDescItem, "AssignNewActivityVersionFromLast")
        agent.execute(doneJob)

        assert caDescItem.getViewpoint(resHandler.name, "0")
        assert caDescItem.getCollection(BuiltInCollections.ACTIVITY, 0).size() == 1
    }

    /**
     * 
     * @param name
     * @param folder
     * @param caXML
     */
    public void editCompActDesc(String name, String folder, String caXML) {
        def resHandler = new DefaultResourceImportHandler("CA")

        ItemProxy caDescItem = agent.getItem("${resHandler.typeRoot}/$folder/$name")
        assert caDescItem && caDescItem.getName() == name

        Job doneJob = getDoneJob(caDescItem, "EditDefinition")
        doneJob.setOutcome(caXML)
        agent.execute(doneJob)

        doneJob = getDoneJob(caDescItem, "AssignNewActivityVersionFromLast")
        agent.execute(doneJob)

        assert caDescItem.getViewpoint(resHandler.name, "0")
        assert caDescItem.getCollection(BuiltInCollections.ACTIVITY, 0).size()
    }

    /**
     * 
     * @param name
     * @param folder
     */
    public ItemProxy createNewDescriptionItem(String name, String folder) {
        ItemProxy descFactory = agent.getItem("/domain/desc/dev/DescriptionFactory")
        assert descFactory && descFactory.getName() == "DescriptionFactory"

        Job doneJob = getDoneJob(descFactory, "CreateNewDescription")
        doneJob.setOutcome( DevXMLUtility.getNewDevObjectDefXML(name: name, folder: folder) )

        agent.execute(doneJob)

        return descFactory
    }

    public ItemProxy editDescriptionAndCreateItem(String name, String folder, PropertyDescriptionList propDesc, String setWorkflowXML, String devObjectDefXML) {
        ItemProxy descriptionItem = agent.getItem("/$folder/$name")
        assert descriptionItem && descriptionItem.getName() == name

        Job doneJob = getDoneJob(descriptionItem, "SetPropertyDescription")
        doneJob.setOutcome( Gateway.getMarshaller().marshall(propDesc) )
        agent.execute(doneJob)

        doneJob = getDoneJob(descriptionItem, "SetInstanceWorkflow")
        doneJob.setOutcome( setWorkflowXML )
        agent.execute(doneJob)

        doneJob = getDoneJob(descriptionItem, "CreateNewInstance")
        doneJob.setOutcome( devObjectDefXML )
        agent.execute(doneJob)
        
        String instanceName = doneJob.getOutcome().getField("SubFolder") + "/" + doneJob.getOutcome().getField("ObjectName")

        return agent.getItem(instanceName)
    }

    public void ElementaryActivityDef(String name, String folder, Closure cl) {
        createNewElemActDesc(name, folder)
        editElemActDesc(name, folder, 'role', 'schema', 0)
    }

    public void Schema(String name, String folder, Closure cl) {
        createNewSchema(name, folder)
        editSchema(name, folder, SchemaBuilder.build(name, 0, cl).XSD)
    }
}
