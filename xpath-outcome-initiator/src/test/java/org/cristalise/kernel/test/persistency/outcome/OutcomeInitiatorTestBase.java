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

package org.cristalise.kernel.test.persistency.outcome;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.persistency.outcome.EmptyOutcomeInitiator;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.utils.Logger;
import org.junit.BeforeClass;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.ElementSelectors;

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
        Logger.addLogStream(System.out, 8);
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

        when(j.getSchema()).thenReturn(new Schema("TestSchema", -1, null, xsd));
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

        Diff diffIdentical = DiffBuilder.compare(expected).withTest(actual)
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndAllAttributes))
                .ignoreComments()
                .ignoreWhitespace()
                .checkForSimilar()
                .build();

        if(diffIdentical.hasDifferences()) Logger.warning(diffIdentical.toString());

        return !diffIdentical.hasDifferences();
    }
}
