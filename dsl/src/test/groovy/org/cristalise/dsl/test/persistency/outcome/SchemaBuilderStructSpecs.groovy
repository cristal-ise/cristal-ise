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
import org.cristalise.dsl.test.builders.SchemaTestBuilder
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.test.utils.CristalTestSetup

import spock.lang.Specification


/**
 *
 */
class SchemaBuilderStructSpecs extends Specification implements CristalTestSetup {

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


    def 'Complex example using PatientDetails from Basic Tutorial'() {
        expect:
        SchemaTestBuilder.build('test', 'PatientDetails', 0) {
            struct(name: 'PatientDetails', documentation: 'This is the Schema for Basic Tutorial') {
                attribute(name: 'InsuranceNumber', type: 'string', default: '123456789ABC')
                field(name: 'DateOfBirth',     type: 'date', documentation: 'DateOfBirth docs')
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
                field(name: 'State',  type: 'string', values: states)
    
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
