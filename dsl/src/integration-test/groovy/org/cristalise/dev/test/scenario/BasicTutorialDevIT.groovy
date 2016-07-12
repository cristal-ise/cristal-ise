package org.cristalise.dev.test.scenario;

import org.cristalise.dsl.persistency.outcome.OutcomeBuilder
import org.cristalise.dsl.property.PropertyDescriptionBuilder
import org.cristalise.kernel.test.KernelScenarioTestBase
import org.junit.Before
import org.junit.Test


/**
 * 
 *
 */
class BasicTutorialDevIT extends KernelScenarioTestBase {

    @Before
    void before() {
        super.beforeClient('src/integration-test/conf/testClient.conf', 'src/integration-test/conf/devServer.clc')
    }

    @Test
    public void execute() {
        String schemaName = "PatientDetails-$timeStamp"
        String elemActName = "SetPatientDetails-$timeStamp"
        String compActName = "PatientLifecycle-$timeStamp"
        String factoryName = "PatientFactory-$timeStamp"

        createNewSchema(schemaName, folder)
        editSchema(schemaName, folder, new File("src/integration-test/data/PatientDetails.xsd").text)

        createNewElemActDesc(elemActName, folder)
        editElemActDesc(elemActName, folder, "Admin", schemaName, 0)

        createNewCompActDesc(compActName, folder)
        editCompActDesc(compActName, folder, elemActName, 0)

        createNewDescriptionItem(factoryName, folder)

        editDescriptionAndCreateItem( 
            factoryName, folder, 
            PropertyDescriptionBuilder.build {
                PropertyDesc("Name")
                PropertyDesc(name: "Type", defaultValue: "Patient", isMutable: false, isClassIdentifier: true)
            },
            OutcomeBuilder.build("ChooseWorkflow") { 
                WorkflowDefinitionName(compActName)
                WorkflowDefinitionVersion("0")
            },
            OutcomeBuilder.build("NewDevObjectDef") {
                ObjectName("Patient-$timeStamp")
                SubFolder(folder)
            }
        );
    }
}
