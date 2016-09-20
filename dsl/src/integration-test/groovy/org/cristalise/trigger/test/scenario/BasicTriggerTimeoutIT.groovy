package org.cristalise.trigger.test.scenario;

import org.cristalise.kernel.test.KernelScenarioTestBase
import org.junit.Test


/**
 *
 *
 */
class BasicTriggerTimeoutIT extends KernelScenarioTestBase {

    @Test
    public void execute() {
        def timeoutSchema = Schema("TimeoutSchema-$timeStamp", folder) {
            struct(name: "Timeout") {
                field(name: "Actions")
            }
        }

        def triggerTestActSchema = Schema("TriggerTestActSchema-$timeStamp", folder) {
            struct(name: "Outcome") {
                field(name: "Message")
            }
        }

        def triggerTestActDef = ElementaryActivityDef("TriggerTestAct-$timeStamp", folder) {
            Property(AlarmOn: false)
            Property(TimeoutDuration: 3, TimeoutUnit: "SECOND")
            Property(TimeoutSchemaType: timeoutSchema.name, TimeoutSchemaVersion: 0)
            Schema(triggerTestActSchema)
            StateMachine("TriggerStateMachine", 0)
        }

        def triggerWF = CompositeActivityDef("TriggerWF-$timeStamp", folder) {
            ElemActDef('TriggerTestAct',  triggerTestActDef)
        }

        DescriptionItem("TriggerableItemFactory-$timeStamp", folder) {
            Property(Type: "Factory")
        }
    }
}
