package org.cristalise.restapi.test

import static io.restassured.RestAssured.*
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
class RestapiTestBase {

    static String apiUri

    static String user = 'user'
    static String pwd = 'test'
    
    String userUuid
    Cookie cauthCookie

    static final int STATUS_OK = 200

    @BeforeClass
    public static void init() {
        Logger.addLogStream(System.out, 5)
        Properties props = AbstractMain.readPropertyFiles("src/main/bin/client.conf", "src/main/bin/integTest.clc", null)
        apiUri = props.get('REST.URI')
    }

    def login() {
        Response loginResp =
            given()
                .accept(ContentType.JSON)
                .params('user', user, 'pass', pwd )
            .when()
                .get(apiUri+"/login")
            .then()
                .cookie('cauth')
                .statusCode(STATUS_OK)
                .extract().response()

        cauthCookie = loginResp.getDetailedCookie('cauth');
        userUuid = loginResp.body().jsonPath().getString('Login.uuid.value')

        assert userUuid
    }

    def logout(String reason) {
        if (reason) {
            given()
                .accept(ContentType.JSON)
                .cookie(cauthCookie)
                .params('reason', reason)
           .when()
                .get(apiUri+"/logout")
           .then()
                .statusCode(STATUS_OK)
        }
        else {
            given()
                .accept(ContentType.JSON)
                .cookie(cauthCookie)
            .when()
                .get(apiUri+"/logout")
            .then()
                .statusCode(STATUS_OK)
        }
    }

    int checkEvent(Integer id, String name) {
        if (id == null) {
            String histBody = 
                given()
                    .accept(ContentType.JSON)
                    .cookie(cauthCookie)
                .when()
                    .get(apiUri+"/item/$userUuid/history")
                .then()
                    .statusCode(STATUS_OK)
                .extract().response().body().asString()

            def histJson = new JSONArray(histBody)
            def event = histJson.getJSONObject(histJson.length()-1)

            assert event && event.getJSONObject('activity').get('name') == name

            return event.getInt('id')
        }
        else {
            given()
                .accept(ContentType.JSON)
                .cookie(cauthCookie)
            .when()
                .get(apiUri+"/item/$userUuid/history/$id")
            .then()
                .statusCode(STATUS_OK)
            .body('activity.name', equalTo(name))

            return id
        }
    }
}
