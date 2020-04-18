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
import org.cristalise.dsl.persistency.outcome.Field
import org.cristalise.dsl.persistency.outcome.SchemaBuilder
import org.cristalise.dsl.test.builders.SchemaTestBuilder
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.test.utils.CristalTestSetup

import spock.lang.Specification


/**
 *
 */
class SchemaBuilderSpecs extends Specification implements CristalTestSetup {

    def setup()   { loggerSetup()    }
    def cleanup() { cristalCleanup() }


    def 'Schema can be built from XSD file'() {
        expect:
        SchemaBuilder.build("Test", "TestData", 0, "src/test/data/TestData.xsd").schema.som.isValid()
    }


    def 'Empty specification throws InvalidDataException'() {
        when:
        SchemaBuilder.build('Test', 'TestData', 0) {}.schema.som.isValid()

        then:
        thrown(InvalidDataException)
    }


    def 'Empty named Structure builds a valid Schema'() {
        expect:
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData')
        }
        .compareXML("""<?xml version='1.0' encoding='utf-8'?>
                       <xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                         <xs:element name='TestData'>
                           <xs:complexType/>
                         </xs:element>
                       </xs:schema>""")
    }


    def 'Building empty Structure with documentation adds annotation to the element'() {
        expect:
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData', documentation: "Test data documentation")
        }
        .compareXML("""<?xml version='1.0' encoding='utf-8'?>
                       <xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                         <xs:element name='TestData'>
                           <xs:annotation>
                             <xs:documentation>Test data documentation</xs:documentation>
                           </xs:annotation>
                           <xs:complexType/>
                         </xs:element>
                       </xs:schema>""")
    }


    def 'Structure can have a list of Attributes which default type is string'() {
        expect:
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData') {
                attribute(name:'stringAttr1')
                attribute(name:'stringAttr2')
            }
        }
        .compareXML("""<?xml version='1.0' encoding='utf-8'?>
                       <xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                         <xs:element name='TestData'>
                           <xs:complexType>
                             <xs:attribute name="stringAttr1" type="xs:string"/>
                             <xs:attribute name="stringAttr2" type="xs:string"/>
                           </xs:complexType>
                         </xs:element>
                       </xs:schema>""")
    }


    def 'Structure can define sequence of Fields which default type is string and multiplicity is 1'() {
        expect:
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData', useSequence: true) { 
                field(name:'stringField1')
                field(name:'stringField2')
            }
        }.compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                          <xs:element name='TestData'>
                            <xs:complexType>
                              <xs:sequence>
                                <xs:element name='stringField1' type='xs:string' minOccurs='1' maxOccurs='1' />
                                <xs:element name='stringField2' type='xs:string' minOccurs='1' maxOccurs='1' />
                              </xs:sequence>
                            </xs:complexType>
                          </xs:element>
                        </xs:schema>""")
    }


    def 'Structure can define anyField with defaults minOccurs=0 and processContents=lax'() {
        expect:
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData', useSequence: true) {
                field(name:'stringField1')
                anyField()
            }
        }.compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                          <xs:element name='TestData'>
                            <xs:complexType>
                              <xs:sequence>
                                <xs:element name='stringField1' type='xs:string' minOccurs='1' maxOccurs='1' />
                                <xs:any minOccurs='0' processContents='lax'/>
                              </xs:sequence>
                            </xs:complexType>
                          </xs:element>
                        </xs:schema>""")
    }


    def 'Structure can define an unordered set of Fields xs:all which default type is string and multiplicity is 1'() {
        expect:
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData') { 
                field(name:'stringField1')
                field(name:'stringField2')
            }
        }.compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                          <xs:element name='TestData'>
                            <xs:complexType>
                              <xs:all minOccurs='0'>
                                <xs:element name='stringField1' type='xs:string' minOccurs='1' maxOccurs='1' />
                                <xs:element name='stringField2' type='xs:string' minOccurs='1' maxOccurs='1' />
                              </xs:all>
                            </xs:complexType>
                          </xs:element>
                        </xs:schema>""")
    }


    def 'Attribute accepts a number of types: string, boolean, integer, decimal, date, time, dateTime'() {
        expect: "Accepted types are ${org.cristalise.dsl.persistency.outcome.Attribute.types}"
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData') {
                Attribute.types.each {
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


    def 'Field accepts a number of types: string, boolean, integer, decimal, date, time, dateTime'() {
        expect: "Accepted types are ${org.cristalise.dsl.persistency.outcome.Field.types}"
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData') {
                Field.types.each {
                    field(name:"${it}Field", type: it)
                }
            }
        }.compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                          <xs:element name='TestData'>
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
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData') { 
                field(name: 'byteField', type: 'byte')
            }
        }

        then:
        thrown(InvalidDataException)
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
                attribute(name: 'byteField', multiplicity: '1..1')
            }
        }.compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                          <xs:element name='TestData'>
                            <xs:complexType>
                              <xs:attribute name="byteField" type='xs:string' use="required"/>
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
 

    def 'Field can specify multiplicity'() {
        expect:
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData', useSequence: true) {
                field(name:'default')
                field(name:'many',        multiplicity:'*')
                field(name:'fivehundred', multiplicity:'500')
                field(name:'zeroToMany',  multiplicity:'0..*')
                field(name:'oneToFive',   multiplicity:'1..5')
                field(name:'reset',       multiplicity:'')
            }
        }.compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                            <xs:element name='TestData'>
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
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData') {
                field(name:'inclusiveInt', type:'integer', range:'[0..10]')
                field(name:'exclusiveInt', type:'integer', range:'(0..10)')
                field(name:'inclusiveDec', type:'decimal', range:'[0.1..0.11]')
                field(name:'exclusiveDec', type:'decimal', range:'(0.1..0.11)')
            }
        }.compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                          <xs:element name='TestData'>
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
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData') {
                field(name:'inclusive', type:'integer', minInclusive:0, maxInclusive: 10)
                field(name:'exclusive', type:'integer', minExclusive:0, maxExclusive: 10)
                field(name:'minExclusive', type:'integer', minExclusive:0)
                field(name:'maxInclusive', type:'integer', maxInclusive: 10)
            }
        }.compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                          <xs:element name='TestData'>
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


    def 'Field can define the default value'() {
        expect:
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData') {
                field(name: 'Gender', type: 'string', default: 'female')
            }
        }.compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                            <xs:element name='TestData'>
                                <xs:complexType>
                                    <xs:all minOccurs='0'>
                                        <xs:element minOccurs="1" maxOccurs="1" name="Gender" type='xs:string' default="female"/>
                                    </xs:all>
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


    def 'Field can define a predefined set of values'() {
        expect:
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData', useSequence: true) {
                field(name: 'Gender', type: 'string', values: ['male', 'female'])
            }
        }.compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                            <xs:element name='TestData'>
                                <xs:complexType>
                                    <xs:sequence>
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

    def 'Field value can be restricted by reqex pattern'() {
        expect:
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData') {
                field(name: 'Gender', type: 'string', pattern: 'male|female')
            }
        }.compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                          <xs:element name='TestData'>
                            <xs:complexType>
                              <xs:all minOccurs='0'>
                                <xs:element minOccurs="1" maxOccurs="1" name="Gender">
                                  <xs:simpleType>
                                    <xs:restriction base="xs:string">
                                      <xs:pattern value="male|female"/>
                                    </xs:restriction>
                                  </xs:simpleType>
                                </xs:element>
                              </xs:all>
                            </xs:complexType>
                          </xs:element>
                        </xs:schema>""")
    }


    def 'Field value of decimal type can be restricted by totalDigits and fractionDigits'() {
        expect:
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData') {
                field(name: 'efficiency', type: 'decimal', totalDigits: 3, fractionDigits: 2)
            }
        }.compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                          <xs:element name='TestData'>
                            <xs:complexType>
                              <xs:all minOccurs='0'>
                                <xs:element minOccurs="1" maxOccurs="1" name="efficiency">
                                  <xs:simpleType>
                                    <xs:restriction base="xs:decimal">
                                      <xs:totalDigits value="3"/>
                                      <xs:fractionDigits value="2"/>
                                    </xs:restriction>
                                  </xs:simpleType>
                                </xs:element>
                              </xs:all>
                            </xs:complexType>
                          </xs:element>
                        </xs:schema>""")
    }


    def 'Field can have documentation'() {
        expect:
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData') {
                field(name: 'Field', documentation: 'Field has Documentation')
            }
        }.compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                          <xs:element name='TestData'>
                            <xs:complexType>
                              <xs:all minOccurs='0'>
                                <xs:element name='Field' type='xs:string' minOccurs='1' maxOccurs='1'>
                                  <xs:annotation>
                                    <xs:documentation>Field has Documentation</xs:documentation>
                                  </xs:annotation>
                                </xs:element>
                              </xs:all>
                            </xs:complexType>
                          </xs:element>
                        </xs:schema>""")
    }

    
    def 'Field can have Unit which is added as attribute of type string'() {
        expect:
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData', useSequence: true) {
                field(name: 'Weight', type: 'decimal') { unit() }
            }
        }.compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                            <xs:element name='TestData'>
                                <xs:complexType>
                                    <xs:sequence>
                                        <xs:element name='Weight' minOccurs='1' maxOccurs='1'>
                                            <xs:complexType>
                                                <xs:simpleContent>
                                                    <xs:extension base='xs:decimal'>
                                                        <xs:attribute name='unit' type='xs:string' use='required'/>
                                                    </xs:extension>
                                                </xs:simpleContent>
                                            </xs:complexType>
                                        </xs:element>
                                    </xs:sequence>
                                </xs:complexType>
                            </xs:element>
                        </xs:schema>""")
    }


    def 'Unit can specify the list of values it contains with default'() {
        expect:
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData', useSequence: true) {
                field(name: 'Weight', type: 'decimal') { unit(values: ['g', 'kg'], default: 'kg') }
            }
        }.compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                            <xs:element name='TestData'>
                                <xs:complexType>
                                    <xs:sequence>
                                        <xs:element name='Weight' minOccurs='1' maxOccurs='1'>
                                            <xs:complexType>
                                                <xs:simpleContent>
                                                    <xs:extension base='xs:decimal'>
                                                        <xs:attribute name='unit' default='kg' use='optional'>
                                                            <xs:simpleType>
                                                                <xs:restriction base="xs:string">
                                                                   <xs:enumeration value="g" />
                                                                   <xs:enumeration value="kg" />
                                                                </xs:restriction>
                                                            </xs:simpleType>
                                                        </xs:attribute>
                                                    </xs:extension>
                                                </xs:simpleContent>
                                            </xs:complexType>
                                        </xs:element>
                                    </xs:sequence>
                                </xs:complexType>
                            </xs:element>
                        </xs:schema>""")
    }


    def 'Complex example using PatientDetails from Basic Tutorial'() {
        expect:
        SchemaTestBuilder.build('test', 'PatientDetails', 0) {
            struct(name: 'PatientDetails', documentation: 'This is the Schema for Basic Tutorial') {
                attribute(name: 'InsuranceNumber', type: 'string', default: '123456789ABC')
                field(name: 'DateOfBirth',     type: 'date', documentation: 'DateOfBirth docs') {dynamicForms(updateScriptRef : "Script:0")}
                field(name: 'Gender',          type: 'string', values: ['male', 'female'])
                field(name: 'Weight',          type: 'decimal') { unit(values: ['g', 'kg'], default: 'kg') }
            }
        }.compareXML(
            """<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                 <xs:element name="PatientDetails">
                   <xs:annotation>
                     <xs:documentation>This is the Schema for Basic Tutorial</xs:documentation>
                   </xs:annotation>
                   <xs:complexType>
                   <xs:all minOccurs="0">
                   <xs:element name='DateOfBirth' type='xs:date' minOccurs='1' maxOccurs='1'>
                     <xs:annotation>
                       <xs:documentation>DateOfBirth docs</xs:documentation>
                       <xs:appinfo>
                         <dynamicForms>
                           <additional>
                             <updateScriptRef>Script:0</updateScriptRef>
                           </additional>
                         </dynamicForms>
                       </xs:appinfo>
                     </xs:annotation>
                    </xs:element>
                    <xs:element minOccurs="1" maxOccurs="1" name="Gender">
                       <xs:simpleType>
                         <xs:restriction base="xs:string">
                           <xs:enumeration value="male" />
                           <xs:enumeration value="female" />
                         </xs:restriction>
                       </xs:simpleType>
                     </xs:element>
                     <xs:element name='Weight' minOccurs='1' maxOccurs='1'>
                       <xs:complexType>
                         <xs:simpleContent>
                           <xs:extension base='xs:decimal'>
                             <xs:attribute name='unit' default='kg' use='optional'>
                               <xs:simpleType>
                                 <xs:restriction base='xs:string'>
                                   <xs:enumeration value='g' />
                                   <xs:enumeration value='kg' />
                                 </xs:restriction>
                               </xs:simpleType>
                             </xs:attribute>
                           </xs:extension>
                         </xs:simpleContent>
                       </xs:complexType>
                     </xs:element>
                   </xs:all>
                   <xs:attribute name="InsuranceNumber" type="xs:string" default= "123456789ABC"/>
                 </xs:complexType>
               </xs:element>
             </xs:schema>""")
    }

    def 'Structure can define nested Structure with multiplicity'() {
        expect:
        def titles = ['Mr','Mrs','Miss','Ms','Sir','Dr','dr']
        def states = ['UNINITIALISED', 'ACTIVE', 'DEACTIVETED']
        def phones = ['MOBILE', 'WORK', 'HOME', 'WORK_FAX', 'HOME_FAX', 'OTHER', 'CUSTOM']
    
        SchemaTestBuilder.build('test', 'Person', 0) {
            struct(name: 'Person', documentation: 'Person data', useSequence: true) {
                field(name: 'Title',  type: 'string', values: titles)
                field(name: 'Name',   type: 'string')
                field(name: 'State',  type: 'string', values: states) { dynamicForms (hidden: true) }
    
                // next 3 structs were copied from contactMech
                struct(name: 'Phone', documentation: 'Defines Phone entries', multiplicity: '0..*', useSequence: true) {
                    field(name: 'Number',     type: 'string')
                    field(name: 'Type',       type: 'string', values: phones)
                    field(name: 'CustomType', type: 'string')
                }
            }
        }.compareXML(
            """<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                  <xs:element name='Person'>
                    <xs:annotation>
                      <xs:documentation>Person data</xs:documentation>
                    </xs:annotation>
                    <xs:complexType>
                      <xs:sequence>
                        <xs:element name='Title' minOccurs='1' maxOccurs='1'>
                          <xs:simpleType>
                            <xs:restriction base='xs:string'>
                              <xs:enumeration value='Mr' />
                              <xs:enumeration value='Mrs' />
                              <xs:enumeration value='Miss' />
                              <xs:enumeration value='Ms' />
                              <xs:enumeration value='Sir' />
                              <xs:enumeration value='Dr' />
                              <xs:enumeration value='dr' />
                            </xs:restriction>
                          </xs:simpleType>
                        </xs:element>
                        <xs:element name='Name' type='xs:string' minOccurs='1' maxOccurs='1' />
                        <xs:element name='State' minOccurs='1' maxOccurs='1'>
                          <xs:annotation>
                            <xs:appinfo>
                              <dynamicForms>
                                <hidden>true</hidden>
                              </dynamicForms>
                            </xs:appinfo>
                          </xs:annotation>
                          <xs:simpleType>
                            <xs:restriction base='xs:string'>
                              <xs:enumeration value='UNINITIALISED' />
                              <xs:enumeration value='ACTIVE' />
                              <xs:enumeration value='DEACTIVETED' />
                            </xs:restriction>
                          </xs:simpleType>
                        </xs:element>
                        <xs:element name='Phone' minOccurs='0' maxOccurs='unbounded'>
                          <xs:annotation>
                            <xs:documentation>Defines Phone entries</xs:documentation>
                          </xs:annotation>
                          <xs:complexType>
                            <xs:sequence>
                              <xs:element name='Number' type='xs:string' minOccurs='1' maxOccurs='1' />
                              <xs:element name='Type' minOccurs='1' maxOccurs='1'>
                                <xs:simpleType>
                                  <xs:restriction base='xs:string'>
                                    <xs:enumeration value='MOBILE' />
                                    <xs:enumeration value='WORK' />
                                    <xs:enumeration value='HOME' />
                                    <xs:enumeration value='WORK_FAX' />
                                    <xs:enumeration value='HOME_FAX' />
                                    <xs:enumeration value='OTHER' />
                                    <xs:enumeration value='CUSTOM' />
                                  </xs:restriction>
                                </xs:simpleType>
                              </xs:element>
                              <xs:element name='CustomType' type='xs:string' minOccurs='1' maxOccurs='1' />
                            </xs:sequence>
                          </xs:complexType>
                        </xs:element>
                      </xs:sequence>
                    </xs:complexType>
                  </xs:element>
                </xs:schema>"""
        )
    }

    
    def 'Field can have attribute'() {
        expect:
        SchemaTestBuilder.build('test', 'PatientDetails', 0) {
            struct(name: 'PatientDetails', documentation: 'This is the Schema for Basic Tutorial') {
                attribute(name: 'InsuranceNumber', type: 'string', default: '123456789ABC')
                field(name: 'DateOfBirth', type: 'date')
                field(name: 'Gender',      type: 'string', values: ['male', 'female'])
                field(name: 'Weight',      type: 'decimal') { 
                    attribute(name: 'unit', type: 'string', values: ['g', 'kg'], default: 'kg')
                }
            }
        }.compareXML(
            """<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                 <xs:element name="PatientDetails">
                   <xs:annotation>
                     <xs:documentation>This is the Schema for Basic Tutorial</xs:documentation>
                   </xs:annotation>
                   <xs:complexType>
                   <xs:all minOccurs="0">
                     <xs:element minOccurs="1" maxOccurs="1" name="DateOfBirth" type="xs:date"/>
                     <xs:element minOccurs="1" maxOccurs="1" name="Gender">
                       <xs:simpleType>
                         <xs:restriction base="xs:string">
                           <xs:enumeration value="male" />
                           <xs:enumeration value="female" />
                         </xs:restriction>
                       </xs:simpleType>
                     </xs:element>
                     <xs:element name='Weight' minOccurs='1' maxOccurs='1'>
                       <xs:complexType>
                         <xs:simpleContent>
                           <xs:extension base='xs:decimal'>
                             <xs:attribute name='unit' default='kg'>
                               <xs:simpleType>
                                 <xs:restriction base='xs:string'>
                                   <xs:enumeration value='g' />
                                   <xs:enumeration value='kg' />
                                 </xs:restriction>
                               </xs:simpleType>
                             </xs:attribute>
                           </xs:extension>
                         </xs:simpleContent>
                       </xs:complexType>
                     </xs:element>
                   </xs:all>
                   <xs:attribute name="InsuranceNumber" type="xs:string" default= "123456789ABC"/>
                 </xs:complexType>
               </xs:element>
             </xs:schema>""")
    }

    def 'Structure definition keeps the order of fields and structs'() {
        expect:
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData') { 
                field(name:'stringField1')
                struct(name: 'TestData1') { 
                    field(name:'stringField1')
                }
                field(name:'stringField2')
                struct(name: 'TestData2') { 
                    field(name:'stringField1')
                }
                field(name:'stringField3')
            }
        }.compareXML("""<?xml version='1.0' encoding='utf-8'?>
                        <xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                          <xs:element name='TestData'>
                            <xs:complexType>
                              <xs:all minOccurs='0'>
                                <xs:element name='stringField1' type='xs:string' minOccurs='1' maxOccurs='1' />
                                <xs:element name='TestData1'>
                                  <xs:complexType>
                                    <xs:all minOccurs='0'>
                                      <xs:element name='stringField1' type='xs:string' minOccurs='1' maxOccurs='1' />
                                    </xs:all>
                                  </xs:complexType>
                                </xs:element>
                                <xs:element name='stringField2' type='xs:string' minOccurs='1' maxOccurs='1' />
                                <xs:element name='TestData2'>
                                  <xs:complexType>
                                    <xs:all minOccurs='0'>
                                      <xs:element name='stringField1' type='xs:string' minOccurs='1' maxOccurs='1' />
                                    </xs:all>
                                  </xs:complexType>
                                </xs:element>
                                <xs:element name='stringField3' type='xs:string' minOccurs='1' maxOccurs='1' />
                              </xs:all>
                            </xs:complexType>
                          </xs:element>
                        </xs:schema>""")
    }
}
