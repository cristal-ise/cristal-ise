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
package org.cristalise.dsl.test.querying

import org.cristalise.dsl.querying.QueryBuilder
import org.cristalise.kernel.test.utils.CristalTestSetup

import spock.lang.Specification


/**
 *
 *
 */
class QueryBuilderSpecs extends Specification implements CristalTestSetup {
    def setupSpec() {
        inMemorySetup()
    }

    def cleanupSpec() {
        cristalCleanup()
    }

    def 'Specifying xquery without dialect'() {
        expect:
        QueryBuilder.build("testing", "MyFirstQuery", 0) {
            parameter(name: 'uuid', type: 'java.lang.String')
            query(language: "existdb:xquery") {
'''<TRList>{
    for $prop in collection('weighbridge')/Property[@name='Type']
    where $prop = "Weighbridge"
    return local:queryTransaction(util:collection-name($prop))
    }</TRList>'''
            }
        }
        .compareXML(
'''<cristalquery name="MyFirstQuery" version="0">
    <parameter name="uuid" type="java.lang.String"/>
    <rootElement value='QueryResult' />
    <recordElement value='Record' />
    <query language="existdb:xquery"><![CDATA[
    <TRList>{
    for $prop in collection('weighbridge')/Property[@name='Type']
    where $prop = "Weighbridge"
    return local:queryTransaction(util:collection-name($prop))
    }</TRList>
    ]]></query>
</cristalquery>''')
    }

    def 'Specifying sql without dialect'() {
        expect:
        QueryBuilder.build("testing", "MyFirstQuery", 0) {
            parameter(name: 'domainPath', type: 'java.lang.String')
            rootElement("MyFirstQueryResult")
            recordElement("Item")
            query(language: "sql") {
"""SELECT ip."UUID",
    MAX(ip."VALUE") FILTER (WHERE ip."NAME" = 'Module')  AS "Module",
        MAX(ip."VALUE") FILTER (WHERE ip."NAME" = 'Type')    AS "Type",
        MAX(ip."VALUE") FILTER (WHERE ip."NAME" = 'Name')    AS "Name",
        MAX(ip."VALUE") FILTER (WHERE ip."NAME" = 'Version') AS "Version"
    FROM "ITEM_PROPERTY" ip
    LEFT JOIN "DOMAIN_PATH" dp ON ip."UUID" = dp."TARGET"
    WHERE dp."PATH" LIKE '@{domainPath}%'
    GROUP BY ip."UUID", dp."PATH"
    ORDER BY "Type", "Name", "Version";"""
            }
        }
            .compareXML(
"""<cristalquery name="MyFirstQuery" version="0">
  <parameter name="domainPath" type="java.lang.String"/>
  <rootElement value='MyFirstQueryResult' />
  <recordElement value='Item' />
  <query language="sql"><![CDATA[ 
    SELECT ip."UUID",
    MAX(ip."VALUE") FILTER (WHERE ip."NAME" = 'Module')  AS "Module",
        MAX(ip."VALUE") FILTER (WHERE ip."NAME" = 'Type')    AS "Type",
        MAX(ip."VALUE") FILTER (WHERE ip."NAME" = 'Name')    AS "Name",
        MAX(ip."VALUE") FILTER (WHERE ip."NAME" = 'Version') AS "Version"
    FROM "ITEM_PROPERTY" ip
    LEFT JOIN "DOMAIN_PATH" dp ON ip."UUID" = dp."TARGET"
    WHERE dp."PATH" LIKE '@{domainPath}%'
    GROUP BY ip."UUID", dp."PATH"
    ORDER BY "Type", "Name", "Version";
 ]]></query>
</cristalquery>""")
    }

    def 'Specifying sql with dialect'() {
        expect:
        QueryBuilder.build("testing", "MyDialectQuery", 0) {
            query(language: "sql", dialect: "postgres") {
                "SELECT * FROM dual"
            }
        }
        .compareXML(
"""<cristalquery name="MyDialectQuery" version="0">
    <rootElement value='QueryResult' />
    <recordElement value='Record' />
    <query language="sql" dialect="postgres"><![CDATA[ SELECT * FROM dual ]]></query>
</cristalquery>""")
    }
}
