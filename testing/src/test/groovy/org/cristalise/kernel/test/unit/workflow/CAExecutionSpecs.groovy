package org.cristalise.kernel.test.unit.workflow;

import static org.junit.Assert.*

import org.cristalise.kernel.common.InvalidTransitionException
import org.cristalise.kernel.lifecycle.instance.Activity
import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.lifecycle.WfBuilder

import spock.lang.Specification


class CAExecutionSpecs extends Specification {

    WfBuilder util

    def setup() {
        String[] args = ['-logLevel', '8', '-config', 'src/test/conf/testServer.conf', '-connect', 'src/test/conf/testInMemory.clc']
        Gateway.init(AbstractMain.readC2KArgs(args))
        Gateway.connect()

        util = new WfBuilder()
    }

    def cleanup() {
        Gateway.close()
    }

    def 'Execute ElemAct using Done transition'() {
        given: "Workflow contaning single ElemAct"
        util.buildWf { ElemAct() }

        when: "requesting ElemAct Done transition"
        util.requestAction(util.act0, "Done")

        then: "ElemAct state is Finished"
        util.checkActStatus(util.rootCA, [state: "Started",  active: true])
        util.checkActStatus(util.act0,   [state: "Finished", active: true])

        when: "requesting Root CompAct Complete transition"
        util.requestAction(util.rootCA, "Complete")

        then: "Root CompAct state is Finished"
        util.checkActStatus(util.rootCA, [state: "Finished", active: false])
        util.checkActStatus(util.act0,   [state: "Finished", active: true])
    }

    def 'Execute ElemAct using Start/Complete transition'() {
        given: "Workflow contaning single ElemAct"
        util.buildWf { ElemAct() }

        when: "requesting ElemAct Start transition"
        util.requestAction( util.act0, "Start")

        then: "ElemAct state is Started"
        util.checkActStatus(util.rootCA, [state: "Started", active: true])
        util.checkActStatus(util.act0,   [state: "Started", active: true])

        when: "requesting ElemAct Complete transition"
        util.requestAction( util.act0, "Complete" )

        then: "ElemAct state is Finished"
        util.checkActStatus(util.rootCA, [state: "Started",  active: true])
        util.checkActStatus(util.act0,   [state: "Finished", active: true])

        when: "requesting Root CompAct Complete transition"
        util.requestAction(util.rootCA, "Complete")

        then: "Root CompAct state is Finished"
        util.checkActStatus(util.rootCA, [state: "Finished", active: false])
        util.checkActStatus(util.act0,   [state: "Finished", active: true])
    }

    def 'Execute sequence of ElemActs using Done transition'() {
        given: "Workflow contaning sequence of two ElemAct"
        util.buildWf { ElemAct(); ElemAct() }
        Activity act1 = (Activity)util.wf.search("workflow/domain/1")

        when: "requesting first ElemAct Done transition"
        util.requestAction(util.act0, "Done")

        then: "first ElemAct state is Finished and second is still Waiting"
        util.checkActStatus(util.rootCA, [state: "Started",  active: true])
        util.checkActStatus(util.act0,   [state: "Finished", active: false])
        util.checkActStatus(act1,        [state: "Waiting",  active: true])

        when: "requesting second ElemAct Done transition"
        util.requestAction(act1, "Done")

        then: "first ElemAct state is Finished and second is Finished"
        util.checkActStatus(util.rootCA, [state: "Started",  active: true])
        util.checkActStatus(util.act0,   [state: "Finished", active: false])
        util.checkActStatus(act1,        [state: "Finished", active: true])
        
        when: "requesting Root CompAct Complete transition"
        util.requestAction(util.rootCA, "Complete")

        then: "all Acts are Finished and inactive"
        util.checkActStatus(util.rootCA, [state: "Finished", active: false])
        util.checkActStatus(util.act0,   [state: "Finished", active: false])
        util.checkActStatus(act1,        [state: "Finished", active: true])
    }

    def 'Execute ElemcAct in CompAct using Done transition'() {
        given: "Workflow contaning CompAct containig one ElemAct"
        util.buildWf { 
            CompAct { 
                ElemAct() 
            }
        }

        Activity act1 = (Activity)util.wf.search("workflow/domain/1")

        when: "requesting first ElemAct Done transition"
        util.requestAction(act1, "Done")

        then: "ElemAct and CompAct state is Finished"
        util.checkActStatus(util.rootCA, [state: "Started",  active: true])
        util.checkActStatus(util.act0,   [state: "Finished", active: true])  //CA
        util.checkActStatus(act1,        [state: "Finished", active: false]) //EA

        when: "requesting Root CompAct Complete transition"
        util.requestAction(util.rootCA, "Complete")

        then: "Root CompAct state is Finished"
        util.checkActStatus(util.rootCA, [state: "Finished", active: false])
        util.checkActStatus(util.act0,   [state: "Finished", active: true])  //CA
        util.checkActStatus(act1,        [state: "Finished", active: false]) //EA
    }

    def 'Empty Wf cannot be executed'() {
        given: "empty Workflow"
        util.buildWf(false) {
            //checks before Wf.init()
            checkActStatus(rootCA, [state: "Waiting", active: false])
        }

        when: "requesting Root CompAct Complete transition"
        util.requestAction(util.rootCA, "Complete")

        then: "InvalidTransitionException is thrown"
        thrown InvalidTransitionException
    }

    public void 'Cannot execute Wf containing empty CompAct'() {
        given: "empty Workflow containing empty CompAct"
        util.buildWf(false) {
            CompAct {}

            //checks before Wf.init()
            checkActStatus(rootCA, [state: "Waiting", active: false])
            checkActStatus((Activity)wf.search("workflow/domain/0"),   [state: "Waiting", active: false])  //CA
        }
        //checks after Wf.init()
        util.checkActStatus(util.rootCA, [state: "Started", active: true])
        util.checkActStatus(util.act0,   [state: "Waiting", active: true])  //CA

        when: "requesting CompAct Complete transition"
        util.requestAction(util.act0, "Complete")

        then: "InvalidTransitionException is thrown"
        thrown InvalidTransitionException
    }

    def 'Cannot Complete Root Compact without finishing all ElemActs'() {
        given: "Workflow contaning single ElemAct"
        util.buildWf { ElemAct() }

        when: "requesting Root CompAct Complete transition"
        util.requestAction(util.rootCA, "Complete")

        then: "InvalidTransitionException is thrown"
        thrown InvalidTransitionException
    }

    def 'Cannot Complete Root Compact without finishing all CompActs'() {
        given: "Workflow contaning single and empty CompAct"
        util.buildWf(false) { CompAct{} }

        when: "requesting Root CompAct Complete transition"
        util.requestAction(util.rootCA, "Complete")

        then: "InvalidTransitionException is thrown"
        thrown InvalidTransitionException
    }

}
