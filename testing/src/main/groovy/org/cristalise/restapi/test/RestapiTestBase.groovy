package org.cristalise.restapi.test

import static io.restassured.RestAssured.*
import static io.restassured.http.ContentType.JSON
import static org.cristalise.kernel.security.BuiltInAuthc.ADMIN_ROLE
import static org.cristalise.restapi.RestHandler.PASSWORD
import static org.cristalise.restapi.RestHandler.USERNAME
import static org.hamcrest.Matchers.*

import java.nio.charset.StandardCharsets

import org.cristalise.kernel.entity.imports.ImportAgent
import org.cristalise.kernel.entity.imports.ImportItem
import org.cristalise.kernel.entity.imports.ImportRole
import org.cristalise.kernel.lifecycle.instance.predefined.ImportImportAgent
import org.cristalise.kernel.lifecycle.instance.predefined.ImportImportItem
import org.cristalise.kernel.lifecycle.instance.predefined.ImportImportRole
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStep
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.property.Property
import org.cristalise.kernel.test.KernelScenarioTestBase
import org.json.JSONArray
import org.json.JSONObject
import org.json.XML;
import org.junit.jupiter.api.BeforeAll

import groovy.json.JsonBuilder
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.restassured.http.ContentType
import io.restassured.http.Cookie
import io.restassured.response.Response

@CompileStatic @Slf4j
class RestapiTestBase extends KernelScenarioTestBase {

    static String apiUri

    String userUuid
    Cookie cauthCookie
    String serverPath = '/servers/localhost'

    static final int STATUS_OK = 200

    @BeforeAll
    public void init() {
        Properties props = AbstractMain.readPropertyFiles("src/main/bin/client.conf", "src/main/bin/integTest.clc", null)
        apiUri = props.get('REST.URI')

        serverPath = '/servers/' + InetAddress.getLocalHost().getHostName()
    }

    public static String encodeString(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.ISO_8859_1))
    }

    def login() {
        login 'user', 'test'
    }

    def loginPost() {
        loginPost 'user', 'test'
    }

    def loginPost(String user, String pwd) {
        JSONObject inputs = new JSONObject((USERNAME): encodeString(user), (PASSWORD): encodeString(pwd))

        Response loginResp =
            given().log().all()
                .contentType(JSON)
                .accept(JSON)
                .body(inputs.toString())
            .when()
                .post(apiUri+"/login")
            .then()
                .cookie('cauth')
                .statusCode(STATUS_OK)
                .extract().response()

        log.debug('loginPost() - response:{}', loginResp.body().asString())

        cauthCookie = loginResp.getDetailedCookie('cauth');
        userUuid = loginResp.body().jsonPath().getString('Login.uuid')

        assert userUuid && ItemPath.isUUID(userUuid)
    }

    def login(String user, String pwd) {
        Response loginResp =
            given()
                .accept(JSON)
                .params('user', user, 'pass', pwd )
            .when()
                .get(apiUri+"/login")
            .then()
                .cookie('cauth')
                .statusCode(STATUS_OK)
            .extract().response()

        log.debug('login() - response:{}', loginResp.body().asString())

        cauthCookie = loginResp.getDetailedCookie('cauth');
        userUuid = loginResp.body().jsonPath().getString('Login.uuid')

        assert userUuid && ItemPath.isUUID(userUuid)
    }

    def logout(String reason = null) {
        if (reason) {
            given()
                .accept(JSON)
                .cookie(cauthCookie)
                .params('reason', reason)
           .when()
                .get(apiUri+"/logout")
           .then()
                .statusCode(STATUS_OK)
        }
        else {
            given()
                .accept(JSON)
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
                    .accept(JSON)
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
                .accept(JSON)
                .cookie(cauthCookie)
            .when()
                .get(apiUri+"/item/$userUuid/history/$id")
            .then()
                .statusCode(STATUS_OK)
                .body('activity.name', equalTo(name))

            return id
        }
    }

    private String getJobForm(String urlPostFix, String uuid, String activityPath, String transition) {
        def responseBody = given().log().all()
            .accept(JSON)
                .cookie(cauthCookie)
                .queryParam('transition', transition)
            .when()
                .get("${apiUri}/item/$uuid/job/form${urlPostFix}/${activityPath}".toString())
            .then()
                .statusCode(STATUS_OK)
            .extract().response().body().asString()

        return responseBody
    }

    String getJobFormTemplate(String uuid, String activityPath, String transition) {
        return getJobForm('Template', uuid, activityPath, transition)
    }
    
    String getJobFormModel(String uuid, String activityPath, String transition) {
        return getJobForm('Model', uuid, activityPath, transition)
    }
    
    String getJobFormLayout(String uuid, String activityPath, String transition) {
        return getJobForm('Layout', uuid, activityPath, transition)
    }

    String executePredefStep(String uuid,  Class<?> predefStep, ContentType contentType = JSON, String...params) {
        def inputs = ""
        def predefStepName = predefStep.getSimpleName()

        if (params != null && params.length > 0) {
            def predefStepSchemaName = PredefinedStep.getPredefStepSchemaName(predefStepName);
    
            if (contentType == JSON) {
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
            .accept(JSON)
            .cookie(cauthCookie)
            .body(inputs)
        .when()
            .post(apiUri+"/item/$uuid/workflow/predefined/"+predefStepName)
        .then()
            .statusCode(STATUS_OK)
        .extract().response().body().asString()

        return responseBody
    }

    String executeActivity(String uuid, String actPath, ContentType contentType = JSON, String outcome) {
        return given()
            .contentType(contentType)
            .accept(JSON)
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

    String checkViewpoint(String uuid, String schema, String view) {
        return given()
            .cookie(cauthCookie)
        .when()
            .get(apiUri+"/item/$uuid/viewpoint/$schema/$view")
        .then()
            .statusCode(STATUS_OK)
        .extract().response().body().asString()
    }

    String executeActivity(String uuid, String actPath, ContentType outcomeType = JSON, String outcome, File attachment) {
        return given().log().all()
            .header("Content-Type", "multipart/form-data")
            .multiPart("outcome", outcome) // sets text/plain
            .multiPart("file", attachment) // sets application/octet-stream and fileName in Content-Disposition
            .cookie(cauthCookie)
            .accept(JSON)
            .when()
                .post(apiUri+"/item/$uuid/workflow/domain/${actPath}?transition=Done")
            .then()
                .statusCode(STATUS_OK)
            .extract().response().asString()
    }

    String executeScript(String uuid, String scriptName, ContentType contentType = JSON, String inputs) {
        return given()
            .contentType(contentType)
            .accept(JSON)
            .cookie(cauthCookie)
            .body(inputs)
        .when()
            .post(apiUri+"/item/$uuid/scriptResult/?script=${scriptName}&version=0")
        .then()
            .statusCode(STATUS_OK)
        .extract().response().body().asString()
    }

    String resolveDomainPath(String path) {
        if (path.startsWith('/')) path = path.substring(1)

        String responseBody = 
        given()
            .accept(JSON)
            .cookie(cauthCookie)
        .when()
            .get(apiUri+"/domain/$path")
        .then()
            .statusCode(STATUS_OK)
        .extract().response().body().asString()

        return new JSONObject(responseBody).getString("uuid")
    }

    JSONArray resolveRole(String name) {
        String responseBody =
        given()
            .accept(JSON)
            .cookie(cauthCookie)
        .when()
            .get(apiUri+"/role/$name")
        .then()
            .statusCode(STATUS_OK)
        .extract()
            .response().body().asString()

        return new JSONArray(responseBody)
    }

    String createNewItem(String name, ContentType type) {
        def newItem = new ImportItem(name, '/restapiTests', null, 'NoWorkflow')
        newItem.getProperties().add(new Property('Type', 'Dummy', false))
        def serverItemUUID = resolveDomainPath(serverPath)

        def param = Gateway.marshaller.marshall(newItem)
        if (type == JSON) param = XML.toJSONObject(param).toString()

        executePredefStep(serverItemUUID, ImportImportItem.class, type, param)

        def uuid = resolveDomainPath("/restapiTests/$name")
        assert uuid && ItemPath.isUUID(uuid)

        return uuid
    }

    String createNewAgent(String name, String pwd, ContentType type) {
        def newAgent = new ImportAgent(name, pwd)
        newAgent.addRole(ADMIN_ROLE)
        def param = Gateway.marshaller.marshall(newAgent)
        def serverItemUUID = resolveDomainPath(serverPath)

        if (type == JSON) param = XML.toJSONObject(param).toString()
        executePredefStep(serverItemUUID, ImportImportAgent.class, type, param)

        def agents = resolveRole('Admin')
        def uuid = ""

        for (int i = 0; i < agents.length(); i++) {
            def json = agents.getJSONObject(i)

            if (json.getString('name') == name) uuid = json.getString('uuid')
        }

        assert uuid && ItemPath.isUUID(uuid)

        return uuid
    }

    void createNewRole(String name, ContentType type) {
        def newRole = new ImportRole(name)
        newRole.jobList = false
        def param = Gateway.marshaller.marshall(newRole)
        def serverItemUUID = resolveDomainPath(serverPath)

        if (type == JSON) param = XML.toJSONObject(param).toString()

        executePredefStep(serverItemUUID, ImportImportRole.class, type, param)

        resolveRole(name)
    }
}
