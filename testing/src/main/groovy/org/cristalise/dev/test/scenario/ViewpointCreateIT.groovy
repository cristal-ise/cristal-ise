package org.cristalise.dev.test.scenario

import static org.cristalise.dev.dsl.DevXMLUtility.recordToXML

import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.test.KernelScenarioTestBase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

/**
 * 
 *
 */
@CompileStatic
@TestInstance(Lifecycle.PER_CLASS)
class ViewpointCreateIT extends KernelScenarioTestBase {
    String schemaName  = "ViewpointTest"
    String elemActName = "SetViewpoint"
    String compActName = "ViewpointTest"
    String factoryName = "ViewpointTestFactory"
    String itemName    = "ViewpointTest"

    ItemProxy viewpointTest

	/**
	 * 
	 * @param actDefList
	 */
    @CompileDynamic
    private void setupItem(String viewpoint) {
        def schema = Schema("$schemaName-$timeStamp", folder) {
            struct(name: schemaName) {
                field(name: 'StringField', type: 'string')
            }
        }

        def ea = ElementaryActivityDef("$elemActName-$timeStamp", folder) {
            if(viewpoint) Property(Viewpoint: viewpoint)
            Property(OutcomeInit: "Empty")
            Schema(schema)
        }

        def wf = CompositeActivityDef("$compActName-$timeStamp", folder) {
            Layout {
                ElemActDef(elemActName,  ea)
            }
        }

        def factory = DescriptionItem("$factoryName-$timeStamp", folder) {
            PropertyDesc(name: "Type", defaultValue: "ViewpointTest", isMutable: false, isClassIdentifier: true)
            Workflow(wf)
        }

        executeDoneJob(
            factory,
            "CreateNewInstance",
            recordToXML('NewDevObjectDef', [ObjectName: "$itemName-$timeStamp", SubFolder: folder])
        )

        viewpointTest = agent.getItem("$folder/$itemName-$timeStamp")
    }

    @Test
    public void defaultLastView() {
        setupItem('')
        executeDoneJob(viewpointTest, elemActName)
        assert viewpointTest.checkViewpoint("$schemaName-$timeStamp", "last")
    }

    @Test
    public void namedView() {
        setupItem("dummy")
        executeDoneJob(viewpointTest, elemActName)
        assert viewpointTest.checkViewpoint("$schemaName-$timeStamp", "last")
        assert viewpointTest.checkViewpoint("$schemaName-$timeStamp", "dummy")
    }

    @Test
    public void viewNameFromOutcome() {
        setupItem("xpath:/$schemaName/StringField")
        executeDoneJob(viewpointTest, elemActName)
        assert viewpointTest.checkViewpoint("$schemaName-$timeStamp", "last")
        assert viewpointTest.checkViewpoint("$schemaName-$timeStamp", "string") //this value is set by the EmptyOutcomeInitiator
    }
}
