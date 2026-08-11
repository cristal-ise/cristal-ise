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

package org.cristalise.dev.test.scenario

import static org.cristalise.dev.dsl.DevXMLUtility.recordToXML

import org.cristalise.dsl.persistency.outcome.OutcomeBuilder
import org.cristalise.kernel.entity.Job
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.test.KernelScenarioTestBase
import org.junit.jupiter.api.Test

class XPathOutcomeInitTestIT extends KernelScenarioTestBase {
    
    public static final String FactorytPath = "/domain/desc/integTest/XPathOutcomeInitTest_DetailsFactory"
    public static final String FactoryActName = "XPathOutcomeInitTest_CreateDescription"

    private ItemProxy createNewXPathOutcomeInitTest_Details(String name, String folder) {
        executeDoneJob(
            agent.getItem(FactorytPath),
            FactoryActName,
            recordToXML('NewDevObjectDef', [ObjectName: name, SubFolder: folder])
        )

        ItemProxy devItem = agent.getItem("$folder/$name")
        assert devItem && devItem.getName() == name
        return devItem
    }

    def XPathOutcomeInitTest_Details(String name, String folder, Closure cl) {
        ItemProxy devItem = createNewXPathOutcomeInitTest_Details(name, folder)

        Job doneJob = getDoneJob(devItem, "XPathOutcomeInitTest_SetDetails")
        doneJob.setOutcome( OutcomeBuilder.build(cl) )
        agent.execute(doneJob)

        doneJob = getDoneJob(devItem, "AssignNewVersionFromLast")
        agent.execute(doneJob)

        assert devItem.getViewpoint('XPathOutcomeInitTest_Details', "0")
    }

    //NOT USED YET
    def createItems_XPathOutcomeInitTest_Details() {
         XPathOutcomeInitTest_Details("XPathOutcomeInitTest_Second-$timeStamp", folder) {
             Fields(slotID: caSlotIDs[1]) {
                 Field {
                     FieldName("/TwoFieldSchema/stringField-0")
                     FieldValue("activity//./First:/OneFieldSchema/stringField-0")
                 }
             }
         }
 
         XPathOutcomeInitTest_Details("XPathOutcomeInitTest_Third-$timeStamp", folder) {
             Fields(slotID: caSlotIDs[2]) {
                 Field {
                     FieldName("/ThreeFieldSchema/stringField-0")
                     FieldValue("activity//./First:/OneFieldSchema/stringField-0")
                 }
                 Field {
                     FieldName("/ThreeFieldSchema/stringField-1")
                     FieldValue("activity//./Second:/TwoFieldSchema/stringField-1")
                 }
             }
         }
    }

    def setFieldAndExecuteJob(ItemProxy proxy, String actName, String xpath) {
        def job = getDoneJob(proxy, actName)
        def outcome = job.getOutcome()
        outcome.setFieldByXPath(xpath, actName)
        agent.execute(job)
    }

    @Test
    public void execute() {
        def elemActNames = [First: 'OneFieldSchema', Second: 'TwoFieldSchema', Third: 'ThreeFieldSchema']
        def actDefs = []
        def xpathInitDefs = []

        elemActNames.eachWithIndex { actName, schemaName, index ->

            def schema = Schema(schemaName+"-$timeStamp", folder) {
                struct(name: schemaName) {
                    for (int i in 0..index) {
                        field(name:"stringField-$i")
                    }
                }
            }

            actDefs[index] = ElementaryActivityDef(actName+"-$timeStamp", folder) {
                Role('Admin')
                Property(OutcomeInit: "XPath")
                if (index == 1) {
                    Property('/TwoFieldSchema/stringField-0': 'activity//./First:/OneFieldSchema/stringField-0')
                }
                else if (index == 2) {
                    Property('/ThreeFieldSchema/stringField-0': 'activity//./First:/OneFieldSchema/stringField-0')
                    Property('/ThreeFieldSchema/stringField-1': 'activity//./Second:/TwoFieldSchema/stringField-1')
                }
                Schema(schema)
            }
        }

        //compActDefFactoryName = "/domain/desc/integTest/XPathOutcomeInitTest_CADefFactory"

        def wf = CompositeActivityDef("XPathOutcomeInitTestWF-$timeStamp", folder) {
            Layout {
                ElemActDef('First',  actDefs[0])
                ElemActDef('Second', actDefs[1])
                ElemActDef('Third',  actDefs[2])
            }
        }

        def factory = DescriptionItem("XPathOutcomeInitTestFactory-$timeStamp", folder) {
            PropertyDesc(name: "Type", defaultValue: "XPathOutcomeInit", isMutable: false, isClassIdentifier: true)
            Workflow(wf)
        }

        executeDoneJob(
            factory,
            "CreateNewInstance",
            recordToXML('NewDevObjectDef', [ObjectName: "XPathOutcomeInitTest-$timeStamp", SubFolder: folder])
        )

        def item = agent.getItem("$folder/XPathOutcomeInitTest-$timeStamp")

        setFieldAndExecuteJob(item, 'First',  '/OneFieldSchema/stringField-0')
        setFieldAndExecuteJob(item, 'Second', '/TwoFieldSchema/stringField-1')
        setFieldAndExecuteJob(item, 'Third',  '/ThreeFieldSchema/stringField-2')

        def outcome = item.getViewpoint("ThreeFieldSchema-$timeStamp", 'last').getOutcome()

        assert outcome.getField("stringField-0") == "First"
        assert outcome.getField("stringField-1") == "Second"
        assert outcome.getField("stringField-2") == "Third"
    }
}
