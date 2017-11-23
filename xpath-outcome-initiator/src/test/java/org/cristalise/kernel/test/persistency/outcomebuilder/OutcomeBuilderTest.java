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

import java.util.HashMap;
import java.util.Map;

import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder;
import org.cristalise.kernel.test.persistency.XMLUtils;
import org.cristalise.kernel.utils.Logger;
import org.junit.Before;
import org.junit.Test;

public class OutcomeBuilderTest extends XMLUtils {

    @Before
    public void setUp() throws Exception {
        Logger.addLogStream(System.out, 8);
    }

    @Test
    public void multiRootXSDFile() throws Exception {
        Schema schema = new Schema("Storage", 0, getXSD("Storage"));

        OutcomeBuilder ob = new OutcomeBuilder("StorageDetails", schema);
        Logger.msg(ob.getXml());

        ob = new OutcomeBuilder("StorageAmount", schema);
        Logger.msg(ob.getXml());

        ob = new OutcomeBuilder("Storage", schema);
        Logger.msg(ob.getXml());
    }

    @Test
    public void addRecord() throws Exception {
        OutcomeBuilder ob = new OutcomeBuilder(new Schema("SiteCharacteristicsData", 0, getXSD("SiteCharacteristicsData")));

        Map<String, String> upsRecord = new HashMap<String, String>();
        upsRecord.put("Manufacturer", "acme");
        upsRecord.put("Phases",       "final");
        upsRecord.put("Power",        "super");
        upsRecord.put("Remarks",      "irrelevant");
        upsRecord.put("TimeAutonomy", "daily");
        upsRecord.put("UsedFor",      "creation");

        ob.addRecord("/SiteCharacteristicsData/UPS", upsRecord);
        //ob.addRecord("/SiteCharacteristicsData/UPS", upsRecord);

        Logger.msg(ob.getXml());

        assert XMLUtils.compareXML(getXML("siteCharacteristicsData_ups"), ob.getXml());
    }

    @Test
    public void exportViewTemplate() throws Exception {
        OutcomeBuilder ob = new OutcomeBuilder(new Schema("SiteCharacteristicsData", 0, getXSD("SiteCharacteristicsData")), false);

        String template = ob.exportViewTemplate();

        assert template != null;
    }

    private void checkEmptyOutcome(String type) throws Exception {
        String xsd      = getXSD(type);
        String expected = getXML(type);

        OutcomeBuilder actual = new OutcomeBuilder(new Schema(type, 0, xsd));

        Logger.msg(actual.getXml());

        assert compareXML(expected, actual.getXml());
    }

    @Test
    public void booleanField() throws Exception {
        checkEmptyOutcome("BooleanField");
    }

    @Test
    public void stringField() throws Exception {
        OutcomeBuilder actual = new OutcomeBuilder(new Schema("StringField", 0, getXSD("StringField")));

        actual.putField("characters", "string");

        Logger.msg(actual.getXml());

        assert compareXML(getXML("StringField"), actual.getXml());
    }
}
