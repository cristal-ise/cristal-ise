package org.cristalise.kernel.test.lifecycle.instance;

//import static org.cristalise.kernel.lifecycle.instance.WfVertex.Types.*

import org.cristalise.dsl.persistency.outcome.OutcomeBuilder
import org.cristalise.dsl.persistency.outcome.SchemaBuilder
import org.cristalise.dsl.scripting.ScriptBuilder
import org.cristalise.dsl.test.lifecycle.instance.WorkflowTestBuilder
import org.cristalise.kernel.process.Gateway
import org.cristalise.test.CristalTestSetup

import spock.lang.Specification


class RoutingScriptExecSpecs extends Specification implements CristalTestSetup {

    WorkflowTestBuilder wfBuilder

    def setup() {
        inMemoryServer()
        wfBuilder = new WorkflowTestBuilder()
    }

    def cleanup() {
        println Gateway.getMarshaller().marshall(wfBuilder.wf)
        cristalCleanup()
    }

    def 'OrSplit enables branch(es) using RoutingScript'() {
        given: "Wf = first-OrSplit(script:1)((left)(right))-last"
        
        String module = 'testing'
        String schemaName = 'TestData'

        ScriptBuilder.create(module, "CounterScript", 0) {
            input("counter", "java.lang.String")
            output('java.lang.Integer')
            javascript { "java.lang.Integer(counter % 2);" }
        }

        def schema = SchemaBuilder.create(module, schemaName, 0) { loadXSD("src/test/data/${schemaName}.xsd") }

        wfBuilder.buildAndInitWf() {
            ElemAct("first") {
                Property(SchemaType: schemaName, SchemaVersion: 0, Viewpoint: 'last')
            }
            OrSplit(RoutingScriptName: 'CounterScript', RoutingScriptVersion: 0) {
                Property(counter: "activity//./workflow/domain/first:/${schemaName}/counter")

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

        when: "requesting EA(left) Done transition"
        wfBuilder.requestAction("left", "Done")

        then: "EA(left) is Finished but EA(right) is disabled"
        wfBuilder.checkActStatus("left",  [state: "Finished", active: false])
        wfBuilder.checkActStatus("right", [state: "Waiting",  active: false])
        wfBuilder.checkActStatus("last",  [state: "Waiting",  active: true])
        
        when: "requesting EA(last) Done transition"
        wfBuilder.requestAction("last", "Done")

        then: "EA(last) is Finished and disabled"
        wfBuilder.checkActStatus("left",  [state: "Finished", active: false])
        wfBuilder.checkActStatus("right", [state: "Waiting",  active: false])
        wfBuilder.checkActStatus("last",  [state: "Finished", active: false])
    }
}
