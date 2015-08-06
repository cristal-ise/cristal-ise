package org.cristalise.dsl.test.lifecycle.instance;

import static org.junit.Assert.*

import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway
import org.junit.After
import org.junit.Before
import org.junit.Test


class NestedCompActGenerationTests {

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
        Gateway.close()
    }


    def checkSeq() {
        util.checkActPath('rootCA', 'workflow/domain')
        util.checkActPath('ca',     'workflow/domain/0')
        util.checkActPath('ca',     'workflow/domain/ca')
        util.checkActPath('first',  'workflow/domain/ca/first')
        util.checkActPath('second', 'workflow/domain/ca/second')
        util.checkActPath('third',  'workflow/domain/ca/third')
        util.checkActPath('last',   'workflow/domain/ca/last')

        util.checkSequence('first', 'second', 'third', 'last')
    }

    @Test
    public void 'CompAct(first-second-third-last)'() {
        util.build {
            CompAct('ca') {
                ElemAct("first")
                ElemAct("second")
                ElemAct("third")
                ElemAct("last")
            }
        }
        checkSeq()
    }

    @Test
    public void 'Block(CompAct(first-second-third-last))'() {
        util.build {
            Block {
                CompAct('ca') {
                    ElemAct("first")
                    ElemAct("second")
                    ElemAct("third")
                    ElemAct("last")
                }
            }
        }
        checkSeq()
    }

    @Test
    public void 'Block(CompAct(first-Block(second-third-last)))'() {
        util.build {
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
        checkSeq()
    }

    @Test
    public void 'first-CompAct(second-third-last)'() {
        util.build {
            ElemAct("first")
            CompAct('ca') {
                ElemAct("second")
                ElemAct("third")
                ElemAct("last")
            }
        }
        util.checkActPath('first',  'workflow/domain/0')
        util.checkActPath('ca',     'workflow/domain/ca')
        util.checkActPath('first',  'workflow/domain/first')
        util.checkActPath('second', 'workflow/domain/ca/second')
        util.checkActPath('third',  'workflow/domain/ca/third')
        util.checkActPath('last',   'workflow/domain/ca/last')

        util.checkNext('first', 'ca')
        util.checkSequence('second', 'third', 'last')
    }

    @Test
    public void 'CompAct(first-second-third)-last'() {
        util.build {
            CompAct('ca') {
                ElemAct("first")
                ElemAct("second")
                ElemAct("third")
            }
            ElemAct("last")
        }

        util.checkActPath('ca',     'workflow/domain/ca')
        util.checkActPath('first',  'workflow/domain/ca/first')
        util.checkActPath('second', 'workflow/domain/ca/second')
        util.checkActPath('third',  'workflow/domain/ca/third')
        util.checkActPath('last',   'workflow/domain/last')

        util.checkNext('ca', 'last')
        util.checkSequence('first', 'second', 'third')
    }

    @Test
    public void 'first-CompAct(second-third)-last'() {
        util.build {
            ElemAct("first")
            CompAct('ca') {
                ElemAct("second")
                ElemAct("third")
            }
            ElemAct("last")
        }
        util.checkActPath('ca',     'workflow/domain/ca')
        util.checkActPath('first',  'workflow/domain/first')
        util.checkActPath('second', 'workflow/domain/ca/second')
        util.checkActPath('third',  'workflow/domain/ca/third')
        util.checkActPath('last',   'workflow/domain/last')

        util.checkSequence('first', 'ca','last')
        util.checkNext('second', 'third')
    }

    @Test
    public void 'CompAct(CompAct(first-second-third-last))'() {
        util.build {
            CompAct('ca') {
                CompAct('ca1') {
                    ElemAct("first")
                    ElemAct("second")
                    ElemAct("third")
                    ElemAct("last")
                }
            }
        }
        util.checkActPath('ca',     'workflow/domain/ca')
        util.checkActPath('ca1',    'workflow/domain/ca/ca1')
        util.checkActPath('first',  'workflow/domain/ca/ca1/first')
        util.checkActPath('second', 'workflow/domain/ca/ca1/second')
        util.checkActPath('third',  'workflow/domain/ca/ca1/third')
        util.checkActPath('last',   'workflow/domain/ca/ca1/last')

        util.checkSequence('first', 'second', 'third', 'last')
    }

    @Test
    public void 'CompAct(first-CompAct(second-third-last))'() {
        util.build {
            CompAct('ca') {
                ElemAct("first")
                CompAct('ca1') {
                    ElemAct("second")
                    ElemAct("third")
                    ElemAct("last")
                }
            }
        }
        util.checkActPath('ca',     'workflow/domain/ca')
        util.checkActPath('ca1',    'workflow/domain/ca/ca1')
        util.checkActPath('first',  'workflow/domain/ca/first')
        util.checkActPath('second', 'workflow/domain/ca/ca1/second')
        util.checkActPath('third',  'workflow/domain/ca/ca1/third')
        util.checkActPath('last',   'workflow/domain/ca/ca1/last')

        util.checkSequence('second', 'third', 'last')
    }


    @Test
    public void 'CompAct(CompAct(first-CompAct(second-CompAct(third))-last))'() {
        util.build {
            CompAct('ca') {
                CompAct('ca1') {
                    ElemAct("first")
                    CompAct('ca2') {
                        ElemAct("second")
                        CompAct('ca3') {
                            ElemAct("third")
                        }
                    }
                    ElemAct("last")
                }
            }
        }
        util.checkActPath('ca',     'workflow/domain/ca')
        util.checkActPath('ca1',    'workflow/domain/ca/ca1')
        util.checkActPath('ca2',    'workflow/domain/ca/ca1/ca2')
        util.checkActPath('ca3',    'workflow/domain/ca/ca1/ca2/ca3')
        util.checkActPath('first',  'workflow/domain/ca/ca1/first')
        util.checkActPath('second', 'workflow/domain/ca/ca1/ca2/second')
        util.checkActPath('third',  'workflow/domain/ca/ca1/ca2/ca3/third')
        util.checkActPath('last',   'workflow/domain/ca/ca1/last')

        util.checkSequence('first',  'ca2', 'last')
        util.checkSequence('second', 'ca3')
    }

    @Test
    public void 'CompAct(first-second)-CompAct(third-last)'() {
        util.build {
            CompAct('ca1') {
                ElemAct("first")
                ElemAct("second")
            }
            CompAct('ca2') {
                ElemAct("third")
                ElemAct("last")
            }
        }
        util.checkActPath('ca1',    'workflow/domain/ca1')
        util.checkActPath('ca2',    'workflow/domain/ca2')
        util.checkActPath('first',  'workflow/domain/ca1/first')
        util.checkActPath('second', 'workflow/domain/ca1/second')
        util.checkActPath('third',  'workflow/domain/ca2/third')
        util.checkActPath('last',   'workflow/domain/ca2/last')
       
        util.checkSequence('first', 'second')
        util.checkSequence('third', 'last')
        util.checkSequence('ca1',   'ca2')
    }
}
