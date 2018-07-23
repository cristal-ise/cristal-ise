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

import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder;
import org.cristalise.kernel.test.persistency.XMLUtils;
import org.cristalise.kernel.utils.Logger;
import org.json.JSONObject;
import org.json.XML;
import org.junit.Before;
import org.junit.Test;

public class BuildStructureFromJsonTest extends XMLUtils {

    String dir = "src/test/data/outcomeBuilder";

    @Before
    public void setUp() throws Exception {
        Logger.addLogStream(System.out, 8);
    }

    private void checkJson2XmlOutcome(String type, String postfix) throws Exception {
        JSONObject actualJson = new JSONObject(getJSON(dir, type+postfix));
        String expected = getXML(dir, type+postfix);

        Logger.msg(2, "Actual json:%s", actualJson.toString());
        OutcomeBuilder builder = new OutcomeBuilder(new Schema(type, 0, getXSD(dir, type)), true);
        builder.addJsonInstance(actualJson);;

        Logger.msg(2, "Expected xml:%s", expected);
        Logger.msg(2, "Actual xml:%s", builder.getXml());

        assert compareXML(expected, builder.getXml());
    }

    private void checkXml2Json2XmlOutcome(String type, String postFix) throws Exception {
        OutcomeBuilder builder = new OutcomeBuilder(new Schema(type, 0, getXSD(dir, type)), true);
        String expected = getXML(dir, type+postFix);
        JSONObject actualJson = XML.toJSONObject(expected);

        Logger.msg(2, "Actual json:%s", actualJson.toString());
        builder.addJsonInstance(actualJson);;

        Logger.msg(2, "Expected xml:%s", expected);
        Logger.msg(2, "Actual xml:%s", builder.getXml());

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

        Logger.msg(2, "Actual json:%s", actualJson.toString());
        builder.addJsonInstance(actualJson);;

        Logger.msg(2, "Expected xml:%s", expected);
        Logger.msg(2, "Actual xml:%s", builder.getXml());

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
        checkJson2XmlOutcome("Employee", "");
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
}
