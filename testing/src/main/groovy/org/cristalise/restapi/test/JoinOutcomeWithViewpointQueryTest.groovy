package org.cristalise.restapi.test

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.json.pointer.JsonPointer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

import static io.restassured.http.ContentType.JSON

import org.junit.jupiter.api.Test
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@Slf4j @CompileStatic
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JoinOutcomeWithViewpointQueryTest extends RestapiTestBase {

    String queryName = 'JoinOutcomeWithViewpoint'

    @BeforeAll
    void beforeAll() {
        login()
    }

    @AfterAll
    void afterAll() {
        logout()
    }

    private JsonObject getInputs(String uuid, String schema) {
        JsonObject inputs = new JsonObject()
        inputs.put('uuid', uuid)
        inputs.put('schema', schema)
        inputs.put('offset', 0)
        inputs.put('limit', 10)
        return inputs
    }

    void checkResult(String resultString) {
        JsonObject result = new JsonObject(resultString)
        log.info "POST JSON result: $result"

        assert result.getJsonObject('OutcomeWithViewpoint')
        assert JsonPointer.from('/OutcomeWithViewpoint/Record').queryJson(result) instanceof JsonArray
    }

    @Test
    void testQueryResultPostJSON() {
        String jsonInputs = getInputs(userUuid, 'DummySchema').encode()

        String result = executeQueryPost(queryName, 0, jsonInputs, JSON)
        checkResult(result)
    }
}
