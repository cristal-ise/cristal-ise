package org.cristalise.trigger.test.scenario;

import static java.util.concurrent.TimeUnit.*
import static org.awaitility.Awaitility.await

import org.awaitility.Awaitility
import org.cristalise.dsl.test.builders.AgentTestBuilder
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.ActivityDef
import org.cristalise.kernel.lifecycle.CompositeActivityDef
import org.cristalise.kernel.persistency.outcome.Schema
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.KernelScenarioTestBase
import org.junit.Ignore;
import org.junit.Test

/**
 *
 */
class BasicTriggerTimeoutIT extends KernelScenarioTestBase {

    Schema warningSchema, timeoutSchema, triggerTestActSchema
    ActivityDef triggerTestActDef
    CompositeActivityDef triggerWF

    ItemProxy        factory
    ItemProxy        triggerable
    AgentTestBuilder triggerAgent

    public void bootstrap(boolean warningOn, boolean timeoutOn) {
        Awaitility.setDefaultTimeout(5, SECONDS)
        Awaitility.setDefaultPollInterval(200, MILLISECONDS)
        Awaitility.setDefaultPollDelay(200, MILLISECONDS)

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
            Property(WarningOn: warningOn, WarningDuration: 2, WarningUnit: "SECOND")
            Property(WarningSchemaType: warningSchema.name, WarningSchemaVersion: 0)
            Property(TimeoutOn: timeoutOn, TimeoutDuration: 5, TimeoutUnit: "SECOND")
            Property(TimeoutSchemaType: timeoutSchema.name, TimeoutSchemaVersion: 0)
            Role("User")
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

        triggerAgent = new AgentTestBuilder(Gateway.lookup.getAgentPath("triggerAgent"))
    }

//    private void executeJob(AgentTestBuilder atb, String trans, Outcome outcome = null) {
//        atb.executeJob(triggerable, "TriggerTestAct", trans, outcome)
//    }

    private void startActivity() {
        def job = triggerable.getJobByTransitionName("TriggerTestAct", "Start", agent.path)
        assert job && job.transition.name == "Start"
        agent.execute(job)
    }

    @Test
    public void 'Warning Transition is Enabled'() {
        bootstrap(true, false)

        triggerAgent.checkJobList([])
        checkJobs(triggerable, [[stepName: "TriggerTestAct", agentRole: "Admin", transitionName: "Start"],
                                [stepName: "TriggerTestAct", agentRole: "Admin", transitionName: "Done"]])

        startActivity()

        await("Activity started").until { triggerAgent.jobList.size() == 1 }

        triggerAgent.checkJobList([[stepName: "TriggerTestAct", agentRole: "TriggerAdmin", transitionName: "Warning"]])
        checkJobs(triggerable, [[stepName: "TriggerTestAct", agentRole: "Admin", transitionName: "Complete"],
                                [stepName: "TriggerTestAct", agentRole: "Admin", transitionName: "Suspend" ],
                                [stepName: "TriggerTestAct", agentRole: "Admin", transitionName: "Warning" ]])

        await("Warning trigger fired once" ).pollDelay(2, SECONDS).until { checkOutcomeCount(triggerable, "WarningSchema-$timeStamp", 1) }
        await("Warning trigger fired twice").pollDelay(2, SECONDS).until { checkOutcomeCount(triggerable, "WarningSchema-$timeStamp", 2) }

        triggerAgent.checkJobList([[stepName: "TriggerTestAct", agentRole: "TriggerAdmin", transitionName: "Warning"]])
        checkJobs(triggerable, [[stepName: "TriggerTestAct", agentRole: "Admin", transitionName: "Complete"],
                                [stepName: "TriggerTestAct", agentRole: "Admin", transitionName: "Suspend" ],
                                [stepName: "TriggerTestAct", agentRole: "Admin", transitionName: "Warning" ]])
        
    }

    @Ignore("not ready yet") @Test
    public void 'Timeout Transition is Enabled'() {
        //bootstrap(false, true)
    }

    @Ignore("not ready yet") @Test
    public void 'Both Warning and Timeout Transitions are Enabled'() {
        //bootstrap(true, true)
    }

}
