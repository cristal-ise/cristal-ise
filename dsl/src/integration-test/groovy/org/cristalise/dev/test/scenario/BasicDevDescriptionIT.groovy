package org.cristalise.dev.test.scenario;

import static org.junit.Assert.*
import groovy.transform.CompileStatic

import org.cristalise.kernel.test.KernelScenarioTestBase
import org.junit.Before
import org.junit.Test



/**
 * 
 *
 */
@CompileStatic
class BasicDevDescriptionIT extends KernelScenarioTestBase {

    @Before
    void before() {
        super.beforeClient('src/integration-test/conf/testClient.conf', 'src/integration-test/conf/devServer.clc')
    }

    @Test
    public void createAndEditElemActDesc() {
        String name = "TestEADesc-$timeStamp"
        String folder = "IntegrationTest"

        createNewElemActDesc(name, folder)
        editElemActDesc(name, folder, "User", "Errors", 0)
	}

    @Test
    public void createAndEditSchema() {
        String schema = "PatientDetails"

        String name = "$schema-$timeStamp"
        String folder = "IntegrationTest"

        createNewSchema(name, folder)
        editSchema(name, folder, new File("src/integration-test/data/${schema}.xsd").text)
    }

    @Test
    public void createAndEditCompActDesc() {
        String name = "TestCADesc-$timeStamp"
        String folder = "IntegrationTest"

        createNewCompActDesc(name, folder)
        editCompActDesc(name, folder, "CreateNewItem", 0)
    }
}
