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
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * 
 */
public class EmptyOutcomeInitiatorTest {

    public static final String root = "src/test/data/";

    EmptyOutcomeInitiator emptyOI;

    @BeforeClass
    public static void setup() {
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLUnit.setIgnoreDiffBetweenTextAndCDATA(true);
        XMLUnit.setIgnoreComments(true);
    }

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

        when(j.getSchema()).thenReturn(new Schema(xsd));
        when(j.getActPropString("SchemaRootElementName")).thenReturn(rootElement);

        return j;
    }

    public boolean compareXML(String expected, String actual) throws SAXException, IOException {
        DetailedDiff diff = new DetailedDiff( XMLUnit.compareXML( expected, actual) );

        if(!diff.identical()) Logger.warning(diff.toString());

        return diff.similar();
    }

    private void checkEmptyOutcome(String type) throws IOException, Exception, InvalidDataException, SAXException {
        String xsd      = new String(Files.readAllBytes(Paths.get(root+type+".xsd")));
        String expected = new String(Files.readAllBytes(Paths.get(root+type+".xml")));

        Job j = getJob(xsd);

        String actual = emptyOI.initOutcome(j);

        //Logger.msg(actual);

        if(!compareXML(expected, actual)) fail("");
    }

    @Test
    public void generateDateXML() throws Exception {
        checkEmptyOutcome("DateField");
    }

    @Test
    public void generateTimeXML() throws Exception {
        checkEmptyOutcome("TimeField");
    }

    @Test
    public void generateDateTimeXML() throws Exception {
        checkEmptyOutcome("DateTimeField");
    }

    public void generateStringXML() throws Exception {
        checkEmptyOutcome("StringField");
    }

    @Test
    public void generateBooleanXML() throws Exception {
        checkEmptyOutcome("BooleanField");
    }
    
    @Test
    public void generateIntegerXML() throws Exception {
        checkEmptyOutcome("IntergerField");
    }

    @Test
    public void generateDecimalXML() throws Exception {
        checkEmptyOutcome("DecimalField");
    }

    @Test
    public void generateStateMachineXML() throws Exception {
        checkEmptyOutcome("StateMachine");
    }

    @Test
    public void generatePatientDetailsXML() throws Exception {
        checkEmptyOutcome("PatientDetails");
    }

    @Test
    public void generateModuleXML() throws Exception {
        checkEmptyOutcome("Module");
    }
}
