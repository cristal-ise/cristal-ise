package org.cristalise.dev.test.scenario;

import static org.junit.Assert.*
import groovy.transform.CompileStatic

import org.cristalise.kernel.test.KernelScenarioTestBase
import org.junit.Test


@CompileStatic
class BasicDevDescriptionIT extends KernelScenarioTestBase {
    
    public static final String testDataRoot = "src/main/data";

    @Test
    public void createAndEditElemActDesc() {
        String name = "TestEADesc-$timeStamp"

        createNewElemActDesc(name, folder)
        editElemActDesc(name, folder, "User", "Errors", 0)
	}

    @Test
    public void createAndEditSchema() {
        String schema = "PatientDetails"

        String name = "$schema-$timeStamp"

        createNewSchema(name, folder)
        editSchema(name, folder, new File("$testDataRoot/${schema}.xsd").text)
    }

    @Test
    public void createAndEditCompActDesc() {
        String name = "TestCADesc-$timeStamp"

        createNewCompActDesc(name, folder)
        editCompActDesc(name, folder, "CreateNewItem", 0)
    }

    @Test
    public void createAndEditScript() {
        String name = "Script-$timeStamp"

        createNewScript(name, folder)
        editScript(name, folder, new File("$testDataRoot/TestScript.xml").text)
    }

    @Test
    public void createAndEditQuery() {
        String name = "Query-$timeStamp"

        createNewQuery(name, folder)
        editQuery(name, folder, new File("$testDataRoot/TestQuery.xml").text)
    }
}
