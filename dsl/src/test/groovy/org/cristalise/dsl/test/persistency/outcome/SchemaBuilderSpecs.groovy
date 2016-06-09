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
                           <xs:complexType>
                             <xs:sequence />
                           </xs:complexType>
                         </xs:element>
                       </xs:schema>""")
    }


    def 'Building empty Structure with documentation adds xs:annotation to the xs:element'() {
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
                           <xs:complexType>
                             <xs:sequence />
                           </xs:complexType>
                         </xs:element>
                       </xs:schema>""")
    }


    def 'Default type is string for Fields'() {
        expect:
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData') { 
                field(name:'stringField')
            }
        }.compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                            <xs:element name='TestData'>
                                <xs:complexType>
                                    <xs:sequence>
                                        <xs:element name='stringField' type='xs:string' minOccurs='1' maxOccurs='1' />
                                    </xs:sequence>
                                </xs:complexType>
                            </xs:element>
                        </xs:schema>""")
    }


    def 'Field only accepts a number of types: string, boolean, integer, decimal, date, time, dateTime'() {
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
                                    <xs:sequence>
                                        <xs:element name='stringField'   type='xs:string'   minOccurs='1' maxOccurs='1' />
                                        <xs:element name='booleanField'  type='xs:boolean'  minOccurs='1' maxOccurs='1' />
                                        <xs:element name='integerField'  type='xs:integer'  minOccurs='1' maxOccurs='1' />
                                        <xs:element name='decimalField'  type='xs:decimal'  minOccurs='1' maxOccurs='1' />
                                        <xs:element name='dateField'     type='xs:date'     minOccurs='1' maxOccurs='1' />
                                        <xs:element name='timeField'     type='xs:time'     minOccurs='1' maxOccurs='1' />
                                        <xs:element name='dateTimeField' type='xs:dateTime' minOccurs='1' maxOccurs='1' />
                                    </xs:sequence>
                                </xs:complexType>
                            </xs:element>
                        </xs:schema>""")
    }


    def 'Unknown field type throws InvalidDataException'() {
        when: "Accepted types are ${org.cristalise.dsl.persistency.outcome.Field.types}"
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData') { 
                field(name: 'byteField', type: 'byte')
            }
        }

        then:
        thrown(InvalidDataException)
    }

    def 'Field can specify multiplicity'() {
        expect:
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData') {
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
                                        <xs:element name='many'        type='xs:string' minOccurs='0' />
                                        <xs:element name='fivehundred' type='xs:string' minOccurs='500' maxOccurs='500' />
                                        <xs:element name='zeroToMany'  type='xs:string' minOccurs='0' />
                                        <xs:element name='oneToFive'   type='xs:string' minOccurs='1' maxOccurs='5' />
                                        <xs:element name='reset'       type='xs:string' />
                                    </xs:sequence>
                                </xs:complexType>
                            </xs:element>
                        </xs:schema>""")
    }

    def 'Field can have a predefined set of values'() {
        expect:
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData') {
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


    def 'Field can have Unit which is added as attribute of type string'() {
        expect:
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData') {
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


    def 'Unit can specify the list of values it contains'() {
        expect:
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData') {
                field(name: 'Weight', type: 'decimal') { unit(values: ['g', 'kg']) }
            }
        }.compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                            <xs:element name='TestData'>
                                <xs:complexType>
                                    <xs:sequence>
                                        <xs:element name='Weight' minOccurs='1' maxOccurs='1'>
                                            <xs:complexType>
                                                <xs:simpleContent>
                                                    <xs:extension base='xs:decimal'>
                                                        <xs:attribute name='unit' use='required'>
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
                field(name: 'InsuranceNumber', type: 'string', default: '123456789ABC')
                field(name: 'DateOfBirth',     type: 'date')
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
                   <xs:sequence>
                     <xs:element minOccurs="1" maxOccurs="1" name="InsuranceNumber" type="xs:string" default= "123456789ABC"/>
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
                   </xs:sequence>
                 </xs:complexType>
               </xs:element>
             </xs:schema>""")
    }
}
