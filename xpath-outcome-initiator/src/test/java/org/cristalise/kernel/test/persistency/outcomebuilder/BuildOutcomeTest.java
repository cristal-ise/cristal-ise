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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder;
import org.cristalise.kernel.test.persistency.XMLUtils;
import org.cristalise.kernel.utils.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class BuildOutcomeTest extends XMLUtils {

    String dir = "src/test/data/outcomeBuilder";

    @Before
    public void setUp() throws Exception {
        Logger.addLogStream(System.out, 8);
    }

    private Map<String,String> getParamsFromCSV(String csvFile) throws Exception {
        Map<String,String> params = new LinkedHashMap<String, String>();

        Stream<String> stream = Files.lines(Paths.get(dir + "/" + csvFile));

        stream.forEach(line -> {
            String[] values = line.split(",");
            String key = values[0];
            String val = null;

            if (values.length == 2) val = values[1];

            params.put(key, val);
        });

        stream.close();

        return params;
    }

    private void buildOutcomeFromCSV(OutcomeBuilder ob, String csvFile) throws Exception {
        Map<String, Map<String, String>> records = new LinkedHashMap<>();

        Map<String,String> params = getParamsFromCSV(csvFile);

        for (Map.Entry<String, String> entry : params.entrySet()) {

            String path  = StringUtils.substringBeforeLast(entry.getKey(), ".").replace('.', '/');
            String field = StringUtils.substringAfterLast(entry.getKey(), ".");
            String value = entry.getValue();

            if (field.contains("[")) {
                path = path + "[" + StringUtils.substringBetween(field, "[", "]") +"]";
                field = StringUtils.substringBefore(field, "[");
            }

            if (!records.containsKey(path)) records.put(path, new LinkedHashMap<String, String>());

            records.get(path).put(field, value);
        }

        for (String path : records.keySet()) {
            Logger.msg(path);
            ob.addRecord(StringUtils.substringBefore(path, "["), records.get(path));
        }
    }

    @Test
    public void buildEmptySiteCharacteristicsData_AddOptionalUPSRecord() throws Exception {
        OutcomeBuilder ob = new OutcomeBuilder(new Schema("SiteCharacteristicsData", 0, getXSD(dir, "SiteCharacteristicsData")));

        Map<String, String> upsRecord = new HashMap<String, String>();
        upsRecord.put("Manufacturer", "acme");
        upsRecord.put("Phases",       "final");
        upsRecord.put("Power",        "super");
        upsRecord.put("Remarks",      "irrelevant");
        upsRecord.put("TimeAutonomy", "daily");
        upsRecord.put("UsedFor",      "creation");

        ob.addRecord("/SiteCharacteristicsData/UPS", upsRecord);

        //Test the order of adding fields and the use of xpath or just a simple name
        ob.addfield("/SiteCharacteristicsData/BuildingType", "semi-detached");
        ob.addfield("/SiteCharacteristicsData/AHOwner",      "mine");
        ob.addfield("BuildingTypeRemarks",                   "awsome");

        Logger.msg(ob.getXml());

        assert XMLUtils.compareXML(getXML(dir, "siteCharacteristicsData_ups"), ob.getXml());
    }

    @Test @Ignore
    public void buildSiteCharacteristicsData_FromCSV() throws Exception {
        OutcomeBuilder ob = new OutcomeBuilder(new Schema("SiteCharacteristicsData", 0, getXSD(dir, "SiteCharacteristicsData")));

        buildOutcomeFromCSV(ob, "SiteCharacteristicsData.csv");

        Logger.msg(ob.getXml());

        assert XMLUtils.compareXML(getXML(dir, "siteCharacteristicsData_csv"), ob.getXml());
    }
}
