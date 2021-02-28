package org.cristalise.kernel.test.scenario;

import org.cristalise.kernel.collection.Dependency
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.instance.predefined.AddMemberToCollection
import org.cristalise.kernel.lifecycle.instance.predefined.AddMembersToCollection
import org.cristalise.kernel.lifecycle.instance.predefined.UpdateDependencyMember
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.KernelScenarioTestBase
import org.cristalise.kernel.utils.CastorHashMap
import org.junit.Before
import org.junit.Test

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

    /**
     *
     * @param count
     * @return
     */
    private List<ItemProxy> setupPatients(int count) {
        def factory = agent.getItem("/$folder/PatientFactory")
        def createItemJob = factory.getJobByName('InstantiateItem', agent)
        def o = createItemJob.getOutcome()

        List<ItemProxy> patients = []

        count.times { int idx ->
            def name = "Patient-${timeStamp}"

            o.setField('Name', name)
            //o.setField('SubFolder', timeStamp)
            agent.execute(createItemJob)

            def p = agent.getItem("$folder/Patients/$name")

            //executeDoneJob(p, 'SetDetails')
            //executeDoneJob(p, 'SetUrinSample')

            patients << p
        }

        return patients
    }

    /**
     *
     * @param count
     * @return
     */
    private List<ItemProxy> setupDoctors(int count) {
        def factory = agent.getItem("/$folder/DoctorFactory")
        def createItemJob = factory.getJobByName('InstantiateItem', agent)
        def o = createItemJob.getOutcome()

        List<ItemProxy> doctors = []

        count.times { int idx ->
            def name = "Doctor-${timeStamp}"

            o.setField('Name', name)
            //o.setField('SubFolder', timeStamp)
            agent.execute(createItemJob)

            doctors << agent.getItem("$folder/Doctors/$name")
        }

        return doctors
    }

    @Test
    public void testAddMemberToCollection_AddMembersToCollection_UpdateDependencyMember() {
        def doctor = setupDoctors(1)[0]
        def patients = setupPatients(3)

        def dep0 = new Dependency("Patients");

        CastorHashMap memberProps1 = new CastorHashMap();
        memberProps1.put("Name", "P1");
        memberProps1.put("Disease", "covid19--");
        //memberProps1.put("MemberUpdateSchema", "Patient:0");
        dep0.addMember(patients[0].getPath(), memberProps1, '', null);

        agent.execute(doctor, AddMemberToCollection, Gateway.marshaller.marshall(dep0))

        def dep = new Dependency("Patients");

        CastorHashMap memberProps2 = new CastorHashMap();
        memberProps2.put("Name", "P2");
        memberProps2.put("Disease", "covid19");
        //memberProps2.put("MemberUpdateSchema", "Patient:0");
        dep.addMember(patients[1].getPath(), memberProps2, '', null);

        CastorHashMap memberProps3 = new CastorHashMap();
        memberProps3.put("Name", "P3");
        memberProps3.put("Disease", "covid19++");
        memberProps3.put("MemberUpdateSchema", "Patient:0");
        dep.addMember(patients[2].getPath(), memberProps3, '', null);

        def result = agent.execute(doctor, AddMembersToCollection, Gateway.marshaller.marshall(dep))

        def depPrime = (Dependency)Gateway.marshaller.unmarshall(result)

        assert depPrime.classProps == 'Type'
        assert depPrime.getMembers().list.size() == 3
        assert depPrime.getMember(0).getChildUUID() == patients[0].getPath().getUUID().toString()
        assert depPrime.getMember(0).getProperties().size() == 3
        assert depPrime.getMember(1).getChildUUID() == patients[1].getPath().getUUID().toString()
        assert depPrime.getMember(1).getProperties().size() == 3
        assert depPrime.getMember(2).getChildUUID() == patients[02].getPath().getUUID().toString()
        assert depPrime.getMember(2).getProperties().size() == 4

        CastorHashMap memberUpdate = new CastorHashMap()
        memberUpdate.put("Name", "P3a")
        memberUpdate.put("Disease", "covid19+")
        def updateParams = new String[4]
        updateParams[0] = "Patients"
        updateParams[1] = "2"
        updateParams[2] = patients[2].getPath().getUUID().toString()
        updateParams[3] = Gateway.getMarshaller().marshall(memberUpdate)
        agent.execute(doctor, UpdateDependencyMember, updateParams)

        def depFinal = (Dependency)doctor.getCollection('Patients')
        assert depFinal.classProps == 'Type'
        assert depFinal.getMembers().list.size() == 3

        assert depFinal.getMember(0).getChildUUID() == patients[0].getPath().getUUID().toString()
        assert depFinal.getMember(0).getProperties().size() == 3
        assert depFinal.getMember(0).getProperties()['Name'] == 'P1'
        assert depFinal.getMember(0).getProperties()['Type'] == 'Patient'
        assert depFinal.getMember(0).getProperties()['Disease'] == 'covid19--'
        // Property 'MemberUpdateSchema' was not added to this member

        assert depFinal.getMember(1).getChildUUID() == patients[1].getPath().getUUID().toString()
        assert depFinal.getMember(1).getProperties().size() == 3
        assert depFinal.getMember(1).getProperties()['Name'] == 'P2'
        assert depFinal.getMember(1).getProperties()['Type'] == 'Patient'
        assert depFinal.getMember(1).getProperties()['Disease'] == 'covid19'
        // Property 'MemberUpdateSchema' was not added to this member

        assert depFinal.getMember(2).getChildUUID() == patients[2].getPath().getUUID().toString()
        assert depFinal.getMember(2).getProperties().size() == 4
        assert depFinal.getMember(2).getProperties()['Name'] == 'P3a'
        assert depFinal.getMember(2).getProperties()['Type'] == 'Patient'
        assert depFinal.getMember(2).getProperties()['Disease'] == 'covid19+'
        assert depFinal.getMember(2).getProperties()['MemberUpdateSchema'] == 'Patient:0'
    }

    @Test
    public void testAutomaticUpdateOfBidirectionalDependency() {
        def doctor = setupDoctors(1)[0]
        def patient = setupPatients(1)[0]

        def addPatientJob = doctor.getJobByName('AddPatient', agent)
        assert addPatientJob
        def o = addPatientJob.getOutcome()
        o.setField('MemberName', patient.name)

        agent.execute(addPatientJob)

        def depPatients = (Dependency)doctor.getCollection('Patients')

        assert depPatients.getMembers().list.size() == 1
        assert depPatients.getMember(0).getChildUUID() == patient.getPath().getUUID().toString()

        def depDoctor = (Dependency)patient.getCollection('Doctor')

        assert depDoctor.getMembers().list.size() == 1
        assert depDoctor.getMember(0).getChildUUID() == doctor.getPath().getUUID().toString()
    }
}
