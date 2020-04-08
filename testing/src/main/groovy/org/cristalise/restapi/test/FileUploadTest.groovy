package org.cristalise.restapi.test

import io.restassured.builder.RequestSpecBuilder
import io.restassured.response.Response
import io.restassured.specification.RequestSpecification

import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.ActivityDef
import org.cristalise.kernel.process.Gateway
import org.junit.Test

import groovy.transform.CompileStatic
import io.restassured.http.ContentType

import static io.restassured.RestAssured.given

class FileUploadTest extends RestapiTestBase {

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

        def wf = CompositeActivityDef("EmployeeWorkflow-$timeStamp", folder) {
            ElemActDef('UpdateProfile',  ea0)
            ElemActDef('UpdateContract', ea1)
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

    @Test
    public void 'Test File Upload Scenarios'() throws Exception {
        login('user', 'test')

        def itemUuid = setupItem()

        File file = File.createTempFile("xml-data-$timeStamp", ".xml")

        executeActivityMultipart(
            itemUuid,
            'UpdateProfile', 
            [
                'file': file.getBytes().toString(),
                outcome:  "<ProfileDetails><FullName>Wierd Employee</FullName><ProfilePicture>${file.getName()}</ProfilePicture></ProfileDetails>".toString()
            ]
        )
        logout(null)
    }
}
