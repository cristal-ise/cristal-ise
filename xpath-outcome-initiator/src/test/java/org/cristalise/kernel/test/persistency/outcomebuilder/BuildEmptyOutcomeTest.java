/**
 * This file is part of the CRISTAL-iSE XPath Outcome Initiator module.
 * Copyright (c) 2001-2016 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.kernel.test.persistency.outcomebuilder;

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.persistency.outcomebuilder.Field;
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.test.persistency.XMLUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BuildEmptyOutcomeTest extends XMLUtils {

    String dir = "src/test/data/outcomeBuilder";

    @Before
    public void setUp() throws Exception {
        Properties props = new Properties();
        props.put("Webui.inputField.boolean.defaultValue", "false");
        props.put("Webui.inputField.decimal.defaultValue", "0.0");
        props.put("Webui.inputField.integer.defaultValue", "0");
        props.put("Authenticator", "Shiro");
        Gateway.init(props);
    }

    private void checkEmptyOutcome(String type) throws Exception {
        String xsd      = getXSD(dir, type);
        String expected = getXML(dir, type);

        OutcomeBuilder actual = new OutcomeBuilder(new Schema(type, 0, xsd));

        log.info(actual.getXml());

        assert compareXML(expected, actual.getXml());
    }

    @Test
    public void anyField() throws Exception {
        checkEmptyOutcome("AnyField");
    }

    @Test
    public void booleanField() throws Exception {
        checkEmptyOutcome("BooleanField");
    }

    @Test @Ignore
    public void integerFieldWithUnit() throws Exception {
        checkEmptyOutcome("IntegerFieldWithUnit");
    }

    @Test
    public void integerFieldOptional() throws Exception {
        checkEmptyOutcome("IntegerFieldOptional");
    }

    @Test
    public void rootWithAttr() throws Exception {
        checkEmptyOutcome("RootWithAttr");
    }

    @Test
    public void rootWithOptionalAttr() throws Exception {
        checkEmptyOutcome("RootWithOptionalAttr");
    }

    @Test @Ignore
    public void patientDetails() throws Exception {
        String type = "PatientDetails";
        String xsd  = getXSD(dir, type);

        Map<String, String> args = new HashMap<>();
        args.put("CURRENTDATE", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        CompiledTemplate expr = TemplateCompiler.compileTemplate(getXML(dir, type));
        String expected = (String)TemplateRuntime.execute(expr, args);

        OutcomeBuilder actual = new OutcomeBuilder(new Schema(type, 0, xsd));

        log.info(actual.getXml());

        assert compareXML(expected, actual.getXml());
    }

    @Test
    public void stateMachine() throws Exception {
        checkEmptyOutcome("StateMachine");
    }

    @Test
    public void module() throws Exception {
        checkEmptyOutcome("Module");
    }

    @Test
    public void buildEmptyStringField_SetField() throws Exception {
        OutcomeBuilder actual = new OutcomeBuilder(new Schema("StringField", 0, getXSD(dir, "StringField")));

        actual.putField("characters", "");

        log.info(actual.getXml());

        assert compareXML(getXML("StringField"), actual.getXml());
    }

    @Test
    public void multiRootXSDFile() throws Exception {
        Schema schema = new Schema("Storage", 0, getXSD(dir, "Storage"));

        OutcomeBuilder ob = new OutcomeBuilder("StorageDetails", schema);
        log.info(ob.getXml());

        ob = new OutcomeBuilder("StorageAmount", schema);
        log.info(ob.getXml());

        ob = new OutcomeBuilder("Storage", schema);
        log.info(ob.getXml());
    }

    @Test
    public void getReferencedItemType() throws Exception {
        OutcomeBuilder builder = new OutcomeBuilder("Reference", new Schema("Reference", 0, getXSD(dir, "Reference")), false);
        Field field = (Field)builder.findChildStructure("ItemRef");
        
        assertEquals("UnitTest", field.getAppInfoNodeElementValue("reference", "itemType"));
    }

    @Test
    public void checkDependnecyXsd() throws Exception {
        String xsd = Gateway.getResource().getTextResource(null, "boot/OD/Dependency.xsd");
        new OutcomeBuilder(new Schema("Dependency", 0, null, xsd));
    }
}
