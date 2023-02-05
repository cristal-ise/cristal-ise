package org.cristalise.restapi.test

import org.cristalise.kernel.lifecycle.instance.predefined.agent.Sign
import org.junit.jupiter.api.Test

import groovy.transform.CompileStatic
import io.restassured.http.ContentType

@CompileStatic
class AgentPredefinedStepsTest extends RestapiTestBase {

    @Test
    public void 'Sign can be used to create a SimpleElectronicSignature record'() {
        def name = "TestAgent-$timeStamp"
        login('user', 'test')
        createNewAgent(name, 'test', ContentType.XML)
        logout(null)

        login(name, 'test')

        StringBuffer xml = new StringBuffer("<SimpleElectonicSignature>");
        xml.append("<AgentName>").append(name)  .append("</AgentName>");
        xml.append("<Password>") .append("test").append("</Password>");

        xml.append("<ExecutionContext>");
        xml.append("<ItemPath>")     .append("ItemPath")     .append("</ItemPath>");
        xml.append("<SchemaName>")   .append("SchemaName")   .append("</SchemaName>");
        xml.append("<SchemaVersion>").append("SchemaVersion").append("</SchemaVersion>");
        xml.append("<ActivityType>") .append("ActivityType") .append("</ActivityType>");
        xml.append("<ActivityName>") .append("ActivityName") .append("</ActivityName>");
        xml.append("<StepPath>")     .append("StepPath")     .append("</StepPath>");
        xml.append("</ExecutionContext>");

        xml.append("</SimpleElectonicSignature>");
        executePredefStep(userUuid, Sign.class, ContentType.XML, xml.toString())

        logout(null)
    }
}
