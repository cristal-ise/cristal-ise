package org.cristalise.trigger.test.scenario;

import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.ActivityDef
import org.cristalise.kernel.lifecycle.CompositeActivityDef
import org.cristalise.kernel.persistency.outcome.Schema
import org.cristalise.kernel.test.KernelScenarioTestBase
import org.junit.Test


/**
 *
 */
class BasicTriggerTimeoutIT extends KernelScenarioTestBase {

    Schema warningSchema, timeoutSchema, triggerTestActSchema
    ActivityDef triggerTestActDef
    CompositeActivityDef triggerWF

    ItemProxy factory
    ItemProxy triggerable

    public void initilaise(boolean warningOn, boolean timeoutOn) {
        warningSchema = Schema("WarningSchema-$timeStamp", folder) {
            struct(name: "Warning") {
                field(name: "Actions")
            }
        }

        timeoutSchema = Schema("TimeoutSchema-$timeStamp", folder) {
            struct(name: "Timeout") {
                field(name: "Actions")
            }
        }

        triggerTestActSchema = Schema("TriggerTestActSchema-$timeStamp", folder) {
            struct(name: "TriggerTestAct") {
                field(name: "Message")
            }
        }

        triggerTestActDef = ElementaryActivityDef("TriggerTestAct-$timeStamp", folder) {
            Property(OutcomeInit: "XPath")
            Property(WarningOn: warningOn, WarningDuration: 5, WarningtUnit: "SECOND")
            Property(WarningSchemaType: warningSchema.name, WarningShemaVersion: 0)
            Property(TimeoutOn: timeoutOn, TimeoutDuration: 8, TimeoutUnit: "SECOND")
            Property(TimeoutSchemaType: timeoutSchema.name, TimeoutSchemaVersion: 0)
            Schema(triggerTestActSchema)
            StateMachine("TriggerStateMachine", 0)
        }

        triggerWF = CompositeActivityDef("TriggerWF-$timeStamp", folder) {
            ElemActDef('TriggerTestAct',  triggerTestActDef)
        }

        factory = DescriptionItem("TriggerableItemFactory-$timeStamp", folder) {
            PropertyDesc(name: "Type", defaultValue: "Triggerable", isMutable: false, isClassIdentifier: true)
            Workflow(triggerWF)
        }

        createNewItemByFactory(factory, "CreateNewInstance", "Triggerable-$timeStamp", folder)

        triggerable = agent.getItem("$folder/Triggerable-$timeStamp")
    }

    private void startActivity(ItemProxy proxy) {
        def job = proxy.getJobByTransitionName("TriggerTestAct", "Start", agent.path)
        assert job && job.transition.name == "Start"
        agent.execute(job)
    }

    @Test
    public void 'Warning Transition is Enabled'() {
        initilaise(true, false)

        startActivity(triggerable)
    }

    @Test
    public void 'Timeout Transition is Enabled'() {
        //initilaise(false, true)
    }

    @Test
    public void 'Both Warning and Timeout Transitions are Enabled'() {
        //initilaise(true, true)
    }

}
