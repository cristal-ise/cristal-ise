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
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder;
import org.cristalise.kernel.test.persistency.XMLUtils;
import org.junit.Before;
import org.junit.Test;

public class BuildOutcomeTest extends XMLUtils {

    String dir = "src/test/data/outcomeBuilder";

    @Before
    public void setUp() throws Exception {
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
        ob.addField("/SiteCharacteristicsData/BuildingType", "semi-detached");
        ob.addField("/SiteCharacteristicsData/AHOwner",      "mine");
        ob.addField("BuildingTypeRemarks",                   "awsome");

        assert XMLUtils.compareXML(getXML(dir, "siteCharacteristicsData_ups"), ob.getXml());
    }

    @Test
    public void buildSiteCharacteristicsData_FromCSV() throws Exception {
        OutcomeBuilder ob = new OutcomeBuilder(new Schema("SiteCharacteristicsData", 0, getXSD(dir, "SiteCharacteristicsData")));

        buildOutcomeFromCSV(ob, "SiteCharacteristicsData.csv");

        assert XMLUtils.compareXML(getXML(dir, "siteCharacteristicsData_csv"), ob.getXml());
    }

    @Test
    public void buildTable_ScientificData() throws Exception {
      String expected = getXML(dir, "ScientificData");
      Schema xsd = new Schema("ScientificData", 0, getXSD(dir, "ScientificData"));

      Outcome actual = new Outcome(expected, xsd);

      // tests bug #239
      new OutcomeBuilder(xsd, actual);

      //at this point the XML is not inline with the XSD, but that acceptable for this test
      assert compareXML(expected, actual.getData());
    }
}
