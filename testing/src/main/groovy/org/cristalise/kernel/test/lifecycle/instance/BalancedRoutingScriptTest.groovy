package org.cristalise.kernel.test.lifecycle.instance;

//import static org.cristalise.kernel.lifecycle.instance.WfVertex.Types.*

import org.cristalise.dsl.persistency.outcome.OutcomeBuilder
import org.cristalise.dsl.persistency.outcome.SchemaBuilder
import org.cristalise.dsl.scripting.ScriptBuilder
import org.cristalise.dsl.test.builders.WorkflowTestBuilder
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.utils.CristalTestSetup
import org.junit.After
import org.junit.Before
import org.junit.Test

import spock.lang.Specification


class BalancedRoutingScriptTest extends Specification implements CristalTestSetup {

    static WorkflowTestBuilder wfBuilder

    def setupSpec() {
        inMemoryServer('src/main/bin/inMemoryServer.conf', 'src/main/bin/inMemory.clc', null, true)
        wfBuilder = new WorkflowTestBuilder()
    }

    def cleanup() {
        if (wfBuilder && wfBuilder.wf) println Gateway.getMarshaller().marshall(wfBuilder.wf)
    }

    def cleanupSpec() {
        if (wfBuilder && wfBuilder.wf) println Gateway.getMarshaller().marshall(wfBuilder.wf)
        cristalCleanup()
    }

    def 'Loop-OrSplit-AndSPlit using RoutingScript'() {
        given:
        String module = 'integTest'
        String schemaName = 'TestData'
        int repetition = 10

        ScriptBuilder.create(module, "LoopScript", 0) {
            input("counter", "java.lang.String")
            output('java.lang.Boolean')
            javascript { "counter < $repetition;" }
        }

        ScriptBuilder.create(module, "OrSplitScript", 0) {
            input("counter", "java.lang.String")
            output('java.lang.Integer')
            javascript { "new java.lang.Integer(counter % 2 + 1);" }
        }

        def schema = SchemaBuilder.create(module, schemaName, 0) { 
            struct(name: schemaName) {
                field(name:'counter', type: 'integer')
            }
        }

        wfBuilder.buildAndInitWf {
            Loop(RoutingScriptName: 'LoopScript', RoutingScriptVersion: 0) {
                Property(counter: "activity//./incrementer:/TestData/counter")

                ElemAct("incrementer")  {
                    Property(SchemaType: schemaName, SchemaVersion: 0, Viewpoint: 'last')
                }

                OrSplit(RoutingScriptName: 'OrSplitScript', RoutingScriptVersion: 0) {
                    Property(counter: "activity//./incrementer:/TestData/counter")

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

        when:
        for(i in (0..repetition)) {
            wfBuilder.requestAction("incrementer", "Done", OutcomeBuilder.build(schema) {counter i})

            if(i % 2) {
                wfBuilder.requestAction("left", "Done")
            }
            else {
                wfBuilder.requestAction("right-right", "Done")
                wfBuilder.requestAction("right-left" , "Done")
            }

            wfBuilder.checkActStatus("last",  [state: "Waiting", active: !(i < repetition)])
        }

        wfBuilder.requestAction("last", "Done")

        then:
        wfBuilder.checkActStatus("last",  [state: "Finished", active: false])
    }
}
