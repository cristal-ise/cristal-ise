

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

import org.cristalise.dsl.persistency.outcome.OutcomeBuilder
import org.cristalise.kernel.entity.agent.Job
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.test.KernelScenarioTestBase
import org.junit.Test


class XPathOutcomeInitTestIT extends KernelScenarioTestBase {

    private ItemProxy createNewXPathOutcomeInitTest_Details(String name, String folder) {
        return createNewDevItem(
            "/domain/desc/integTest/XPathOutcomeInitTest_DetailsFactory",
            "XPathOutcomeInitTest_CreateDescription", 
            name,
            folder)
    }

    def XPathOutcomeInitTest_Details(String name, String folder, Closure cl) {
        createNewXPathOutcomeInitTest_Details(name, folder)

        ItemProxy devItem = agent.getItem("/domain/integTest/$name")
        assert devItem && devItem.getName() == name

        Job doneJob = getDoneJob(devItem, "XPathOutcomeInitTest_SetDetails")
        doneJob.setOutcome( OutcomeBuilder.build(cl) )
        agent.execute(doneJob)

        doneJob = getDoneJob(devItem, "AssignNewVersionFromLast")
        agent.execute(doneJob)

        assert devItem.getViewpoint('XPathOutcomeInitTest_Details', "0")
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
                Role('dev')
                Schema(schema)
            }
        }

        compActDefFactoryName = "/domain/desc/integTest/XPathOutcomeInitTest_CADefFactory"
        
        def caSlotIDs = []

        CompositeActivityDef("XPathOutcomeInitTestWF-$timeStamp", folder) {
            caSlotIDs[0] = ElemActDef('First',  actDefs[0])
            caSlotIDs[1] = ElemActDef('Second', actDefs[1])
            caSlotIDs[2] = ElemActDef('Third',  actDefs[2])
        }

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
}
