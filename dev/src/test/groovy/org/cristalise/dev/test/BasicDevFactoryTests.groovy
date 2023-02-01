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
package org.cristalise.dev.test;

import static org.assertj.core.api.Assertions.assertThat
import static org.cristalise.dev.scaffold.CRUDItemCreator.UpdateMode.ERASE
import static org.cristalise.kernel.collection.BuiltInCollections.*
import static org.cristalise.kernel.process.resource.BuiltInResources.*
import java.time.LocalDateTime
import org.cristalise.dev.scaffold.DevItemCreator
import org.cristalise.kernel.entity.DomainContext
import org.cristalise.kernel.entity.imports.ImportAgent
import org.cristalise.kernel.entity.imports.ImportItem
import org.cristalise.kernel.entity.imports.ImportRole
import org.cristalise.kernel.entity.proxy.AgentProxy
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.ActivityDef
import org.cristalise.kernel.lifecycle.CompositeActivityDef
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine
import org.cristalise.kernel.persistency.outcome.Schema
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.property.PropertyDescriptionList
import org.cristalise.kernel.querying.Query
import org.cristalise.kernel.scripting.Script
import org.cristalise.kernel.test.utils.CristalTestSetup
import org.cristalise.kernel.test.utils.KernelXMLUtility
import org.cristalise.kernel.utils.DescriptionObject
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j


@CompileStatic @Slf4j
class BasicDevFactoryTests implements CristalTestSetup {

//    ItemProxy item

    static Properties props = new Properties()
    static String timeStamp = null
    static String folder = "devtest"

    static boolean initialised

    DevItemCreator creator
    AgentProxy agent

    @BeforeClass
    public static void setup() {
        initialised = false
        props.put('Resource.moduleUseFileNameWithVersion', 'dev,devtest')
        timeStamp = LocalDateTime.now().format("yyyy-MM-dd_HH-mm-ss_SSS")
    }

    @Before
    public void init() {
        //cristal in memory server has to be initialised only once
        if (!initialised) {
            inMemoryServer(props) //it is not static therefore cannot be called from @BeforeClass
            initialised = true
        }

        log.info '++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++'

        agent = Gateway.getAgentProxy('devtest')
        creator = new DevItemCreator(folder, ERASE, agent)
    }

    @AfterClass
    public static void teardown() {
        Gateway.close()
    }

    public static final String testDataRoot = "src/test/data";

    private void assertViewpoint(ItemProxy item, DescriptionObject descOrig, String view) {
        def schemaName = descOrig.resourceType.getSchemaName()
        def descPrimeOutcome = item.getViewpoint(schemaName, view).outcome

        if ( ! [SCHEMA_RESOURCE, SCRIPT_RESOURCE, QUERY_RESOURCE].contains(descOrig.resourceType)) {
            def descPrime = agent.unmarshall(descPrimeOutcome.getData())

            if (descOrig.resourceType != COMP_ACT_DESC_RESOURCE) {
                assertThat(descPrime).isEqualToComparingFieldByFieldRecursively(descOrig);
            }
        }

        //LocalObjectLoader does not work with current in-memory persistency
//        def descPrime = descOrig.resourceType.loadDescriptionObject(schemaName, 0)
//        assertThat(descOrig).isEqualToComparingFieldByFieldRecursively(descPrime);
    }

    private ItemProxy assertCreateAndUpdateDevItem(DescriptionObject descObj) {
        def item = creator.createItemWithUpdateAndAssignNewVersion(descObj)

        assertViewpoint(item, descObj, 'last')
        assertViewpoint(item, descObj, '0')

        assert item.getMasterSchema(descObj.resourceType.getSchemaName(), 0)
        //assert item.getAggregateScript()
        
        return item
    }

    @Test
    public void createAndEditDomainContext() {
        def dc = new DomainContext('/devtest/kovax')
        assertCreateAndUpdateDevItem(dc)
    }

    @Test
    public void createAndEditElemActDesc() {
        def actDef = new ActivityDef("TestEADesc-$timeStamp", null)
        assertCreateAndUpdateDevItem(actDef)
	}

    @Test
    public void createAndEditSchema() {
        def schema = new Schema("PatientDetails-$timeStamp", 0, new File("$testDataRoot/PatientDetails.xsd").text)
        assertCreateAndUpdateDevItem(schema)
    }

    @Test
    public void createAndEditCompActDesc() {
        def name = "TestCADesc-$timeStamp"
        def activityName = 'CreateNewItem' //must be an existing ElementaryActivity
        def caXML = KernelXMLUtility.getCompositeActivityDefXML(Name: name, ActivityName: activityName, ActivityVersion: 0)

        def caDef = (CompositeActivityDef)agent.unmarshall(caXML)
        def item = assertCreateAndUpdateDevItem(caDef)

        assert item.getCollection(ACTIVITY, (Integer)0).size() == 1

    }

    @Test
    public void createAndEditScript() {
        def script = new Script("Script-$timeStamp", 0, null, new File("$testDataRoot/TestScript.xml").text)
        assertCreateAndUpdateDevItem(script)
    }

    @Test
    public void createAndEditQuery() {
        def query = new Query("Query-$timeStamp", 0, null, new File("$testDataRoot/TestQuery.xml").text)
        assertCreateAndUpdateDevItem(query)
    }

    @Test
    public void createAndEditStateMachine() {
        def smXML = new File("$testDataRoot/TestStateMachine.xml").text
        def sm = (StateMachine)agent.unmarshall(smXML)
        sm.name = "StateMachine-$timeStamp"
        assertCreateAndUpdateDevItem(sm)
    }

    @Test
    public void createAndEditPropertyDescription() {
        def pdlXML = new File("$testDataRoot/TestPropertyDescription.xml").text
        def pdl = (PropertyDescriptionList)agent.unmarshall(pdlXML)
        pdl.name = "PropertyDescription-$timeStamp"
        assertCreateAndUpdateDevItem(pdl)
    }

    @Test
    public void createAndEditAgentDesc() {
        def agentXML = new File("$testDataRoot/TestAgentDesc.xml").text
        def agentObj = (ImportAgent)agent.unmarshall(agentXML)
        agentObj.name = "AgentDesc-$timeStamp"
        assertCreateAndUpdateDevItem(agentObj)
    }

    @Test
    public void createAndEditItemDesc() {
        def itemXML = new File("$testDataRoot/TestItemDesc.xml").text
        def itemObj = (ImportItem)agent.unmarshall(itemXML)
        itemObj.name = "ItemDesc-$timeStamp"
        assertCreateAndUpdateDevItem(itemObj)
    }

    @Test
    public void createAndEditRoleDesc() {
        def roleXML = new File("$testDataRoot/TestRoleDesc.xml").text
        def roleObj = (ImportRole)agent.unmarshall(roleXML)
        roleObj.name = "RoleDesc-$timeStamp"
        assertCreateAndUpdateDevItem(roleObj)
    }

    @Test @Ignore('Test Unimplemented')
    public void createAndEditModule() {
    }
}
