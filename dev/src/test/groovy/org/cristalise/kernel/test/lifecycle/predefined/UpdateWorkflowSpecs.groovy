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
package org.cristalise.kernel.test.lifecycle.predefined

import org.cristalise.dsl.test.builders.ItemTestBuilder
import org.cristalise.dsl.test.builders.WorkflowTestBuilder
import org.cristalise.kernel.test.utils.CristalTestSetup

import spock.lang.Specification


/**
 *
 */
class UpdateWorkflowSpecs extends Specification implements CristalTestSetup {

    static WorkflowTestBuilder origWf

    def setupSpec() {
        inMemoryServer(null, true)
        origWf = new WorkflowTestBuilder()
    }

    def cleanupSpec() { cristalCleanup() }

    def 'Update empty wf'() {
        given:
        def wfDef

        def item = ItemTestBuilder.create(name: "dummyFactory", folder: "testing") {
            DependencyDescription('workflow') {
                Member(itemPath: "/desc/ActivityDesc/domain/TestWorkflow") {
                    Property("Version": 0)
                }
            }
        }

        origWf.buildAndInitWf {
            //EA('EA1')
        }

        //def newWf = new WorkflowBuilder()

        when:
        origWf.requestAction('workflow/predefined/UpdateWorkflowFromDescription', 'Done', '')

        then:
        origWf.checkActStatus('EA1',[state: "Finished", active: false])
    }
}
