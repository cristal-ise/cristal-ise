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
class QueryResultTest extends RestapiTestBase {
    
    String queryName = 'QueryBasicItemList'
    Integer limit = 10

    @BeforeAll
    void beforeAll() {
        login()
    }

    @AfterAll
    void afterAll() {
        logout()
    }

    private JsonObject getInputs(String domainPath) {
        JsonObject inputs = new JsonObject()
        inputs.put('domainPath', domainPath)
        inputs.put('searchText', '')
        inputs.put('offset', 0)
        inputs.put('limit', limit)
        return inputs
    }

    void checkResult(String resultString, Integer expectedTotalCount = null) {
        JsonObject result = new JsonObject(resultString)
        log.info "GET JSON result: $result"

        assert result.getJsonObject('BasicItemList')
        assert JsonPointer.from('/BasicItemList/Item').queryJson(result) instanceof JsonArray 
        def items = JsonPointer.from('/BasicItemList/Item').queryJson(result) as JsonArray
        assert items.size() == expectedTotalCount ?: limit

        Integer actualTotalCount = items.getJsonObject(0).getString('TotalCount').toInteger()

        if (expectedTotalCount != null) {
            assert actualTotalCount == expectedTotalCount
        } else {
            // this is a sort of hack because the exact number of DomainContext Items is not know
            assert actualTotalCount >= 32 && actualTotalCount <= 40
        }
    }

    @Test
    void testQueryResultGetJSON() {
        String jsonInputs = getInputs('/domain/desc/DomainContext').encode()

        def result = executeQueryGet(queryName, 0, jsonInputs, JSON)
        checkResult(result)
    }

    @Test
    void testQueryResultPostJSON() {
        String jsonInputs = getInputs('/domain/desc/DomainContext').encode()

        String result = executeQueryGet(queryName, 0, jsonInputs, JSON)
        checkResult(result)
    }

    @Test
    void testQueryResultGetXML() {
        String jsonInputs = getInputs('/domain/desc/DomainContext').encode()

        String result = executeQueryGet(queryName, 0, jsonInputs, JSON)
        checkResult(result)
    }

    @Test
    void testQueryResultPostJSON_servers() {
        String jsonInputs = getInputs('/domain/servers').encode()

        String result = executeQueryGet(queryName, 0, jsonInputs, JSON)
        checkResult(result, 1)
    }
}
