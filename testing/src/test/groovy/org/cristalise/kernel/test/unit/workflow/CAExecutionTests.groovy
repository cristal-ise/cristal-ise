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
    Activity          ea0    = null
    Activity          ea1    = null

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

    private void createSequentialWf(List wfList) {
        rootCA = new CompositeActivity()
        wf = new Workflow(rootCA, new ServerPredefinedStepContainer())

        boolean first = true;
        WfVertex prevVertex = null

        wfList.each { type ->
            WfVertex currentVertex = rootCA.newChild((WfVertex.Types)type, "", first, null)
            prevVertex?.addNext(currentVertex)
            first = false
            prevVertex = currentVertex
        }

        ea0 = (Activity)wf.search("workflow/domain/0")
        ea1 = (Activity)wf.search("workflow/domain/1") //this migth return null

        checkActStatus(rootCA, [state: "Waiting", active: false])
        checkActStatus(ea0,    [state: "Waiting", active: false])
        if(ea1) checkActStatus(ea1, [state: "Waiting", active: false])

        wf.initialise(itemPath, agentPath)

        checkActStatus(rootCA, [state: "Started", active: true])
        checkActStatus(ea0,    [state: "Waiting", active: true])
        if(ea1) checkActStatus(ea1, [state: "Waiting", active: false])
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

        requestAction(ea0, "Done")
        
        checkActStatus(rootCA, [state: "Started",  active: true])
        checkActStatus(ea0,    [state: "Finished", active: true])

        requestAction(rootCA, "Complete")

        checkActStatus(rootCA, [state: "Finished", active: false])
        checkActStatus(ea0,    [state: "Finished", active: true])
    }

    @Test
    public void singleAct_StartFinish() {
        createSequentialWf( [Types.Atomic] )

        requestAction( ea0, "Start")

        checkActStatus(rootCA, [state: "Started", active: true])
        checkActStatus(ea0,    [state: "Started", active: true])

        requestAction( ea0, "Complete" )

        checkActStatus(rootCA, [state: "Started",  active: true])
        checkActStatus(ea0,    [state: "Finished", active: true])

        requestAction(rootCA, "Complete")

        checkActStatus(rootCA, [state: "Finished", active: false])
        checkActStatus(ea0,    [state: "Finished", active: true])
    }


    @Test
    public void twoActs_Done() {
        createSequentialWf( [Types.Atomic, Types.Atomic] )

        requestAction(ea0, "Done")

        checkActStatus(rootCA,  [state: "Started",  active: true])
        checkActStatus(ea0,     [state: "Finished", active: false])
        checkActStatus(ea1,     [state: "Waiting",  active: true])

        requestAction(ea1, "Done")

        checkActStatus(rootCA,  [state: "Started",  active: true])
        checkActStatus(ea0,     [state: "Finished", active: false])
        checkActStatus(ea1,     [state: "Finished", active: true])
        
        requestAction(rootCA, "Complete")

        checkActStatus(rootCA,  [state: "Finished", active: false])
        checkActStatus(ea0,     [state: "Finished", active: false])
        checkActStatus(ea1,     [state: "Finished", active: true])
    }


    @Test(expected = InvalidTransitionException.class)
    public void singleAct_OnlyCompleteCA() {
        createSequentialWf( [Types.Atomic] )

        requestAction(rootCA, "Complete")
    }
}
