package org.cristalise.restapi.test

import static io.restassured.RestAssured.*
import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals
import static org.hamcrest.Matchers.*

import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.utils.Logger
import org.json.JSONArray
import org.junit.BeforeClass
import org.junit.Test

import groovy.transform.CompileStatic
import io.restassured.http.ContentType
import io.restassured.http.Cookie
import io.restassured.response.Response

@CompileStatic
class PathAccessTest extends RestapiTestBase {
    @Test
    public void getBulkAliases() throws Exception {
        login()
        
        Response aliasesResp =
            given()
                .accept(ContentType.JSON)
                .cookie(cauthCookie)
                .params('search', "['$userUuid', '00000000-0000-0000-0000-00000000000c', '00000000-0000-0000-0000-000000000410', '00000000-0000-0000-0000-000000010410']")
            .when()
                .get(apiUri+"/domain/aliases")
            .then()
                .statusCode(STATUS_OK)
                .extract().response()
        
        def expected ="""[{
                              "uuid": "050a754a-be0c-41aa-8f59-ab39caf4effe",
                              "name": "user",
                              "error": "Agent has no aliases"
                          },
                          {
                              "uuid": "00000000-0000-0000-0000-00000000000c",
                              "name": "Default",
                              "domainPaths": ["/domain/desc/StateMachine/kernel/Default"]
                          },
                          {
                              "uuid": "00000000-0000-0000-0000-000000000410",
                              "name": "ManagePropertyDesc",
                              "domainPaths": ["/domain/desc/ActivityDesc/kernel/ManagePropertyDesc"]
                          },
                          {
                              "uuid": "00000000-0000-0000-0000-000000010410",
                              "error": "IDL:org.cristalise.kernel/common/ObjectNotFoundException:1.0  Path does not exist:00000000-0000-0000-0000-000000010410"
                          }]"""

        assertJsonEquals(expected, aliasesResp.body.asString());
    }
}
