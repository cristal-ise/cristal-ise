package org.cristalise.kernel.test.scenario;

import org.cristalise.kernel.entity.imports.ImportAgent
import org.cristalise.kernel.entity.imports.ImportRole
import org.cristalise.kernel.entity.proxy.AgentProxy
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.instance.predefined.agent.SetAgentPassword
import org.cristalise.kernel.lifecycle.instance.predefined.agent.SetAgentRoles
import org.cristalise.kernel.lifecycle.instance.predefined.agent.Sign
import org.cristalise.kernel.lifecycle.instance.predefined.server.CreateNewAgent
import org.cristalise.kernel.lifecycle.instance.predefined.server.CreateNewRole
import org.cristalise.kernel.lookup.RolePath
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.utils.CristalTestSetup

import spock.lang.Specification



/**
 * 
 */
class AgentPredefinedStepsSpecs extends Specification implements CristalTestSetup {

    AgentProxy agent
    String timeStamp = new Date().format("yyyy-MM-dd_HH-mm-ss_SSS")

    def setup()   { 
        cristalInit(8, 'src/main/bin/client.conf', 'src/main/bin/integTest.clc')
        agent = Gateway.connect('user', 'test')
        timeStamp = new Date().format("yyyy-MM-dd_HH-mm-ss_SSS")
    }

    def cleanup() { cristalCleanup() }


    private ItemProxy getServerItem() { return agent.getItem("/domain/servers/localhost") }

    private RolePath createRole(String role) {
        def importRole = new ImportRole()
        importRole.name = role
        importRole.jobList = false

        agent.execute(serverItem, CreateNewRole.class, Gateway.getMarshaller().marshall(importRole));

        return Gateway.getLookup().getRolePath(role)
    }

    private AgentProxy createAgent(String name, String role) {
        def importRole = new ImportRole()
        importRole.name = role

        def importAgent = new ImportAgent()
        importAgent.name = name
        importAgent.password = "dummepwd"
        importAgent.roles.add(importRole)

        agent.execute(serverItem, CreateNewAgent.class, Gateway.getMarshaller().marshall(importAgent));

        return Gateway.getProxyManager().getAgentProxy( Gateway.getLookup().getAgentPath(name) )
    }

    def 'SetAgentRole can be used to assign/unassing roles with agent'() {
        given:
        String role = "TestRole-$timeStamp()"
        String name = "TestAgent-$timeStamp()"

        createRole(role)
        def newAgent = createAgent(name, role)

        when:
        String[] params = [ role ];
        agent.execute(newAgent, SetAgentRoles.class, params);

        then:
        newAgent.getRoles().length == 1
        newAgent.getRoles()[0].name == role

        when:
        params = [];
        agent.execute(newAgent, SetAgentRoles.class, params);

        then:
        newAgent.getRoles().length == 0
    }

    def 'SetAgentPassword can be called by admin or by the agent itself'() {
        when: 'Admin changes the password of triggerAgent'
        String[] params = [ 'test', timeStamp ]
        def triggerAgent = Gateway.getProxyManager().getAgentProxy( Gateway.getLookup().getAgentPath('triggerAgent') )
        assert ! triggerAgent.getPath().isPasswordTemporary()
        agent.execute(triggerAgent, SetAgentPassword.class, params)

        then: 'The password becomes temporary'
        Gateway.getLookup().getAgentPath('triggerAgent').isPasswordTemporary()
        Gateway.getSecurityManager().authenticate('triggerAgent', timeStamp, null)

        when: 'triggerAgent changes its own the password'
        params = [ timeStamp, 'test' ]
        triggerAgent.execute(triggerAgent, SetAgentPassword.class, params)

        then: 'The password is NOT temporary anymore'
        ! Gateway.getLookup().getAgentPath('triggerAgent').isPasswordTemporary()
        Gateway.getSecurityManager().authenticate('triggerAgent', 'test', null)
    }

    def 'Sign can be used to create a SimpleElectronicSignature record'() {
        given:
        String role = "TestRole-$timeStamp()"
        String name = "TestAgent-$timeStamp()"

        createRole(role)
        AgentProxy newAgent = createAgent(name, role)

        when:
        StringBuffer xml = new StringBuffer("<SimpleElectonicSignature>");
        xml.append("<AgentName>").append(name)      .append("</AgentName>");
        xml.append("<Password>") .append("dummepwd").append("</Password>");

        xml.append("<ExecutionContext>");
        xml.append("<ItemPath>")     .append("ItemPath")     .append("</ItemPath>");
        xml.append("<SchemaName>")   .append("SchemaName")   .append("</SchemaName>");
        xml.append("<SchemaVersion>").append("SchemaVersion").append("</SchemaVersion>");
        xml.append("<ActivityType>") .append("ActivityType") .append("</ActivityType>");
        xml.append("<ActivityName>") .append("ActivityName") .append("</ActivityName>");
        xml.append("<StepPath>")     .append("StepPath")     .append("</StepPath>");
        xml.append("</ExecutionContext>");

        xml.append("</SimpleElectonicSignature>");
        agent.execute(newAgent, Sign.class, xml.toString());

        then:
        newAgent.getViewpoint('SimpleElectonicSignature', 'last')
    }
}
