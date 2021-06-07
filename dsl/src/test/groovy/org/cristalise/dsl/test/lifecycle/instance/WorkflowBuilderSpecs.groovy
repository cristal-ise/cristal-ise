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
import org.cristalise.kernel.lifecycle.instance.Activity
import org.cristalise.kernel.lifecycle.instance.CompositeActivity
import org.cristalise.kernel.test.utils.CristalTestSetup;

import spock.lang.Specification


/**
 *
 */
class WorkflowBuilderSpecs extends Specification implements CristalTestSetup {

    static WorkflowTestBuilder wfBuilder

    def setupSpec() {
        inMemorySetup()
        wfBuilder = new WorkflowTestBuilder()
    }

    def cleanupSpec() {
        cristalCleanup()
    }

    def 'Split can only contain Block not ElemAct'() {
        when: "Split contains ElemAct"
        wfBuilder.build {
            AndSplit {
                ElemAct()
            }
        }
        then: "UnsupportedOperationException is thrown"
        thrown UnsupportedOperationException
    }

    def 'Split can only contain Block not CompAct'() {
        when: "Split contains CompAct"
        wfBuilder.build {
            AndSplit {
                CompAct {}
            }
        }
        then: "UnsupportedOperationException is thrown"
        thrown UnsupportedOperationException
    }

    def 'Split can only contain Block not Split'() {
        when: "Split contains Split"
        wfBuilder.build {
            AndSplit {
                AndSplit {}
            }
        }
        then: "UnsupportedOperationException is thrown"
        thrown UnsupportedOperationException
    }

    def 'Each type is counted independently for auto naming'() {
        when: "Split contains Split of Splits"
        wfBuilder.build {
            EA()
            AndSplit {
                B { OrSplit {
                    B { AndSplit {} }
                    B { EA() }
                } }
            }
            AndSplit {
                B { CA {
                    Loop {
                        XOrSplit {}
                    }
                } }
            }
            CA { EA() }
        }

        then:
        wfBuilder.checkSequence('EA','AndSplit')
        wfBuilder.checkSequence('AndJoin','AndSplit2')
        wfBuilder.checkSequence('AndJoin2','CA1')

        wfBuilder.checkSplit('AndSplit', ['OrSplit'])
        wfBuilder.checkJoin ('AndJoin',  ['OrJoin'])

        wfBuilder.checkSplit('OrSplit', ['AndSplit1','EA1'])
        wfBuilder.checkJoin ('OrJoin',  ['AndJoin1', 'EA1'])

        wfBuilder.checkSplit('AndSplit2', ['CA'])
        wfBuilder.checkJoin ('AndJoin2',  ['CA'])

        wfBuilder.checkSplit('LoopSplit', ['LoopJoin_first', 'LoopJoin_last'])
        wfBuilder.checkJoin ('LoopJoin_first',  ['LoopSplit'])
        wfBuilder.checkJoin ('LoopJoin_last',  ['LoopSplit'])

        wfBuilder.checkSequence('LoopJoin_first', 'XOrSplit')
        wfBuilder.checkSequence('XOrJoin', 'LoopSplit')
    }

    def 'B can be used as alias of Block'() {
        expect: "Workflow contains B"
        wfBuilder.build { 
            B{
                assert parentCABlock
                ElemAct('lonely')
            }
        }
        wfBuilder.checkActPath("lonely", "workflow/domain/lonely")
        wfBuilder.wf.search("workflow/domain/lonely") instanceof Activity
    }

    def 'EA can be used as alias of ElemAct'() {
        expect: "Workflow contains EA"
        wfBuilder.build {
            EA('lonely')
        }
        wfBuilder.checkActPath("lonely", "workflow/domain/lonely")
        wfBuilder.wf.search("workflow/domain/lonely") instanceof Activity
    }

    def 'CA can be used as alias of CompAct'() {
        expect: "Workflow contains CA"
        wfBuilder.build {
            CA('lonely') {}
        }
        wfBuilder.checkActPath("lonely", "workflow/domain/lonely")
        wfBuilder.wf.search("workflow/domain/lonely") instanceof CompositeActivity
    }
}
