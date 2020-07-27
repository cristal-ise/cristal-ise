package org.cristalise.kernel.test.scenario;

import org.cristalise.kernel.entity.agent.Job
import org.cristalise.kernel.entity.imports.ImportAgent
import org.cristalise.kernel.entity.imports.ImportItem
import org.cristalise.kernel.entity.imports.ImportPermission
import org.cristalise.kernel.entity.imports.ImportRole
import org.cristalise.kernel.entity.proxy.AgentProxy
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.instance.predefined.Erase
import org.cristalise.kernel.lifecycle.instance.predefined.server.ConfigureLogback
import org.cristalise.kernel.lifecycle.instance.predefined.server.CreateNewAgent
import org.cristalise.kernel.lifecycle.instance.predefined.server.CreateNewRole
import org.cristalise.kernel.lifecycle.instance.predefined.server.RemoveRole
import org.cristalise.kernel.lookup.RolePath
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.property.Property
import org.cristalise.kernel.test.KernelScenarioTestBase
import org.cristalise.kernel.utils.LocalObjectLoader
import org.junit.Before
import org.junit.Test

import groovy.transform.CompileStatic


/**
 * 
 */
@CompileStatic
class ItemWithoutDescriptionIT extends KernelScenarioTestBase {

    ItemProxy serverItem

    @Before
    public void before() {
        super.before();

        serverItem = agent.getItem("/domain/servers/localhost")
        assert serverItem && serverItem.getName() == "localhost"
    }

    private RolePath createRole(String roleName) {
        def role = new ImportRole()
        role.name = roleName
        role.jobList = false
        executeDoneJob(serverItem, "CreateNewRole", Gateway.marshaller.marshall(role))
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
        Job j = executeDoneJob(serverItem, "CreateNewAgent", Gateway.marshaller.marshall(agent))
        return Gateway.getProxyManager().getAgentProxy( Gateway.getLookup().getAgentPath(name) )
    }

    private ItemProxy createItem(String name) {
        def item = new ImportItem(name, '/domain/itemTest', null, 'NoWorkflow')
        item.properties.add(new Property('Type', 'Item'))
        Job j = executeDoneJob(serverItem, "CreateNewItem", Gateway.marshaller.marshall(item))
        return agent.getItem("/domain/itemTest/$name")
    }

    @Test
    public void 'CreateNewRole and RemoveRole predefined step of ServerItem'() {
        String role = "TestRole-$timeStamp"
        createRole(role)
        removeRole(role)
    }

    @Test
    public void 'CreateNewAgent with initialPath using predefined step of ServerItem'() {
        String role = "TestRole-$timeStamp"
        String name = "TestAgent-$timeStamp"

        ImportRole newRole = new ImportRole()
        newRole.setName(role)
        newRole.jobList = false
        newRole.permissions.add(new ImportPermission('dom1:Func1,Func2'))
        newRole.permissions.add(new ImportPermission('dom2:Func1:toto'))

        agent.execute(serverItem, CreateNewRole.class, agent.marshall(newRole))
        
        def rp = Gateway.getLookup().getRolePath(role)

        ImportAgent newAgent = new ImportAgent('/itemTest/agents', name, 'pwd');
        newAgent.addRoles([rp]);
    
        agent.execute(serverItem, CreateNewAgent.class, agent.marshall(newAgent));
    }

    @Test
    public void 'CreateNewAgent predefined step of ServerItem'() {
        String role = "TestRole-$timeStamp"
        String name = "TestAgent-$timeStamp"

        createRole(role)
        def newAgent = createAgent(name, role)
        agent.execute(newAgent, Erase.class)
        removeRole(role)
    }

    @Test
    public void 'CreateNewItem predefined step of ServerItem'() {
        def newItem = createItem("TestItem-$timeStamp")
        agent.execute(newItem, Erase.class)
    }

    @Test
    public void 'ConfigureLogback predefined step of ServerItem'() {
        OutcomeBuilder ob = new OutcomeBuilder(LocalObjectLoader.getSchema('LoggerConfig', 0))

        ob.addField("Root", "WARN")
        ob.addRecord('/LoggerConfig/Logger', [Name: 'org.cristalise.storage', Level: 'TRACE'])
        ob.addRecord('/LoggerConfig/Logger', [Name: 'org.apache.shiro',       Level: 'DEBUG'])

        agent.execute(serverItem, ConfigureLogback.class, ob.xml)
    }
}

