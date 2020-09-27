package org.cristalise.kernel.test.events

import org.cristalise.dsl.test.builders.AgentTestBuilder
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.ActivityDef
import org.cristalise.kernel.lifecycle.instance.predefined.Erase
import org.cristalise.kernel.lookup.RolePath
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.KernelScenarioTestBase
import org.junit.After
import org.junit.Ignore
import org.junit.Test

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import spock.util.concurrent.PollingConditions


/**
 *
 *
 */
@CompileStatic
class ItemAuditTrailIT extends KernelScenarioTestBase {
    ItemProxy auditTrail

    @CompileDynamic
    private ItemProxy setup() {
        def ea = ElementaryActivityDef("EA-$timeStamp", folder) {}

        def ca = CompositeActivityDef("CA-$timeStamp", folder) {
            ElemActDef('EA',  ea)
        }

        def wf = CompositeActivityDef("WF-$timeStamp", folder) {
            CompActDef('CA',  ca)
        }

        return DescriptionItem("AuditTrailFactory-$timeStamp", folder) {
            PropertyDesc(name: "Type", defaultValue: 'AuditTrail', isMutable: false, isClassIdentifier: true)
            Workflow(wf)
        }
    }
    
    private void checkEvent(int id, int transId, String stepName) {
        def event = auditTrail.getEvent(id)
        assert event.getTransition() == transId;
        assert event.getStepName() == stepName;
    }

    @Test
    public void 'Check events generated by a CA(EA) workflow'() {
        def factory = setup()

        createNewItemByFactory(factory, "CreateNewInstance", "AuditTrail-$timeStamp", folder)

        auditTrail = agent.getItem("$folder/AuditTrail-$timeStamp")

        checkEvent(0, 0, 'Initialize')
        checkEvent(1, 0, 'workflow')
        checkEvent(2, 0, 'domain')
        checkEvent(3, 0, 'CA')

        executeDoneJob(auditTrail, 'EA')

        checkEvent(4, 0, 'EA')
        checkEvent(5, 1, 'CA')
    }
}