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
import static org.junit.Assert.assertEquals;

import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.persistency.outcomebuilder.Field;
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder;
import org.cristalise.kernel.persistency.outcomebuilder.field.ComboField;
import org.cristalise.kernel.test.persistency.XMLUtils;
import org.json.JSONObject;
import org.json.XML;
import org.junit.Ignore;
import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BuildStructureWithArraysTest extends XMLUtils {

    String dir = "src/test/data/outcomeBuilder";


    /**
     * Read expected XML file and convert to JSON to create a new XML using OutcomeBuilder which was initialized with a Schema
     */
    private void checkXml2Json2XmlOutcome(String type, String postfix) throws Exception {
        String expected = getXML(dir, type+postfix);
        JSONObject actualJson = XML.toJSONObject(expected);

        log.info("Actual json:\n{}", actualJson.toString(2));

        // test if the XML.toJSONObject() produces the expected JSON
        JSONObject expectedJson = new JSONObject(getJSON(dir, type+postfix));
        assertJsonEquals(expectedJson, actualJson);

        OutcomeBuilder builder = new OutcomeBuilder(new Schema(type, 0, getXSD(dir, type)), true);
        builder.addJsonInstance(actualJson);;

        log.info("Expected xml:\n{}", expected);
        log.info("Actual xml:\n{}", builder.getXml());

        assert compareXML(expected, builder.getXml());
    }

    @Test @Ignore("org.json.XML.toJSONObject() cannot convert a  Field with string of json array format")
    public void singleFieldContainingArrayValue() throws Exception {
        checkXml2Json2XmlOutcome("EmployeeWithSkills", "");
    }

    @Test
    public void multipleFieldsLoadXml() throws Exception {
        String xsd      = getXSD(dir, "Fields2JsonArray");
        String expected = getXML(dir, "Fields2JsonArray");

        OutcomeBuilder actual = new OutcomeBuilder(new Schema("Fields2JsonArray", 0, xsd), expected);

        // this test is not so valuable, because the getXml() simply reads the Outcome provided in the contructor
        log.info(actual.getXml());
        assert compareXML(expected, actual.getXml());

        Field skills = (Field)actual.findChildStructure("Skills");
        ComboField comboField = (ComboField) skills.getFieldInstance();

        assertEquals("Debugging,Programming", comboField.getText());
    }

    @Test
    public void multipleFieldsConverted2JsonArray() throws Exception {
//        OutcomeBuilder builder = new OutcomeBuilder(new Schema("Fields2JsonArray", 0, getXSD(dir, "Fields2JsonArray")), true);
//        Field field = (Field)builder.findChildStructure("Skills");
//
//        String expected = getXML(dir, "Fields2JsonArray");
//        JSONObject actualJson = XML.toJSONObject(expected);
//        builder.addJsonInstance(actualJson);;

        checkXml2Json2XmlOutcome("Fields2JsonArray", "");
    }
}
