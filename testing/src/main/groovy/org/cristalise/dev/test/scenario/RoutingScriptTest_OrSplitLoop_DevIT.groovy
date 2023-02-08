package org.cristalise.dev.test.scenario;

import org.cristalise.dsl.persistency.outcome.OutcomeBuilder
import org.cristalise.dsl.property.PropertyDescriptionBuilder
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.test.KernelScenarioTestBase
import org.junit.jupiter.api.Test

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

@CompileStatic
class RoutingScriptTest_OrSplitLoop_DevIT extends KernelScenarioTestBase {
    
    private static final String baseDir = 'src/main/data/RoutingScriptTest'

    @Test
    public void execute() {
        String schemaName =  "CounterSchema"
        Map elemActNames = ["ActCounter" : schemaName, "EmptyAct": ""]
        List routingScriptNames = ["CounterScript", "GreaterThanTenScript"]
        String compActName = "RoutingScriptWorkflow"
        String factoryName = "RoutingScriptFactory"

        //TODO: use SchemaBuilder
        def schema = Schema("$schemaName-$timeStamp", folder, new File("$baseDir/OD/${schemaName}.xsd").text)

        routingScriptNames.each { String name ->
            //TODO: use ScriptBuilder
            Script("$name-$timeStamp", folder, new File("$baseDir/SC/${name}.xml").text)
        }

        elemActNames.each { String actName, String actSchema ->
            ElementaryActivityDef("$actName-$timeStamp", folder) {
                if (actSchema) Schema(schema)
            }
        }

        def vars = [:]
        vars.WORKFLOW_NAME  = "RoutingScriptWorkflow-$timeStamp"
        vars.LOOP_SCRIPT    = "GreaterThanTenScript-$timeStamp"
        vars.ORSPLIT_SCRIPT = "CounterScript-$timeStamp"
        vars.ACT_COUNTER    = "ActCounter-$timeStamp"
        vars.ACT_EMPTY      = "EmptyAct-$timeStamp"

        def caXML = evalMVELTemplate("$baseDir/CA/${compActName}.xml", vars)
        def caDef = CompositeActivityDef("$compActName-$timeStamp", folder, caXML)

        def descItem = DescriptionItem("$factoryName-$timeStamp", folder) {
            PropertyDesc(name: "Type", defaultValue: "RoutingScriptTest", isMutable: false, isClassIdentifier: true)
            Workflow(caDef)
        }

        def instance = createItemFromDescription(descItem, "RoutingScriptTest_OrSplitLoop_DevIT-$timeStamp", folder)

        executeCounterJob(instance, "ActCounter", 3);
        executeJob(instance, "ActOdd")
        executeCounterJob(instance, "ActCounter", 10);
        executeJob(instance, "ActEven")
        executeCounterJob(instance, "ActCounter", 13);
        executeJob(instance, "ActOdd")
        executeJob(instance, "Last")
    }

    @CompileDynamic
    private void executeCounterJob(ItemProxy proxy, String actName, int number) {
        def job = proxy.getJobByName(actName, agent)
        assert job.transition.name == "Done"
        job.setOutcome(OutcomeBuilder.build("CounterSchema") {counter number})
        agent.execute(job)
    }

    private void executeJob(ItemProxy proxy, String actName) {
        def job = proxy.getJobByName(actName, agent)
        assert job.transition.name == "Done"
        agent.execute(job)
    }
}
