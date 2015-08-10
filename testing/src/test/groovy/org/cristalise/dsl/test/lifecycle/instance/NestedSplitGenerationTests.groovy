package org.cristalise.dsl.test.lifecycle.instance;

import static org.junit.Assert.*

import org.cristalise.dsl.lifecycle.instance.SplitDelegate
import org.cristalise.kernel.lifecycle.instance.WfVertex.Types
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

    def createAndCheck_Split(Types type) {
        util.build {
            "$type" {
                Block { ElemAct("left")  }
                Block { ElemAct("right") }
            }
        }
        util.checkActPath('right', 'workflow/domain/right')
        util.checkActPath('left',  'workflow/domain/left')

        def splitName = SplitDelegate.getNamePrefix(type)
        def joinName = splitName.replace('Split', 'Join')
        
        util.checkSplit(splitName, ['left','right'])
        util.checkJoin (joinName,  ['left','right'])
    }

    def createAndCheck_Act_Split_Act(Types type) {
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

        def splitName = SplitDelegate.getNamePrefix(type)
        def joinName = splitName.replace('Split', 'Join')

        util.checkSequence('first', splitName)
        util.checkSequence(joinName, 'last')

        util.checkSplit(splitName, ['left','right'])
        util.checkJoin (joinName,  ['left','right'])
    }

    @Test
    public void 'Split((left)(right))'() {
        createAndCheck_Split( Types.AndSplit )
        createAndCheck_Split( Types.OrSplit )
        createAndCheck_Split( Types.XOrSplit )
    }

    @Test
    public void 'first-Split((left)(right))-last'() {
        createAndCheck_Act_Split_Act( Types.AndSplit )
        createAndCheck_Act_Split_Act( Types.OrSplit )
        createAndCheck_Act_Split_Act( Types.XOrSplit )
    }

    @Test
    public void 'Loop((first)))'() {
        util.build {
            Loop {
                B{ ElemAct("first") }
            }
        }
        util.checkActPath('first', 'workflow/domain/first')

        util.checkSplit('LoopSplit', ['first','LoopJoin'])
        util.checkJoin ('LoopJoin',  ['first','LoopSplit'])
    }

    @Test
    public void 'first-Block(And((inner)))-last'() {
        util.build {
            EA("first")
            Block {
                AndSplit {
                    B{ EA("left") }
                    B{ CA("right") {} }
                }
            }
            EA("last")
        }
        util.checkActPath('first', 'workflow/domain/first')
        util.checkActPath('left',  'workflow/domain/left')
        util.checkActPath('right', 'workflow/domain/right')
        util.checkActPath('last',  'workflow/domain/last')

        util.checkSplit('AndSplit', ['left','right'])
        util.checkJoin ('AndJoin',  ['left','right'])

        util.checkSequence('first','AndSplit')
        util.checkSequence('AndJoin','last')
    }

    @Test
    public void 'AndSplit(AndSplit((left1)(right1))(right))'() {
        util.build {
            AndSplit() {
                Block {
                    AndSplit {
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

        util.checkSplit('AndSplit', ['AndSplit1', 'right'])
        util.checkJoin ('AndJoin',  ['AndJoin1',  'right'])

        util.checkSplit('AndSplit1', ['left1','right1'])
        util.checkJoin ('AndJoin1',  ['left1','right1'])
    }

    @Test
    public void 'AndSplit((left)AndSplit((left1)(right1)))'() {
        util.build {
            AndSplit {
                Block { ElemAct("left") }
                Block {
                    AndSplit {
                        Block { ElemAct("left1") }
                        Block { ElemAct("right1") }
                    }
                }
            }
        }

        util.checkActPath('left',   'workflow/domain/left')
        util.checkActPath('left1',  'workflow/domain/left1')
        util.checkActPath('right1', 'workflow/domain/right1')

        util.checkSplit('AndSplit', ['AndSplit1', 'left'])
        util.checkJoin ('AndJoin',  ['AndJoin1',  'left'])

        util.checkSplit('AndSplit1', ['left1','right1'])
        util.checkJoin ('AndJoin1',  ['left1','right1'])
    }
}
