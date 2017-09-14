package org.cristalise.kernel.persistency.outcomebuilder;

import java.io.IOException;

import org.cristalise.kernel.utils.FileStringUtility;
import org.junit.Test;

public class OutcomeBulderTest {

    @Test
    public void test() throws Exception {
        //OutcomeBuilder op = new OutcomeBuilder( getXSD("Module"), getXML("module"), true);
        OutcomeBuilder op = new OutcomeBuilder( getXSD("PatientDetails"), true);
        //OutcomeBuilder op = new OutcomeBuilder( getXSD("Item"), true);
        //OutcomeBuilder op = new OutcomeBuilder( getXSD("PatientDetails"), true);

        op.initialise();
    }

    public String getXML(String name) throws IOException {
        return FileStringUtility.url2String(OutcomeBulderTest.class.getResource("/"+name+".xml"));
    }

    public String getXSD(String name) throws IOException {
        return FileStringUtility.url2String(OutcomeBulderTest.class.getResource("/"+name+".xsd"));
    }

}
