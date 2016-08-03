package org.cristalise.dev.test.scenario;

import org.cristalise.dsl.persistency.outcome.OutcomeBuilder
import org.cristalise.dsl.property.PropertyDescriptionBuilder
import org.cristalise.kernel.entity.proxy.AgentProxy
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.test.KernelScenarioTestBase
import org.junit.Test


/**
 * 
 * 
 */
class RoutingScriptTest_OrSplitLoop_DevIT extends KernelScenarioTestBase {

    @Test
    public void execute() {
        String schemaName =  "CounterSchema"
        Map elemActNames = ["ActCounter" : schemaName, "EmptyAct": ""]
        List routingScriptNames = ["CounterScript", "GreaterThanTenScript"]
        String compActName = "RoutingScriptWorkflow"
        String factoryName = "RoutingScriptFactory"

        createNewSchema(schemaName+"-$timeStamp", folder)
        //TODO: use SchemaBuilder instead
        editSchema(schemaName+"-$timeStamp", folder, new File("src/integration-test/data/RoutingScriptTest/OD/${schemaName}.xsd").text)

        routingScriptNames.each { String name ->
            createNewScript(name+"-$timeStamp", folder)
            //TODO: use ScriptBuilder instead
            editScript(name+"-$timeStamp", folder, new File("src/integration-test/data/RoutingScriptTest/SC/${name}.xml").text)
        }

        elemActNames.each { String name, String schema ->
            createNewElemActDesc(name+"-$timeStamp", folder)
            editElemActDesc(name+"-$timeStamp", folder, "Admin", (schema==null ? null : schema+"-$timeStamp"), 0)
        }

        createNewCompActDesc(compActName+"-$timeStamp", folder)

        def vars = [:]
        vars.WORKFLOW_NAME  = "RoutingScriptWorkflow-$timeStamp"
        vars.LOOP_SCRIPT    = "GreaterThanTenScript-$timeStamp"
        vars.ORSPLIT_SCRIPT = "CounterScript-$timeStamp"
        vars.ACT_COUNTER    = "ActCounter-$timeStamp"
        vars.ACT_EMPTY      = "EmptyAct-$timeStamp"

        def caXML = evalMVELTemplate("src/integration-test/data/RoutingScriptTest/CA/${compActName}.xml", vars)

        //println "-----------------------------------------------------------------------------------"
        //println caXML
        //println "-----------------------------------------------------------------------------------"
        
        editCompActDesc(compActName+"-$timeStamp", folder, caXML)

        createNewDescriptionItem(factoryName+"-$timeStamp", folder)

        ItemProxy instance = editDescriptionAndCreateItem( 
            factoryName+"-$timeStamp", folder, 
            PropertyDescriptionBuilder.build {
                PropertyDesc("Name")
                PropertyDesc(name: "Type", defaultValue: "RoutingScriptTest", isMutable: false, isClassIdentifier: true)
            },
            OutcomeBuilder.build("ChooseWorkflow") { 
                WorkflowDefinitionName(compActName+"-$timeStamp")
                WorkflowDefinitionVersion("0")
            },
            OutcomeBuilder.build("NewDevObjectDef") {
                ObjectName("RoutingScriptTest_OrSplitLoop_DevIT-$timeStamp")
                SubFolder(folder)
            }
        );

        executeCounterJob(instance, "ActCounter", 3);
        executeJob(instance, "ActOdd")
        executeCounterJob(instance, "ActCounter", 10);
        executeJob(instance, "ActEven")
        executeCounterJob(instance, "ActCounter", 13);
        executeJob(instance, "ActOdd")
        executeJob(instance, "Last")
    }

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
