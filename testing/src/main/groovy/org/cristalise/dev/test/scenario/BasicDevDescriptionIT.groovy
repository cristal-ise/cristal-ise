package org.cristalise.dev.test.scenario;

import org.cristalise.kernel.test.KernelScenarioTestBase
import org.cristalise.kernel.test.utils.KernelXMLUtility
import org.junit.Ignore
import org.junit.Test

import groovy.transform.CompileStatic


@CompileStatic
class BasicDevDescriptionIT extends KernelScenarioTestBase {
    
    public static final String testDataRoot = "src/main/data";

    @Test
    public void createAndEditElemActDesc() {
        String name = "TestEADesc-$timeStamp"

        def item = createNewElemActDesc(name, folder)
        editElemActDesc(name, folder, "User", "Errors", 0)

        assert item.getMasterSchema('ElementaryActivityDef', 0)
        //assert item.getAggregateScript()
	}

    @Test
    public void createAndEditSchema() {
        String schema = "PatientDetails"

        String name = "$schema-$timeStamp"

        def item = createNewSchema(name, folder)
        editSchema(name, folder, new File("$testDataRoot/${schema}.xsd").text)

        assert item.getMasterSchema()
        //assert item.getAggregateScript()
    }

    @Test
    public void createAndEditCompActDesc() {
        String name = "TestCADesc-$timeStamp"
        String activityName = 'CreateNewItem' //must be an existing ElementaryActivity

        def item = createNewCompActDesc(name, folder)
        String caXML = KernelXMLUtility.getCompositeActivityDefXML(Name: name, ActivityName: activityName, ActivityVersion: 0)
        editCompActDesc(name, folder, caXML, 1)

        assert item.getMasterSchema('CompositeActivityDef', 0)
        //assert item.getAggregateScript()
    }

    @Test
    public void createAndEditScript() {
        String name = "Script-$timeStamp"

        def item = createNewScript(name, folder)
        editScript(name, folder, new File("$testDataRoot/TestScript.xml").text)

        assert item.getMasterSchema()
        //assert item.getAggregateScript()
    }

    @Test
    public void createAndEditQuery() {
        String name = "Query-$timeStamp"

        def item = createNewQuery(name, folder)
        editQuery(name, folder, new File("$testDataRoot/TestQuery.xml").text)

        assert item.getMasterSchema()
        //assert item.getAggregateScript()
    }

    @Test
    public void createAndEditStateMachine() {
        String name = "StateMachine-$timeStamp"

        def item = createNewStateMachine(name, folder)
        editStateMachine(name, folder, new File("$testDataRoot/TestStateMachine.xml").text)

        assert item.getMasterSchema()
        //assert item.getAggregateScript()
    }

    @Test
    public void createAndEditPropertyDescription() {
        String name = "PropertyDescription-$timeStamp"

        def item = createNewPropertyDescription(name, folder)
        editPropertyDescription(name, folder, new File("$testDataRoot/TestPropertyDescription.xml").text)

        assert item.getMasterSchema()
        //assert item.getAggregateScript()
    }

    @Test
    public void createAndEditAgentDesc() {
        String name = "AgentDesc-$timeStamp"

        def item = createNewAgentDesc(name, folder)
        editAgentDesc(name, folder, new File("$testDataRoot/TestAgentDesc.xml").text)

        //assert item.getMasterSchema()
        //assert item.getAggregateScript()
    }

    @Test
    public void createAndEditItemDesc() {
        String name = "ItemDesc-$timeStamp"

        def item = createNewItemDesc(name, folder)
        editItemDesc(name, folder, new File("$testDataRoot/TestItemDesc.xml").text)

        //assert item.getMasterSchema()
        //assert item.getAggregateScript()
    }

    @Test
    public void createAndEditRoleDesc() {
        String name = "RoleDesc-$timeStamp"

        def item = createNewRoleDesc(name, folder)
        editRoleDesc(name, folder, new File("$testDataRoot/TestRoleDesc.xml").text)

        //assert item.getMasterSchema()
        //assert item.getAggregateScript()
    }

    @Test @Ignore('Test Unimplemented')
    public void createAndEditModule() {
    }
}
