package org.cristalise.kernel.test.unit.workflow;

import static org.junit.Assert.*

import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.lifecycle.WfBuilder
import org.junit.After
import org.junit.Before
import org.junit.Test


class NestedBlockGenerationTests {

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
    
    def checkWf() {
        util.checkActStatus("first",  [state: "Waiting", active: true])
        util.checkActStatus("second", [state: "Waiting", active: false])
        util.checkActStatus("third",  [state: "Waiting", active: false])
        util.checkActStatus("last",   [state: "Waiting", active: false])

        util.requestAction("first", "Done")

        util.checkActStatus("first",  [state: "Finished", active: false])
        util.checkActStatus("second", [state: "Waiting",  active: true])
        util.checkActStatus("third",  [state: "Waiting",  active: false])
        util.checkActStatus("last",   [state: "Waiting",  active: false])
        
        util.requestAction("second", "Done")

        util.checkActStatus("first",  [state: "Finished", active: false])
        util.checkActStatus("second", [state: "Finished", active: false])
        util.checkActStatus("third",  [state: "Waiting",  active: true])
        util.checkActStatus("last",   [state: "Waiting",  active: false])
        
        util.requestAction("third", "Done")

        util.checkActStatus("first",  [state: "Finished", active: false])
        util.checkActStatus("second", [state: "Finished", active: false])
        util.checkActStatus("third",  [state: "Finished", active: false])
        util.checkActStatus("last",   [state: "Waiting",  active: true])

        util.requestAction("last", "Done")

        util.checkActStatus("first",  [state: "Finished", active: false])
        util.checkActStatus("second", [state: "Finished", active: false])
        util.checkActStatus("third",  [state: "Finished", active: false])
        util.checkActStatus("last",   [state: "Finished", active: true])

    }

    @Test
    public void 'Block(first-second-third-last)'() {
        util.buildWf(false) {
            Block {
                ElemAct("first")
                ElemAct("second")
                ElemAct("third")
                ElemAct("last")
            }
        }

        checkWf()
    }

    @Test
    public void 'first-Block(second-third-last)'() {
        util.buildWf(false) {
            ElemAct("first")
            Block {
                ElemAct("second")
                ElemAct("third")
                ElemAct("last")
            }
        }

        checkWf()
    }

    @Test
    public void 'Block(first-second-third)-last'() {
        util.buildWf(false) {
            Block {
                ElemAct("first")
                ElemAct("second")
                ElemAct("third")
            }
            ElemAct("last")
        }

        checkWf()
    }

    @Test
    public void 'first-Block(second-third)-last'() {
        util.buildWf(false) {
            ElemAct("first")
            Block {
                ElemAct("second")
                ElemAct("third")
            }
            ElemAct("last")
        }

        checkWf()
    }

    @Test
    public void 'Block(Block(first-second-third-last))'() {
        util.buildWf(false) {
            Block {
                Block {
                    ElemAct("first")
                    ElemAct("second")
                    ElemAct("third")
                    ElemAct("last")
                }
            }
        }

        checkWf()
    }

    @Test
    public void 'Block(first-Block(second-third-last))'() {
        util.buildWf(false) {
            Block {
                ElemAct("first")
                Block {
                    ElemAct("second")
                    ElemAct("third")
                    ElemAct("last")
                }
            }
        }

        checkWf()
    }

    @Test
    public void 'first-Block(second-Block(third-last))'() {
        util.buildWf(false) {
            ElemAct("first")
            Block {
                ElemAct("second")
                Block {
                    ElemAct("third")
                    ElemAct("last")
                }
            }
        }

        checkWf()
    }

    @Test
    public void 'first-Block(second-Block(third))-last'() {
        util.buildWf(false) {
            ElemAct("first")
            Block {
                ElemAct("second")
                Block {
                    ElemAct("third")
                }
            }
            ElemAct("last")
        }

        checkWf()
    }

    @Test
    public void 'Block(first-Block(second-Block(third)))-last'() {
        util.buildWf(false) {
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

        checkWf()
    }

    @Test
    public void 'Block(first-Block(second)-Block(third))-last'() {
        util.buildWf(false) {
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

        checkWf()
    }
}
