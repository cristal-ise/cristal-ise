package org.cristalise.dev.test.scenario

import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.test.KernelScenarioTestBase
import org.junit.Test


/**
 * 
 *
 */
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
            ElemActDef(elemActName,  ea)
        }

        def factory = DescriptionItem("$factoryName-$timeStamp", folder) {
            PropertyDesc(name: "Type", defaultValue: "ViewpointTest", isMutable: false, isClassIdentifier: true)
            Workflow(wf)
        }

        createNewItemByFactory(factory, "CreateNewInstance", "$itemName-$timeStamp", folder)

        viewpointTest = agent.getItem("$folder/$itemName-$timeStamp")
    }

    @Test
    public void defaultLastView() {
        setupItem()
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
