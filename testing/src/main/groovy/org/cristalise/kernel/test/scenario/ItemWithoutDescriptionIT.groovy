package org.cristalise.kernel.test.scenario

import groovy.transform.CompileStatic

import org.cristalise.kernel.entity.Job
import org.cristalise.kernel.entity.imports.ImportAgent
import org.cristalise.kernel.entity.imports.ImportItem
import org.cristalise.kernel.entity.imports.ImportRole
import org.cristalise.kernel.entity.proxy.AgentProxy
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.instance.predefined.Erase
import org.cristalise.kernel.lifecycle.instance.predefined.server.ConfigureLogback
import org.cristalise.kernel.lifecycle.instance.predefined.server.RemoveRole
import org.cristalise.kernel.lookup.RolePath
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.property.Property
import org.cristalise.kernel.test.KernelScenarioTestBase
import org.cristalise.kernel.utils.LocalObjectLoader
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

/**
 * 
 */
@CompileStatic
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ItemWithoutDescriptionIT extends KernelScenarioTestBase {

    private RolePath createRole(String roleName) {
        def role = new ImportRole()
        role.name = roleName
        role.jobList = false
        executeDoneJob(serverItem, 'CreateNewRole', Gateway.marshaller.marshall(role))
        return Gateway.getLookup().getRolePath(roleName);
    }

    private void removeRole(String role) {
        String[] params = [ role ];
        agent.execute(serverItem, RemoveRole.class, params);
        assert ! Gateway.getLookup().exists(new RolePath(role, false));
    }

    private AgentProxy createAgent(String name, String roleName) {
        def agent = new ImportAgent(name, 'test')
        def role = new ImportRole()
        role.name = roleName
        agent.roles.add(role)
        executeDoneJob(serverItem, 'CreateNewAgent', Gateway.marshaller.marshall(agent))
        return Gateway.getAgentProxy( Gateway.getLookup().getAgentPath(name) )
    }

    private ItemProxy createItem(String name) {
        def item = new ImportItem(name, '/domain/itemTest', null, 'NoWorkflow')
        item.properties.add(new Property('Type', 'Item'))
        Job j = executeDoneJob(serverItem, 'CreateNewItem', Gateway.marshaller.marshall(item))
        return agent.getItem("/domain/itemTest/$name")
    }

    @Test @Order(1)
    public void 'CreateNewRole and RemoveRole predefined step'() {
        String role = "TestRole-$timeStamp"
        createRole(role)
        removeRole(role)
    }

    @Test @Order(1)
    public void 'CreateNewAgentpredefined step with initialPath'() {
        def role = "TestRole-$timeStamp"
        def name = "TestAgent-$timeStamp"
        def folder = '/itemTest/agents' // i.e. initialPath

        ImportRole newRole = new ImportRole()
        newRole.setName(role)
        newRole.jobList = false
        newRole.permissions.add('dom1:Func1,Func2:')
        newRole.permissions.add('dom2:Func1:toto')

        executeDoneJob(serverItem, 'CreateNewRole', agent.marshall(newRole))

        newRole.jobList = null
        newRole.permissions.clear()
        ImportAgent newAgent = new ImportAgent(folder, name, 'pwd');
        newAgent.roles.add(newRole)

        executeDoneJob(serverItem, 'CreateNewAgent', agent.marshall(newAgent))

        assert agent.getItem("$folder/$name")
    }

    @Test @Order(1)
    public void 'CreateNewAgent predefined step'() {
        String role = "TestRole-$timeStamp"
        String name = "TestAgent-$timeStamp"

        createRole(role)
        def newAgent = createAgent(name, role)
        agent.execute(newAgent, Erase.class)
        removeRole(role)
    }

    @Test @Order(1)
    public void 'CreateNewItem predefined step'() {
        def newItem = createItem("TestItem-$timeStamp")
        agent.execute(newItem, Erase.class)
    }

    @Test @Order(2)
    public void 'ConfigureLogback predefined step'() {
        def ob = new OutcomeBuilder(LocalObjectLoader.getSchema('LoggerConfig', 0))

        ob.addField("Root", "WARN")
        ob.addRecord('/LoggerConfig/Logger', [Name: 'org.cristalise.storage', Level: 'TRACE'])
        ob.addRecord('/LoggerConfig/Logger', [Name: 'org.apache.shiro',       Level: 'DEBUG'])

        agent.execute(serverItem, ConfigureLogback.class, ob.xml)
    }

    @Test @Order(3)
    public void 'Reset ConfigureLogback predefined'() {
        def ob = new OutcomeBuilder(LocalObjectLoader.getSchema('LoggerConfig', 0))

        ob.addField("Root", "INFO")

        agent.execute(serverItem, ConfigureLogback.class, ob.xml)
    }
}
