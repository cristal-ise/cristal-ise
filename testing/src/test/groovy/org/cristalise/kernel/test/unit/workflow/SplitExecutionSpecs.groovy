package org.cristalise.kernel.test.unit.workflow;

//import static org.cristalise.kernel.lifecycle.instance.WfVertex.Types.*

import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.lifecycle.WfBuilder

import spock.lang.Specification


class SplitExecutionSpecs extends Specification {

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


    def 'first-AndSplit((left)(right))-last'() {
        given: "Workflow contaning AndSplit with two Blocks"
        util.buildAndInitWf() {
            ElemAct("first")
            AndSplit {
                Block { ElemAct("left")  }
                Block { ElemAct("right") }
            }
            ElemAct("last")
        }

        println Gateway.getMarshaller().marshall(util.wf)
        
        util.checkNext('first','AndSplit')
        util.checkNext('AndSplit','left')
        util.checkNext('AndSplit','right')
        util.checkNext('left','Join')
        util.checkNext('right','Join')
        util.checkNext('Join','last')

        util.checkActStatus("first", [state: "Waiting", active: true])
        util.checkActStatus("left",  [state: "Waiting", active: false])
        util.checkActStatus("right", [state: "Waiting", active: false])
        util.checkActStatus("last",  [state: "Waiting", active: false])

        when: "requesting ElemAct(first) Done transition"
        util.requestAction("first", "Done")

        then: "ElemAct(first) state is Finished"
        util.checkActStatus("first", [state: "Finished", active: false])
        util.checkActStatus("left",  [state: "Waiting",  active: true])
        util.checkActStatus("right", [state: "Waiting",  active: true])
        util.checkActStatus("last",  [state: "Waiting",  active: false])

        when: "requesting ElemAct(left) Done transition"
        util.requestAction("left", "Done")

        then: "ElemAct(left) state is Finished"
        util.checkActStatus("left",  [state: "Finished", active: false])
        util.checkActStatus("right", [state: "Waiting",  active: true])
        util.checkActStatus("last",  [state: "Waiting",  active: false])

        when: "requesting ElemAct(right) Done transition"
        util.requestAction("right", "Done")

        then: "ElemAct(right) state is Finished"
        util.checkActStatus("left",  [state: "Finished", active: false])
        util.checkActStatus("right", [state: "Finished", active: false])
        util.checkActStatus("last",  [state: "Waiting",  active: true])

        when: "requesting ElemAct(last) Done transition"
        util.requestAction("last", "Done")

        then: "ElemAct(last) state is Finished"
        util.checkActStatus("left",  [state: "Finished", active: false])
        util.checkActStatus("right", [state: "Finished", active: false])
        util.checkActStatus("last",  [state: "Finished", active: false])
    }

    def 'first-AndSplit(AndSplit((left2)(right2))(right1))-last'() {
        given: "Workflow contaning AndSplit with two Blocks"
        util.buildAndInitWf(false) {
            ElemAct("first")
            AndSplit {
                AndSplit {
                    Block { ElemAct("left2")  }
                    Block { ElemAct("right2")  }
                }
                Block { ElemAct("right1") }
            }
            ElemAct("last")
        }
        util.checkNext('first','AndSplit')
        util.checkNext('Join','last')
        
        util.checkActStatus("first",  [state: "Waiting", active: true])
        util.checkActStatus("left2",  [state: "Waiting", active: false])
        util.checkActStatus("right2", [state: "Waiting", active: false])
        util.checkActStatus("right1", [state: "Waiting", active: false])
        util.checkActStatus("last",   [state: "Waiting", active: false])

        when: "requesting ElemAct(first) Done transition"
        util.requestAction("first", "Done")

        then: "ElemAct(first) state is Finished"
        util.checkActStatus("first",  [state: "Finished", active: false])
        util.checkActStatus("left2",  [state: "Waiting",  active: true])
        util.checkActStatus("right2", [state: "Waiting",  active: true])
        util.checkActStatus("right1", [state: "Waiting",  active: true])
        util.checkActStatus("last",   [state: "Waiting",  active: false])
    }

}
