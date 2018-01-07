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
import org.junit.Ignore;
import org.junit.Test;

import lombok.SneakyThrows;

public class BuildStructureFromJsonTest extends XMLUtils {

    String dir = "src/test/data/outcomeBuilder";

    @Before
    public void setUp() throws Exception {
        Logger.addLogStream(System.out, 8);
    }

    private void checkJsonOutcome(String type) throws Exception {
        OutcomeBuilder builder = new OutcomeBuilder(new Schema(type, 0, getXSD(dir, type)), true);
        String expected = getXML(dir, type+"Updated");
        JSONObject expectedJson = XML.toJSONObject(expected);

        Logger.msg(expectedJson.toString());
        Logger.msg(XML.toString(expectedJson));

        builder.addJsonInstance(expectedJson);;

        Logger.msg(builder.getXml());

        assert compareXML(expected, builder.getXml());
    }

    @Test
    public void integerFieldWithUnit() throws Exception {
        checkJsonOutcome("IntegerFieldWithUnit");
    }

    @Test @Ignore
    public void integerFieldOptional() throws Exception {
        checkJsonOutcome("IntegerFieldOptional");
    }

    @Test
    public void booleanField() throws Exception {
        checkJsonOutcome("BooleanField");
    }

    @Test
    public void rootWithAttr() throws Exception {
        checkJsonOutcome("RootWithAttr");
    }

    @Test
    public void rootWithOptionalAttr() throws Exception {
        checkJsonOutcome("RootWithOptionalAttr");
    }

    @Test
    public void patientDetails() throws Exception {
        checkJsonOutcome("PatientDetails");
    }
}
