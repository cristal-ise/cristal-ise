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
package org.cristalise.kernel.test.persistency;

import static org.cristalise.kernel.persistency.outcome.Outcome.isIdentical;
import static org.cristalise.kernel.utils.FileStringUtility.resource2String;
import static org.junit.jupiter.api.Assertions.*;

import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.querying.Query;
import org.cristalise.kernel.querying.QueryParsingException;
import org.cristalise.kernel.test.process.MainTest;
import org.cristalise.kernel.utils.FileStringUtility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Properties;

class QueryXMLTest {
    
    private static class TestQuery extends Query {
        public TestQuery(String xml) throws QueryParsingException {
            super(xml);
        }

        @Override
        public void validateXML(String xml) {
            // skip validation for unit test
        }
    }

    @BeforeAll
    public static void setup() throws Exception {
        Properties props = FileStringUtility.loadConfigFile(MainTest.class.getResource("/server.conf").getPath());
        Gateway.init(props);
    }

    @Test
    public void testDialectAttribute() throws Exception {
        String xml = "<cristalquery name='TestQuery' version='1'>" +
                     "  <query language='sql' dialect='postgres'><![CDATA[SELECT * FROM dual]]></query>" +
                     "</cristalquery>";
        
        Query query = new TestQuery(xml);
        assertEquals("postgres", query.getDialect());
    }

    @Test
    public void testDialectParsing() throws Exception {
        String xml = "<cristalquery name='TestQuery' version='1'>" +
                     "  <query language='sql' dialect='oracle'><![CDATA[SELECT * FROM dual]]></query>" +
                     "</cristalquery>";
        
        Query query = new TestQuery(xml);
        assertEquals("oracle", query.getDialect(), "Dialect should be parsed from XML");
        
        String generatedXml = query.getQueryXML();
        assertTrue(generatedXml.contains("dialect='oracle'"), "Generated XML should contain dialect attribute");
    }

    @Test
    public void testDefaultDialect() throws Exception {
        String xml = "<cristalquery name='TestQuery' version='1'>" +
                     "  <query language='sql'><![CDATA[SELECT * FROM dual]]></query>" +
                     "</cristalquery>";
        
        Query query = new TestQuery(xml);
        assertNull(query.getDialect(), "Default dialect should be null");
    }

    @Test
    @Disabled("Castor XML mapping is not done for Query")
    public void testQueryCDATAHandling() throws Exception {
        String origQueryXML = resource2String(CastorXMLTest.class, "/testQuery.xml");
        String marshalledQueryXML = Gateway.getMarshaller().marshall(Gateway.getMarshaller().unmarshall(origQueryXML));

        assertTrue(isIdentical(origQueryXML, marshalledQueryXML));
    }

    @Test
    public void testQueryParsing() throws Exception {
        String origXml = resource2String(CastorXMLTest.class, "/testQuery.xml");
        Query q = new Query(origXml);

        assertEquals("TestQuery", q.getName());
        assertEquals(0, (int)q.getVersion());
        assertEquals("existdb:xquery", q.getLanguage());

        assertEquals(1, q.getParameters().size());
        assertEquals("uuid", q.getParameters().getFirst().getName());
        assertEquals("java.lang.String", q.getParameters().getFirst().getType().getName());

        assertTrue(q.getQuery().startsWith("\n<TRList>"));
        assertTrue(q.getQuery().endsWith("</TRList>\n    "));

        assertTrue(isIdentical(origXml, q.getQueryXML()));
    }

    @Test
    public void testSqlQueryParsing() throws Exception {
        String origXml = resource2String(CastorXMLTest.class, "/testQuerySql.xml");
        Query q = new Query(origXml);

        assertEquals("TestQuerySql", q.getName());
        assertEquals(0, (int)q.getVersion());
        assertEquals("sql", q.getLanguage());
        assertEquals("History", q.getRootElement());
        assertEquals("Event", q.getRecordElement());

        assertEquals(1, q.getParameters().size());
        assertEquals("uuid", q.getParameters().getFirst().getName());
        assertEquals("java.lang.String", q.getParameters().getFirst().getType().getName());

        assertTrue(q.getQuery().startsWith("\nselect"));
        assertTrue(q.getQuery().endsWith("'@{schemaName}'\n    "));

        assertTrue(isIdentical(origXml, q.getQueryXML()));
    }
}
