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
package org.cristalise.dsl.test.persistency.outcome

import org.cristalise.dsl.persistency.outcome.SchemaBuilder
import org.cristalise.test.CristalTestSetup

import spock.lang.Specification


/**
 *
 */
class SchemaBuilderSpecs extends Specification implements CristalTestSetup {

    def setup() {
        loggerSetup()
    }

    def cleanup() {
        cristalCleanup()
    }
    
    def 'Simple TestData type'() {
        given:
        def sb = SchemaBuilder.build("Test", "TestData", 0, "src/test/data/TestData.xsd")

        when:
        def stb = SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData') {
                field(name: 'counter', type: 'integer')
            }
        }

        then:
        stb.compareXML(sb.schema.schema)
    }

    def 'PatientDetails of Basic Tutorial'() {
        expect:
        SchemaTestBuilder.build('test', 'PatientDetails', 0) {
            struct(name: 'PatientDetails', documentation: 'This is the Schema for Basic Tutorial') {
                field(name: 'InsuranceNumber', type: 'string', default: '123456789ABC')
                field(name: 'DateOfBirth',     type: 'dateTime')
                field(name: 'Gender',          type: 'string', values: ['male', 'female'])
            }
        }.compareXML(
            """<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                    <xs:element name="PatientDetails">
                        <xs:annotation>
                            <xs:documentation>This is the Schema for Basic Tutorial</xs:documentation>
                        </xs:annotation>
                        <xs:complexType>
                            <xs:sequence>
                                <xs:element minOccurs="1" maxOccurs="1" name="InsuranceNumber" type="xs:string" default= "123456789ABC"/>
                                <xs:element minOccurs="1" maxOccurs="1" name="DateOfBirth" type="xs:dateTime"/>
                                <xs:element minOccurs="1" maxOccurs="1" name="Gender">
                                    <xs:simpleType>
                                        <xs:restriction base="xs:string">
                                           <xs:enumeration value="male" />
                                           <xs:enumeration value="female" />
                                        </xs:restriction>
                                    </xs:simpleType>
                                </xs:element>
                            </xs:sequence>
                        </xs:complexType>
                    </xs:element>
                </xs:schema>""")
    }
}
