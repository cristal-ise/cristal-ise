package org.cristalise.restapi.test

import io.restassured.builder.RequestSpecBuilder
import io.restassured.response.Response
import io.restassured.specification.RequestSpecification

import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.ActivityDef
import org.cristalise.kernel.process.Gateway
import org.junit.jupiter.api.Test

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import io.restassured.http.ContentType

import static io.restassured.RestAssured.given

@CompileStatic
class FileUploadTest extends RestapiTestBase {

    @CompileDynamic
    private String setupItem() {
        init('src/main/bin/client.conf', 'src/main/bin/integTest.clc')

        def schema0 = Schema("ProfileDetails-$timeStamp", folder) {
            struct(name: 'ProfileDetails') {
                field(name: 'FullName', type: 'string')
                field(name: 'ProfilePicture', type: 'string')
            }
        }

        def ea0 = ElementaryActivityDef("UpdateProfile-$timeStamp", folder) {
            Property(OutcomeInit: "Empty")
            Schema(schema0)
        }

        def schema1 = Schema("ContractDetails-$timeStamp", folder) {
            struct(name: 'ContractDetails') {
                field(name: 'Position', type: 'string')
                field(name: 'ContractFilePfd', type: 'string')
            }
        }

        def ea1 = ElementaryActivityDef("UpdateContract-$timeStamp", folder) {
            Property(OutcomeInit: "Empty")
            Schema(schema1)
        }

        def schema2 = Schema("ListOfPublications-$timeStamp", folder) {
            struct(name: 'ListOfPublications') {
                field(name: 'Comment', type: 'string')
                field(name: 'TextFile', type: 'string')
            }
        }

        def ea2 = ElementaryActivityDef("UpdateListOfPublications-$timeStamp", folder) {
            Property(OutcomeInit: "Empty")
            Schema(schema2)
        }

        def wf = CompositeActivityDef("EmployeeWorkflow-$timeStamp", folder) {
            Layout {
                ElemActDef('UpdateProfile',  ea0)
                ElemActDef('UpdateContract', ea1)
                ElemActDef('UpdateListOfPublications', ea2)
            }
        }

        def factory = DescriptionItem("EmployeeFactory-$timeStamp", folder) {
            PropertyDesc(name: "Type", defaultValue: "Employee", isMutable: false, isClassIdentifier: true)
            Workflow(wf)
        }

        createNewItemByFactory(factory, "CreateNewInstance", "Employee-$timeStamp", folder)
        def uuid = agent.getItem("$folder/Employee-$timeStamp").getPath().getUUID().toString()

        Gateway.close()
        
        return uuid
    }

    /**
     * Use this for development when descriptions do not change anymore.
     * Creare a new item based on existing Descriptions and Factory
     * @param ts timestamp used previously to create a factory
     * @return
     */
    private String setupItem(String factoryTimestamp) {
        init('src/main/bin/client.conf', 'src/main/bin/integTest.clc')

        def factory = agent.getItem("$folder/EmployeeFactory-$factoryTimestamp")
        createNewItemByFactory(factory, "CreateNewInstance", "Employee-$timeStamp", folder)
        def uuid = agent.getItem("$folder/Employee-$timeStamp").getPath().getUUID().toString()

        Gateway.close()

        return uuid
    }

    @Test
    public void TestOutcomeAttachmentScenarios() throws Exception {
        login('user', 'test')

        def itemUuid = setupItem()
//        def itemUuid = setupItem('2020-04-09_18-28-31_097')
//        timeStamp = '2020-04-09_18-28-31_097'

        File profilePic    = new File('src/main/data', 'ProfilePicture.png')
        File contractPdf   = new File('src/main/data', 'Contract.pdf')
        File listOfPubsTxt = new File('src/main/data', 'ListOfPublications.txt')

        executeActivity(
            itemUuid,
            'UpdateProfile', 
            ContentType.JSON,
            "{'ProfileDetails': {'FullName': 'Wierd Employee','ProfilePicture': '${profilePic.getName()}'}}",
            profilePic,
        )

        checkOutcome(   itemUuid, "ProfileDetails-$timeStamp", 0, 3)
        checkAttachment(itemUuid, "ProfileDetails-$timeStamp", 0, 3)

        executeActivity(
            itemUuid,
            'UpdateContract', 
            ContentType.XML,
            "<ContractDetails><Position>Dummy Manager</Position><ContractFilePfd>${contractPdf.getName()}</ContractFilePfd></ContractDetails>",
            //"{'ContractDetails': {'Position': 'Dummy Manager','ContractFilePfd': '${contractPdf.getName()}'}}",
            contractPdf,
        )

        checkOutcome(   itemUuid, "ContractDetails-$timeStamp", 0, 4)
        checkAttachment(itemUuid, "ContractDetails-$timeStamp", 0, 4)

        executeActivity(
            itemUuid,
            'UpdateListOfPublications',
            ContentType.JSON,
            "{'ListOfPublications': {'Comment': 'Dummy Comment','TextFile': '${listOfPubsTxt.getName()}'}}",
            listOfPubsTxt,
        )

        checkOutcome(   itemUuid, "ListOfPublications-$timeStamp", 0, 5)
        checkAttachment(itemUuid, "ListOfPublications-$timeStamp", 0, 5)

        logout(null)
    }
}
