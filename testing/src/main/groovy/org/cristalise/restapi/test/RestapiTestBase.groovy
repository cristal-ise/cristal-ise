package org.cristalise.restapi.test

import static io.restassured.RestAssured.*
import static org.hamcrest.Matchers.*

import java.time.LocalDateTime

import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStep
import org.cristalise.kernel.lifecycle.instance.predefined.server.CreateNewAgent
import org.cristalise.kernel.lifecycle.instance.predefined.server.CreateNewItem
import org.cristalise.kernel.lifecycle.instance.predefined.server.CreateNewRole
import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.test.KernelScenarioTestBase
import org.cristalise.kernel.test.utils.KernelXMLUtility
import org.json.JSONArray
import org.json.JSONObject
import org.json.XML;
import org.junit.Before
import org.junit.BeforeClass

import groovy.json.JsonBuilder
import groovy.transform.CompileStatic
import io.restassured.builder.RequestSpecBuilder
import io.restassured.http.ContentType
import io.restassured.http.Cookie
import io.restassured.response.Response
import io.restassured.specification.RequestSpecification

@CompileStatic
class RestapiTestBase extends KernelScenarioTestBase {

    static String apiUri

    String userUuid
    Cookie cauthCookie

    static final int STATUS_OK = 200

    @BeforeClass
    public static void init() {
        org.cristalise.kernel.utils.Logger.addLogStream(System.out, 5)
        Properties props = AbstractMain.readPropertyFiles("src/main/bin/client.conf", "src/main/bin/integTest.clc", null)
        apiUri = props.get('REST.URI')
    }

    @Before
    public void before() {
        timeStamp = getNowString()
    }

    public static String getNowString() {
        return LocalDateTime.now().format("yyyy-MM-dd_HH-mm-ss_SSS")
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

        //agent = Gateway.proxyManager.getAgentProxy(Gateway.lookup.getAgentPath(user))

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
                    .queryParam('descending', true)
                .when()
                    .get(apiUri+"/item/$uuid/history")
                .then()
                    .statusCode(STATUS_OK)
                .extract().response().body().asString()

            def histJson = new JSONArray(histBody)
            def event = histJson.getJSONObject(0)

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
        def predefStepName = predefStep.getSimpleName()

        if (params != null && params.length > 0) {
            def predefStepSchemaName = PredefinedStep.getPredefStepSchemaName(predefStepName);
    
            if (contentType == ContentType.JSON) {
                if (predefStepSchemaName == 'PredefinedStepOutcome') inputs = new JsonBuilder(params).toString()
                else                                                 inputs = params[0]
            }
            else {
                if (predefStepSchemaName == 'PredefinedStepOutcome') inputs = PredefinedStep.bundleData(params)
                else                                                 inputs = params[0]
            }
        }

        String responseBody = given().log().all()
            .contentType(contentType)
            .accept(ContentType.JSON)
            .cookie(cauthCookie)
            .body(inputs)
        .when()
            .post(apiUri+"/item/$uuid/workflow/predefined/"+predefStepName)
        .then()
            .statusCode(STATUS_OK)
            .extract().response().body().asString()

        return responseBody
    }

    String executeActivity(String uuid, String actPath, ContentType contentType, String outcome) {
        return given()
            .contentType(contentType)
            .accept(ContentType.JSON)
            .cookie(cauthCookie)
            .body(outcome)
        .when()
            .post(apiUri+"/item/$uuid/workflow/domain/${actPath}?transition=Done")
        .then()
            .statusCode(STATUS_OK)
            .extract().response().body().asString()
    }

    String checkAttachment(String uuid, String schema, int version, int event) {
        return given()
            .cookie(cauthCookie)
        .when()
            .get(apiUri+"/item/$uuid/attachment/$schema/$version/$event")
        .then()
            .statusCode(STATUS_OK)
        .extract().response().body().asString()
    }

    String checkOutcome(String uuid, String schema, int version, int event) {
        return given()
            .cookie(cauthCookie)
        .when()
            .get(apiUri+"/item/$uuid/outcome/$schema/$version/$event")
        .then()
            .statusCode(STATUS_OK)
        .extract().response().body().asString()
    }

    String executeActivity(String uuid, String actPath, ContentType outcomeType, String outcome, File attachment) {

        return given().log().all()
            .header("Content-Type", "multipart/form-data")
            .multiPart("outcome", outcome) // sets text/plain
            .multiPart("file", attachment) // sets application/octet-stream and fileName in Content-Disposition
            .cookie(cauthCookie)
            .accept(ContentType.JSON)
            .when()
                .post(apiUri+"/item/$uuid/workflow/domain/${actPath}?transition=Done")
            .then()
                .statusCode(STATUS_OK)
            .extract().response().asString()
    }

    String executePredefStep(String uuid,  Class<?> predefStep, String...params) {
        return executePredefStep(uuid, predefStep, ContentType.JSON, params)
    }

    String resolveDomainPath(String path) {
        String responseBody = 
        given()
            .accept(ContentType.JSON)
            .cookie(cauthCookie)
        .when()
            .get(apiUri+"/domain$path")
        .then()
            .statusCode(STATUS_OK)
            .extract().response().body().asString()

        return new JSONObject(responseBody).getString("uuid")
    }

    JSONArray resolveRole(String name) {
        String responseBody =
        given()
            .accept(ContentType.JSON)
            .cookie(cauthCookie)
        .when()
            .get(apiUri+"/role/$name")
        .then()
            .statusCode(STATUS_OK)
            .extract().response().body().asString()

        return new JSONArray(responseBody)
    }

    String createNewItem(String name, ContentType type) {
        def param = KernelXMLUtility.getItemXML(name: name, type: 'Dummy', workflow: 'NoWorkflow', initialPath: '/restapiTests')
        def serverItemUUID = resolveDomainPath('/servers/localhost')

        if (type == ContentType.JSON) param = XML.toJSONObject(param).toString()

        executePredefStep(serverItemUUID, CreateNewItem.class, type, param)
        def uuid = resolveDomainPath("/restapiTests/$name")
        assert uuid
        return uuid
    }

    String createNewAgent(String name, String pwd, ContentType type) {
        def param = KernelXMLUtility.getAgentXML(name: name, password: pwd, Role: 'Admin')
        def serverItemUUID = resolveDomainPath('/servers/localhost')

        if (type == ContentType.JSON) param = XML.toJSONObject(param).toString()

        executePredefStep(serverItemUUID, CreateNewAgent.class, type, param)

        def agents = resolveRole('Admin')
        def uuid = ""

        for (int i = 0; i < agents.length(); i++) {
            def json = agents.getJSONObject(i)

            if (json.getString('name') == name) uuid = json.getString('uuid')
        }

        assert uuid

        return uuid
    }

    void createNewRole(String name, ContentType type) {
        def param = KernelXMLUtility.getRoleXML(name: name)
        def serverItemUUID = resolveDomainPath('/servers/localhost')

        if (type == ContentType.JSON) param = XML.toJSONObject(param).toString()

        executePredefStep(serverItemUUID, CreateNewRole.class, type, param)

        resolveRole(name)
    }
}
