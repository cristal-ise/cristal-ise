package org.cristalise.dev.test.scenario;

import static org.junit.Assert.*
import groovy.transform.CompileStatic

import org.cristalise.dev.test.utils.DevXMLUtility
import org.cristalise.kernel.entity.agent.Job
import org.cristalise.kernel.entity.proxy.AgentProxy
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway
import org.junit.After
import org.junit.Before
import org.junit.Test



/**
 * 
 * @author kovax
 *
 */
@CompileStatic
class BasicDevDescriptionIT {

    AgentProxy agent
    String timeStamp

    @Before
    void before() {
        String[] args = ['-logLevel', '8', '-config', 'bin/client.conf', '-connect', 'bin/dev.clc']

        Properties props = AbstractMain.readC2KArgs(args)
        Gateway.init(props)
        agent = Gateway.connect("dev", "test")

        timeStamp = new Date().format("yyyy-MM-dd_HH:mm:ss_SSS")
    }

    @After
    void tearDown() {
        Gateway.close()
    }


    /**
     * 
     * @param eaFactory
     * @param actName
     * @return
     */
    private Job getDoneJob(ItemProxy proxy, String actName) {
        Job j = proxy.getJobByName(actName, agent)
        assert j && j.getStepName() == actName
        return j
    }


    private void createNewEADesc(String name, String folder) {
        ItemProxy eaDescFactory = agent.getItem("/domain/desc/dev/ElementaryActivityDefFactory")
        assert eaDescFactory && eaDescFactory.getName() == "ElementaryActivityDefFactory"

        Job doneJob = getDoneJob(eaDescFactory, "CreateNewElementaryActivityDef")
        doneJob.setOutcome( DevXMLUtility.getNewDevObjectDefXML(name: name, folder: folder) )

        agent.execute(doneJob)
    }


    private void editEADesc(String name, String folder, String role, String schema) {
        ItemProxy eaDesc = agent.getItem("/domain/desc/ActivityDesc/$folder/$name")
        assert eaDesc && eaDesc.getName() == name

        Job doneJob = getDoneJob(eaDesc, "EditDefinition")
        doneJob.setOutcome( DevXMLUtility.getActivityDefXML(Name: name, AgentRole: role, SchemaType: schema) )

        agent.execute(doneJob)
    }


    @Test
    public void createAndEditEADesc() {
        String name = "TestEADesc-$timeStamp"
        String folder = "TestEADesc"

        createNewEADesc(name, folder)
        editEADesc(name, folder, "Admin", "Errors")
	}

}
