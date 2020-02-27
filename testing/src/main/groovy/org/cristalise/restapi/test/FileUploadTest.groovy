package org.cristalise.restapi.test

import io.restassured.builder.RequestSpecBuilder
import io.restassured.response.Response
import io.restassured.specification.RequestSpecification
import org.junit.Test

import groovy.transform.CompileStatic
import io.restassured.http.ContentType

import static io.restassured.RestAssured.given

@CompileStatic
class FileUploadTest extends RestapiTestBase {

    @Test
    public void 'Test File Upload Scenarios'() throws Exception {
        login('user', 'test')
        def name = "TestItem-$timeStamp"
        def itemUuid = createNewItem(name, ContentType.XML)

        def path = apiUri + "/item/$itemUuid/workflow/domain/Update?transition=Done"
        Map<String, String> formData  = new HashMap<String, String>()
        File file = File.createTempFile("xml-data-$timeStamp", ".xml")
        formData.put("file", file.getBytes().toString())
        String fileName = file.getName()
        formData.put("outcome", "<File_Details><FileName>"+ fileName +"</FileName><Size>2124</Size><Type>text/xml</Type></File_Details>")

        Response res = doPostForm(path, formData)
        System.out.println(res.body.asString())

    }

    protected Response doPostForm(String url, Map<String, String> formData) {
        RequestSpecBuilder requestSpecBuilder = new RequestSpecBuilder();
        requestSpecBuilder.setContentType("multipart/form-data")
        for (Map.Entry<String,String> entry : formData.entrySet())
        {
            String key = entry.getKey()
            String value = entry.getValue()
            requestSpecBuilder.addMultiPart(key, value)
        }
        RequestSpecification req = requestSpecBuilder.build();
        return given()
                    .spec(req)
                    .cookie(cauthCookie)
                .when()
                    .post(url)
                .then()
                    .statusCode(STATUS_OK)
                    .extract().response()
    }
}
