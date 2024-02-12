package org.cristalise.restapi.test

import static org.cristalise.kernel.process.AbstractMain.readPropertyFiles
import org.cristalise.kernel.lifecycle.instance.predefined.BulkErase
import org.cristalise.kernel.lookup.SearchFilter
import org.cristalise.kernel.persistency.outcome.Viewpoint
import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.property.Property
import org.junit.jupiter.api.Test
import groovy.transform.CompileStatic
import io.restassured.http.ContentType

@CompileStatic
class BulkEraseTest extends RestapiTestBase {
    
    final static int numberOfItems = 27

    @Test
    public void 'BulErase Items posting XML'() throws Exception {
        Gateway.init(readPropertyFiles('src/main/bin/client.conf', 'src/main/bin/integTest.clc', null))
        login('user', 'test')

        for (def idx in 1..numberOfItems) {
            createNewItem("TestItem-${timeStamp}-${idx}", ContentType.XML)
        }

        SearchFilter sf = new SearchFilter()
        sf.setSearchRoot('/restapiTests')
        sf.properties.add(new Property('Type', 'Dummy'))

        def uid = resolveDomainPath(serverPath)
        executePredefStep(uid, BulkErase.class, ContentType.XML, Gateway.getMarshaller().marshall(sf))

        def vpString = checkViewpoint(uid, 'SearchFilter', 'last')
        def sf2 = (SearchFilter)Gateway.getMarshaller().unmarshall(vpString)

        assert sf2.getRecordsFound() == numberOfItems

        logout(null)
        Gateway.close()
    }
}
