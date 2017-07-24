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
package org.cristalise.dsl.test.lifecycle.instance

import org.cristalise.dsl.test.builders.WorkflowTestBuilder;
import org.cristalise.kernel.test.utils.CristalTestSetup;

import spock.lang.Specification


/**
 *
 */
class WFBuilderPropertySpecs extends Specification implements CristalTestSetup {

    WorkflowTestBuilder wfBuilder

    def setup() {
        inMemorySetup()
        wfBuilder = new WorkflowTestBuilder()
    }

    def cleanup() {
        cristalCleanup()
    }

    def 'ElemAct can specify Properites'() {
        when: "Workflow contains EA"
        wfBuilder.build {
            ElemAct('lonely') {
                Property(stringVal: '1')
                Property(intVal: 1, booleanVal: true)
            }
        }

        then:""
        wfBuilder.wf.search("workflow/domain/lonely").properties.stringVal  == '1'
        wfBuilder.wf.search("workflow/domain/lonely").properties.intVal     == 1
        wfBuilder.wf.search("workflow/domain/lonely").properties.booleanVal == true
    }

    def 'CompAct can specify Properites'() {
        when: "Workflow contains CA"
        wfBuilder.build {
            CompAct('lonely') {
                Property(stringVal: '1')
                Property(intVal: 1, booleanVal: true)
            }
        }

        then:""
        wfBuilder.wf.search("workflow/domain/lonely").properties.stringVal  == '1'
        wfBuilder.wf.search("workflow/domain/lonely").properties.intVal     == 1
        wfBuilder.wf.search("workflow/domain/lonely").properties.booleanVal == true
    }

    def 'Split can specify Properites at two places'() {
        when: "Workflow contains EA"
        wfBuilder.build {
            AndSplit(stringVal: '1') {
                Property(intVal: 1, booleanVal: true)
            }
        }

        then:""
        wfBuilder.wf.search("workflow/domain/AndSplit").properties.stringVal  == '1'
        wfBuilder.wf.search("workflow/domain/AndSplit").properties.intVal     == 1
        wfBuilder.wf.search("workflow/domain/AndSplit").properties.booleanVal == true
    }

    def 'Default RoutingScript is added to Split if nothing specified'() {
        when: "Wf contains OrSplit"
        wfBuilder.build {
            OrSplit {}
        }
        then: "RoutingScriptName and Version were added to Properties"
        wfBuilder.wf.search("workflow/domain/OrSplit").properties.RoutingScriptName  == "javascript:\"true\";"
        wfBuilder.wf.search("workflow/domain/OrSplit").properties.RoutingScriptVersion  == ""
    }

    def 'Default RoutingScriptis added to Loop if nothing specified'() {
        when: "Wf contains OrSplit"
        wfBuilder.build {
            Loop {}
        }
        then: "RoutingScriptName and Version were added to Properties"
        wfBuilder.wf.search("workflow/domain/LoopSplit").properties.RoutingScriptName  == "javascript:\"true\";"
        wfBuilder.wf.search("workflow/domain/LoopSplit").properties.RoutingScriptVersion  == ""
    }

    def 'OrSplit can use javascript keyword to specify hardcoded RoutingScript'() {
        when: "Wf contains OrSplit"
        wfBuilder.build {
            OrSplit(javascript: true) {}
        }
        then: "RoutingScriptName and Version were added and javascript was removed from Properties"
        wfBuilder.wf.search("workflow/domain/OrSplit").properties.RoutingScriptName  == "javascript:\"true\";"
        wfBuilder.wf.search("workflow/domain/OrSplit").properties.RoutingScriptVersion  == ""
        wfBuilder.wf.search("workflow/domain/OrSplit").properties.javascript  == null
    }

    def 'XOrSplit can use javascript keyword to specify hardcoded RoutingScript'() {
        when: "Wf contains XOrSplit"
        wfBuilder.build {
            XOrSplit(javascript: true) {}
        }
        then: "RoutingScriptName and Version were added and javascript was removed from Properties"
        wfBuilder.wf.search("workflow/domain/XOrSplit").properties.RoutingScriptName  == "javascript:\"true\";"
        wfBuilder.wf.search("workflow/domain/XOrSplit").properties.RoutingScriptVersion  == ""
        wfBuilder.wf.search("workflow/domain/XOrSplit").properties.javascript  == null
    }


}
