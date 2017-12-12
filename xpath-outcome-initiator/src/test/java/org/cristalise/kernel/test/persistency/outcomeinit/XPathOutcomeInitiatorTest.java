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
package org.cristalise.kernel.test.persistency.outcomeinit;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.persistency.outcomeinit.XPathOutcomeInitiator;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.Logger;
import org.junit.Test;

/**
 *
 */
public class XPathOutcomeInitiatorTest extends OutcomeInitiatorTestBase {

    private void checkUpdatedOutcome(String type, String xpath, String value, String prefix) throws Exception {
        String xsd      = getXSD(type);
        String expected = getXML(type+"Updated");

        Job j = mockJob(xsd);

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(xpath, value);
        when(j.matchActPropNames(prefix)).thenReturn(resultMap);

        CastorHashMap actProps = new CastorHashMap();
        actProps.put(xpath, value);
        actProps.put("IntValue", "123");
        when(j.getActProps()).thenReturn(actProps);

        XPathOutcomeInitiator xpathOI = new XPathOutcomeInitiator(prefix);
        String actual = xpathOI.initOutcome(j);

        Logger.msg(actual);

        if(!compareXML(expected, actual)) fail("");
    }

    @Test
    public void updateSingleElement() throws Exception {
        checkUpdatedOutcome("IntegerField", "xpath:/IntegerField/counter", "123", "xpath:");
    }

    @Test
    public void updateNodeElement() throws Exception {
        checkUpdatedOutcome("StateMachine", "/StateMachine", "<State id='30' name='new' proceeds='false'/>", "/");
    }

    @Test
    public void updateSingleElementUsingMVEL() throws Exception {
        checkUpdatedOutcome("IntegerField", "xpath:/IntegerField/counter", "@{IntValue}", "xpath:");
    }
}
