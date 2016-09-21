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

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.*
import static org.cristalise.kernel.collection.BuiltInCollections.*

import groovy.transform.CompileStatic

import org.cristalise.kernel.entity.agent.Job
import org.cristalise.kernel.entity.proxy.AgentProxy
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.ActivityDef
import org.cristalise.kernel.lifecycle.CompositeActivityDef
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.process.resource.DefaultResourceImportHandler
import org.cristalise.kernel.property.PropertyDescriptionList
import org.cristalise.kernel.test.utils.KernelXMLUtility
import org.cristalise.kernel.utils.Logger;


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
    public String stateMachineFactoryName = "/domain/desc/dev/ScriptFactory"
    public String descItemFactoryName     = "/domain/desc/dev/DescriptionFactory"

    /**
     * 
     * @param proxy
     * @param actName
     * @return
     */
    public Job getDoneJob(ItemProxy proxy, String actName) {
        Logger.msg "DevItemUtility.getDoneJob() - proxy:"+proxy.name
        Job j = proxy.getJobByName(actName, agent)
        assert j && j.getStepName() == actName && j.transition.name == "Done"
        return j
    }
    
    /**
     * 
     * @param factoryPath
     * @param factoryActName
     * @param name
     * @param folder
     * @return
     */
    public ItemProxy createNewItemByFactory(String factoryPath, String factoryActName, String name, String folder) {
        ItemProxy factory = agent.getItem(factoryPath)
        assert factory && factory.getName() == factoryPath.substring(factoryPath.lastIndexOf('/')+1)

        Job doneJob = getDoneJob(factory, factoryActName)
        doneJob.setOutcome( DevXMLUtility.getNewDevObjectDefXML(name: name, folder: folder) )

        agent.execute(doneJob)
        return factory
    }
    
    /**
     * 
     * @param type
     * @return
     */
    public String getFactoryPath(String type) {
        switch(type) {
            case "EA": return elemActDefFactoryName
            case "CA": return compActDefFactoryName
            case "OD": return schemaFactoryName
            case "SC": return scriptFactoryName
            case "SM": return stateMachineFactoryName
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
    public ItemProxy createNewDevItem(String type, String factoryActName, String name, String folder) {
        createNewItemByFactory(getFactoryPath(type), factoryActName, name, folder)

        if(type == "DescItem") {
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
    public ItemProxy editDevItem(String type, String editActName, String newVersionActName, String name, String folder, String xml) {
        def resHandler = new DefaultResourceImportHandler(type)

        ItemProxy devItem = agent.getItem("${resHandler.typeRoot}/$folder/$name")
        assert devItem && devItem.getName() == name

        Job doneJob = getDoneJob(devItem, editActName)
        doneJob.setOutcome( xml )
        agent.execute(doneJob)

        doneJob = getDoneJob(devItem, newVersionActName)
        agent.execute(doneJob)

        assert devItem.getViewpoint(resHandler.name, "0")

        return devItem
    }

    /**
     * 
     * @param name
     * @param folder
     */
    public ItemProxy createNewElemActDesc(String name, String folder) {
        return createNewDevItem( 'EA', "CreateNewElementaryActivityDef", name, folder)
    }

    /**
     * 
     * @param name
     * @param folder
     */
    public ItemProxy createNewSchema(String name, String folder) {
        return createNewDevItem( 'OD', "CreateNewSchema", name, folder)
    }

    /**
     * 
     * @param name
     * @param folder
     */
    public ItemProxy createNewScript(String name, String folder) {
        return createNewDevItem( 'SC', "CreateNewScript", name, folder)
    }

    /**
     * 
     * @param name
     * @param folder
     */
    public ItemProxy createNewCompActDesc(String name, String folder) {
        return createNewDevItem( 'CA', "CreateNewCompositeActivityDef", name, folder)
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
        if(schemaName && !schemaName.startsWith("-")) {
            doneJob = getDoneJob(eaDescItem, "SetSchema")
            doneJob.setOutcome( KernelXMLUtility.getDescObjectDetailsXML(id: schemaName, version: schemaVersion) )
            agent.execute(doneJob)
        }

        doneJob = getDoneJob(eaDescItem, "AssignNewActivityVersionFromLast")
        agent.execute(doneJob)

        if(schemaName && !schemaName.startsWith("-")) {
            assert eaDescItem.getViewpoint(resHandler.name, "0")
            assert eaDescItem.getCollection(SCHEMA, 0).size() == 1
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

        Job doneJob = getDoneJob(eaDescItem, "EditDefinition")
        doneJob.setOutcome( Gateway.getMarshaller().marshall(eaDef) )
        agent.execute(doneJob)

        if(eaDef.schema) {
            doneJob = getDoneJob(eaDescItem, "SetSchema")
            doneJob.setOutcome( KernelXMLUtility.getDescObjectDetailsXML(id: eaDef.schema.name, version: eaDef.schema.version) )
            agent.execute(doneJob)
        }

        if(eaDef.stateMachine) {
            doneJob = getDoneJob(eaDescItem, "OverrideStateMachine")
            doneJob.setOutcome( KernelXMLUtility.getDescObjectDetailsXML(id: eaDef.stateMachine.name, version: eaDef.stateMachine.version) )
            agent.execute(doneJob)
        }

        if(eaDef.script) {
            doneJob = getDoneJob(eaDescItem, "AssignScript")
            doneJob.setOutcome( KernelXMLUtility.getDescObjectDetailsXML(id: eaDef.script.name, version: eaDef.script.version) )
            agent.execute(doneJob)
        }

        doneJob = getDoneJob(eaDescItem, "AssignNewActivityVersionFromLast")
        agent.execute(doneJob)

        assert eaDescItem.getViewpoint(resHandler.name, "0")

        if(eaDef.schema)       assert eaDescItem.getCollection(SCHEMA,        0).size() == 1
        if(eaDef.script)       assert eaDescItem.getCollection(SCRIPT,        0).size() == 1
        if(eaDef.stateMachine) assert eaDescItem.getCollection(STATE_MACHINE, 0).size() == 1
    }

    /**
     * 
     * @param name
     * @param folder
     * @param xsd
     */
    public ItemProxy editSchema(String name, String folder, String xsd) {
        return editDevItem("OD", "EditDefinition", "AssignNewSchemaVersionFromLast", name, folder, xsd)
    }

    /**
     * 
     * @param name
     * @param folder
     * @param xsd
     */
    public ItemProxy editScript(String name, String folder, String scriptXML) {
        return editDevItem("SC", "EditDefinition", "AssignNewScriptVersionFromLast", name, folder, scriptXML)
    }

    /**
     * 
     * @param name
     * @param folder
     * @param caXML
     * @param actCollSize
     */
    public ItemProxy editCompActDesc(String name, String folder, String caXML, int actCollSize = 0) {
        def caDescItem = editDevItem( "CA", "EditDefinition", "AssignNewActivityVersionFromLast", name, folder, caXML)

        assert caDescItem.getCollection(ACTIVITY, 0).size()
        if(actCollSize) assert caDescItem.getCollection(ACTIVITY, 0).size() == actCollSize
        
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
        return createNewDevItem( 'DescItem', "CreateNewDescription", name, folder)
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
        return editDescriptionItem(descriptionItem, propDesc, chooseWorkflowXML);
    }

    /**
     * 
     * @param descriptionItem
     * @param propDesc
     * @param chooseWorkflowXML
     * @return
     */
    public ItemProxy editDescriptionItem(ItemProxy descriptionItem, PropertyDescriptionList propDesc, String chooseWorkflowXML) {
        Job doneJob = getDoneJob(descriptionItem, "SetPropertyDescription")
        doneJob.setOutcome( Gateway.getMarshaller().marshall(propDesc) )
        agent.execute(doneJob)

        doneJob = getDoneJob(descriptionItem, "SetInstanceWorkflow")
        doneJob.setOutcome(chooseWorkflowXML)
        agent.execute(doneJob)

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
        Job doneJob = getDoneJob(descriptionItem, "CreateNewInstance")
        doneJob.setOutcome( devObjectDefXML )
        agent.execute(doneJob)

        String instancePath = doneJob.getOutcome().getField("SubFolder") + "/" + doneJob.getOutcome().getField("ObjectName")

        return agent.getItem(instancePath)
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
