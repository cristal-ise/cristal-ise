package org.cristalise.restapi.test

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.json.pointer.JsonPointer

import static io.restassured.http.ContentType.JSON
import static io.restassured.http.ContentType.XML

import org.junit.jupiter.api.Test
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@Slf4j @CompileStatic
class QueryResultTest extends RestapiTestBase {
    
    String queryName = 'QueryBasicItemList'

    static JsonObject getInputs() {
        JsonObject inputs = new JsonObject()
        inputs.put('domainPath', '/domain/desc/DomainContext')
        inputs.put('offset', 0)
        inputs.put('limit', 10)
        return inputs
    }

    static void checkResult(String resultString) {
        JsonObject result = new JsonObject(resultString)
        log.info "GET JSON result: $result"

        assert result.getJsonObject('BasicItemList')
        assert JsonPointer.from('/BasicItemList/Item').queryJson(result) instanceof JsonArray 
        def items = JsonPointer.from('/BasicItemList/Item').queryJson(result) as JsonArray
        assert items.size() == 10
    }

    @Test
    void testQueryResultGetJSON() {
        login()

        checkResult executeQueryGet( queryName, 0, inputs.encode(), JSON)

        logout()
    }

    @Test
    void testQueryResultPostJSON() {
        login()
        
        checkResult executeQueryPost(queryName, 0, inputs.encode(), JSON)
        
        logout()
    }

    @Test
    void testQueryResultGetXML() {
        login()

        checkResult executeQueryGet(queryName, 0, inputs.encode(), JSON)
        
        logout()
    }
}
