/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
package org.cristalise.kernel.test.process;

import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import java.util.StringTokenizer;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.persistency.outcome.OutcomeValidator;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.persistency.outcome.SchemaValidator;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.utils.FileStringUtility;
import org.junit.Before;
import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MainTest {

    @Before
    public void setup() throws InvalidDataException, IOException {
        Properties props = FileStringUtility.loadConfigFile(MainTest.class.getResource("/server.conf").getPath());
        Gateway.init(props);
    }

    @Test
    public void testBootItems() throws Exception {
        HashMap<String, OutcomeValidator> validators = new HashMap<String, OutcomeValidator>();
        validators.put("CA",      new OutcomeValidator(getSchema("CompositeActivityDef",  0, "boot/OD/CompositeActivityDef.xsd")));
        validators.put("EA",      new OutcomeValidator(getSchema("ElementaryActivityDef", 0, "boot/OD/ElementaryActivityDef.xsd")));
        validators.put("SC",      new OutcomeValidator(getSchema("Script",                0, "boot/OD/Script.xsd")));
        validators.put("SM",      new OutcomeValidator(getSchema("StateMachine",          0, "boot/OD/StateMachine.xsd")));
        validators.put("context", new OutcomeValidator(getSchema("DomainContext",         0, "boot/OD/DomainContext.xsd")));
        validators.put("OD",      new SchemaValidator());

        String bootItems = FileStringUtility.url2String(Gateway.getResource().getKernelResourceURL("boot/allbootitems.txt"));
        StringTokenizer str = new StringTokenizer(bootItems, "\n\r");
        while (str.hasMoreTokens()) {
            String thisItem = str.nextToken();
            StringTokenizer str2 = new StringTokenizer(thisItem, "/,");
            String id = str2.nextToken();
            assert id != null;
            String itemType = str2.nextToken(), resName = str2.nextToken();
            log.info("Validating " + itemType+" "+resName);
            OutcomeValidator validator = validators.get(itemType);
            String data = Gateway.getResource().getTextResource(
                    null, "boot/" + itemType + "/"+ resName + (itemType.equals("OD") ? ".xsd" : ".xml"));
            assert data != null : "Boot " + itemType + " data item " + thisItem + " not found";
            String errors = validator.validate(data);

            assert errors.length() == 0 : "Kernel resource " + itemType + " "+ resName + " has errors :" + errors;

            //Outcome and Script cannot be marshaled
            if (!itemType.equals("OD") && !itemType.equals("SC")) {
                log.info("Remarshalling " + itemType + " "+ resName);
                long then = System.currentTimeMillis();
                Object unmarshalled = Gateway.getMarshaller().unmarshall(data);
                assert unmarshalled != null;
                String remarshalled = Gateway.getMarshaller().marshall(unmarshalled);
                long now = System.currentTimeMillis();
                log.info("Marshall/remarshall of " + itemType + " "+ resName + " took " + (now - then) + "ms");
                errors = validator.validate(remarshalled);
                assert errors.length() == 0 : "Remarshalled resource " + itemType + " "+ resName + " has errors :" + errors + "\nRemarshalled form:\n" + remarshalled;
                // XMLDiff cannot be used here, because remarshalled xml will have lot of extra optional attributes
                //assert new Outcome(data).isIdentical(new Outcome(remarshalled));
            }

            if (itemType.equals("SC")) {
                log.info("Parsing script " + resName);
                new Script(resName, 0, null, data);
            }
        }
    }

    private static Schema getSchema(String name, int version, String resPath) throws ObjectNotFoundException {
        return new Schema(name, version, null, Gateway.getResource().getTextResource(null, resPath));
    }

    @Test
    public void testScriptParsing() throws Exception {
        OutcomeValidator valid = new OutcomeValidator(getSchema("Script", 0, "boot/OD/Script.xsd"));

        String testScriptString = FileStringUtility.url2String(MainTest.class.getResource("/TestScript.xml"));
        String errors = valid.validate(testScriptString);
        assert errors.length() == 0 : "Test script not valid to schema: " + errors;

        Script testScript = new Script("TestScript", 0, null, testScriptString);
        assert testScript.getInputParams().size() == 1 : "Script input param count wrong";
        assert testScript.getInputParams().get("test") != null : "Could not retrieve script input param value";
        testScript.setInputParamValue("test", "Test");
        assert testScript.getInputParams().get("test").getInitialised() : "Script is not initialized when it should be";

        Object result = testScript.execute();
        assert result != null : "Script returned null result";
        assert result instanceof String : "Script failed to return a String";
        assert ((String) result).equals("TestTest") : "Script failed to produce correct result: " + result;
    }

    @Test
    public void testStateMachine() throws Exception {
        log.info("Validating test state machine");
        String smXml = FileStringUtility.url2String(MainTest.class.getResource("/TestStateMachine.xml"));
        StateMachine sm = (StateMachine) Gateway.getMarshaller().unmarshall(smXml);
        sm.validate();
        assert sm.isCoherent() : "Test StateMachine is reporting that it is not coherent";
    }
}
