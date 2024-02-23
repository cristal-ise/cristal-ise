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

import org.cristalise.dsl.persistency.outcome.Attribute;
import org.cristalise.dsl.test.builders.SchemaTestBuilder
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.test.utils.CristalTestSetup

import spock.lang.Specification


/**
 *
 */
class SchemaBuilderAttributeSpecs extends Specification implements CristalTestSetup {

    def setup()   {}
    def cleanup() {}

    def 'Attribute accepts a number of types: string, boolean, integer, decimal, date, time, dateTime'() {
        expect: "Accepted types are ${org.cristalise.dsl.persistency.outcome.Attribute.types.findAll { it != 'anyType' }}"
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData') {
                Attribute.types.findAll { it != 'anyType' }.each {
                    attribute(name:"${it}Attribute", type: it)
                }
            }
        }.compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                          <xs:element name='TestData'>
                            <xs:complexType>
                              <xs:attribute name='stringAttribute'   type='xs:string'  />
                              <xs:attribute name='booleanAttribute'  type='xs:boolean' />
                              <xs:attribute name='integerAttribute'  type='xs:integer' />
                              <xs:attribute name='decimalAttribute'  type='xs:decimal' />
                              <xs:attribute name='dateAttribute'     type='xs:date'    />
                              <xs:attribute name='timeAttribute'     type='xs:time'    />
                              <xs:attribute name='dateTimeAttribute' type='xs:dateTime'/>
                            </xs:complexType>
                          </xs:element>
                        </xs:schema>""")
    }

    def 'Unknown Attribute type throws InvalidDataException'() {
        when: "Accepted types are ${org.cristalise.dsl.persistency.outcome.Attribute.types}"
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData') { 
                attribute(name: 'byteField', type: 'byte')
            }
        }

        then:
        thrown(InvalidDataException)
    }

    def 'Attribute can use multiplicity to specify if it is optional or not'() {
        expect:
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData') {
                attribute(name: 'required', multiplicity: '1..1')
                attribute(name: 'optional', multiplicity: '0..1')
            }
        }.compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                          <xs:element name='TestData'>
                            <xs:complexType>
                              <xs:attribute name="required" type='xs:string' use="required"/>
                              <xs:attribute name="optional" type='xs:string' />
                            </xs:complexType>
                          </xs:element>
                        </xs:schema>""")
    }

    def 'Attribute can specify range'() {
        expect:
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData') {
                attribute(name:'inclusiveInt', type:'integer', range:'[0..10]')
                attribute(name:'exclusiveInt', type:'integer', range:'(0..10)')
                attribute(name:'inclusiveDec', type:'decimal', range:'[0.1..0.11]')
                attribute(name:'exclusiveDec', type:'decimal', range:'(0.1..0.11)')
            }
        }.compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                          <xs:element name='TestData'>
                            <xs:complexType>
                              <xs:attribute name="inclusiveInt">
                                <xs:simpleType>
                                  <xs:restriction base="xs:integer">
                                    <xs:minInclusive value="0"/>
                                    <xs:maxInclusive value="10"/>
                                  </xs:restriction>
                                </xs:simpleType>
                              </xs:attribute>
                              <xs:attribute name="exclusiveInt">
                                <xs:simpleType>
                                  <xs:restriction base="xs:integer">
                                    <xs:minExclusive value="0"/>
                                    <xs:maxExclusive value="10"/>
                                  </xs:restriction>
                                </xs:simpleType>
                              </xs:attribute>
                              <xs:attribute name="inclusiveDec">
                                <xs:simpleType>
                                  <xs:restriction base="xs:decimal">
                                    <xs:minInclusive value="0.1"/>
                                    <xs:maxInclusive value="0.11"/>
                                  </xs:restriction>
                                </xs:simpleType>
                              </xs:attribute>
                              <xs:attribute name="exclusiveDec">
                                <xs:simpleType>
                                  <xs:restriction base="xs:decimal">
                                    <xs:minExclusive value="0.1"/>
                                    <xs:maxExclusive value="0.11"/>
                                  </xs:restriction>
                                </xs:simpleType>
                              </xs:attribute>
                            </xs:complexType>
                          </xs:element>
                        </xs:schema>""")
    }

    def 'Attribute can specify minInclusive/maxInclusive/minExclusive/maxExclusive restrictions'() {
        expect:
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData') {
                attribute(name:'inclusive', type:'integer', minInclusive:0, maxInclusive: 10)
                attribute(name:'exclusive', type:'integer', minExclusive:0, maxExclusive: 10)
                attribute(name:'minExclusive', type:'integer', minExclusive:0)
                attribute(name:'maxInclusive', type:'integer', maxInclusive: 10)
            }
        }.compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                          <xs:element name='TestData'>
                            <xs:complexType>
                              <xs:attribute name="inclusive">
                                <xs:simpleType>
                                  <xs:restriction base="xs:integer">
                                    <xs:minInclusive value="0"/>
                                    <xs:maxInclusive value="10"/>
                                  </xs:restriction>
                                </xs:simpleType>
                              </xs:attribute>
                              <xs:attribute name="exclusive">
                                <xs:simpleType>
                                  <xs:restriction base="xs:integer">
                                    <xs:minExclusive value="0"/>
                                    <xs:maxExclusive value="10"/>
                                  </xs:restriction>
                                </xs:simpleType>
                              </xs:attribute>
                              <xs:attribute name="minExclusive">
                                <xs:simpleType>
                                  <xs:restriction base="xs:integer">
                                    <xs:minExclusive value="0"/>
                                  </xs:restriction>
                                </xs:simpleType>
                              </xs:attribute>
                              <xs:attribute name="maxInclusive">
                                <xs:simpleType>
                                  <xs:restriction base="xs:integer">
                                    <xs:maxInclusive value="10"/>
                                  </xs:restriction>
                                </xs:simpleType>
                              </xs:attribute>
                            </xs:complexType>
                          </xs:element>
                        </xs:schema>""")
    }

    def 'Attribute CANNOT specify multiplicity other than 0..1 and 1..1'() {
        when: "attribute specifies multiplicity"
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData') {
                attribute(name: 'byteField', multiplicity: '2')
            }
        }

        then: "InvalidDataException is thrown"
        thrown(InvalidDataException)
    }

    def 'Attribute CANNOT specify documentation'() {
        when: "attribute specifies multiplicity"
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData') {
                attribute(name: 'attr1', documentation: 'docs')
            }
        }

        then: "InvalidDataException is thrown"
        thrown(InvalidDataException)
    }
 

    def 'Attribute can define the default value'() {
            expect:
            SchemaTestBuilder.build('Test', 'TestData', 0) {
                struct(name: 'TestData') {
                    attribute(name: 'Gender', type: 'string', default: 'female')
                }
            }.compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                              <xs:element name='TestData'>
                                <xs:complexType>
                                  <xs:attribute name="Gender" type='xs:string' default="female"/>
                                </xs:complexType>
                              </xs:element>
                            </xs:schema>""")
        }


    def 'Attribute can define a predefined set of values'() {
        expect:
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData') {
                attribute(name: 'Gender', type: 'string', values: ['male', 'female'])
            }
        }.compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                          <xs:element name='TestData'>
                            <xs:complexType>
                              <xs:attribute name="Gender">
                                <xs:simpleType>
                                  <xs:restriction base="xs:string">
                                    <xs:enumeration value="male" />
                                    <xs:enumeration value="female" />
                                  </xs:restriction>
                                </xs:simpleType>
                              </xs:attribute>
                            </xs:complexType>
                          </xs:element>
                        </xs:schema>""")
    }


    def 'Attribute value can be restricted by reqex pattern'() {
        expect:
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData') {
                attribute(name: 'Gender', type: 'string', pattern: 'male|female')
            }
        }.compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                          <xs:element name='TestData'>
                            <xs:complexType>
                              <xs:attribute name="Gender">
                                <xs:simpleType>
                                  <xs:restriction base="xs:string">
                                    <xs:pattern value="male|female"/>
                                  </xs:restriction>
                                </xs:simpleType>
                              </xs:attribute>
                            </xs:complexType>
                          </xs:element>
                        </xs:schema>""")
    }


    def 'Attribute value of decimal type can be restricted by totalDigits and fractionDigits'() {
        expect:
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData') {
                attribute(name: 'efficiency', type: 'decimal', totalDigits: 3, fractionDigits: 2)
            }
        }.compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                          <xs:element name='TestData'>
                            <xs:complexType>
                              <xs:attribute name="efficiency">
                                <xs:simpleType>
                                  <xs:restriction base="xs:decimal">
                                    <xs:totalDigits value="3"/>
                                    <xs:fractionDigits value="2"/>
                                  </xs:restriction>
                                </xs:simpleType>
                              </xs:attribute>
                            </xs:complexType>
                          </xs:element>
                        </xs:schema>""")
    }
}
