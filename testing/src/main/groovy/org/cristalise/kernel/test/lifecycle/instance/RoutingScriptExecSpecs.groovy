package org.cristalise.kernel.test.lifecycle.instance;

import org.cristalise.dsl.persistency.outcome.OutcomeBuilder
import org.cristalise.dsl.persistency.outcome.SchemaBuilder
import org.cristalise.dsl.scripting.ScriptBuilder
import org.cristalise.dsl.test.builders.WorkflowTestBuilder
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.utils.CristalTestSetup

import spock.lang.Specification


class RoutingScriptExecSpecs extends Specification implements CristalTestSetup {

    static WorkflowTestBuilder wfBuilder

    def setupSpec() {
        //skips boostrap!!!
        inMemoryServer('src/main/bin/inMemoryServer.conf', 'src/main/bin/inMemory.clc', null, true)
        wfBuilder = new WorkflowTestBuilder()
    }

    def cleanup() {
        println Gateway.getMarshaller().marshall(wfBuilder.wf)
    }
    
    def cleanupSpec() {
        cristalCleanup()
    }

    def 'OrSplit enables branch(es) using RoutingScript and ActivityDataHelper with relative path'() {
        given: "Wf = first-OrSplit(CounterScript)((left)(right))-last"

        String module = 'integTest'
        String schemaName = 'TestData'

        ScriptBuilder.create(module, "CounterScript01", 0) {
            input("counter", "java.lang.String")
            output('java.lang.Integer')
            javascript { "new java.lang.Integer(counter % 2);" }
        }

        def schema = SchemaBuilder.create(module, schemaName, 0, "src/main/data/${schemaName}.xsd")

        wfBuilder.buildAndInitWf() {
            ElemAct("first") {
                Property(SchemaType: schemaName, SchemaVersion: 0, Viewpoint: 'last')
            }
            OrSplit(RoutingScriptName: 'CounterScript01', RoutingScriptVersion: 0) {
                Property(counter: "activity//./first:/TestData/counter")

                Block { ElemAct("left")  }
                Block { ElemAct("right") }
            }
            ElemAct("last")
        }

        when: "requesting ElemAct(first) Done transition"
        wfBuilder.requestAction("first", "Done", OutcomeBuilder.build(schema) {counter 3})

        then: "EA(left) is enabled but EA(right) is disabled"
        wfBuilder.checkActStatus("left",  [state: "Waiting", active: true])
        wfBuilder.checkActStatus("right", [state: "Waiting", active: false])
        wfBuilder.checkActStatus("last",  [state: "Waiting", active: false])
    }

    def 'OrSplit enables branch(es) using RoutingScript and ActivityDataHelper with absolute path'() {
        given: "Wf = first-OrSplit(CounterScript)((left)(right))-last"

        String module = 'integTest'
        String schemaName = 'TestData'

        ScriptBuilder.create(module, "CounterScript02", 0) {
            input("counter", "java.lang.String")
            output('java.lang.Integer')
            javascript { "new java.lang.Integer(counter % 2);" }
        }

        def schema = SchemaBuilder.create(module, schemaName, 0, "src/main/data/${schemaName}.xsd")

        wfBuilder.buildAndInitWf() {
            ElemAct("first") {
                Property(SchemaType: schemaName, SchemaVersion: 0, Viewpoint: 'last')
            }
            OrSplit(RoutingScriptName: 'CounterScript02', RoutingScriptVersion: 0) {
                Property(counter: "activity//workflow/domain/first:/TestData/counter")

                Block { ElemAct("left")  }
                Block { ElemAct("right") }
            }
            ElemAct("last")
        }

        when: "requesting ElemAct(first) Done transition"
        wfBuilder.requestAction("first", "Done", OutcomeBuilder.build(schema) {counter 3})

        then: "EA(left) is enabled but EA(right) is disabled"
        wfBuilder.checkActStatus("left",  [state: "Waiting", active: true])
        wfBuilder.checkActStatus("right", [state: "Waiting", active: false])
        wfBuilder.checkActStatus("last",  [state: "Waiting", active: false])
    }

    def 'LoopSplit using RoutingScript and ActivityDataHelper'() {
        given: "Wf = Loop(incrementer)"

        String module = 'integTest'
        String schemaName = 'TestData'

        ScriptBuilder.create(module, "CounterScript03", 0) {
            input("counter", "java.lang.String")
            output('java.lang.Boolean')
            javascript { "counter < 10;" }
        }

        def schema = SchemaBuilder.create(module, schemaName, 0, "src/main/data/${schemaName}.xsd")

        wfBuilder.buildAndInitWf {
            Loop(RoutingScriptName: 'CounterScript03', RoutingScriptVersion: 0) {
                Property(counter: "activity//./incrementer:/TestData/counter")

                ElemAct("incrementer")  {
                    Property(SchemaType: schemaName, SchemaVersion: 0, Viewpoint: 'last')
                }
            }
            ElemAct("last")
        }

        for(i in (0..10)) {
            when: "requesting ElemAct(incrementer) Done transition"
            wfBuilder.requestAction("incrementer", "Done", OutcomeBuilder.build(schema) {counter i})

            then: "EA(incrementer) is enabled, i=$i"
            wfBuilder.checkActStatus("incrementer",  [state: (i < 10)?"Waiting":"Finished", active: (i < 10)])
        }

        wfBuilder.checkActStatus("incrementer", [state: "Finished", active: false])
        wfBuilder.checkActStatus("last",        [state: "Waiting", active: true])
    }

    def 'RoutingScript can only use String for input parameter'() {
        given: "Wf = first-OrSplit(CounterScript)((left)(right))-last"

        String module = 'integTest'
        String schemaName = 'TestData'

        ScriptBuilder.create(module, "CounterScript04", 0) {
            input("counter", "java.lang.Integer")
            output('java.lang.Integer')
            javascript { "new java.lang.Integer(counter % 2);" }
        }

        def schema = SchemaBuilder.create(module, schemaName, 0, "src/main/data/${schemaName}.xsd")

        wfBuilder.buildAndInitWf() {
            ElemAct("first") {
                Property(SchemaType: schemaName, SchemaVersion: 0, Viewpoint: 'last')
            }
            OrSplit(RoutingScriptName: 'CounterScript04', RoutingScriptVersion: 0) {
                Property(counter: "activity//./first:/TestData/counter")

                Block { ElemAct("left")  }
                Block { ElemAct("right") }
            }
            ElemAct("last")
        }

        when: "requesting ElemAct(first) Done transition"
        wfBuilder.requestAction("first", "Done", OutcomeBuilder.build(schema) {counter 3})

        then: "InvalidDataException is thrown"
        thrown InvalidDataException
    }
}
