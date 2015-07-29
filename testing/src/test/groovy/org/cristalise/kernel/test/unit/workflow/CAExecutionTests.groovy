package org.cristalise.kernel.test.unit.workflow;

import static org.junit.Assert.*
import groovy.transform.CompileStatic

import org.cristalise.kernel.common.InvalidTransitionException
import org.cristalise.kernel.lifecycle.instance.Activity
import org.cristalise.kernel.lifecycle.instance.CompositeActivity
import org.cristalise.kernel.lifecycle.instance.WfVertex
import org.cristalise.kernel.lifecycle.instance.Workflow
import org.cristalise.kernel.lifecycle.instance.WfVertex.Types
import org.cristalise.kernel.lifecycle.instance.predefined.server.ServerPredefinedStepContainer
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine
import org.cristalise.kernel.lifecycle.instance.stateMachine.Transition
import org.cristalise.kernel.lookup.AgentPath
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.process.Gateway
import org.junit.Before
import org.junit.Test


@CompileStatic
class CAExecutionTests extends WorkflowTestBase {

    private ItemPath itemPath = null
    private AgentPath agentPath = null

    private StateMachine eaSM = null
    private StateMachine caSM = null
    
    Workflow          wf     = null
    CompositeActivity rootCA = null
    Activity          act0   = null
    Activity          act1   = null

    @Before
    public void init() {
        super.init()

        eaSM = (StateMachine)Gateway.getMarshaller().unmarshall(Gateway.getResource().getTextResource(null, "boot/SM/Default.xml"));
        caSM = (StateMachine)Gateway.getMarshaller().unmarshall(Gateway.getResource().getTextResource(null, "boot/SM/CompositeActivity.xml"));

        itemPath  = new ItemPath()
        agentPath = new AgentPath(new ItemPath(), "dev")
    }

    private int getTransID(StateMachine sm, String name) {
        Transition t = sm.getTransitions().find{ it.name == name }
        assert t, "Transition name '$name' is invalid for StateMachine $sm.name"
        return t.id
    }

    private void checkActStatus(Activity act, Map status) {
        assert act
        assert act.getStateName() == "$status.state"
        assert act.getActive() == status.active
    }

    private void createSequentialCA(CompositeActivity ca, List caList) {
        boolean first = true;
        WfVertex prevVertex = null

        caList.each { member ->
            WfVertex currentVertex

            if(member instanceof List) {
                 currentVertex = ca.newChild(Types.Composite, "", first, null)
                 createSequentialCA((CompositeActivity)currentVertex, (List)member)
            }
            else {
                currentVertex = ca.newChild((WfVertex.Types)member, "", first, null)
            }

            prevVertex?.addNext(currentVertex)
            first = false
            prevVertex = currentVertex
        }
    }

    private void createSequentialWf(List wfList, boolean doChecks = true) {
        rootCA = new CompositeActivity()
        wf     = new Workflow(rootCA, new ServerPredefinedStepContainer())

        createSequentialCA(rootCA, wfList)

        act0 = (Activity)wf.search("workflow/domain/0")
        act1 = (Activity)wf.search("workflow/domain/1") //this migth return null

        if(doChecks) {
            checkActStatus(         rootCA, [state: "Waiting", active: false])
            checkActStatus(         act0,   [state: "Waiting", active: false])
            if(act1) checkActStatus(act1,   [state: "Waiting", active: false])
        }

        wf.initialise(itemPath, agentPath)

        if(doChecks) {
            checkActStatus(rootCA, [state: "Started", active: true])

            if(act0 instanceof CompositeActivity) {
                checkActStatus(         act0, [state: "Started", active: true])
                if(act1) checkActStatus(act1, [state: "Waiting", active: true])
            }
            else {
                checkActStatus(         act0, [state: "Waiting", active: true])
                if(act1) checkActStatus(act1, [state: "Waiting", active: false])
            }
        }
    }

    private void requestAction(Activity act, String trans) {
        int transID = -1

        if(act instanceof CompositeActivity) transID = getTransID(caSM, trans)
        else                                 transID = getTransID(eaSM, trans)

        wf.requestAction(agentPath, act.path, itemPath, transID, "")
    }

    @Test
    public void singleAct_Done() {
        createSequentialWf( [Types.Atomic] )

        requestAction(act0, "Done")
        
        checkActStatus(rootCA, [state: "Started",  active: true])
        checkActStatus(act0,   [state: "Finished", active: true])

        requestAction(rootCA, "Complete")

        checkActStatus(rootCA, [state: "Finished", active: false])
        checkActStatus(act0,   [state: "Finished", active: true])
    }

    @Test
    public void singleAct_StartFinish() {
        createSequentialWf( [Types.Atomic] )

        requestAction( act0, "Start")

        checkActStatus(rootCA, [state: "Started", active: true])
        checkActStatus(act0,   [state: "Started", active: true])

        requestAction( act0, "Complete" )

        checkActStatus(rootCA, [state: "Started",  active: true])
        checkActStatus(act0,   [state: "Finished", active: true])

        requestAction(rootCA, "Complete")

        checkActStatus(rootCA, [state: "Finished", active: false])
        checkActStatus(act0,   [state: "Finished", active: true])
    }

    @Test
    public void twoActs_Done() {
        createSequentialWf( [Types.Atomic, Types.Atomic] )

        requestAction(act0, "Done")

        checkActStatus(rootCA, [state: "Started",  active: true])
        checkActStatus(act0,   [state: "Finished", active: false])
        checkActStatus(act1,   [state: "Waiting",  active: true])

        requestAction(act1, "Done")

        checkActStatus(rootCA, [state: "Started",  active: true])
        checkActStatus(act0,   [state: "Finished", active: false])
        checkActStatus(act1,   [state: "Finished", active: true])
        
        requestAction(rootCA, "Complete")

        checkActStatus(rootCA, [state: "Finished", active: false])
        checkActStatus(act0,   [state: "Finished", active: false])
        checkActStatus(act1,   [state: "Finished", active: true])
    }

    @Test(expected = InvalidTransitionException.class)
    public void singleAct_OnlyCompleteCA() {
        createSequentialWf( [Types.Atomic] )

        requestAction(rootCA, "Complete")
    }

    @Test(expected = InvalidTransitionException.class)
    public void emptyWf_Complete() {
        createSequentialWf([], false)

        checkActStatus(rootCA, [state: "Waiting", active: true])

        requestAction(rootCA, "Complete")
    }

    @Test
    public void wfWithEmptyCompAct_Complete() {
        createSequentialWf([[]], false)

        checkActStatus(rootCA, [state: "Started", active: true])
        checkActStatus(act0,   [state: "Waiting", active: true])  //CA

        requestAction(rootCA, "Complete")
    }

    @Test
    public void wfWithCompAct_FullyExecuted() {
        createSequentialWf([[Types.Atomic]] )
        
        requestAction(act1, "Done")

        checkActStatus(rootCA, [state: "Started",  active: true])
        checkActStatus(act0,   [state: "Finished", active: true])  //CA
        checkActStatus(act1,   [state: "Finished", active: false]) //EA

        requestAction(rootCA, "Complete")

        checkActStatus(rootCA, [state: "Finished", active: false])
        checkActStatus(act0,   [state: "Finished", active: true])  //CA
        checkActStatus(act1,   [state: "Finished", active: false]) //EA
    }
}
