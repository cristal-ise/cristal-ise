package org.cristalise.restapi.test

import static io.restassured.RestAssured.*
import static org.hamcrest.Matchers.*

import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStep
import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.utils.Logger
import org.json.JSONArray
import org.junit.BeforeClass
import org.junit.Test

import groovy.json.JsonBuilder
import groovy.transform.CompileStatic
import io.restassured.http.ContentType
import io.restassured.http.Cookie
import io.restassured.response.Response

@CompileStatic
class RestapiTestBase {

    static String apiUri

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
        login 'user', 'test'
    }

    def login(String user, String pwd) {
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
        return checkEvent(userUuid, id, name)
    }

    int checkEvent(String uuid, Integer id, String name) {
        if (id == null) {
            String histBody = 
                given()
                    .accept(ContentType.JSON)
                    .cookie(cauthCookie)
                .when()
                    .get(apiUri+"/item/$uuid/history")
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

    String executePredefStep(String uuid,  Class<?> predefStep, ContentType contentType, String...params) {
        def inputs = ""
        
        if (params == null) params = new String[0]

        if (contentType == ContentType.JSON) inputs = new JsonBuilder(params).toString()
        else inputs = PredefinedStep.bundleData(params)

        String responseBody = given()
            .contentType(contentType)
            .accept(ContentType.JSON)
            .cookie(cauthCookie)
            .body(inputs)
        .when()
            .post(apiUri+"/item/$uuid/workflow/predefined/"+predefStep.getSimpleName())
        .then()
            .statusCode(STATUS_OK)
            .extract().response().body().asString()

        return responseBody
    }

    String executePredefStep(String uuid,  Class<?> predefStep, String...params) {
        return executePredefStep(uuid, predefStep, ContentType.JSON, params)
    }
}
