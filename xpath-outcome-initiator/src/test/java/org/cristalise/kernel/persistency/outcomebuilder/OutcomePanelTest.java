package org.cristalise.kernel.persistency.outcomebuilder;

import java.io.IOException;

import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder;
import org.cristalise.kernel.utils.FileStringUtility;
import org.junit.Test;

public class OutcomePanelTest {

    @Test
    public void test() throws Exception {
        OutcomeBuilder op = new OutcomeBuilder( getXSD("Module"), getXML("module"), true);
        //OutcomePanel op = new OutcomePanel( getXSD("PatientDetails"), true);
        //OutcomePanel op = new OutcomePanel( getXSD("Item"), true);
        //OutcomePanel op = new OutcomePanel( getXSD("PatientDetails"), true);

        op.initialise();
    }

    public String getXML(String name) throws IOException {
        return FileStringUtility.url2String(OutcomePanelTest.class.getResource("/"+name+".xml"));
    }

    public String getXSD(String name) throws IOException {
        return FileStringUtility.url2String(OutcomePanelTest.class.getResource("/"+name+".xsd"));
    }

}
