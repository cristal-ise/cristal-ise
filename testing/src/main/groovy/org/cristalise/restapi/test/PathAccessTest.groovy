package org.cristalise.restapi.test

import static io.restassured.RestAssured.*
import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals

import org.junit.jupiter.api.Test

import groovy.transform.CompileStatic
import io.restassured.http.ContentType
import io.restassured.response.Response

@CompileStatic
class PathAccessTest extends RestapiTestBase {
    @Test
    public void getBulkAliases() throws Exception {
        login()

        //Thread.sleep(31000); //response cookie is added when 30s is passed between the cookie creation and the request

        Response aliasesResp =
            given()
                .accept(ContentType.JSON)
                .cookie(cauthCookie)
                .params('search', "['$userUuid', '00000000-0000-0000-0000-00000000000c', '00000000-0000-0000-0000-000000000410', '00000000-0000-0000-0000-000000010410']")
            .when()
                .get(apiUri+"/domain/aliases")
            .then()
                //.cookie('cauth') //response cookie is added when 30s is passed between the cookie creation and the request
                .statusCode(STATUS_OK)
                .extract().response()

        def expected ="""[{
                              "uuid": "$userUuid",
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
                              "error": "Path does not exist:00000000-0000-0000-0000-000000010410"
                          }]""".toString()

        assertJsonEquals(expected, aliasesResp.body.asString());
    }

    @Test
    public void getContextTree() throws Exception {
        login()

        Response contextTreeResp =
            given()
                .accept(ContentType.JSON)
                .cookie(cauthCookie)
                .params('search', 'tree')
            .when()
                .get(apiUri+"/domain/desc")
            .then()
                .statusCode(STATUS_OK)
                .extract().response()

        def json = contextTreeResp.body.jsonPath().getJsonObject('rows[0]') as Map

        assert json.type == 'domain'
        assert json.name == 'ActivityDesc'
        assert json.path == '/domain/desc/ActivityDesc'
        assert json.url == 'http://localhost:8081/api/domain/desc/ActivityDesc'

        json = contextTreeResp.body.jsonPath().getJsonObject('rows[1]') as Map

        assert json.type == 'domain'
        assert json.name == 'dev'
        assert json.path == '/domain/desc/ActivityDesc/dev'
        assert json.url == 'http://localhost:8081/api/domain/desc/ActivityDesc/dev'

        json = contextTreeResp.body.jsonPath().getJsonObject('rows[2]') as Map

        assert json.type == 'domain'
        assert json.name == 'integTest'
        assert json.path == '/domain/desc/ActivityDesc/integTest'
        assert json.url == 'http://localhost:8081/api/domain/desc/ActivityDesc/integTest'
    }
}
