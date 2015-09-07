package org.cristalise.kernel.test.lifecycle.instance;

//import static org.cristalise.kernel.lifecycle.instance.WfVertex.Types.*

import org.cristalise.dsl.persistency.outcome.OutcomeBuilder
import org.cristalise.dsl.persistency.outcome.SchemaBuilder
import org.cristalise.dsl.scripting.ScriptBuilder
import org.cristalise.dsl.test.lifecycle.instance.WorkflowTestBuilder
import org.cristalise.test.CristalTestSetup
import org.junit.After
import org.junit.Before
import org.junit.Test


class BalancedRoutingScriptTest implements CristalTestSetup {

    WorkflowTestBuilder wfBuilder

    @Before
    public void setup() {
        inMemoryServer(1)
        wfBuilder = new WorkflowTestBuilder()
    }

    @After
    public void cleanup() {
        cristalCleanup()
    }

    @Test
    public void 'Loop-OrSplit-AndSPlit using RoutingScript'() {
        String module = 'testing'
        String schemaName = 'TestData'

        ScriptBuilder.create(module, "LoopScript", 0) {
            input("counter", "java.lang.String")
            output('java.lang.Boolean')
            javascript { "counter < 1000;" }
        }

        ScriptBuilder.create(module, "OrSplitScript", 0) {
            input("counter", "java.lang.String")
            output('java.lang.Integer')
            javascript { "java.lang.Integer(counter % 2 + 1);" }
        }

        def schema = SchemaBuilder.create(module, schemaName, 0) { 
            struct(name: schemaName) {
                field(name:'counter', type: 'integer')
            }
        }

        wfBuilder.buildAndInitWf {
            Loop(RoutingScriptName: 'LoopScript', RoutingScriptVersion: 0) {
                Property(counter: "activity//./workflow/domain/incrementer:/TestData/counter")

                ElemAct("incrementer")  {
                    Property(SchemaType: schemaName, SchemaVersion: 0, Viewpoint: 'last')
                }

                OrSplit(RoutingScriptName: 'OrSplitScript', RoutingScriptVersion: 0) {
                    Property(counter: "activity//./workflow/domain/incrementer:/TestData/counter")

                    Block {
                        AndSplit {
                            B { ElemAct("right-right") }
                            B { ElemAct("right-left") }
                        }
                    }
                    Block {
                        ElemAct("left")
                    }
                }
            }
            ElemAct("last")
        }

        for(i in (0..1000)) {
            wfBuilder.requestAction("incrementer", "Done", OutcomeBuilder.build(schema) {counter i})

            if(i % 2) {
                wfBuilder.requestAction("left", "Done")
            }
            else {
                wfBuilder.requestAction("right-right", "Done")
                wfBuilder.requestAction("right-left" , "Done")
            }

            wfBuilder.checkActStatus("last",  [state: "Waiting", active: !(i < 1000)])
        }

        wfBuilder.requestAction("last", "Done")
        wfBuilder.checkActStatus("last",  [state: "Finished", active: true])
    }

}
