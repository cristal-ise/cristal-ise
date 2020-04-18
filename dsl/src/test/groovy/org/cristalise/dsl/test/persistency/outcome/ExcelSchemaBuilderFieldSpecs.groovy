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

import org.cristalise.dsl.test.builders.SchemaTestBuilder
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.test.utils.CristalTestSetup

import spock.lang.Specification


/**
 *
 */
class ExcelSchemaBuilderFieldSpecs extends Specification implements CristalTestSetup {

    def setup()   { loggerSetup()    }
    def cleanup() { cristalCleanup() }

    def xlsxFile = 'src/test/data/ExcelSchemaBuilderField.xlsx'

    def 'Field accepts a number of types: string, boolean, integer, decimal, date, time, dateTime'() {
        expect: "Accepted types are ${org.cristalise.dsl.persistency.outcome.Field.types}"
        SchemaTestBuilder.excel('test', 'Types', 0, xlsxFile)
        .compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                          <xs:element name='Types'>
                            <xs:complexType>
                              <xs:all minOccurs='0'>
                                <xs:element name='stringField'   type='xs:string'   minOccurs='1' maxOccurs='1' />
                                <xs:element name='booleanField'  type='xs:boolean'  minOccurs='1' maxOccurs='1' />
                                <xs:element name='integerField'  type='xs:integer'  minOccurs='1' maxOccurs='1' />
                                <xs:element name='decimalField'  type='xs:decimal'  minOccurs='1' maxOccurs='1' />
                                <xs:element name='dateField'     type='xs:date'     minOccurs='1' maxOccurs='1' />
                                <xs:element name='timeField'     type='xs:time'     minOccurs='1' maxOccurs='1' />
                                <xs:element name='dateTimeField' type='xs:dateTime' minOccurs='1' maxOccurs='1' />
                              </xs:all>
                            </xs:complexType>
                          </xs:element>
                        </xs:schema>""")
    }

    def 'Unknown Field type throws InvalidDataException'() {
        when: "Accepted types are ${org.cristalise.dsl.persistency.outcome.Field.types}"
        SchemaTestBuilder.excel('test', 'UnknownType', 0, xlsxFile)

        then:
        thrown(InvalidDataException)
    }

    def 'Field can specify multiplicity'() {
        expect:
        SchemaTestBuilder.excel('test', 'Multiplicity', 0, xlsxFile)
        .compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                            <xs:element name='Multiplicity'>
                                <xs:complexType>
                                    <xs:sequence>
                                        <xs:element name='default'     type='xs:string' minOccurs='1' maxOccurs='1' />
                                        <xs:element name='many'        type='xs:string' minOccurs='0' maxOccurs='unbounded' />
                                        <xs:element name='fivehundred' type='xs:string' minOccurs='500' maxOccurs='500' />
                                        <xs:element name='zeroToMany'  type='xs:string' minOccurs='0' maxOccurs='unbounded' />
                                        <xs:element name='oneToFive'   type='xs:string' minOccurs='1' maxOccurs='5' />
                                        <xs:element name='reset'       type='xs:string' />
                                    </xs:sequence>
                                </xs:complexType>
                            </xs:element>
                        </xs:schema>""")
    }

    def 'Field can specify range'() {
        expect:
        SchemaTestBuilder.excel('test', 'Range', 0, xlsxFile)
        .compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                          <xs:element name='Range'>
                            <xs:complexType>
                              <xs:all minOccurs='0'>
                                <xs:element name="inclusiveInt" minOccurs="1" maxOccurs="1">
                                  <xs:simpleType>
                                    <xs:restriction base="xs:integer">
                                      <xs:minInclusive value="0"/>
                                      <xs:maxInclusive value="10"/>
                                    </xs:restriction>
                                  </xs:simpleType>
                                </xs:element>
                                <xs:element name="exclusiveInt" minOccurs="1" maxOccurs="1">
                                  <xs:simpleType>
                                    <xs:restriction base="xs:integer">
                                      <xs:minExclusive value="0"/>
                                      <xs:maxExclusive value="10"/>
                                    </xs:restriction>
                                  </xs:simpleType>
                                </xs:element>
                                <xs:element name="inclusiveDec" minOccurs="1" maxOccurs="1">
                                  <xs:simpleType>
                                    <xs:restriction base="xs:decimal">
                                      <xs:minInclusive value="0.1"/>
                                      <xs:maxInclusive value="0.11"/>
                                    </xs:restriction>
                                  </xs:simpleType>
                                </xs:element>
                                <xs:element name="exclusiveDec" minOccurs="1" maxOccurs="1">
                                  <xs:simpleType>
                                    <xs:restriction base="xs:decimal">
                                      <xs:minExclusive value="0.1"/>
                                      <xs:maxExclusive value="0.11"/>
                                    </xs:restriction>
                                  </xs:simpleType>
                                </xs:element>
                              </xs:all>
                            </xs:complexType>
                          </xs:element>
                        </xs:schema>""")
    }

    def 'Field can specify minInclusive/maxInclusive/minExclusive/maxExclusive restrictions'() {
        expect:
        SchemaTestBuilder.excel('test', 'XSDExclusive', 0, xlsxFile)
        .compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                          <xs:element name='XSDExclusive'>
                            <xs:complexType>
                              <xs:all minOccurs='0'>
                                <xs:element name="inclusive" minOccurs="1" maxOccurs="1">
                                  <xs:simpleType>
                                    <xs:restriction base="xs:integer">
                                      <xs:minInclusive value="0"/>
                                      <xs:maxInclusive value="10"/>
                                    </xs:restriction>
                                  </xs:simpleType>
                                </xs:element>
                                <xs:element name="exclusive" minOccurs="1" maxOccurs="1">
                                  <xs:simpleType>
                                    <xs:restriction base="xs:integer">
                                      <xs:minExclusive value="0"/>
                                      <xs:maxExclusive value="10"/>
                                    </xs:restriction>
                                  </xs:simpleType>
                                </xs:element>
                                <xs:element name="minExclusive" minOccurs="1" maxOccurs="1">
                                  <xs:simpleType>
                                    <xs:restriction base="xs:integer">
                                      <xs:minExclusive value="0"/>
                                    </xs:restriction>
                                  </xs:simpleType>
                                </xs:element>
                                <xs:element name="maxInclusive" minOccurs="1" maxOccurs="1">
                                  <xs:simpleType>
                                    <xs:restriction base="xs:integer">
                                      <xs:maxInclusive value="10"/>
                                    </xs:restriction>
                                  </xs:simpleType>
                                </xs:element>
                              </xs:all>
                            </xs:complexType>
                          </xs:element>
                        </xs:schema>""")
    }
}
