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
package org.cristalise.dev.test.scaffold

import static org.cristalise.dev.scaffold.CRUDItemCreator.UpdateMode.ERASE

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.format.DateTimeFormatter

import org.cristalise.dev.dsl.DevItemDSL
import org.cristalise.dev.scaffold.CRUDItemCreator
import org.cristalise.kernel.collection.Dependency
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.utils.CristalTestSetup
import org.cristalise.kernel.test.utils.KernelXMLUtility
import org.cristalise.kernel.utils.CastorHashMap
import org.cristalise.kernel.utils.LocalObjectLoader
import org.json.JSONObject
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic @Slf4j
class DevScaffoldedModuleTests extends DevItemDSL implements CristalTestSetup {

    ItemProxy item
    CRUDItemCreator creator

    static Properties props = new Properties()
    static String timeStamp = null
    static String folder = "devtest"
    
    static boolean initialised

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

        log.info '======================================================================================'

        agent = Gateway.getAgentProxy('devtest')
        creator = new CRUDItemCreator(folder, ERASE, agent)
    }

    @AfterClass
    public static void teardown() {
        Gateway.close()
    }

    @Test
    void 'Check all Factories exists'() {
        agent.getItem('/desc/AgentDesc/devtest/devtest')

        agent.getItem('/desc/ItemDesc/devtest/TestAgentFactory')
        agent.getItem('/desc/ItemDesc/devtest/TestAgentUseConstructorFactory')
        agent.getItem('/desc/ItemDesc/devtest/TestItemFactory')
        agent.getItem('/desc/ItemDesc/devtest/TestItemGeneratedNameFactory')
        agent.getItem('/desc/ItemDesc/devtest/TestItemUseConstructorFactory')
        agent.getItem('/desc/ItemDesc/devtest/TestItemUseConstructorGeneratedNameFactory')
        agent.getItem('/devtest/TestAgentFactory')
        agent.getItem('/devtest/TestAgentUseConstructorFactory')
        agent.getItem('/devtest/TestItemFactory')
        agent.getItem('/devtest/TestItemGeneratedNameFactory')
        agent.getItem('/devtest/TestItemUseConstructorFactory')
        agent.getItem('/devtest/TestItemUseConstructorGeneratedNameFactory')
        agent.getItem('/devtest/CarFactory')
        agent.getItem('/devtest/ClubMemberFactory')
    }

    @Test
    public void 'Create Item using Constructor'() {
        item = creator.createItemWithConstructorAndCheck(
            Name: "ItemUsingConstructor-$timeStamp",
            Description: 'ItemUsingConstructor description',
            "/$folder/TestItemUseConstructorFactory")

        assert item.getMasterSchema()
        assert item.getAggregateScript()
    }

    @Test
    public void 'Create Item using Update'() {
        item = creator.createItemWithUpdateAndCheck(
            Name: "ItemUsingUpdate-$timeStamp",
            Description: 'ItemUsingUpdate description',
            "/$folder/TestItemFactory")

        assert item.getMasterSchema()
        assert item.getAggregateScript()

        assert item.getProperty('State') == 'ACTIVE'

        def updateJob     = item.getJobByName('Update', agent)
        def activateJob   = item.getJobByName('Activate', agent)
        def deactivateJob = item.getJobByName('Deactivate', agent)

        assert updateJob, "Cannot get Job for Activity 'Update' of Item '$item.name'"
        assert activateJob == null, "Job must be null for Activity 'Activate' of Item '$item.name'"
        assert deactivateJob, "Cannot get Job for Activity 'Deactivate' of Item '$item.name'"

        agent.execute(deactivateJob)

        assert item.getProperty('State') == 'INACTIVE'

        updateJob     = item.getJobByName('Update', agent)
        activateJob   = item.getJobByName('Activate', agent)
        deactivateJob = item.getJobByName('Deactivate', agent)

        assert updateJob, "Cannot get Job for Activity 'Update' of Item '$item.name'"
        assert activateJob, "Cannot get Job for Activity 'Activate' of Item '$item.name'"
        assert deactivateJob == null, "Job must be null for Activity 'Deactivate' of Item '$item.name'"
    }

    @Test
    public void 'Create Item using Update - generated from excel'() {
        item = creator.createItemWithUpdateAndCheck(
            Name: "ItemExcelUsingUpdate-$timeStamp",
            Description: 'ItemUsingUpdate description - generated from excel',
            DateOfBirth: '1969-02-23',
            Age: '51',
            "/$folder/TestItemExcelFactory")

        assert item.getMasterSchema()
        assert item.getAggregateScript()

        assert item.getProperty('State') == 'ACTIVE'

        def updateJob     = item.getJobByName('Update', agent)
        def activateJob   = item.getJobByName('Activate', agent)
        def deactivateJob = item.getJobByName('Deactivate', agent)

        assert updateJob, "Cannot get Job for Activity 'Update' of Item '$item.name'"
        assert activateJob == null, "Job must be null for Activity 'Activate' of Item '$item.name'"
        assert deactivateJob, "Cannot get Job for Activity 'Deactivate' of Item '$item.name'"

        agent.execute(deactivateJob)

        assert item.getProperty('State') == 'INACTIVE'

        updateJob     = item.getJobByName('Update', agent)
        activateJob   = item.getJobByName('Activate', agent)
        deactivateJob = item.getJobByName('Deactivate', agent)

        assert updateJob, "Cannot get Job for Activity 'Update' of Item '$item.name'"
        assert activateJob, "Cannot get Job for Activity 'Activate' of Item '$item.name'"
        assert deactivateJob == null, "Job must be null for Activity 'Deactivate' of Item '$item.name'"
    }

    @Test
    public void 'Create Item using Update and Generated Name'() {
        item = creator.createItemWithUpdateAndCheck(
            Description: 'ItemUsingUpdateGenretedName description',
            "/$folder/TestItemGeneratedNameFactory")

        assert item.getMasterSchema()
        assert item.getAggregateScript()
    }

    @Test
    public void 'Create Agent using Constructor'() {
        item = creator.createItemWithConstructorAndCheck(
            Name: "AgentUsingConstructor-$timeStamp",
            Description: 'AgentUsingConstructor description',
            "/$folder/TestAgentUseConstructorFactory")

        assert item.getMasterSchema()
        assert item.getAggregateScript()
    }

    @Test
    public void 'Create Agent using Update'() {
        item = creator.createItemWithUpdateAndCheck(
            Name: "AgentUsingUpdate-$timeStamp",
            Description: 'AgentUsingUpdate description',
            "/$folder/TestAgentFactory")

        assert item.getMasterSchema()
        assert item.getAggregateScript()
    }

    @Test
    public void 'Create Items from CSV'() {
        def csv = new File('src/test/data/TestItemExcel.csv')
        creator.createItems(csv, 'TestItemExcel')

        agent.getItem("/$folder/TestItemExcels/TestItemExcel1")
        agent.getItem("/$folder/TestItemExcels/TestItemExcel2")
    }

    @Test
    public void 'Execute update script generated from Expression'() {
        def script = LocalObjectLoader.getScript('TestItemExcel_DetailsAgeUpdateExpression', 0)

        def inputs = new CastorHashMap()
        def json = new JSONObject()
        def dateOfBirth = '1969-02-23'
        def age = Period.between(LocalDate.parse(dateOfBirth, DateTimeFormatter.ofPattern('yyyy-MM-dd')), LocalDate.now()).getYears()

        json.put('DateOfBirth', dateOfBirth)
        inputs.put("TestItemExcel_Details", json, false)
        def result = (Map)script.evaluate(inputs)
        def expected = "<TestItemExcel_Details><Age>$age</Age></TestItemExcel_Details>"
        assert KernelXMLUtility.compareXML(expected, (String)result.TestItemExcel_DetailsXml)

        json.put('DateOfDeath', '1971-02-23')
        result = (Map)script.evaluate(inputs)
        expected = '<TestItemExcel_Details><Age>2</Age></TestItemExcel_Details>'
        assert KernelXMLUtility.compareXML(expected, (String)result.TestItemExcel_DetailsXml)
    }

    @Test
    public void 'Create Car, Motorcycle and add them to ClubMember'() {
        def car = creator.createItemWithUpdateAndCheck(
            Name: "Car-$timeStamp",
            RegistrationPlate: 'IG 94-11',
            "/$folder/CarFactory")

        def motorcycle = creator.createItemWithUpdateAndCheck(
            Name: "Motorcycle-$timeStamp",
            RegistrationPlate: 'JTG 345',
            "/$folder/MotorcycleFactory")

        def clubMember = creator.createItemWithUpdateAndCheck(
            Name: "ClubMember-$timeStamp",
            Email: 'mate@people.hu',
            "/$folder/ClubMemberFactory")

        assert clubMember.getCollection('Cars').members.list.size() == 0
        clubMember.getCollection('Motorcycles').members.list.size() == 0
        car.getCollection('ClubMember').members.list.size() == 0
        motorcycle.getCollection('ClubMember').members.list.size() == 0

        //Add to Cars
        def addToCarsJob = clubMember.getJobByName('AddToCars', agent)
        assert addToCarsJob, "Cannot get Job $addToCarsJob of Item '$clubMember.name'"

        addToCarsJob.getOutcome().setField("MemberName", "Car-$timeStamp",)
        agent.execute(addToCarsJob)

        def clubMemberCars = (Dependency)clubMember.getCollection('Cars')
        def carClubMember  = (Dependency)car.getCollection('ClubMember')

        assert clubMemberCars.members.list.size() == 1
        assert clubMemberCars.getMember(0).itemPath == car.path

        assert carClubMember.members.list.size() == 1
        assert carClubMember.getMember(0).itemPath == clubMember.path

        //Add to Motorcycles
        def addToMotorcyclesJob = clubMember.getJobByName('AddToMotorcycles', agent)
        assert addToMotorcyclesJob, "Cannot get Job $addToCarsJob of Item '$clubMember'"

        addToMotorcyclesJob.getOutcome().setField("MemberName", "Motorcycle-$timeStamp",)
        agent.execute(addToMotorcyclesJob)

        def clubMemberMotorcycles = (Dependency)clubMember.getCollection('Motorcycles')
        def motorcycleClubMember  = (Dependency)motorcycle.getCollection('ClubMember')

        assert clubMemberMotorcycles.members.list.size() == 1
        assert clubMemberMotorcycles.getMember(0).itemPath == motorcycle.path

        assert motorcycleClubMember.members.list.size() == 1
        assert motorcycleClubMember.getMember(0).itemPath == clubMember.path
    }
}
