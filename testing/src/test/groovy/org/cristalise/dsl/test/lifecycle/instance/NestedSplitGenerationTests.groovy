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

    def createAndCheck_Split(String type) {
        util.build {
            "$type" {
                Block { ElemAct("left")  }
                Block { ElemAct("right") }
            }
        }
        util.checkActPath('right', 'workflow/domain/right')
        util.checkActPath('left',  'workflow/domain/left')

        util.checkSplit(type,   ['left','right'])
        util.checkJoin ('Join', ['left','right'])
    }

    def createAndCheck_Act_Split_Act(String type) {
        util.build {
            ElemAct("first")
            "$type" {
                Block { ElemAct("left")  }
                Block { ElemAct("right") }
            }
            ElemAct("last")
        }
        util.checkActPath('first', 'workflow/domain/first')
        util.checkActPath('right', 'workflow/domain/right')
        util.checkActPath('left',  'workflow/domain/left')
        util.checkActPath('last',  'workflow/domain/last')

        util.checkSequence('first', type)
        util.checkSequence('Join', 'last')

        util.checkSplit(type,   ['left','right'])
        util.checkJoin ('Join', ['left','right'])
    }

    @Test
    public void 'Split((left)(right))'() {
        createAndCheck_Split('AndSplit')
        createAndCheck_Split('OrSplit')
        createAndCheck_Split('XOrSplit')
    }

    @Test
    public void 'first-Split((left)(right))-last'() {
        createAndCheck_Act_Split_Act('AndSplit')
        createAndCheck_Act_Split_Act('OrSplit')
        createAndCheck_Act_Split_Act('XOrSplit')
    }

    @Test
    public void 'Loop((first)))'() {
        util.build {
            Loop {
                B{ ElemAct("first") }
            }
        }
        util.checkActPath('first', 'workflow/domain/first')

        util.checkSplit('LoopSplit', ['first','Join'])
        util.checkJoin ('Join', ['first','LoopSplit'])
    }

    @Test
    public void 'first-Block(And((inner)))-last'() {
        util.build {
            EA("first")
            Block {
                AndSplit {
                    B{ EA("inner") }
                }
            }
            EA("last")
        }
        util.checkActPath('first', 'workflow/domain/first')
        util.checkActPath('inner', 'workflow/domain/inner')
        util.checkActPath('last',  'workflow/domain/last')

        util.checkSplit('AndSplit', ['inner'])
        util.checkJoin ('Join',     ['inner'])

        util.checkSequence('first','AndSplit')
        util.checkSequence('Join','last')
    }

    @Test(expected=RuntimeException)
    public void 'Split cannot have EA'() {
        util.build {
            AndSplit {
                ElemAct("first")
            }
        }
    }

    @Test(expected=RuntimeException)
    public void 'Split cannot have CA'() {
        util.build {
            AndSplit {
                CompAct {}
            }
        }
    }

    @Test(expected=RuntimeException)
    public void 'Split cannot have Split'() {
        util.build {
            AndSplit {
                AndSplit {}
            }
        }
    }

    @Test
    public void 'AndSplit(AndSplit((left1)(right1))(right))'() {
        util.build {
            AndSplit() {
                Block {
                    AndSplit('AndSplit1') {
                        Block { ElemAct("left1") }
                        Block { ElemAct("right1") }
                   }
                }
                Block { ElemAct("right") }
            }
        }

        util.checkActPath('right',  'workflow/domain/right')
        util.checkActPath('left1',  'workflow/domain/left1')
        util.checkActPath('right1', 'workflow/domain/right1')

        util.checkSplit('AndSplit',['AndSplit1','right'])
        util.checkJoin ('Join',    ['Join1',    'right'])

        util.checkSplit('AndSplit1',['left1','right1'])
        util.checkJoin ('Join1',    ['left1','right1'])
    }

    @Test
    public void 'AndSplit((left)AndSplit((left1)(right1)))'() {
        util.build {
            AndSplit {
                Block { ElemAct("left") }
                Block {
                    AndSplit('AndSplit1') {
                        Block { ElemAct("left1") }
                        Block { ElemAct("right1") }
                    }
                }
            }
        }

        util.checkActPath('left',   'workflow/domain/left')
        util.checkActPath('left1',  'workflow/domain/left1')
        util.checkActPath('right1', 'workflow/domain/right1')

        util.checkSplit('AndSplit',['AndSplit1','left'])
        util.checkJoin ('Join',    ['Join1',    'left'])

        util.checkSplit('AndSplit1',['left1','right1'])
        util.checkJoin ('Join1',    ['left1','right1'])
    }
}
