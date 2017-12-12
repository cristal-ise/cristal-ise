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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder;
import org.cristalise.kernel.test.persistency.XMLUtils;
import org.cristalise.kernel.utils.Logger;
import org.json.JSONArray;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;

public class NgDynamicFormsTest extends XMLUtils {

    String dir = "src/test/data/outcomeBuilder";

    @Before
    public void setUp() throws Exception {
        Logger.addLogStream(System.out, 8);
    }

    @Test
    public void ngForm_PatientDetails() throws Exception {
        OutcomeBuilder builder = new OutcomeBuilder("PatientDetails", new Schema("PatientDetails", 0, getXSD(dir, "PatientDetails")), false);

        // Update the JSON file (MVEL template) to set the current date
        Map<String, String> args = new HashMap<>();
        args.put("CURRENTDATE", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        CompiledTemplate expr = TemplateCompiler.compileTemplate(getJSON(dir, "PatientDetailsNGForms"));
        String expectedJson = (String)TemplateRuntime.execute(expr, args);

        JSONArray expected = new JSONArray(expectedJson);
        JSONArray actual   = builder.generateNgDynamicFormsJson();

        Logger.msg(actual.toString(2));

        assertJsonEquals(expected, actual);
    }
    
    @Test @Ignore
    public void ngForm_Order() throws Exception {
        OutcomeBuilder builder = new OutcomeBuilder("Storage", new Schema("Storage", 0, getXSD(dir, "Storage")), false);
        
        JSONArray actual   = builder.generateNgDynamicFormsJson();

        Logger.msg(actual.toString(2));

        assert false;
    }

}
