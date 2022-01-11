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

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;

import java.io.File;

import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder;
import org.cristalise.kernel.test.persistency.XMLUtils;
import org.json.JSONObject;
import org.json.XML;
import org.junit.Before;
import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BuildStructureFromJsonTest extends XMLUtils {

    String dir = "src/test/data/outcomeBuilder";

    @Before
    public void setUp() throws Exception {
    }

    /**
     * Read JSON file to create a new XML using OutcomeBuilder which was initialized with a Schema
     */
    private void checkJson2XmlOutcome(String type, String postfix) throws Exception {
        String expected = getXML(dir, type+postfix);
        JSONObject actualJson = new JSONObject(getJSON(dir, type+postfix));

        log.info("Actual json:\n{}", actualJson.toString(2));
        OutcomeBuilder builder = new OutcomeBuilder(new Schema(type, 0, getXSD(dir, type)), true);
        builder.addJsonInstance(actualJson);

        log.info("Expected xml:\n{}", expected);
        log.info("Actual xml:\n{}", builder.getXml(false));

        assert compareXML(expected, builder.getXml());
    }

    /**
     * Read expected XML file and convert to JSON to create a new XML using OutcomeBuilder which was initialized with a Schema
     */
    private void checkXml2Json2XmlOutcome(String type, String postfix) throws Exception {
        String expected = getXML(dir, type+postfix);
        JSONObject actualJson = XML.toJSONObject(expected);

        log.info("Actual json:\n{}", actualJson.toString(2));

        // tests if the XML.toJSONObject() produces the expected JSON
        if (new File(dir, type+postfix+".json").exists()) {
            JSONObject expectedJson = new JSONObject(getJSON(dir, type+postfix));
            assertJsonEquals(expectedJson, actualJson);
        }

        OutcomeBuilder builder = new OutcomeBuilder(new Schema(type, 0, getXSD(dir, type)), true);
        builder.addJsonInstance(actualJson);;

        log.info("Expected xml:\n{}", expected);
        log.info("Actual xml:\n{}", builder.getXml());

        assert compareXML(expected, builder.getXml());
    }

    @Test
    public void integerFieldWithUnit() throws Exception {
        checkXml2Json2XmlOutcome("IntegerFieldWithUnit", "Updated");
    }

    @Test
    public void integerFieldOptional() throws Exception {
        checkXml2Json2XmlOutcome("IntegerFieldOptional", "Updated");
    }

    @Test
    public void integerFieldOptional_empty() throws Exception {
        OutcomeBuilder builder = new OutcomeBuilder(new Schema("IntegerFieldOptional", 0, getXSD(dir, "IntegerFieldOptional")), true);
        String expected = getXML(dir, "IntegerFieldOptional");

        JSONObject actualJson = new JSONObject(getJSON(dir, "IntegerFieldOptional"));

        log.info("Actual json:{}", actualJson.toString());
        builder.addJsonInstance(actualJson);;

        log.info("Expected xml:{}", expected);
        log.info("Actual xml:{}", builder.getXml());

        assert compareXML(expected, builder.getXml());
    }

    @Test
    public void booleanField() throws Exception {
        checkXml2Json2XmlOutcome("BooleanField", "Updated");
    }

    @Test
    public void rootWithAttr() throws Exception {
        checkXml2Json2XmlOutcome("RootWithAttr", "Updated");
    }

    @Test
    public void rootWithOptionalAttr() throws Exception {
        checkXml2Json2XmlOutcome("RootWithOptionalAttr", "Updated");
    }

    @Test
    public void patientDetails() throws Exception {
        checkXml2Json2XmlOutcome("PatientDetails", "Updated");
    }

    @Test
    public void employee_ComplexType_sequence() throws Exception {
        checkXml2Json2XmlOutcome("Employee", "");
    }

    @Test
    public void table_stateMachine_optionalAttribute() throws Exception {
        checkXml2Json2XmlOutcome("StateMachine", "Updated");
    }

    @Test
    public void table_optionalField() throws Exception {
        checkJson2XmlOutcome("Table", "");
        checkJson2XmlOutcome("Table", "Updated");
    }

    @Test
    public void sequence_mandatoryField_optionalField_mandatoryField() throws Exception {
        checkJson2XmlOutcome("DeviceWithLabels", "");
    }

    @Test
    public void field_contains_array_value() throws Exception {
        checkJson2XmlOutcome("EmployeeWithSkills", "");
    }
}
