package org.cristalise.restapi.test

import org.cristalise.kernel.lifecycle.instance.predefined.ImportImportAgent
import org.cristalise.kernel.lifecycle.instance.predefined.ImportImportRole
import org.junit.jupiter.api.Test

import static io.restassured.http.ContentType.JSON

import org.cristalise.kernel.entity.imports.ImportAgent
import org.cristalise.kernel.entity.imports.ImportRole
import org.cristalise.kernel.process.Gateway

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import javax.ws.rs.core.Response;

/**
 *
 *
 */
@Slf4j @CompileStatic
class ScriptAccessRightsTest extends RestapiTestBase {
    private String prefix = 'ScriptAuthz'

    /**
     * 
     * @param count
     * @return
     */
    private String setupPatient() {
        def factory = agent.getItem("/$folder/PatientFactory")
        def createItemJob = factory.getJobByName('InstantiateItem', agent)
        def o = createItemJob.getOutcome()

        def name = "${prefix}_Patient"

        o.setField('Name', name)
        o.setField('SubFolder', timeStamp)
        agent.execute(createItemJob)

        def p = agent.getItem("$folder/Patients/$timeStamp/$name")

        executeDoneJob(p, 'SetDetails')
        executeDoneJob(p, 'SetUrinSample')

        return p.getPath().getUUID().toString()
    }

    private String createRoleAndAgent(String permission, String pwd) {
        String role = "${prefix}_TestRole-$timeStamp"
        String agenName = "${prefix}_TestAgent-$timeStamp"

        ImportRole newRole = new ImportRole()
        newRole.setName(role)
        newRole.jobList = false
        newRole.permissions.add(permission)
        newRole.permissions.add('*:Login,Logout:*')

        def serverItem = agent.getItem(serverPath)

        agent.execute(serverItem, ImportImportRole.class, agent.marshall(newRole))

        def rp = Gateway.getLookup().getRolePath(role)

        ImportAgent newAgent = new ImportAgent("$folder/agents", agenName, pwd);
        newAgent.addRoles([rp]);

        agent.execute(serverItem, ImportImportAgent.class, agent.marshall(newAgent));

        return agenName
    }

    @Test
    public void executeScript_withPermission() {
        def uuid = setupPatient()
        def agentName = createRoleAndAgent('Script:ACTION_EXECUTE:Patient_Aggregate', 'test')

        login(agentName, 'test')

        def result = executeScript(uuid, 'Patient_Aggregate', '{}')
        log.info "${uuid} - $result"

        logout('')
    }

    @Test
    public void executeScript_noPermission() {
        def uuid = setupPatient()
        def agentName = createRoleAndAgent('Script:ACTION_EXECUTE:Some_Other', 'test')

        login(agentName, 'test')

        executeScript(uuid, 'Patient_Aggregate', JSON, Response.Status.UNAUTHORIZED, '{}')

        //assert msg.equals("'$agentName' is NOT permitted to ACTION_EXECUTE script: ")

        logout('')
    }
}
