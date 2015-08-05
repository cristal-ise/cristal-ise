package org.cristalise.kernel.test.unit.workflow;

import static org.junit.Assert.*

import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.lifecycle.WfBuilder
import org.junit.After
import org.junit.Before
import org.junit.Test


class NestedCompActGenerationTests {

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

    def checkWorkflow() {
        util.checkActStatus("first",  [state: "Waiting", active: true])
        util.checkActStatus("second", [state: "Waiting", active: false])
        util.checkActStatus("third",  [state: "Waiting", active: false])
        util.checkActStatus("last",   [state: "Waiting", active: false])

        println "======================================================"
        util.requestAction("first", "Done")

        util.checkActStatus("first",  [state: "Finished", active: false])
        util.checkActStatus("second", [state: "Waiting",  active: true])
        util.checkActStatus("third",  [state: "Waiting",  active: false])
        util.checkActStatus("last",   [state: "Waiting",  active: false])
        
        println "======================================================"
        util.requestAction("second", "Done")

        util.checkActStatus("first",  [state: "Finished", active: false])
        util.checkActStatus("second", [state: "Finished", active: false])
        util.checkActStatus("third",  [state: "Waiting",  active: true])
        util.checkActStatus("last",   [state: "Waiting",  active: false])
        
        println "======================================================"
        util.requestAction("third", "Done")

        util.checkActStatus("first",  [state: "Finished", active: false])
        util.checkActStatus("second", [state: "Finished", active: false])
        util.checkActStatus("third",  [state: "Finished", active: false])
        util.checkActStatus("last",   [state: "Waiting",  active: true])

        println "======================================================"
        util.requestAction("last", "Done")

        util.checkActStatus("first",  [state: "Finished", active: false])
        util.checkActStatus("second", [state: "Finished", active: false])
        util.checkActStatus("third",  [state: "Finished", active: false])
        util.checkActStatus("last",   [state: "Finished", active: false])
    }

    @Test
    public void 'CompAct(first-second-third-last)'() {
        util.buildAndInitWf(false) {
            CompAct('ca') {
                ElemAct("first")
                ElemAct("second")
                ElemAct("third")
                ElemAct("last")
            }
        }

        util.checkActPath('workflow/domain'           ,'rootCA')
        util.checkActPath('workflow/domain/0'         ,'ca')
        util.checkActPath('workflow/domain/ca'        ,'ca')
        util.checkActPath('workflow/domain/ca/first'  ,'first')
        util.checkActPath('workflow/domain/ca/second' ,'second')
        util.checkActPath('workflow/domain/ca/third'  ,'third')
        util.checkActPath('workflow/domain/ca/last'   ,'last')

        util.checkNext('first',  'second')
        util.checkNext('second', 'third')
        util.checkNext('third',  'last')

        checkWorkflow()
    }

    @Test
    public void 'Block(CompAct(first-second-third-last))'() {
        util.buildAndInitWf(false) {
            Block {
                CompAct('ca') {
                    ElemAct("first")
                    ElemAct("second")
                    ElemAct("third")
                    ElemAct("last")
                }
            }
        }
        util.checkActPath('workflow/domain'           ,'rootCA')
        util.checkActPath('workflow/domain/0'         ,'ca')
        util.checkActPath('workflow/domain/ca'        ,'ca')
        util.checkActPath('workflow/domain/ca/first'  ,'first')
        util.checkActPath('workflow/domain/ca/second' ,'second')
        util.checkActPath('workflow/domain/ca/third'  ,'third')
        util.checkActPath('workflow/domain/ca/last'   ,'last')

        util.checkNext('first',  'second')
        util.checkNext('second', 'third')
        util.checkNext('third',  'last')

        checkWorkflow()
    }

    @Test
    public void 'Block(CompAct(first-Block(second-third-last)))'() {
        util.buildAndInitWf(false) {
            Block {
                CompAct('ca') {
                    ElemAct("first")
                    Block {
                        ElemAct("second")
                        ElemAct("third")
                        ElemAct("last")
                    }
                }
            }
        }
        util.checkActPath('workflow/domain'           ,'rootCA')
        util.checkActPath('workflow/domain/0'         ,'ca')
        util.checkActPath('workflow/domain/ca'        ,'ca')
        util.checkActPath('workflow/domain/ca/first'  ,'first')
        util.checkActPath('workflow/domain/ca/second' ,'second')
        util.checkActPath('workflow/domain/ca/third'  ,'third')
        util.checkActPath('workflow/domain/ca/last'   ,'last')

        util.checkNext('first',  'second')
        util.checkNext('second', 'third')
        util.checkNext('third',  'last')

        checkWorkflow()
    }

    @Test
    public void 'first-CompAct(second-third-last)'() {
        util.buildAndInitWf(false) {
            ElemAct("first")
            CompAct('ca') {
                ElemAct("second")
                ElemAct("third")
                ElemAct("last")
            }
        }

        util.checkNext('first',  'ca')
        util.checkNext('second', 'third')
        util.checkNext('third',  'last')

        checkWorkflow()
    }

    @Test
    public void 'CompAct(first-second-third)-last'() {
        util.buildAndInitWf(false) {
            CompAct('ca') {
                ElemAct("first")
                ElemAct("second")
                ElemAct("third")
            }
            ElemAct("last")
        }

        util.checkNext('ca', 'last')
        util.checkNext('first', 'second')
        util.checkNext('second', 'last')

        checkWorkflow()
    }

    @Test
    public void 'first-CompAct(second-third)-last'() {
        util.buildAndInitWf(false) {
            ElemAct("first")
            CompAct('ca') {
                ElemAct("second")
                ElemAct("third")
            }
            ElemAct("last")
        }

        util.checkNext('first',  'ca')
        util.checkNext('ca',     'last')
        util.checkNext('second', 'third')

        checkWorkflow()
    }

    @Test
    public void 'CompAct(CompAct(first-second-third-last))'() {
        util.buildAndInitWf(false) {
            CompAct('ca') {
                CompAct('ca1') {
                    ElemAct("first")
                    ElemAct("second")
                    ElemAct("third")
                    ElemAct("last")
                }
            }
        }

        util.checkNext('first',  'second')
        util.checkNext('second', 'third')
        util.checkNext('third',  'last')

        checkWorkflow()
    }

    @Test
    public void 'CompAct(first-CompAct(second-third-last))'() {
        util.buildAndInitWf(false) {
            CompAct('ca') {
                ElemAct("first")
                CompAct('ca1') {
                    ElemAct("second")
                    ElemAct("third")
                    ElemAct("last")
                }
            }
        }

        checkWorkflow()
    }

    @Test
    public void 'first-CompAct(second-CompAct(third-last))'() {
        util.buildAndInitWf(false) {
            ElemAct("first")
            CompAct {
                ElemAct("second")
                CompAct {
                    ElemAct("third")
                    ElemAct("last")
                }
            }
        }

        checkWorkflow()
    }

    @Test
    public void 'first-CompAct(second-CompAct(third))-last'() {
        util.buildAndInitWf(false) {
            ElemAct("first")
            CompAct {
                ElemAct("second")
                CompAct {
                    ElemAct("third")
                }
            }
            ElemAct("last")
        }

        checkWorkflow()
    }

    @Test
    public void 'CompAct(first-CompAct(second-CompAct(third)))-last'() {
        util.buildAndInitWf(false) {
            CompAct {
                ElemAct("first")
                CompAct {
                    ElemAct("second")
                    CompAct {
                        ElemAct("third")
                    }
                }
            }
            ElemAct("last")
        }

        checkWorkflow()
    }

    @Test
    public void 'CompAct(first-CompAct(second-CompAct(third))-last)'() {
        util.buildAndInitWf(false) {
            CompAct {
                ElemAct("first")
                CompAct {
                    ElemAct("second")
                    CompAct {
                        ElemAct("third")
                    }
                }
                ElemAct("last")
            }
        }

        checkWorkflow()
    }

    @Test
    public void 'CompAct(CompAct(first-CompAct(second-CompAct(third))-last))'() {
        util.buildAndInitWf(false) {
            CompAct {
                CompAct {
                    ElemAct("first")
                    CompAct {
                        ElemAct("second")
                        CompAct {
                            ElemAct("third")
                        }
                    }
                    ElemAct("last")
                }
            }
        }

        checkWorkflow()
    }

    @Test
    public void 'CompAct(first-CompAct(second)-CompAct(third))-last'() {
        util.buildAndInitWf(false) {
            CompAct {
                ElemAct("first")
                CompAct {
                    ElemAct("second")
                }
                CompAct {
                    ElemAct("third")
                }
            }
            ElemAct("last")
        }

        checkWorkflow()
    }

    @Test
    public void 'CompAct(first-second)-CompAct(third-last)'() {
        util.buildAndInitWf(false) {
            CompAct {
                ElemAct("first")
                ElemAct("second")
            }
            CompAct {
                ElemAct("third")
                ElemAct("last")
            }
        }

        checkWorkflow()
    }

    @Test
    public void 'first-CompAct(second)-third-CompAct(last)'() {
        util.buildAndInitWf(false) {
            ElemAct("first")
            CompAct {
                ElemAct("second")
            }
            ElemAct("third")
            CompAct {
                ElemAct("last")
            }
        }

        checkWorkflow()
    }
}
