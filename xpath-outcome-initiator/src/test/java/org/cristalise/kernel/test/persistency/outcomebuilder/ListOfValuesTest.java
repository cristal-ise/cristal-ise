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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder;
import org.cristalise.kernel.test.persistency.XMLUtils;
import org.cristalise.kernel.utils.Logger;
import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;

public class ListOfValuesTest extends XMLUtils {

    String dir = "src/test/data/outcomeBuilder";

    @Before
    public void setUp() throws Exception {
        Logger.addLogStream(System.out, 8);
    }

    @Test
    public void employeeShiftSchedule() throws Exception {
        String type = "EmployeeShiftSchedule";
        OutcomeBuilder builder = new OutcomeBuilder(new Schema(type, 0, getXSD(dir, type)), true);

        String[] names = {"AAA", "BBBB", "CCCCC"};
        List<String> itemNames = new ArrayList<String>(Arrays.asList(names));
        Collections.sort(itemNames);

        HashMap<String, Object> inputs = new HashMap<>();
        inputs.put("memberNames", itemNames);

        JSONArray actual   = builder.generateNgDynamicFormsJson(inputs);

        Logger.msg(actual.toString(2));

        JSONArray expected = new JSONArray(getJSON(dir, type));

        assertJsonEquals(expected, actual);
    }
}
