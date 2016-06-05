/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
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

package org.cristalise.kernel.persistency.outcome;

import static org.junit.Assert.fail;

import java.io.IOException;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.lifecycle.instance.stateMachine.Transition;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.utils.Logger;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * 
 */
public class EmptyOutcomeInitiatorTest {
    
    EmptyOutcomeInitiator emptyOI;
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        emptyOI = new EmptyOutcomeInitiator();
    }
    
    public Job getJob() throws ObjectNotFoundException {
        Job j = new Job();
        j.setItemPath(new ItemPath());
        j.setStepPath("/workflow/0");
        j.setTransition(new Transition());
        j.setOriginStateName("from");
        j.setTargetStateName("to");
        j.setStepName("toto");
        j.setStepType("EA");
        
        try {
            j.setAgentName("toto");
        } catch (Exception e) {}
        
        j.setAgentRole("role");

        return j;
    }
    
    public static boolean compareXML(String expected, String actual) throws SAXException, IOException {
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLUnit.setIgnoreComments(true);

        DetailedDiff diff = new DetailedDiff( XMLUnit.compareXML( expected, actual) );

        if(!diff.identical()) Logger.error(diff.toString());

        return diff.identical();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * 
     * @throws Exception
     */
    @Test
    public void testInitOutcome() throws Exception {
        Job j = getJob();
        
        String xml = emptyOI.initOutcome(j);
        
        assert compareXML("", xml);
        
        fail("Not yet implemented");
    }

}
