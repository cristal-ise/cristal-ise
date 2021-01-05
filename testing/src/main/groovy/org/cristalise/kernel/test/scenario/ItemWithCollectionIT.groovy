package org.cristalise.kernel.test.scenario;

import static org.cristalise.kernel.persistency.ClusterType.COLLECTION

import org.cristalise.dsl.entity.ItemBuilder
import org.cristalise.kernel.collection.Dependency
import org.cristalise.kernel.entity.imports.ImportItem
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.instance.predefined.AddMemberToCollection
import org.cristalise.kernel.lifecycle.instance.predefined.AddMembersToCollection
import org.cristalise.kernel.lifecycle.instance.predefined.UpdateDependencyMember
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
                    Property(MemberUpdateSchema: 'Patient:0')
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
    public void testAddMemberToCollection_AddMembersToCollection_UpdateDependencyMember() {
        def doctor = buildDoctor()
        def patient1 = buildPatient('Patient1')
        CastorHashMap memberProps1 = new CastorHashMap();
        memberProps1.put("Name", "P1");
        memberProps1.put("Disease", "covid19--");
        //memberProps1.put("MemberUpdateSchema", "Patient:0");
        
        agent.execute(doctor, AddMemberToCollection, 'Patients', patient1.getPath().toString(), Gateway.marshaller.marshall(memberProps1))

        def patient2 = buildPatient('Patient2')
        def patient3 = buildPatient('Patient3')

        def dep = new Dependency("Patients");

        CastorHashMap memberProps2 = new CastorHashMap();
        memberProps2.put("Name", "P2");
        memberProps2.put("Disease", "covid19");
        //memberProps2.put("MemberUpdateSchema", "Patient:0");
        dep.addMember(patient2.getPath(), memberProps2, '', null);

        CastorHashMap memberProps3 = new CastorHashMap();
        memberProps3.put("Name", "P3");
        memberProps3.put("Disease", "covid19++");
        memberProps3.put("MemberUpdateSchema", "Patient:0");
        dep.addMember(patient3.getPath(), memberProps3, '', null);

        def result = agent.execute(doctor, AddMembersToCollection, Gateway.marshaller.marshall(dep))

        def depPrime = (Dependency)Gateway.marshaller.unmarshall(result)

        assert depPrime.classProps == 'Type'
        assert depPrime.getMembers().list.size() == 3
        assert depPrime.getMember(0).getChildUUID() == patient1.getPath().getUUID().toString()
        assert depPrime.getMember(0).getProperties().size() == 3
        assert depPrime.getMember(1).getChildUUID() == patient2.getPath().getUUID().toString()
        assert depPrime.getMember(1).getProperties().size() == 3
        assert depPrime.getMember(2).getChildUUID() == patient3.getPath().getUUID().toString()
        assert depPrime.getMember(2).getProperties().size() == 4

        CastorHashMap memberUpdate = new CastorHashMap()
        memberUpdate.put("Name", "P3a")
        memberUpdate.put("Disease", "covid19+")
        def updateParams = new String[4]
        updateParams[0] = "Patients"
        updateParams[1] = "2"
        updateParams[2] = patient3.getPath().getUUID().toString()
        updateParams[3] = Gateway.getMarshaller().marshall(memberUpdate)
        agent.execute(doctor, UpdateDependencyMember, updateParams)

        def depFinal = (Dependency)doctor.getCollection('Patients')
        assert depFinal.classProps == 'Type'
        assert depFinal.getMembers().list.size() == 3

        assert depFinal.getMember(0).getChildUUID() == patient1.getPath().getUUID().toString()
        assert depFinal.getMember(0).getProperties().size() == 3
        assert depFinal.getMember(0).getProperties()['Name'] == 'P1'
        assert depFinal.getMember(0).getProperties()['Type'] == 'Patient'
        assert depFinal.getMember(0).getProperties()['Disease'] == 'covid19--'
        // Property 'MemberUpdateSchema' was not added to this member

        assert depFinal.getMember(1).getChildUUID() == patient2.getPath().getUUID().toString()
        assert depFinal.getMember(1).getProperties().size() == 3
        assert depFinal.getMember(1).getProperties()['Name'] == 'P2'
        assert depFinal.getMember(1).getProperties()['Type'] == 'Patient'
        assert depFinal.getMember(1).getProperties()['Disease'] == 'covid19'
        // Property 'MemberUpdateSchema' was not added to this member

        assert depFinal.getMember(2).getChildUUID() == patient3.getPath().getUUID().toString()
        assert depFinal.getMember(2).getProperties().size() == 4
        assert depFinal.getMember(2).getProperties()['Name'] == 'P3a'
        assert depFinal.getMember(2).getProperties()['Type'] == 'Patient'
        assert depFinal.getMember(2).getProperties()['Disease'] == 'covid19+'
        assert depFinal.getMember(2).getProperties()['MemberUpdateSchema'] == 'Patient:0'
    }
}
