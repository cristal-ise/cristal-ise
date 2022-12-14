package org.cristalise.kernel.test.persistency.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder;
import org.cristalise.kernel.persistency.outcomebuilder.utils.OutcomeUtils;
import org.cristalise.kernel.test.persistency.XMLUtils;
import org.json.JSONObject;
import org.junit.Test;

public class OutcomeUtilsValidValueTest {
    
    @Test
    public void outcomeOfStringValidValue() throws Exception {
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

        outcome.setField("characters", "toto");
        assertTrue (OutcomeUtils.hasField(outcome, "characters"));
        assertTrue(OutcomeUtils.hasValue(outcome, "characters"));
    }

    @Test
    public void mapOfStringValidValue() {
        Map<String, String> map = new HashMap<String, String>();

        assertFalse(OutcomeUtils.hasField(map, "toto"));
        assertFalse(OutcomeUtils.hasValue(map, "toto"));

        map.put("toto", null);
        assertTrue(OutcomeUtils.hasField(map, "toto"));
        assertFalse(OutcomeUtils.hasValue(map, "toto"));

        map.put("toto", "");
        assertTrue (OutcomeUtils.hasField(map, "toto"));
        assertFalse(OutcomeUtils.hasValue(map, "toto"));

        map.put("toto", "kovax");
        assertTrue(OutcomeUtils.hasField(map, "toto"));
        assertTrue(OutcomeUtils.hasValue(map, "toto"));
    }

    @Test
    public void mapOfIntegerValidValue() {
        Map<String, Integer> map = new HashMap<String, Integer>();

        assertFalse(OutcomeUtils.hasField(map, "toto"));
        assertFalse(OutcomeUtils.hasValue(map, "toto"));

        map.put("toto", null);
        assertTrue (OutcomeUtils.hasField(map, "toto"));
        assertFalse(OutcomeUtils.hasValue(map, "toto"));

        map.put("toto", Integer.valueOf("100"));
        assertTrue(OutcomeUtils.hasField(map, "toto"));
        assertTrue(OutcomeUtils.hasValue(map, "toto"));
    }

    @Test
    public void jsonOfStringValidValue() {
        JSONObject json = new JSONObject();

        assertFalse(OutcomeUtils.hasField(json, "toto"));
        assertFalse(OutcomeUtils.hasValue(json, "toto"));

// JSONObject.put() does not add/update null value
//        json.put("toto", (String)null);
//        assertTrue (OutcomeUtils.hasField(json, "toto"));
//        assertFalse(OutcomeUtils.hasValue(json, "toto"));

        json.put("toto", "");
        assertTrue (OutcomeUtils.hasField(json, "toto"));
        assertFalse(OutcomeUtils.hasValue(json, "toto"));

        json.put("toto", "kovax");
        assertTrue(OutcomeUtils.hasField(json, "toto"));
        assertTrue(OutcomeUtils.hasValue(json, "toto"));
    }

    @Test
    public void jsonOfIntegerValidValue() {
        JSONObject json = new JSONObject();

        assertFalse(OutcomeUtils.hasField(json, "toto"));
        assertFalse(OutcomeUtils.hasValue(json, "toto"));

// JSONObject.put() does not add/update null value
//        json.put("toto", (Integer)null);
//        assertTrue (OutcomeUtils.hasField(json, "toto"));
//        assertFalse(OutcomeUtils.hasValue(json, "toto"));

        json.put("toto", Integer.valueOf("100"));
        assertTrue(OutcomeUtils.hasField(json, "toto"));
        assertTrue(OutcomeUtils.hasValue(json, "toto"));
    }
}
