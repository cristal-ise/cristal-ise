package org.cristalise.kernel.test.scenario;

import static org.junit.Assert.*
import groovy.transform.CompileStatic

import org.cristalise.kernel.entity.agent.Job
import org.cristalise.kernel.entity.proxy.AgentProxy
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.test.KernelScenarioTestBase
import org.cristalise.kernel.test.utils.KernelXMLUtility
import org.junit.Before
import org.junit.BeforeClass;
import org.junit.Test



/**
 * 
 */
@CompileStatic
class ItemWithoutDescriptionIT extends KernelScenarioTestBase {

    @Before
    public void before() {
        init('src/test/conf/devClient.conf', 'src/test/conf/devServer.clc')
    }

    /**
     * 
     * @param role
     * @return
     */
    private void createNewRole(String role) {
        ItemProxy roleFactory = agent.getItem("/domain/servers/localhost")
        assert roleFactory && roleFactory.getName() == "localhost"

        Job doneJob = getDoneJob(roleFactory, "CreateNewRole")
        doneJob.setOutcome( KernelXMLUtility.getRoleXML(name: role) )

        agent.execute(doneJob)
    }


    /**
     * 
     * @param role
     * @param name
     * @return
     */
    private void createNewAgent(String role, String name) {
        ItemProxy agentFactory = agent.getItem("/domain/servers/localhost")
        assert agentFactory && agentFactory.getName() == "localhost"

        Job doneJob = getDoneJob(agentFactory, "CreateNewAgent")
        doneJob.setOutcome( KernelXMLUtility.getAgentXML(name: name, password: "test", Role: role) )

        agent.execute(doneJob)
    }
    
 
    /**
     * 
     * @param role
     * @param name
     * @return
     */
    private void createNewItem(String name) {
        ItemProxy factory = agent.getItem("/domain/servers/localhost")
        assert factory && factory.getName() == "localhost"

        Job doneJob = getDoneJob(factory, "CreateNewItem")
        doneJob.setOutcome( KernelXMLUtility.getItemXML( name: name, workflow: 'NoWorkflow', initialPath: '/domain/ItemTest') )

        agent.execute(doneJob)
    }


    @Test
    public void createRole() {
        String role = "TestRole-$timeStamp"

        createNewRole(role)
    }


    @Test
    public void createAgent() {
        String role = "TestRole-$timeStamp"
        String name = "TestAgent-$timeStamp"

        createNewRole(role)
        createNewAgent(role, name)
    }

    
    @Test
    public void createItem() {
        String name = "TestItem-$timeStamp"
        
        createNewItem(name)
    }
}
