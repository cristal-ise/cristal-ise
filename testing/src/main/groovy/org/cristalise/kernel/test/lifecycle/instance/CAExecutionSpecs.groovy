package org.cristalise.kernel.test.lifecycle.instance;

import static org.junit.Assert.*

import org.cristalise.dsl.test.builders.WorkflowTestBuilder;
import org.cristalise.kernel.common.InvalidTransitionException
import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.utils.CristalTestSetup

import spock.lang.Specification


class CAExecutionSpecs extends Specification implements CristalTestSetup {

    WorkflowTestBuilder util

    def setup() {
        inMemoryServer('src/main/bin/inMemoryServer.conf', 'src/main/bin/inMemory.clc', 8)

        util = new WorkflowTestBuilder()
    }

    def cleanup() {
        println Gateway.getMarshaller().marshall(util.wf)
        cristalCleanup()
    }

    def 'Execute ElemAct using Done transition'() {
        given: "Workflow contaning single ElemAct"
        util.buildAndInitWf { ElemAct('first') }

        when: "requesting ElemAct Done transition"
        util.requestAction('first', "Done")

        then: "ElemAct state is Finished"
        util.checkActStatus('rootCA', [state: "Started",  active: true])
        util.checkActStatus('first',  [state: "Finished", active: false])
    }

    def 'Execute ElemAct using Start/Complete transition'() {
        given: "Workflow contaning single ElemAct"
        util.buildAndInitWf { ElemAct('first') }

        when: "requesting ElemAct Start transition"
        util.requestAction( 'first', "Start")

        then: "ElemAct state is Started"
        util.checkActStatus('rootCA', [state: "Started", active: true])
        util.checkActStatus('first',   [state: "Started", active: true])

        when: "requesting ElemAct Complete transition"
        util.requestAction( 'first', "Complete" )

        then: "ElemAct state is Finished"
        util.checkActStatus('rootCA', [state: "Started",  active: true])
        util.checkActStatus('first',  [state: "Finished", active: false])
    }

    def 'Execute sequence of ElemActs using Done transition'() {
        given: "Workflow contaning sequence of two ElemAct"
        util.buildAndInitWf { ElemAct('first'); ElemAct('second') }

        when: "requesting first ElemAct Done transition"
        util.requestAction('first', "Done")

        then: "first ElemAct state is Finished and second is still Waiting"
        util.checkActStatus('rootCA', [state: "Started",  active: true])
        util.checkActStatus('first',  [state: "Finished", active: false])
        util.checkActStatus('second', [state: "Waiting",  active: true])

        when: "requesting second ElemAct Done transition"
        util.requestAction('second', "Done")

        then: "first ElemAct state is Finished and second is Finished"
        util.checkActStatus('rootCA', [state: "Started",  active: true])
        util.checkActStatus('first',  [state: "Finished", active: false])
        util.checkActStatus('second', [state: "Finished", active: false])
    }

    def 'CompAct is automatically finihed when all Activities in sequence are finished'() {
        given: "Workflow contaning CompAct containig 2 ElemAct in a sequence"
        util.buildAndInitWf { 
            CompAct('ca') { 
                ElemAct('first') 
                ElemAct('second') 
            }
        }

        when: "requesting first ElemAct Done transition"
        util.requestAction('first', "Done")

        then: "ElemAct 'first' and CompAct state is Started"
        util.checkActStatus('rootCA', [state: "Started",  active: true])
        util.checkActStatus('ca',     [state: "Started",  active: true])
        util.checkActStatus('first',  [state: "Finished", active: false])
        util.checkActStatus('second', [state: "Waiting",  active: true])

        when: "requesting second ElemAct Done transition"
        util.requestAction('second', "Done")

        then: "ElemAct and CompAct state is Finished"
        util.checkActStatus('rootCA', [state: "Started",  active: true])
        util.checkActStatus('ca',     [state: "Finished", active: false])
        util.checkActStatus('first',  [state: "Finished", active: false])
        util.checkActStatus('second', [state: "Finished", active: false])
    }

    def 'CompAct is automatically finihed when all Activities in AndSplit are finished'() {
        given: "Workflow contaning CompAct containig 2 ElemAct in AndSplit"
        util.buildAndInitWf { 
            CompAct('ca') {
                AndSplit {
                    Block { ElemAct('left')  }
                    Block { ElemAct('right') }
                }
            }
        }
        util.checkActStatus('rootCA', [state: "Started",  active: true])
        util.checkActStatus('ca',     [state: "Started",  active: true])
        util.checkActStatus('left',   [state: "Waiting",  active: true])
        util.checkActStatus('right',  [state: "Waiting",  active: true])

        when: "requesting left ElemAct Done transition"
        util.requestAction('left', "Done")

        then: "ElemAct 'left' and CompAct state is Started"
        util.checkActStatus('rootCA', [state: "Started",  active: true])
        util.checkActStatus('ca',     [state: "Started",  active: true])
        util.checkActStatus('left',   [state: "Finished", active: false])
        util.checkActStatus('right',  [state: "Waiting",  active: true])

        when: "requesting right ElemAct Done transition"
        util.requestAction('right', "Done")

        then: "ElemAct and CompAct state is Finished"
        util.checkActStatus('rootCA', [state: "Started",  active: true])
        util.checkActStatus('ca',     [state: "Finished", active: false])
        util.checkActStatus('left',   [state: "Finished", active: false])
        util.checkActStatus('right',  [state: "Finished", active: false])
    }

    def 'CompAct is automatically finihed when all Activities in Loop are finished'() {
        given: "Workflow contaning CompAct containig 1 ElemAct in Loop"
        util.buildAndInitWf {
            CompAct('ca') {
                Loop(RoutingScriptName: 'javascript:\"false\";') { //loop shall finish automatically
                    ElemAct('one')
                }
            }
        }

        when: "requesting one ElemAct Done transition"
        util.requestAction('one', "Done")

        then: "ElemAct 'one' and CompAct state is Finished"
        util.checkActStatus('rootCA', [state: "Started",  active: true])
        util.checkActStatus('one',    [state: "Finished", active: false])
        util.checkActStatus('ca',     [state: "Finished", active: false])
    }

    def 'CompAct with infinitive Loop never finishes'() {
        given: "Workflow contaning CompAct containig 1 ElemAct in Loop"
        util.buildAndInitWf {
            CompAct('ca') {
                Loop { //by default the DSL creates infinitive Loop 
                    ElemAct('one')
                }
            }
        }

        when: "requesting one ElemAct Done transition"
        util.requestAction('one', "Done")

        then: "ElemAct 'one' and CompAct are still active"
        util.checkActStatus('rootCA', [state: "Started", active: true])
        util.checkActStatus('one',    [state: "Waiting", active: true])
        util.checkActStatus('ca',     [state: "Started", active: true])
    }

    def 'Empty Wf cannot be executed'() {
        given: "empty Workflow"
        util.buildAndInitWf {
            //checks before Wf.init()
            util.checkActStatus('rootCA', [state: "Waiting", active: false])
        }

        when: "requesting Root CompAct Complete transition"
        util.requestAction('rootCA', "Complete")

        then: "InvalidTransitionException is thrown"
        thrown InvalidTransitionException
    }

    public void 'Cannot execute Wf containing empty CompAct'() {
        given: "empty Workflow containing empty CompAct"
        util.buildAndInitWf {
            CompAct('ca') {}

            //checks before Wf.init()
            util.checkActStatus('rootCA', [state: "Waiting", active: false])
            util.checkActStatus('ca',     [state: "Waiting", active: false])
        }
        //checks after Wf.init()
        util.checkActStatus('rootCA', [state: "Started", active: true])
        util.checkActStatus('ca',   [state: "Waiting", active: true])

        when: "requesting CompAct Complete transition"
        util.requestAction('ca', "Complete")

        then: "InvalidTransitionException is thrown"
        thrown InvalidTransitionException
    }

    def 'Cannot complete Compact without finishing all ElemActs'() {
        given: "Workflow containing single CompAct with a single ElemAct"
        util.buildAndInitWf {
            CompAct('ca') {
                ElemAct('first')
            }
        }

        when: "requesting CompAct Complete transition"
        util.requestAction('ca', "Complete")

        then: "InvalidTransitionException is thrown"
        thrown InvalidTransitionException
    }

    def 'Compact can be finished with active children if Abortable'() {
        given: "Workflow containing single CompAct with a single ElemAct"
        util.buildAndInitWf {
            CompAct('ca') {
                Property('Abortable': true)
                ElemAct('first')
            }
        }
        util.checkActStatus('ca',   [state: "Started", active: true])

        when: "requesting Root CompAct Complete transition"
        util.requestAction('ca', "Complete")

        then: "CompAct is finished, child is inactive but Waiting"
        util.checkActStatus('ca',    [state: "Finished", active: false])
        util.checkActStatus('first', [state: "Waiting",  active: false])
    }

    def 'Cannot Complete Root Compact without finishing all CompActs'() {
        given: "Workflow contaning single and empty CompAct"
        util.buildAndInitWf { CompAct{} }

        when: "requesting Root CompAct Complete transition"
        util.requestAction('rootCA', "Complete")

        then: "InvalidTransitionException is thrown"
        thrown InvalidTransitionException
    }
}
