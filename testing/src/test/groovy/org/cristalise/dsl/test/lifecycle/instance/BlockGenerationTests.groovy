/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
package org.cristalise.dsl.test.lifecycle.instance

import static org.junit.Assert.*

import org.cristalise.test.CristalTestSetup
import org.junit.After
import org.junit.Before
import org.junit.Test


class BlockGenerationTests implements CristalTestSetup {

    WorkflowTestBuilder util

    @Before
    public void setup() {
        inMemorySetup()
        util = new WorkflowTestBuilder()
    }

    @After
    public void cleanup() {
        //println Gateway.getMarshaller().marshall(util.wf)
        cristalCleanup()
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
