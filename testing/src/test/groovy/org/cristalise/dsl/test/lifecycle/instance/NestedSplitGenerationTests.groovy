package org.cristalise.dsl.test.lifecycle.instance;

import static org.junit.Assert.*

import org.cristalise.dsl.lifecycle.instance.WfBuilder;
import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway
import org.junit.After
import org.junit.Before
import org.junit.Test


class NestedSplitGenerationTests {

    WfBuilder util

    @Before
    public void setup() {
        String[] args = ['-logLevel', '8', '-config', 'src/test/conf/testServer.conf', '-connect', 'src/test/conf/testInMemory.clc']
        Gateway.init(AbstractMain.readC2KArgs(args))
        Gateway.connect()

        util = new WfBuilder()
    }

    @After
    public void cleanup() {
        Gateway.close()
    }

    @Test
    public void 'AndSplit((first)(left)(right)(last))'() {
        given: "Workflow contaning AndSplit with two Blocks"
        util.buildAndInitWf(false) {
            AndSplit {
                Block { ElemAct("first") }
                Block { ElemAct("left")  }
                Block { ElemAct("right") }
                Block { ElemAct("last")  }
            }
        }

        println Gateway.getMarshaller().marshall(util.wf)
        
        util.checkNext('AndSplit','first')
        util.checkNext('AndSplit','left')
        util.checkNext('AndSplit','right')
        util.checkNext('AndSplit','last')
        util.checkNext('first','Join')
        util.checkNext('left', 'Join')
        util.checkNext('right','Join')
        util.checkNext('last', 'Join')

        util.checkActStatus("first", [state: "Waiting", active: true])
        util.checkActStatus("left",  [state: "Waiting", active: true])
        util.checkActStatus("right", [state: "Waiting", active: true])
        util.checkActStatus("last",  [state: "Waiting", active: true])
    }

    @Test
    public void 'first-AndSplit((left)(right)(last))'() {
        given: "Workflow contaning AndSplit with two Blocks"
        util.buildAndInitWf(false) {
            ElemAct("first")
            AndSplit {
                Block { ElemAct("left")  }
                Block { ElemAct("right") }
                Block { ElemAct("last")  }
            }
        }

        println Gateway.getMarshaller().marshall(util.wf)

        util.checkNext('first','AndSplit')
        util.checkNext('AndSplit','left')
        util.checkNext('AndSplit','right')
        util.checkNext('AndSplit','last')
        util.checkNext('left', 'Join')
        util.checkNext('right','Join')
        util.checkNext('last', 'Join')

        util.checkActStatus("first", [state: "Waiting", active: true])
        util.checkActStatus("left",  [state: "Waiting", active: false])
        util.checkActStatus("right", [state: "Waiting", active: false])
        util.checkActStatus("last",  [state: "Waiting", active: false])

        util.requestAction("first", "Done")

        util.checkActStatus("first", [state: "Finished", active: false])
        util.checkActStatus("left",  [state: "Waiting", active: true])
        util.checkActStatus("right", [state: "Waiting", active: true])
        util.checkActStatus("last",  [state: "Waiting", active: true])
    }
}
