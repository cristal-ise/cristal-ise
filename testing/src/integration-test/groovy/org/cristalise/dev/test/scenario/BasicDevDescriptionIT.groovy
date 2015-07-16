package org.cristalise.dev.test.scenario;

import static org.junit.Assert.*
import groovy.transform.CompileStatic

import org.cristalise.dev.test.utils.DevXMLUtility
import org.cristalise.kernel.entity.agent.Job
import org.cristalise.kernel.entity.proxy.AgentProxy
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.lookup.RolePath
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.KernelScenarioTestBase
import org.junit.Before
import org.junit.Ignore
import org.junit.Test



/**
 * 
 *
 */
@CompileStatic
class BasicDevDescriptionIT extends KernelScenarioTestBase {

    @Before
    void before() {
        super.beforeClient('src/test/conf/devClient.conf', 'src/test/conf/devServer.clc')
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

 
    @Test
    public void checkPath() {
        //Code to experiment with Lookup and LDAP based in dev module data
        
        println "------------------------------------------------------------------------"
        Gateway.getLookup().getChildren(new DomainPath("/")).each {
            println "Domain /: ${it.getClass().getName()} - $it"
        }

        println "------------------------------------------------------------------------"
        Gateway.getLookup().getChildren(new DomainPath("/agent")).each {
            println "Domain /agent: ${it.getClass().getName()} - $it"
        }

        println "------------------------------------------------------------------------"
        Gateway.getLookup().getChildren(new RolePath()).each {
            println "Role /: ${it.getClass().getName()} - $it"
        }
        
        println "------------------------------------------------------------------------"
        Gateway.getLookup().search(new DomainPath("desc"), "dev").each {
            println "Domain search : ${it.getClass().getName()} - $it"
        }

        println "------------------------------------------------------------------------"
        Gateway.getLookup().searchAliases(new ItemPath("8e0d5225-2250-432f-aceb-cd6689ca4883")).each {
            println "Item alias search: ${it.getClass().getName()} - $it"
        }
        
        println "------------------------------------------------------------------------"
        def buff = ""
        ["desc","OutcomeDesc","system","dev"].each {
            buff += "/$it"
            assert Gateway.getLookup().exists(new DomainPath(buff))
//            assert Gateway.getLookup().resolvePath(new DomainPath(buff))
        }
        
        assert Gateway.getLookup().getAgentPath("Admin")

        println "------------------------------------------------------------------------"
        Gateway.getLookup().search(new DomainPath("dev")).each {
            println it.dump()
        }
        //assert new DomainPath("/agent/Admin").getItemPath()
        
    }


    @Test @Ignore("NOT Implemented")
    public void proxyLoaderCacheTest() {
        ItemProxy eaDescFactory = agent.getItem("/domain/desc/dev/ElementaryActivityDefFactory")
        assert eaDescFactory && eaDescFactory.getName() == "ElementaryActivityDefFactory"

        eaDescFactory.queryData("AuditTrail/0")
        println "----------------------------------------------"
        //TODO: Implement testing of the cache, as this second call shall read from it
        eaDescFactory.queryData("AuditTrail/0")
    }
}
