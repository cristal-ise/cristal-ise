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
import static org.cristalise.kernel.persistency.outcomebuilder.SystemProperties.Webui_autoComplete_default;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.test.persistency.XMLUtils;
import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;

public class NgDynamicFormsTest extends XMLUtils {

    String dir = "src/test/data/outcomeBuilder/dynamicForms";

    @Before
    public void setUp() throws Exception {
        Properties props = new Properties();
        props.put("Webui.autoComplete.default", "on");
        props.put("Authenticator", "Shiro");
        props.put("Webui.inputField.boolean.defaultValue", "false");
        props.put("Webui.inputField.decimal.defaultValue", "0.0");
        props.put("Webui.inputField.integer.defaultValue", "0");
        Gateway.init(props);
    }

    @Test
    public void ngForm_PatientDetails() throws Exception {
        OutcomeBuilder builder = new OutcomeBuilder("PatientDetails", new Schema("PatientDetails", 0, getXSD(dir, "PatientDetails")), false);

        Map<String, String> args = new HashMap<>();
        args.put("CURRENTDATE", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        // genetate model & layout
        CompiledTemplate expr = TemplateCompiler.compileTemplate(getJSON(dir, "PatientDetailsNGForms"));
        // Update the JSON file (MVEL template) to set the current date
        String expectedJson = (String)TemplateRuntime.execute(expr, args);
        JSONArray expected = new JSONArray(expectedJson);
        JSONArray actual   = builder.generateNgDynamicFormsJson();
        assertJsonEquals(expected, actual);

        // genetate model only
        expr = TemplateCompiler.compileTemplate(getJSON(dir, "PatientDetailsNGFormsModel"));
        // Update the JSON file (MVEL template) to set the current date
        expectedJson = (String)TemplateRuntime.execute(expr, args);
        expected = new JSONArray(expectedJson);
        actual   = builder.generateNgDynamicFormsJson(false);
        assertJsonEquals(expected, actual);
    }

    @Test
    public void ngForm_Storage() throws Exception {
        OutcomeBuilder builder = new OutcomeBuilder("Storage", new Schema("Storage", 0, getXSD(dir, "Storage")), false);

        JSONArray actual   = builder.generateNgDynamicFormsJson();
        JSONArray expected = new JSONArray(getJSON(dir, "StorageNGForms"));
        assertJsonEquals(expected, actual);

        actual   = builder.generateNgDynamicFormsJson(false);
        expected = new JSONArray(getJSON(dir, "StorageNGFormsModel"));
        assertJsonEquals(expected, actual);
    }

    @Test
    public void ngForm_Table() throws Exception {
        OutcomeBuilder builder = new OutcomeBuilder("Details", new Schema("Table", 0, getXSD(dir, "Table")), false);

        JSONArray actual   = builder.generateNgDynamicFormsJson();
        JSONArray expected = new JSONArray(getJSON(dir, "TableNGForms"));
        assertJsonEquals(expected, actual);

        actual   = builder.generateNgDynamicFormsJson(false);
        expected = new JSONArray(getJSON(dir, "TableNGFormsModel"));
        assertJsonEquals(expected, actual);
    }

    @Test
    public void ngForm_ShowSeconds() throws Exception {
        OutcomeBuilder builder = new OutcomeBuilder("Employee", new Schema("ShowSeconds", 0, getXSD(dir, "ShowSeconds")), false);

        JSONArray actual = builder.generateNgDynamicFormsJson();
        JSONArray expected = new JSONArray(getJSON(dir, "ShowSeconds"));
        assertJsonEquals(expected, actual);

        actual = builder.generateNgDynamicFormsJson(false);
        expected = new JSONArray(getJSON(dir, "ShowSecondsModel"));
        assertJsonEquals(expected, actual);
    }
    
    @Test
    public void ngForm_CustomWidth() throws Exception {
        OutcomeBuilder builder = new OutcomeBuilder("Form", new Schema("DynamicFormsWidthConfiguration", 0, getXSD(dir, "DynamicFormsWidthConfiguration")), false);

        JSONArray actual = builder.generateNgDynamicFormsJson();
        JSONArray expected = new JSONArray(getJSON(dir, "DynamicFormsWidthConfiguration"));
        assertJsonEquals(expected, actual);

        actual = builder.generateNgDynamicFormsJson(false);
        expected = new JSONArray(getJSON(dir, "DynamicFormsWidthConfigurationModel"));
        assertJsonEquals(expected, actual);
    }

    @Test
    public void ngForm_TotalFractionDigits() throws Exception {
        String testName = "TotalFractionDigits";
        OutcomeBuilder builder = new OutcomeBuilder("TestData", new Schema(testName, 0, getXSD(dir, testName)), false);

        JSONArray actual = builder.generateNgDynamicFormsJson();
        JSONArray expected = new JSONArray(getJSON(dir, testName+"NgForms"));
        assertJsonEquals(expected, actual);

        actual = builder.generateNgDynamicFormsJson(false);
        expected = new JSONArray(getJSON(dir, testName+"NgFormsModel"));
        assertJsonEquals(expected, actual);
    }

    @Test
    public void ngForm_PrecisionScale() throws Exception {
        String testName = "PrecisionScale";
        OutcomeBuilder builder = new OutcomeBuilder("PatientDetails", new Schema(testName, 0, getXSD(dir, testName)), false);

        JSONArray actual = builder.generateNgDynamicFormsJson();
        JSONArray expected = new JSONArray(getJSON(dir, testName+"NgForms"));
        assertJsonEquals(expected, actual);

        actual = builder.generateNgDynamicFormsJson(false);
        expected = new JSONArray(getJSON(dir, testName+"NgFormsModel"));
        assertJsonEquals(expected, actual);
    }
    
    @Test
    public void ngForm_Label() throws Exception {
        OutcomeBuilder builder = new OutcomeBuilder("Form", new Schema("DynamicFormsGroupLabel", 0, getXSD(dir, "DynamicFormsGroupLabel")), false);

        JSONArray actual = builder.generateNgDynamicFormsJson();
        JSONArray expected = new JSONArray(getJSON(dir, "DynamicFormsGroupLabel"));
        assertJsonEquals(expected, actual);

        actual = builder.generateNgDynamicFormsJson(false);
        expected = new JSONArray(getJSON(dir, "DynamicFormsGroupLabelModel"));
        assertJsonEquals(expected, actual);
    }

    @Test
    public void ngForm_FormContainer() throws Exception {
        OutcomeBuilder builder = new OutcomeBuilder("Form", new Schema("DynamicFormsContainer", 0, getXSD(dir, "DynamicFormsContainer")), false);

        JSONArray actual = builder.generateNgDynamicFormsJson();
        JSONArray expected = new JSONArray(getJSON(dir, "DynamicFormsContainer"));
        assertJsonEquals(expected, actual);

        actual = builder.generateNgDynamicFormsJson(false);
        expected = new JSONArray(getJSON(dir, "DynamicFormsContainerModel"));
        assertJsonEquals(expected, actual);
    }
    
    @Test
    public void ngForm_FormGroupContainer() throws Exception {
        OutcomeBuilder builder = new OutcomeBuilder("Form", new Schema("DynamicFormsGroupContainer", 0, getXSD(dir, "DynamicFormsGroupContainer")), false);

        JSONArray actual = builder.generateNgDynamicFormsJson();
        JSONArray expected = new JSONArray(getJSON(dir, "DynamicFormsGroupContainer"));
        assertJsonEquals(expected, actual);

        actual = builder.generateNgDynamicFormsJson(false);
        expected = new JSONArray(getJSON(dir, "DynamicFormsGroupContainerModel"));
        assertJsonEquals(expected, actual);
    }

    @Test
    public void ngForm_AutoComplete() throws Exception {
        Gateway.getProperties().remove(Webui_autoComplete_default.toString());
        OutcomeBuilder builder = new OutcomeBuilder("PatientDetails", new Schema("AutoComplete", 0, getXSD(dir, "AutoComplete")), false);

        JSONArray actual = builder.generateNgDynamicFormsJson();
        JSONArray expected = new JSONArray(getJSON(dir, "AutoComplete"));
        assertJsonEquals(expected, actual);

        actual = builder.generateNgDynamicFormsJson(false);
        expected = new JSONArray(getJSON(dir, "AutoCompleteModel"));
        assertJsonEquals(expected, actual);
    }

    @Test
    public void ngForm_Additional() throws Exception {
        OutcomeBuilder builder = new OutcomeBuilder("Additional", new Schema("Additional", 0, getXSD(dir, "Additional")), false);

        JSONArray actual = builder.generateNgDynamicFormsJson();
        JSONArray expected = new JSONArray(getJSON(dir, "Additional"));
        assertJsonEquals(expected, actual);

        actual = builder.generateNgDynamicFormsJson(false);
        expected = new JSONArray(getJSON(dir, "AdditionalModel"));
        assertJsonEquals(expected, actual);
    }

    @Test
    public void ngForm_AnyField() throws Exception {
        OutcomeBuilder builder = new OutcomeBuilder("AnyField", new Schema("AnyField", 0, getXSD(dir, "AnyField")), false);

        JSONArray actual = builder.generateNgDynamicFormsJson();
        JSONArray expected = new JSONArray(getJSON(dir, "AnyField"));
        assertJsonEquals(expected, actual);

        actual = builder.generateNgDynamicFormsJson(false);
        expected = new JSONArray(getJSON(dir, "AnyFieldModel"));
        assertJsonEquals(expected, actual);
    }

    @Test
    public void ngForm_AnyType() throws Exception {
        OutcomeBuilder builder = new OutcomeBuilder("AnyType", new Schema("AnyType", 0, getXSD(dir, "AnyType")), false);

        JSONArray actual = builder.generateNgDynamicFormsJson();
        JSONArray expected = new JSONArray(getJSON(dir, "AnyType"));
        assertJsonEquals(expected, actual);

        actual = builder.generateNgDynamicFormsJson(false);
        expected = new JSONArray(getJSON(dir, "AnyTypeModel"));
        assertJsonEquals(expected, actual);
    }
}
