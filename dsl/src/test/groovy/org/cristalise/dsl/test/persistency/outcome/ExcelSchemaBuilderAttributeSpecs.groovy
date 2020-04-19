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
class ExcelSchemaBuilderAttributeSpecs extends Specification implements CristalTestSetup {

    def setup()   { loggerSetup()    }
    def cleanup() { cristalCleanup() }

    def xlsxFile = 'src/test/data/ExcelSchemaBuilderAttribute.xlsx'

    def 'Attribute accepts a number of types: string, boolean, integer, decimal, date, time, dateTime'() {
        expect: "Accepted types are ${org.cristalise.dsl.persistency.outcome.Attribute.types}"
        SchemaTestBuilder.excel('test', 'Types', 0, xlsxFile)
        .compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                          <xs:element name='Types'>
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
        SchemaTestBuilder.excel('test', 'UnknownType', 0, xlsxFile)

        then:
        thrown(InvalidDataException)
    }

    def 'Attribute can use multiplicity to specify if it is optional or not'() {
        expect:
        SchemaTestBuilder.excel('test', 'Multiplicity', 0, xlsxFile)
        .compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                          <xs:element name='Multiplicity'>
                            <xs:complexType>
                              <xs:attribute name="optional" type='xs:string' />
                              <xs:attribute name="required" type='xs:string' use="required"/>
                            </xs:complexType>
                          </xs:element>
                        </xs:schema>""")
    }
    
    def 'Attribute CANNOT specify multiplicity other than 0..1 and 1..1'() {
        when: "attribute specifies multiplicity"

        SchemaTestBuilder.excel('test', 'WrongMultiplicity', 0, xlsxFile)

        then: "InvalidDataException is thrown"
        thrown(InvalidDataException)
    }

    def 'Attribute can specify range'() {
        expect:
        SchemaTestBuilder.excel('test', 'Range', 0, xlsxFile)
        .compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                          <xs:element name='Range'>
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
        SchemaTestBuilder.excel('test', 'XSDExclusive', 0, xlsxFile)
        .compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                          <xs:element name='XSDExclusive'>
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

    def 'Attribute CANNOT specify documentation'() {
        when: "attribute specifies multiplicity"

        SchemaTestBuilder.excel('test', 'Documentation', 0, xlsxFile)

        then: "InvalidDataException is thrown"
        thrown(InvalidDataException)
    }

    def 'Attribute can define the default value'() {
        expect:
        SchemaTestBuilder.excel('test', 'DefaultValue', 0, xlsxFile)
        .compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                          <xs:element name='DefaultValue'>
                            <xs:complexType>
                              <xs:attribute name="Gender" type='xs:string' default="female"/>
                            </xs:complexType>
                          </xs:element>
                        </xs:schema>""")
        }

    def 'Attribute can define a predefined set of values'() {
        expect:
        SchemaTestBuilder.excel('test', 'Values', 0, xlsxFile)
        .compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                          <xs:element name='Values'>
                            <xs:complexType>
                              <xs:attribute name="Gender">
                                <xs:simpleType>
                                  <xs:restriction base="xs:string">
                                    <xs:enumeration value="male" />
                                    <xs:enumeration value="female" />
                                    <xs:enumeration value="she male" />
                                  </xs:restriction>
                                </xs:simpleType>
                              </xs:attribute>
                            </xs:complexType>
                          </xs:element>
                        </xs:schema>""")
    }

    def 'Attribute value can be restricted by reqex pattern'() {
        expect:
        SchemaTestBuilder.excel('test', 'Pattern', 0, xlsxFile)
        .compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                          <xs:element name='Pattern'>
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
        SchemaTestBuilder.excel('test', 'Digits', 0, xlsxFile)
        .compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                          <xs:element name='Digits'>
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
