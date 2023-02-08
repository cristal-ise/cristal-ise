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
package org.cristalise.kernel.test.persistency.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder;
import org.cristalise.kernel.persistency.outcomebuilder.utils.OutcomeUtils;
import org.cristalise.kernel.test.persistency.XMLUtils;
import org.json.JSONObject;
import org.junit.Test;

public class OutcomeUtilsTest {

    @Test
    public void outcomeOfStringHasValue() throws Exception {
        String type = "OptionalStringField";
        String xsd = XMLUtils.getXSD("src/test/data/outcomeBuilder", type);
        OutcomeBuilder ob = new OutcomeBuilder(new Schema(type, 0, xsd));

        assertFalse(OutcomeUtils.hasField(ob.getOutcome(true), "characters"));
        assertFalse(OutcomeUtils.hasValue(ob.getOutcome(true), "characters"));

        ob.addField("/StringField/characters");
        Outcome outcome = ob.getOutcome(true);
        assertTrue (OutcomeUtils.hasField(outcome, "characters"));
        assertFalse(OutcomeUtils.hasValue(outcome, "characters"));

        outcome.setField("characters", "");
        assertTrue (OutcomeUtils.hasField(outcome, "characters"));
        assertFalse(OutcomeUtils.hasValue(outcome, "characters"));

        outcome.setField("characters", "string");
        assertTrue (OutcomeUtils.hasField(outcome, "characters"));
        assertFalse(OutcomeUtils.hasValue(outcome, "characters"));

        outcome.setField("characters", "null");
        assertTrue (OutcomeUtils.hasField(outcome, "characters"));
        assertFalse(OutcomeUtils.hasValue(outcome, "characters"));

        //valid value
        outcome.setField("characters", "toto");
        assertTrue (OutcomeUtils.hasField(outcome, "characters"));
        assertTrue(OutcomeUtils.hasValue(outcome, "characters"));
    }

    @Test
    public void mapOfStringHasValue() {
        Map<String, String> map = new HashMap<String, String>();

        assertFalse(OutcomeUtils.hasField(map, "toto"));
        assertFalse(OutcomeUtils.hasValue(map, "toto"));

        map.put("toto", null);
        assertTrue(OutcomeUtils.hasField(map, "toto"));
        assertFalse(OutcomeUtils.hasValue(map, "toto"));

        map.put("toto", "");
        assertTrue (OutcomeUtils.hasField(map, "toto"));
        assertFalse(OutcomeUtils.hasValue(map, "toto"));

        map.put("toto", "string");
        assertTrue (OutcomeUtils.hasField(map, "toto"));
        assertFalse(OutcomeUtils.hasValue(map, "toto"));

        map.put("toto", "null");
        assertTrue (OutcomeUtils.hasField(map, "toto"));
        assertFalse(OutcomeUtils.hasValue(map, "toto"));

        //valid value
        map.put("toto", "kovax");
        assertTrue(OutcomeUtils.hasField(map, "toto"));
        assertTrue(OutcomeUtils.hasValue(map, "toto"));
    }

    @Test
    public void mapOfIntegerHasValue() {
        Map<String, Integer> map = new HashMap<String, Integer>();

        assertFalse(OutcomeUtils.hasField(map, "toto"));
        assertFalse(OutcomeUtils.hasValue(map, "toto"));

        map.put("toto", null);
        assertTrue (OutcomeUtils.hasField(map, "toto"));
        assertFalse(OutcomeUtils.hasValue(map, "toto"));

        //valid value
        map.put("toto", Integer.valueOf("100"));
        assertTrue(OutcomeUtils.hasField(map, "toto"));
        assertTrue(OutcomeUtils.hasValue(map, "toto"));
    }

    @Test
    public void jsonOfStringHasValue() {
        JSONObject json = new JSONObject();

        assertFalse(OutcomeUtils.hasField(json, "toto"));
        assertFalse(OutcomeUtils.hasValue(json, "toto"));

        // JSONObject.put() does not add/update null value
        json.put("toto", JSONObject.NULL);
        assertTrue (OutcomeUtils.hasField(json, "toto"));
        assertFalse(OutcomeUtils.hasValue(json, "toto"));

        json.put("toto", "");
        assertTrue (OutcomeUtils.hasField(json, "toto"));
        assertFalse(OutcomeUtils.hasValue(json, "toto"));

        json.put("toto", "string");
        assertTrue (OutcomeUtils.hasField(json, "toto"));
        assertFalse(OutcomeUtils.hasValue(json, "toto"));

        json.put("toto", "null");
        assertTrue (OutcomeUtils.hasField(json, "toto"));
        assertFalse(OutcomeUtils.hasValue(json, "toto"));

        //valid value
        json.put("toto", "kovax");
        assertTrue(OutcomeUtils.hasField(json, "toto"));
        assertTrue(OutcomeUtils.hasValue(json, "toto"));
    }

    @Test
    public void jsonOfIntegerHasValue() {
        JSONObject json = new JSONObject();

        assertFalse(OutcomeUtils.hasField(json, "toto"));
        assertFalse(OutcomeUtils.hasValue(json, "toto"));

        // JSONObject.put() does not add/update null value
        json.put("toto", JSONObject.NULL);
        assertTrue (OutcomeUtils.hasField(json, "toto"));
        assertFalse(OutcomeUtils.hasValue(json, "toto"));

        //valid value
        json.put("toto", Integer.valueOf("100"));
        assertTrue(OutcomeUtils.hasField(json, "toto"));
        assertTrue(OutcomeUtils.hasValue(json, "toto"));
    }

    @Test
    public void checkEmptyInputs() throws Exception {
        Map<String, Object> map = new HashMap<>();
        JSONObject json = new JSONObject();
        Outcome outcome = new Outcome("<root></root>");

        Object[] inputs = {map, json, outcome};

        for (Object input : inputs) {
            assertNull(OutcomeUtils.getBigDecimalOrNull(input, "toto"));
            assertNull(OutcomeUtils.getBigIntegerOrNull(input, "toto"));
            assertNull(OutcomeUtils.getBooleanOrNull(input, "toto"));
            assertNull(OutcomeUtils.getStringOrNull(input, "toto"));
            assertNull(OutcomeUtils.getLocalDateOrNull(input, "toto"));
            assertNull(OutcomeUtils.getLocalDateTimeOrNull(input, "toto"));
            assertNull(OutcomeUtils.getOffsetTimeOrNull(input, "toto"));
            assertNull(OutcomeUtils.getOffsetDateTimeOrNull(input, "toto"));
        }
    }

    @Test
    public void checkNullValues() throws Exception {
        Map<String, Object> map = new HashMap<>();
        JSONObject json = new JSONObject();

        map.put("toto", null);
        json.put("toto", JSONObject.NULL); // JSONObject.put() does not add/update null
        Outcome outcome = new Outcome("<root><toto/></root>");

        Object[] inputs = {map, json, outcome};

        for (Object input : inputs) {
            assertNull(OutcomeUtils.getBigDecimalOrNull(input, "toto"));
            assertNull(OutcomeUtils.getBigIntegerOrNull(input, "toto"));
            assertNull(OutcomeUtils.getBooleanOrNull(input, "toto"));
            assertNull(OutcomeUtils.getStringOrNull(input, "toto"));
            assertNull(OutcomeUtils.getLocalDateOrNull(input, "toto"));
            assertNull(OutcomeUtils.getLocalDateTimeOrNull(input, "toto"));
            assertNull(OutcomeUtils.getOffsetTimeOrNull(input, "toto"));
            assertNull(OutcomeUtils.getOffsetDateTimeOrNull(input, "toto"));
        }
    }

    @Test
    public void checkBlankStringValues() throws Exception {
        Map<String, Object> map = new HashMap<>();
        JSONObject json = new JSONObject();

        map.put("toto", "");
        json.put("toto", "");
        Outcome outcome = new Outcome("<root><toto></toto></root>");

        Object[] inputs = {map, json, outcome};

        for (Object input : inputs) {
            assertNull(OutcomeUtils.getBigDecimalOrNull(input, "toto"));
            assertNull(OutcomeUtils.getBigIntegerOrNull(input, "toto"));
            assertNull(OutcomeUtils.getBooleanOrNull(input, "toto"));
            assertNull(OutcomeUtils.getStringOrNull(input, "toto"));
            assertNull(OutcomeUtils.getLocalDateOrNull(input, "toto"));
            assertNull(OutcomeUtils.getLocalDateTimeOrNull(input, "toto"));
            assertNull(OutcomeUtils.getOffsetTimeOrNull(input, "toto"));
            assertNull(OutcomeUtils.getOffsetDateTimeOrNull(input, "toto"));
        }
    }

    @Test
    public void checkInvalidStringValues() throws Exception {
        Map<String, Object> map = new HashMap<>();
        JSONObject json = new JSONObject();

        map.put("string", "string");
        map.put("null", "null");
        json.put("string", "string");
        json.put("null", "null");
        Outcome outcome = new Outcome("<root><string>string</string><null>null</null></root>");

        Object[] inputs = {map, json, outcome};

        for (Object input : inputs) {
            String inputClass = input.getClass().getSimpleName();

            assertNull("getBigDecimalOrNull("+inputClass+")",     OutcomeUtils.getBigDecimalOrNull(input, "string"));
            assertNull("getBigIntegerOrNull("+inputClass+")",     OutcomeUtils.getBigIntegerOrNull(input, "string"));
            assertNull("getBooleanOrNull("+inputClass+")",        OutcomeUtils.getBooleanOrNull(input, "string"));
            assertNull("getStringOrNull("+inputClass+")",         OutcomeUtils.getStringOrNull(input, "string"));
            assertNull("getLocalDateOrNull("+inputClass+")",      OutcomeUtils.getLocalDateOrNull(input, "string"));
            assertNull("getLocalDateTimeOrNull("+inputClass+")",  OutcomeUtils.getLocalDateTimeOrNull(input, "string"));
            assertNull("getOffsetTimeOrNull("+inputClass+")",     OutcomeUtils.getOffsetTimeOrNull(input, "string"));
            assertNull("getOffsetDateTimeOrNull("+inputClass+")", OutcomeUtils.getOffsetDateTimeOrNull(input, "string"));

            assertNull("getBigDecimalOrNull("+inputClass+")",     OutcomeUtils.getBigDecimalOrNull(input, "null"));
            assertNull("getBigIntegerOrNull("+inputClass+")",     OutcomeUtils.getBigIntegerOrNull(input, "null"));
            assertNull("getBooleanOrNull("+inputClass+")",        OutcomeUtils.getBooleanOrNull(input, "null"));
            assertNull("getStringOrNull("+inputClass+")",         OutcomeUtils.getStringOrNull(input, "null"));
            assertNull("getLocalDateOrNull("+inputClass+")",      OutcomeUtils.getLocalDateOrNull(input, "null"));
            assertNull("getLocalDateTimeOrNull("+inputClass+")",  OutcomeUtils.getLocalDateTimeOrNull(input, "null"));
            assertNull("getOffsetTimeOrNull("+inputClass+")",     OutcomeUtils.getOffsetTimeOrNull(input, "null"));
            assertNull("getOffsetDateTimeOrNull("+inputClass+")", OutcomeUtils.getOffsetDateTimeOrNull(input, "null"));
        }
    }

    @Test
    public void checkIntegerValue() throws Exception {
        Map<String, Object> map = new HashMap<>();
        JSONObject json = new JSONObject();

        map.put("toto", 100);
        json.put("toto", 100);
        Outcome outcome = new Outcome("<root><toto>100</toto></root>");

        Object[] inputs = {map, json, outcome};

        for (Object input : inputs) {
            assertEquals(new BigInteger("100"), OutcomeUtils.getBigIntegerOrNull(input, "toto"));
            assertEquals(new BigDecimal("100"), OutcomeUtils.getBigDecimalOrNull(input, "toto"));
            assertEquals("100",                 OutcomeUtils.getStringOrNull(input, "toto"));

            assertNull(OutcomeUtils.getBooleanOrNull(input, "toto"));
            assertNull(OutcomeUtils.getLocalDateOrNull(input, "toto"));
            assertNull(OutcomeUtils.getLocalDateTimeOrNull(input, "toto"));
            assertNull(OutcomeUtils.getOffsetTimeOrNull(input, "toto"));
            assertNull(OutcomeUtils.getOffsetDateTimeOrNull(input, "toto"));
        }
    }

    @Test
    public void checkDecimalValue() throws Exception {
        Map<String, Object> map = new HashMap<>();
        JSONObject json = new JSONObject();

        map.put("toto", 100.1d);
        json.put("toto", 100.1d);
        Outcome outcome = new Outcome("<root><toto>100.1</toto></root>");

        Object[] inputs = {map, json, outcome};

        for (Object input : inputs) {
            String inputClass = input.getClass().getSimpleName();

            assertEquals("getBigDecimalOrNull("+inputClass+")", new BigDecimal("100.1"), OutcomeUtils.getBigDecimalOrNull(input, "toto"));
            assertEquals("getStringOrNull("+inputClass+")",     "100.1",                 OutcomeUtils.getStringOrNull(input, "toto"));

            //Outcome only handles String
            if (input instanceof Outcome) {
                assertNull("getBigIntegerOrNull("+inputClass+")", OutcomeUtils.getBigIntegerOrNull(input, "toto"));
            }
            else {
                assertEquals("getBigIntegerOrNull("+inputClass+")", new BigInteger("100"), OutcomeUtils.getBigIntegerOrNull(input, "toto"));
            }

            assertNull("getBooleanOrNull("+inputClass+")",          OutcomeUtils.getBooleanOrNull(input, "toto"));
            assertNull("getLocalDateOrNull("+inputClass+")",        OutcomeUtils.getLocalDateOrNull(input, "toto"));
            assertNull("getLocalDateTimeOrNull("+inputClass+")",    OutcomeUtils.getLocalDateTimeOrNull(input, "toto"));
            assertNull("getOffsetTimeOrNull("+inputClass+")",       OutcomeUtils.getOffsetTimeOrNull(input, "toto"));
            assertNull("getOffsetDateTimeOrNull("+inputClass+")",   OutcomeUtils.getOffsetDateTimeOrNull(input, "toto"));
        }
    }

    @Test
    public void checkBooleanValue_False() throws Exception {
        Map<String, Object> map = new HashMap<>();
        JSONObject json = new JSONObject();

        map.put("toto", Boolean.FALSE);
        json.put("toto", Boolean.FALSE);
        Outcome outcome = new Outcome("<root><toto>false</toto></root>");

        Object[] inputs = {map, json, outcome};

        for (Object input : inputs) {
            assertFalse(          OutcomeUtils.getBooleanOrNull(input, "toto"));
            assertEquals("false", OutcomeUtils.getStringOrNull(input, "toto"));

            //Outcome only handles String
            if (input instanceof Outcome) {
                assertNull(OutcomeUtils.getBigDecimalOrNull(input, "toto"));
                assertNull(OutcomeUtils.getBigIntegerOrNull(input, "toto"));
            }
            else {
                assertEquals(new BigDecimal("0"), OutcomeUtils.getBigDecimalOrNull(input, "toto"));
                assertEquals(new BigInteger("0"), OutcomeUtils.getBigIntegerOrNull(input, "toto"));
            }

            assertNull(OutcomeUtils.getLocalDateOrNull(input, "toto"));
            assertNull(OutcomeUtils.getLocalDateTimeOrNull(input, "toto"));
            assertNull(OutcomeUtils.getOffsetTimeOrNull(input, "toto"));
            assertNull(OutcomeUtils.getOffsetDateTimeOrNull(input, "toto"));
        }
    }

    @Test
    public void checkBooleanValue_No() throws Exception {
        Map<String, Object> map = new HashMap<>();
        JSONObject json = new JSONObject();

        map.put("toto", "no");
        json.put("toto", "no");
        Outcome outcome = new Outcome("<root><toto>no</toto></root>");

        Object[] inputs = {map, json, outcome};

        for (Object input : inputs) {
            assertFalse(       OutcomeUtils.getBooleanOrNull(input, "toto"));
            assertEquals("no", OutcomeUtils.getStringOrNull(input, "toto"));

            assertNull(OutcomeUtils.getBigDecimalOrNull(input, "toto"));
            assertNull(OutcomeUtils.getBigIntegerOrNull(input, "toto"));
            assertNull(OutcomeUtils.getLocalDateOrNull(input, "toto"));
            assertNull(OutcomeUtils.getLocalDateTimeOrNull(input, "toto"));
            assertNull(OutcomeUtils.getOffsetTimeOrNull(input, "toto"));
            assertNull(OutcomeUtils.getOffsetDateTimeOrNull(input, "toto"));
        }
    }

    @Test
    public void checkBooleanValue_True() throws Exception {
        Map<String, Object> map = new HashMap<>();
        JSONObject json = new JSONObject();

        map.put("toto", Boolean.TRUE);
        json.put("toto", Boolean.TRUE);
        Outcome outcome = new Outcome("<root><toto>true</toto></root>");

        Object[] inputs = {map, json,outcome};

        for (Object input : inputs) {
            assertTrue(          OutcomeUtils.getBooleanOrNull(input, "toto"));
            assertEquals("true", OutcomeUtils.getStringOrNull(input, "toto"));

            //Outcome only handles String
            if (input instanceof Outcome) {
                assertNull(OutcomeUtils.getBigDecimalOrNull(input, "toto"));
                assertNull(OutcomeUtils.getBigIntegerOrNull(input, "toto"));
            }
            else {
                assertEquals(new BigDecimal("1"), OutcomeUtils.getBigDecimalOrNull(input, "toto"));
                assertEquals(new BigInteger("1"), OutcomeUtils.getBigIntegerOrNull(input, "toto"));
            }

            assertNull(OutcomeUtils.getLocalDateOrNull(input, "toto"));
            assertNull(OutcomeUtils.getLocalDateTimeOrNull(input, "toto"));
            assertNull(OutcomeUtils.getOffsetTimeOrNull(input, "toto"));
            assertNull(OutcomeUtils.getOffsetDateTimeOrNull(input, "toto"));
        }
    }

    @Test
    public void checkBooleanValue_Yes() throws Exception {
        Map<String, Object> map = new HashMap<>();
        JSONObject json = new JSONObject();

        map.put("toto", "yes");
        json.put("toto", "yes");
        Outcome outcome = new Outcome("<root><toto>yes</toto></root>");

        Object[] inputs = {map, json, outcome};

        for (Object input : inputs) {
            assertTrue(         OutcomeUtils.getBooleanOrNull(input, "toto"));
            assertEquals("yes", OutcomeUtils.getStringOrNull(input, "toto"));

            assertNull(OutcomeUtils.getBigDecimalOrNull(input, "toto"));
            assertNull(OutcomeUtils.getBigIntegerOrNull(input, "toto"));
            assertNull(OutcomeUtils.getLocalDateOrNull(input, "toto"));
            assertNull(OutcomeUtils.getLocalDateTimeOrNull(input, "toto"));
            assertNull(OutcomeUtils.getOffsetTimeOrNull(input, "toto"));
            assertNull(OutcomeUtils.getOffsetDateTimeOrNull(input, "toto"));
        }
    }

    @Test
    public void checkValidStringValue() throws Exception {
        Map<String, Object> map = new HashMap<>();
        JSONObject json = new JSONObject();

        map.put("toto", "toto");
        json.put("toto", "toto");
        Outcome outcome = new Outcome("<root><toto>toto</toto></root>");

        Object[] inputs = {map, json, outcome};

        for (Object input : inputs) {
            String inputClass = input.getClass().getSimpleName();
            assertEquals("getStringOrNull("+inputClass+")", "toto", OutcomeUtils.getStringOrNull(input, "toto"));

            assertNull("getBigDecimalOrNull("+inputClass+")",       OutcomeUtils.getBigDecimalOrNull(input, "toto"));
            assertNull("getBigIntegerOrNull("+inputClass+")",       OutcomeUtils.getBigIntegerOrNull(input, "toto"));
            assertNull("getBooleanOrNull("+inputClass+")",          OutcomeUtils.getBooleanOrNull(input, "toto"));
            assertNull("getLocalDateOrNull("+inputClass+")",        OutcomeUtils.getLocalDateOrNull(input, "toto"));
            assertNull("getLocalDateTimeOrNull("+inputClass+")",    OutcomeUtils.getLocalDateTimeOrNull(input, "toto"));
            assertNull("getOffsetTimeOrNull("+inputClass+")",       OutcomeUtils.getOffsetTimeOrNull(input, "toto"));
            assertNull("getOffsetDateTimeOrNull("+inputClass+")",   OutcomeUtils.getOffsetDateTimeOrNull(input, "toto"));
        }
    }
}
