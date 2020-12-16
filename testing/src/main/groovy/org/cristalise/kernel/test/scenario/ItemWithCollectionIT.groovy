package org.cristalise.kernel.test.scenario;

import org.cristalise.dsl.entity.ItemBuilder
import org.cristalise.kernel.collection.Dependency
import org.cristalise.kernel.entity.imports.ImportItem
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.instance.predefined.AddMemberToCollection
import org.cristalise.kernel.lifecycle.instance.predefined.AddMembersToCollection
import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.KernelScenarioTestBase
import org.cristalise.kernel.utils.CastorHashMap
import org.junit.Before
import org.junit.Test

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic


/**
 * 
 */
@CompileStatic
class ItemWithCollectionIT extends KernelScenarioTestBase {

    ItemProxy serverItem

    @Before
    public void before() {
        super.before();

        serverItem = agent.getItem("/domain/servers/localhost")
        assert serverItem && serverItem.getName() == "localhost"
    }

    private ItemProxy createItem(ImportItem ii) {
        executeDoneJob(serverItem, "CreateNewItem", Gateway.marshaller.marshall(ii))
        return Gateway.getProxyManager().getProxy( Gateway.getLookup().resolvePath(new DomainPath("/domain/$folder/${ii.name}")) )
    }

    @CompileDynamic
    private ItemProxy buildDoctor() {
        return createItem(ItemBuilder.build(name: "Doctor-$timeStamp", folder: folder) {
            Property(Type: 'Doctor')
            Dependency('Patients', false, 'Type') {
                Properties {
                    Property(Type: 'Patient')
                }
            }
        })
    }

    @CompileDynamic
    private ItemProxy buildPatient(String name) {
        return createItem(ItemBuilder.build(name: "${name}-${timeStamp}", folder: folder) {
            Property(Type: 'Patient')
        })
    }

    @Test
    public void testAddMemberToCollection_And_AddMembersToCollection() {
        def doctor = buildDoctor()
        def p1 = buildPatient('Patient1')
        CastorHashMap memberProps1 = new CastorHashMap();
        memberProps1.put("Name", "P1");
        memberProps1.put("Disease", "covid19--");

        agent.execute(doctor, AddMemberToCollection, 'Patients', p1.getPath().toString(), Gateway.marshaller.marshall(memberProps1))

        def p2 = buildPatient('Patient2')
        def p3 = buildPatient('Patient3')

        def dep = new Dependency("Patients");

        CastorHashMap memberProps2 = new CastorHashMap();
        memberProps2.put("Name", "P2");
        memberProps2.put("Disease", "covid19");
        dep.addMember(p2.getPath(), memberProps2, '');

        CastorHashMap memberProps3 = new CastorHashMap();
        memberProps3.put("Name", "P3");
        memberProps3.put("Disease", "covid19++");
        dep.addMember(p3.getPath(), memberProps3, '');

        def result = agent.execute(doctor, AddMembersToCollection, Gateway.marshaller.marshall(dep))

        def depPrime = (Dependency)Gateway.marshaller.unmarshall(result)

        assert depPrime.classProps == 'Type'
        assert depPrime.getMembers().list.size() == 3
        assert depPrime.getMember(0).getChildUUID() == p1.getPath().getUUID().toString()
        assert depPrime.getMember(0).getProperties().size() == 3
        assert depPrime.getMember(1).getChildUUID() == p2.getPath().getUUID().toString()
        assert depPrime.getMember(1).getProperties().size() == 3
        assert depPrime.getMember(2).getChildUUID() == p3.getPath().getUUID().toString()
        assert depPrime.getMember(2).getProperties().size() == 3

        CastorHashMap memberUpdate = new CastorHashMap()
        memberUpdate.put("Name", "P3a")
        memberUpdate.put("Disease", "covid19+")
        dep.updatemember(p3.getPath(), memberUpdate)
        def updateParams = new String[4]
        updateParams[0] = "Patients"
        updateParams[1] = "1"
        updateParams[2] = p3.getPath().getUUID().toString()
        updateParams[3] = Gateway.getMarshaller().marshall(memberUpdate)
        def updateResult = agent.execute(doctor, "UpdateDependencyMember", updateParams)
        def depUpdate = (Dependency)Gateway.marshaller.unmarshall(updateResult)
    }
}
