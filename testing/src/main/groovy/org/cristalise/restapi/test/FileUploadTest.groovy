package org.cristalise.restapi.test

import io.restassured.builder.RequestSpecBuilder
import io.restassured.response.Response
import io.restassured.specification.RequestSpecification
import org.junit.Test

import groovy.transform.CompileStatic
import io.restassured.http.ContentType

@CompileStatic
class FileUploadTest extends RestapiTestBase {

    @Test
    public void 'Test File Upload Scenarios'() throws Exception {
        login('hmws', 'test')
        def name = "TestItem-$timeStamp"
        def itemUuid = createNewItem(name, ContentType.XML)

        def path = apiUri + "/item/$itemUuid/workflow/domain/Update?transition=Done"
        Map<String, String> formData  = new HashMap<String, String>()
        formData.put("file", File.createTempFile("xml-data", "xml").getBytes().toString())
        formData.put("outcome", "<File_Details><FileName>working-pro2.xml</FileName><Size>2124</Size><Type>text/xml</Type></File_Details>")

        Response res = doPostForm(path, formData)
        if (res.getStatusCode() == STATUS_OK) {
            System.out.println(res.getBody().toString())
        }
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
        return req.post(url);
    }
}
