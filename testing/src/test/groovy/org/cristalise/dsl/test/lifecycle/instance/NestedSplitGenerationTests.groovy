package org.cristalise.dsl.test.lifecycle.instance;

import static org.junit.Assert.*

import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway
import org.junit.After
import org.junit.Before
import org.junit.Test


class NestedSplitGenerationTests {

    WorkflowTestBuilder util

    @Before
    public void setup() {
        String[] args = ['-logLevel', '8', '-config', 'src/test/conf/testServer.conf', '-connect', 'src/test/conf/testInMemory.clc']
        Gateway.init(AbstractMain.readC2KArgs(args))
        Gateway.connect()

        util = new WorkflowTestBuilder()
    }

    @After
    public void cleanup() {
        //println Gateway.getMarshaller().marshall(util.wf)
        Gateway.close()
    }

    @Test
    public void 'AndSplit((left)(right)'() {
        util.build {
            AndSplit {
                Block { ElemAct("left")  }
                Block { ElemAct("right") }
            }
        }

        util.checkSplit('AndSplit',['left','right'])
        util.checkJoin ('Join',    ['left','right'])
    }

    @Test
    public void 'OrSplit((left)(right)'() {
        util.build {
            OrSplit {
                Block { ElemAct("left")  }
                Block { ElemAct("right") }
            }
        }

        util.checkSplit('OrSplit',['left','right'])
        util.checkJoin ('Join',   ['left','right'])
    }

    @Test
    public void 'XOrSplit((left)(right)'() {
        util.build {
            XOrSplit {
                Block { ElemAct("left")  }
                Block { ElemAct("right") }
            }
        }

        util.checkSplit('XOrSplit',['left','right'])
        util.checkJoin ('Join',    ['left','right'])
    }

    @Test
    public void 'first-AndSplit((left)(right)'() {
        util.build {
            ElemAct("first")
            AndSplit {
                Block { ElemAct("left")  }
                Block { ElemAct("right") }
            }
            ElemAct("last")
        }

        util.checkNext('first',    'AndSplit')
        util.checkSplit('AndSplit',['left','right'])
        util.checkJoin ('Join',    ['left','right'])
        util.checkNext('Join',    'last')
    }

    @Test
    public void 'first-OrSplit((left)(right)'() {
        util.build {
            ElemAct("first")
            OrSplit {
                Block { ElemAct("left")  }
                Block { ElemAct("right") }
            }
            ElemAct("last")
        }

        util.checkNext('first',    'OrSplit')
        util.checkSplit('OrSplit', ['left','right'])
        util.checkJoin ('Join',    ['left','right'])
        util.checkNext('Join',    'last')
    }

    @Test
    public void 'first-XOrSplit((left)(right)'() {
        util.build {
            ElemAct("first")
            XOrSplit {
                Block { ElemAct("left")  }
                Block { ElemAct("right") }
            }
            ElemAct("last")
        }

        util.checkNext('first',    'XOrSplit')
        util.checkSplit('XOrSplit',['left','right'])
        util.checkJoin ('Join',    ['left','right'])
        util.checkNext('Join',    'last')
    }
}
