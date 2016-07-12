package org.cristalise.dev.test.scenario;

import org.cristalise.dsl.persistency.outcome.OutcomeBuilder
import org.cristalise.dsl.property.PropertyDescriptionBuilder
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.test.KernelScenarioTestBase
import org.junit.Before
import org.junit.Test


/**
 * 
 *
 */
class RoutingScriptTest_OrSplitLoop_DevIT extends KernelScenarioTestBase {

    @Before
    void before() {
        super.beforeClient('src/integration-test/conf/testClient.conf', 'src/integration-test/conf/devServer.clc')
    }

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
        editCompActDesc(compActName+"-$timeStamp", folder, new File("src/integration-test/data/RoutingScriptTest/CA/${compActName}.xml").text)

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

        instance.getJobList(agent).size == 1

    }
}
