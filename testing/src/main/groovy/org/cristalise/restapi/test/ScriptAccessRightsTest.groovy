package org.cristalise.restapi.test

import static groovy.test.GroovyAssert.shouldFail
import static io.restassured.http.ContentType.JSON

import org.cristalise.kernel.common.AccessRightsException
import org.cristalise.kernel.entity.imports.ImportAgent
import org.cristalise.kernel.entity.imports.ImportRole
import org.cristalise.kernel.lifecycle.instance.predefined.server.CreateNewAgent
import org.cristalise.kernel.lifecycle.instance.predefined.server.CreateNewRole
import org.cristalise.kernel.process.Gateway
import org.junit.Test

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.restassured.http.ContentType

import javax.ws.rs.core.Response;

/**
 *
 *
 */
@Slf4j @CompileStatic
class ScriptAccessRightsTest extends RestapiTestBase {

    /**
     * 
     * @param count
     * @return
     */
    private List<String> setupPatients(int count) {
        def factory = agent.getItem("/$folder/PatientFactory")
        def createItemJob = factory.getJobByName('CreateItem', agent)
        def o = createItemJob.getOutcome()

        List<String> uuids = []

        count.times { int idx ->
            def name = "Patient${idx+1}"

            o.setField('Name', name)
            o.setField('SubFolder', timeStamp)
            agent.execute(createItemJob)

            def p = agent.getItem("$folder/Patients/$timeStamp/$name")

            executeDoneJob(p, 'SetPatientDetails')
            executeDoneJob(p, 'SetUrinSample')

            uuids << p.getPath().getUUID().toString()
        }

        return uuids
    }

    private String createRoleAndAgent(String permission, String pwd) {
        String role = "TestRole-$timeStamp"
        String agenName = "TestAgent-$timeStamp"

        ImportRole newRole = new ImportRole()
        newRole.setName(role)
        newRole.jobList = false
        newRole.permissions.add(permission)
        newRole.permissions.add('*:Login,Logout:*')

        def serverItem = agent.getItem("/domain/servers/localhost")

        agent.execute(serverItem, CreateNewRole.class, agent.marshall(newRole))

        def rp = Gateway.getLookup().getRolePath(role)

        ImportAgent newAgent = new ImportAgent("$folder/agents", agenName, pwd);
        newAgent.addRoles([rp]);

        agent.execute(serverItem, CreateNewAgent.class, agent.marshall(newAgent));

        return agenName
    }

    @Test
    public void executeScript_withPermission() {
        init('src/main/bin/client.conf', 'src/main/bin/integTest.clc')

        def uuid = setupPatients(1)[0]
        def agentName = createRoleAndAgent('Script:ACTION_EXECUTE:Patient_Aggregate', 'test')

        login(agentName, 'test')

        def result = executeScript(uuid, 'Patient_Aggregate', '{}')
        log.info "${uuid} - $result"

        logout('')

        Gateway.close()
    }

    @Test
    public void executeScript_noPermission() {
        init('src/main/bin/client.conf', 'src/main/bin/integTest.clc')

        def uuid = setupPatients(1)[0]
        def agentName = createRoleAndAgent('Script:ACTION_EXECUTE:Some_Other', 'test')

        login(agentName, 'test')

        executeScript(uuid, 'Patient_Aggregate', JSON, Response.Status.FORBIDDEN, '{}')

        //assert msg.equals("'$agentName' is NOT permitted to ACTION_EXECUTE script: ")

        logout('')

        Gateway.close()
    }
}
