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
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class FixFieldValueTest extends XMLUtils {

    String dir = "src/test/data/outcomeBuilder";

    @Before
    public void setUp() throws Exception {
    }

    private void checkJson2XmlOutcome(String type, String postFix) throws Exception {
        JSONObject actualJson = new JSONObject(getJSON(dir, type+postFix));
        String expected = getXML(dir, type);

        OutcomeBuilder builder = new OutcomeBuilder(new Schema(type, 0, getXSD(dir, type)), true);
        builder.addJsonInstance(actualJson);;

        assert compareXML(expected, builder.getXml());
    }

    @Test
    public void employeeWrongDateFix() throws Exception {
        checkJson2XmlOutcome("Employee", "WrongDate");
    }

    @Test
    public void shiftWrongTimeFix() throws Exception {
        checkJson2XmlOutcome("Shift", "WrongTime");
    }
}
