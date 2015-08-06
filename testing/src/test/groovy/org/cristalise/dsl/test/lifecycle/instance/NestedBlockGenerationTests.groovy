package org.cristalise.dsl.test.lifecycle.instance

import static org.junit.Assert.*

import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway
import org.junit.After
import org.junit.Before
import org.junit.Test


class NestedBlockGenerationTests {

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

    def checkStructure() {
        util.checkActPath('rootCA', 'workflow/domain')
        util.checkActPath('first',  'workflow/domain/first')
        util.checkActPath('second', 'workflow/domain/second')
        util.checkActPath('third',  'workflow/domain/third')
        util.checkActPath('last',   'workflow/domain/last')

        util.checkSequence('first', 'second', 'third', 'last')
    }

    @Test
    public void 'first-second-third-last'() {
        //There is an implicit Block/CompAct created
        util.build {
            ElemAct("first")
            ElemAct("second")
            ElemAct("third")
            ElemAct("last")
        }

        checkStructure()
    }

    @Test
    public void 'Block(first-second-third-last)'() {
        util.build {
            Block {
                ElemAct("first")
                ElemAct("second")
                ElemAct("third")
                ElemAct("last")
            }
        }

        checkStructure()
    }

    @Test
    public void 'first-Block(second-third-last)'() {
        util.build {
            ElemAct("first")
            Block {
                ElemAct("second")
                ElemAct("third")
                ElemAct("last")
            }
        }

        checkStructure()
    }

    @Test
    public void 'Block(first-second-third)-last'() {
        util.build {
            Block {
                ElemAct("first")
                ElemAct("second")
                ElemAct("third")
            }
            ElemAct("last")
        }

        checkStructure()
    }

    @Test
    public void 'first-Block(second-third)-last'() {
        util.build {
            ElemAct("first")
            Block {
                ElemAct("second")
                ElemAct("third")
            }
            ElemAct("last")
        }

        checkStructure()
    }

    @Test
    public void 'Block(Block(first-second-third-last))'() {
        util.build {
            Block {
                Block {
                    ElemAct("first")
                    ElemAct("second")
                    ElemAct("third")
                    ElemAct("last")
                }
            }
        }

        checkStructure()
    }

    @Test
    public void 'Block(first-Block(second-third-last))'() {
        util.build {
            Block {
                ElemAct("first")
                Block {
                    ElemAct("second")
                    ElemAct("third")
                    ElemAct("last")
                }
            }
        }

        checkStructure()
    }

    @Test
    public void 'first-Block(second-Block(third-last))'() {
        util.build {
            ElemAct("first")
            Block {
                ElemAct("second")
                Block {
                    ElemAct("third")
                    ElemAct("last")
                }
            }
        }

        checkStructure()
    }

    @Test
    public void 'first-Block(second-Block(third))-last'() {
        util.build {
            ElemAct("first")
            Block {
                ElemAct("second")
                Block {
                    ElemAct("third")
                }
            }
            ElemAct("last")
        }

        checkStructure()
    }

    @Test
    public void 'Block(first-Block(second-Block(third)))-last'() {
        util.build {
            Block {
                ElemAct("first")
                Block {
                    ElemAct("second")
                    Block {
                        ElemAct("third")
                    }
                }
            }
            ElemAct("last")
        }

        checkStructure()
    }

    @Test
    public void 'Block(first-Block(second-Block(third))-last)'() {
        util.build {
            Block {
                ElemAct("first")
                Block {
                    ElemAct("second")
                    Block {
                        ElemAct("third")
                    }
                }
                ElemAct("last")
            }
        }

        checkStructure()
    }

    @Test
    public void 'Block(Block(first-Block(second-Block(third))-last))'() {
        util.build {
            Block {
                Block {
                    ElemAct("first")
                    Block {
                        ElemAct("second")
                        Block {
                            ElemAct("third")
                        }
                    }
                    ElemAct("last")
                }
            }
        }

        checkStructure()
    }

    @Test
    public void 'Block(first-Block(second)-Block(third))-last'() {
        util.build {
            Block {
                ElemAct("first")
                Block {
                    ElemAct("second")
                }
                Block {
                    ElemAct("third")
                }
            }
            ElemAct("last")
        }

        checkStructure()
    }

    @Test
    public void 'first-Block(second)-third-Block(last)'() {
        util.build {
            ElemAct("first")
            Block { 
                ElemAct("second")
            }
            ElemAct("third")
            Block {
                ElemAct("last")
            }
        }

        checkStructure()
    }

    @Test
    public void 'Block(first-second)-Block(third-last)'() {
        util.build {
            Block {
                ElemAct("first")
                ElemAct("second")
            }
            Block {
                ElemAct("third")
                ElemAct("last")
            }
        }

        checkStructure()
    }
}
