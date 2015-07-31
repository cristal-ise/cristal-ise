package org.cristalise.kernel.test.unit.workflow;

import static org.junit.Assert.*

import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.lifecycle.WfBuilder
import org.junit.After
import org.junit.Before
import org.junit.Test


class NestedCompActExecTests {

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
        util.checkActStatus("last",   [state: "Finished", active: false])
    }

    @Test
    public void 'CompAct(first-second-third-last)'() {
        util.buildWf(false) {
            CompAct('ca1') {
                ElemAct("first")
                ElemAct("second")
                ElemAct("third")
                ElemAct("last")
            }
        }

        checkWf()
    }

    @Test
    public void 'first-CompAct(second-third-last)'() {
        util.buildWf(false) {
            ElemAct("first")
            CompAct {
                ElemAct("second")
                ElemAct("third")
                ElemAct("last")
            }
        }

        checkWf()
    }

    @Test
    public void 'CompAct(first-second-third)-last'() {
        util.buildWf(false) {
            CompAct {
                ElemAct("first")
                ElemAct("second")
                ElemAct("third")
            }
            ElemAct("last")
        }

        checkWf()
    }

    @Test
    public void 'first-CompAct(second-third)-last'() {
        util.buildWf(false) {
            ElemAct("first")
            CompAct {
                ElemAct("second")
                ElemAct("third")
            }
            ElemAct("last")
        }

        checkWf()
    }

    @Test
    public void 'CompAct(CompAct(first-second-third-last))'() {
        util.buildWf(false) {
            CompAct {
                CompAct {
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
    public void 'CompAct(first-CompAct(second-third-last))'() {
        util.buildWf(false) {
            CompAct {
                ElemAct("first")
                CompAct {
                    ElemAct("second")
                    ElemAct("third")
                    ElemAct("last")
                }
            }
        }

        checkWf()
    }

    @Test
    public void 'first-CompAct(second-CompAct(third-last))'() {
        util.buildWf(false) {
            ElemAct("first")
            CompAct {
                ElemAct("second")
                CompAct {
                    ElemAct("third")
                    ElemAct("last")
                }
            }
        }

        checkWf()
    }

    @Test
    public void 'first-CompAct(second-CompAct(third))-last'() {
        util.buildWf(false) {
            ElemAct("first")
            CompAct {
                ElemAct("second")
                CompAct {
                    ElemAct("third")
                }
            }
            ElemAct("last")
        }

        checkWf()
    }

    @Test
    public void 'CompAct(first-CompAct(second-CompAct(third)))-last'() {
        util.buildWf(false) {
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

        checkWf()
    }

    @Test
    public void 'CompAct(first-CompAct(second)-CompAct(third))-last'() {
        util.buildWf(false) {
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

        checkWf()
    }
}
