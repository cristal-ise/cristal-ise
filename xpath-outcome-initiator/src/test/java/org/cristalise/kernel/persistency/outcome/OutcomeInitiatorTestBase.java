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

import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.utils.Logger;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.BeforeClass;
import org.xml.sax.SAXException;

/**
 * 
 */
public class OutcomeInitiatorTestBase {

    public static final String root = "src/test/data/";

    /**
     * Static JUNIT configuration method, configures XMLUnit 
     */
    @BeforeClass
    public static void setup() {
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLUnit.setIgnoreDiffBetweenTextAndCDATA(true);
        XMLUnit.setIgnoreComments(true);
        
        Logger.addLogStream(System.out, 0);
    }

    /**
     * Mocking job 
     * 
     * @param xsd the Schema XML
     * @return the mock Job
     * @throws Exception every exception
     */
    public Job mockJob(String xsd) throws Exception {
        return mockJob(xsd, null);
    }

    /**
     * Mocking job 
     * 
     * @param xsd the Schema XML
     * @param rootElement the name of the element to use for XML generation
     * @return the mock Job
     * @throws Exception
     */
    public Job mockJob(String xsd, String rootElement) throws Exception {
        Job j = mock(Job.class);

        when(j.getSchema()).thenReturn(new Schema(xsd));
        when(j.getActPropString(EmptyOutcomeInitiator.ROOTNAME_PROPNAME)).thenReturn(rootElement);

        return j;
    }

    /**
     * Compares 2 XML string
     * 
     * @param expected the reference XML
     * @param actual the xml under test
     * @return whether the two XMLs are identical or not
     * @throws SAXException
     * @throws IOException
     */
    public boolean compareXML(String expected, String actual) throws SAXException, IOException {
        DetailedDiff diff = new DetailedDiff( XMLUnit.compareXML( expected, actual) );

        if(!diff.identical()) Logger.warning(diff.toString());

        return diff.identical();
    }
}
