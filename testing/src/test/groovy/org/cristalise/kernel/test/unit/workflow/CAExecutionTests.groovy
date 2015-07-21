package org.cristalise.kernel.test.unit.workflow;

import static org.junit.Assert.*
import groovy.transform.CompileStatic

import org.cristalise.kernel.lifecycle.instance.Activity
import org.cristalise.kernel.lifecycle.instance.CompositeActivity
import org.cristalise.kernel.lifecycle.instance.WfVertex
import org.cristalise.kernel.lifecycle.instance.Workflow
import org.cristalise.kernel.lifecycle.instance.predefined.server.ServerPredefinedStepContainer
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine
import org.cristalise.kernel.lookup.AgentPath
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway
import org.junit.After
import org.junit.Before
import org.junit.Test


@CompileStatic
class CAExecutionTests {

    private ItemPath itemPath = null
    private AgentPath agentPath = null

    private StateMachine eaStateMachine = null

    @Before
    public void init() {
        String[] args = ['-logLevel', '8', '-config', 'src/test/conf/testServer.conf', '-connect', 'src/test/conf/testInMemory.clc']
        Gateway.init(AbstractMain.readC2KArgs(args))
        Gateway.connect()
        
        eaStateMachine = (StateMachine)Gateway.getMarshaller().unmarshall(
             Gateway.getResource().getTextResource(null, "boot/SM/Default.xml"));

        itemPath  = new ItemPath()
        agentPath = new AgentPath(new ItemPath(), "dev")
    }

    @After
    public void tearDown() {
        Gateway.close()
    }

    private int eaTransID(String name) {
        return eaStateMachine.getTransitions().find{ it.name == name }.id
    }

    private void checkActStatus(Activity act, Map status) {
        assert act
        assert act.getStateName() == "$status.state"
        assert act.getActive() == status.active
    }

    private Workflow createWf(List wfList) {
        CompositeActivity rootCA = new CompositeActivity()
        Workflow wf = new Workflow(rootCA, new ServerPredefinedStepContainer())
        
        boolean first = true;
        WfVertex prevVertex = null

        wfList.each { type ->
            WfVertex currentVertex = rootCA.newChild((WfVertex.Types)type, "", first, null)
            if(prevVertex) {
                prevVertex.addNext(currentVertex)
            }
            first = false
            prevVertex = currentVertex
        }

        wf.initialise(itemPath, agentPath)
        
        return wf
    }

    @Test
    public void eaRun_Done_SingleAct() {
        Workflow wf = createWf( [WfVertex.Types.Atomic] )

        CompositeActivity rootCA = (CompositeActivity)wf.search("workflow/domain")

        checkActStatus(rootCA,  [state: "Waiting", active: true])

        Activity ea0 = (Activity)wf.search("workflow/domain/0")

        checkActStatus(ea0, [state: "Waiting", active: true])

        wf.requestAction(agentPath, "workflow/domain/0", itemPath, eaTransID("Done"), "")

        checkActStatus(ea0, [state: "Finished", active: true])
    }

    @Test
    public void eaRun_StartFinish_SingleAct() {
        Workflow wf = createWf( [WfVertex.Types.Atomic] )

        CompositeActivity rootCA = (CompositeActivity)wf.search("workflow/domain")

        checkActStatus(rootCA,  [state: "Waiting", active: true])

        Activity ea0 = (Activity)wf.search("workflow/domain/0")

        checkActStatus(ea0, [state: "Waiting", active: true])

        wf.requestAction(agentPath, "workflow/domain/0", itemPath, eaTransID("Start"), "")

        checkActStatus(ea0, [state: "Started", active: true])

        wf.requestAction(agentPath, "workflow/domain/0", itemPath, eaTransID("Complete"), "")

        checkActStatus(ea0, [state: "Finished", active: true])
    }


    @Test
    public void eaRun2Acts() {
        Workflow wf = createWf( [WfVertex.Types.Atomic, WfVertex.Types.Atomic] )

        CompositeActivity rootCA = (CompositeActivity)wf.search("workflow/domain")

        checkActStatus(rootCA,  [state: "Waiting", active: true])

        Activity ea0 = (Activity)wf.search("workflow/domain/0")
        Activity ea1 = (Activity)wf.search("workflow/domain/1")

        checkActStatus(rootCA,  [state: "Waiting", active: true])
        checkActStatus(ea0,     [state: "Waiting", active: true])
        checkActStatus(ea1,     [state: "Waiting", active: false])
        
        wf.requestAction(agentPath, "workflow/domain/0", itemPath, eaTransID("Done"), "")

        checkActStatus(rootCA,  [state: "Waiting",  active: true])
        checkActStatus(ea0,     [state: "Finished", active: false])
        checkActStatus(ea1,     [state: "Waiting",  active: true])
    }

    
    @Test
    public void simpleCARun() {
        CompositeActivity ca = new CompositeActivity()
        Activity act = new Activity()

        ca.addChild(act, null)
        ca.getChildrenGraphModel().setStartVertexId(act.getID())
        
        Workflow wf = new Workflow(ca, new ServerPredefinedStepContainer())

        checkActStatus(ca,  [state: "Waiting", active: false])
        checkActStatus(act, [state: "Waiting", active: false])

        wf.initialise(itemPath, agentPath)

        checkActStatus(ca,  [state: "Waiting", active: true])
//        checkActStatus(act, [state: "Waiting", active: false]) //act should be inactive before CA is requested
        checkActStatus(act, [state: "Waiting", active: true])

//        ca.request(agentPath, itemPath, 0, "")
        wf.requestAction(agentPath, "workflow/domain", itemPath, 0, "")

        checkActStatus(ca,  [state: "Started", active: true])
        checkActStatus(act, [state: "Waiting", active: true])
    }
}
