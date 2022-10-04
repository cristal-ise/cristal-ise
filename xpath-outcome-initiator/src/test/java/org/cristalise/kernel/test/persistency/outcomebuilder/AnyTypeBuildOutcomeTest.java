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

import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder;
import org.cristalise.kernel.test.persistency.XMLUtils;
import org.junit.Before;
import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AnyTypeBuildOutcomeTest extends XMLUtils {

    String dir = "src/test/data/outcomeBuilder";

    @Before
    public void setUp() throws Exception {
        //Gateway.getProperties().setProperty("Validation.useDOM", false);
    }

    @Test 
    public void basicOutcomeValidateTest() throws Exception {
        String type = "AnyTypePredefinedSteps";
        String xsd  = getXSD(dir, type);

        new Outcome(getXML(dir, "AnyTypePredefinedSteps0"), new Schema(type, 0, xsd)).validateAndCheck();
        new Outcome(getXML(dir, "AnyTypePredefinedSteps1"), new Schema(type, 0, xsd)).validateAndCheck();
        new Outcome(getXML(dir, "AnyTypePredefinedSteps2"), new Schema(type, 0, xsd)).validateAndCheck();
    }

    @Test
    public void appendXmlFragmentsToPredefeinedSteps() throws Exception {
        OutcomeBuilder ob = new OutcomeBuilder(new Schema("AnyTypePredefinedSteps", 0, getXSD(dir, "AnyTypePredefinedSteps")));

        ob.getOutcome().setField("StringField", "toto");

        log.info(ob.getXml());

        ob.addField("PredefinedSteps");
        assert compareXML(getXML(dir, "AnyTypePredefinedSteps0"), ob.getXml());

        log.info(ob.getXml());

        ob.getOutcome().appendXmlFragment("/PredefinedStepsTest/PredefinedSteps", "<AddMembersToCollection><Dependency id='1'/></AddMembersToCollection>");
        assert compareXML(getXML(dir, "AnyTypePredefinedSteps1"), ob.getXml());

        log.info(ob.getXml());

        ob.getOutcome().appendXmlFragment("/PredefinedStepsTest/PredefinedSteps", "<AddMembersToCollection><Dependency id='2'/></AddMembersToCollection>");
        assert compareXML(getXML(dir, "AnyTypePredefinedSteps2"), ob.getXml());

        log.info(ob.getXml());
}

    @Test
    public void addFieldsToPredefeinedSteps() throws Exception {
        OutcomeBuilder ob = new OutcomeBuilder(new Schema("AnyTypePredefinedSteps", 0, getXSD(dir, "AnyTypePredefinedSteps")));

        ob.getOutcome().setField("StringField", "toto");

        log.info(ob.getXml());

        ob.addField("PredefinedSteps", "");
        assert compareXML(getXML(dir, "AnyTypePredefinedSteps0"), ob.getXml());

        log.info(ob.getXml());

        ob.addField("/PredefinedStepsTest/PredefinedSteps/AddMembersToCollection");
        ob.getOutcome().appendXmlFragment("/PredefinedStepsTest/PredefinedSteps/AddMembersToCollection[1]", "<Dependency id='1'/>");
        assert compareXML(getXML(dir, "AnyTypePredefinedSteps1"), ob.getXml());

        log.info(ob.getXml());

        ob.addField("/PredefinedStepsTest/PredefinedSteps/AddMembersToCollection");
        ob.getOutcome(false).appendXmlFragment("/PredefinedStepsTest/PredefinedSteps/AddMembersToCollection[2]", "<Dependency id='2'/>");
        assert compareXML(getXML(dir, "AnyTypePredefinedSteps2"), ob.getXml());

        log.info(ob.getXml());
    }
}
