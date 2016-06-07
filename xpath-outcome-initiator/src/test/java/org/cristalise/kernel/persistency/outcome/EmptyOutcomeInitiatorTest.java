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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.utils.Logger;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * 
 */
public class EmptyOutcomeInitiatorTest {
    
    public static final String root = "src/test/data/";
    
    EmptyOutcomeInitiator emptyOI;
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        emptyOI = new EmptyOutcomeInitiator();
    }

    public Job getJob(String xsd) throws Exception {
        return getJob(xsd, null);
    }

    public Job getJob(String xsd, String rootElement) throws Exception {
        Job j = mock(Job.class);

     // stubbing appears before the actual execution
        when(j.getSchema()).thenReturn(new Schema(xsd));
        when(j.getActPropString("SchemaRootElementName")).thenReturn(rootElement);

        return j;
    }

    public boolean compareXML(String expected, String actual) throws SAXException, IOException {
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLUnit.setIgnoreComments(true);

        DetailedDiff diff = new DetailedDiff( XMLUnit.compareXML( expected, actual) );

        if(!diff.identical()) Logger.error(diff.toString());

        return diff.identical();
    }

    private void checkEmptyOutcome(String type) throws IOException, Exception, InvalidDataException, SAXException {
        String xsd      = new String(Files.readAllBytes(Paths.get(root+type+".xsd")));
        String expected = new String(Files.readAllBytes(Paths.get(root+type+".xml")));

        Job j = getJob(xsd);

        String actual = emptyOI.initOutcome(j);

        Logger.msg(actual);

        assert compareXML(expected, actual);
    }

    @Test
    public void testSimpleFields() throws Exception {
        checkEmptyOutcome("IntergerField");
        checkEmptyOutcome("DecimalField");
        checkEmptyOutcome("BooleanField");
        checkEmptyOutcome("StringField");
        checkEmptyOutcome("DateField");
        checkEmptyOutcome("TimeField");
        checkEmptyOutcome("DateTimeField");
    }

    @Test
    public void checkStateMachineInit() throws Exception {
        checkEmptyOutcome("StateMachine");
    }

    @Test
    public void checkPatientDetailsInit() throws Exception {
        checkEmptyOutcome("PatientDetails");
    }
}
