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
import org.cristalise.kernel.test.utils.CristalTestSetup
import org.cristalise.kernel.test.utils.KernelXMLUtility

import spock.lang.Specification


/**
 *
 */
class ExcelSchemaBuilderStructSpecs extends Specification implements CristalTestSetup {

    def setup()   { loggerSetup()    }
    def cleanup() { cristalCleanup() }

    def xlsxFile = "src/test/data/ExcelSchemaBuilderStruct.xlsx"

    def 'Building empty Structure with documentation adds annotation to the element'() {
        expect:
        SchemaTestBuilder.build('test', 'EmptyWithDoc', 0, xlsxFile)
        .compareXML("""<?xml version='1.0' encoding='utf-8'?>
                       <xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                         <xs:element name='EmptyWithDoc'>
                           <xs:annotation>
                             <xs:documentation>EmptyWithDoc documentation</xs:documentation>
                           </xs:annotation>
                           <xs:complexType/>
                         </xs:element>
                       </xs:schema>""")
    }

    def 'Define sequence of Fields which default type is string and multiplicity is 1'() {
        expect:
        SchemaTestBuilder.build('test', 'Stringfields-Seq', 0, xlsxFile)
        .compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                          <xs:element name='Stringfields-Seq'>
                            <xs:complexType>
                              <xs:sequence>
                                <xs:element name='stringField1' type='xs:string' minOccurs='1' maxOccurs='1' />
                                <xs:element name='stringField2' type='xs:string' minOccurs='1' maxOccurs='1' />
                              </xs:sequence>
                            </xs:complexType>
                          </xs:element>
                        </xs:schema>""")
    }

    def 'Define unordered set of Fields which default type is string and multiplicity is 1'() {
        expect:
        SchemaTestBuilder.build('test', 'Stringfields-All', 0, xlsxFile)
        .compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                          <xs:element name='Stringfields-All'>
                            <xs:complexType>
                              <xs:sequence>
                                <xs:element name='stringField1' type='xs:string' minOccurs='1' maxOccurs='1' />
                                <xs:element name='stringField2' type='xs:string' minOccurs='1' maxOccurs='1' />
                              </xs:sequence>
                            </xs:complexType>
                          </xs:element>
                        </xs:schema>""")
    }

    def 'Structure can define nested Structure with multiplicity'() {
        expect:
        SchemaTestBuilder.build('test', 'NestedStructure', 0, xlsxFile)
        .compareXML(
            """<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                  <xs:element name='NestedStructure'>
                    <xs:complexType>
                      <xs:sequence>
                        <xs:element name='stringField1' type='xs:string' minOccurs='1' maxOccurs='1' />
                        <xs:element name='SubStruct' minOccurs='0' maxOccurs='unbounded'>
                          <xs:complexType>
                            <xs:sequence>
                              <xs:element name='stringField11' type='xs:string' minOccurs='1' maxOccurs='1' />
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
        SchemaTestBuilder.build('test', 'StructureOrder', 0, xlsxFile)
        .compareXML("""<?xml version='1.0' encoding='utf-8'?>
                        <xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                          <xs:element name='StructureOrder'>
                            <xs:complexType>
                              <xs:sequence>
                                <xs:element name='stringField1' type='xs:string' minOccurs='1' maxOccurs='1' />
                                <xs:element name='SubStruct1'>
                                  <xs:complexType>
                                    <xs:sequence>
                                      <xs:element name='stringField11' type='xs:string' minOccurs='1' maxOccurs='1' />
                                    </xs:sequence>
                                  </xs:complexType>
                                </xs:element>
                                <xs:element name='stringField2' type='xs:string' minOccurs='1' maxOccurs='1' />
                                <xs:element name='SubStruct2'>
                                  <xs:complexType>
                                    <xs:sequence>
                                      <xs:element name='stringField21' type='xs:string' minOccurs='1' maxOccurs='1' />
                                    </xs:sequence>
                                  </xs:complexType>
                                </xs:element>
                                <xs:element name='stringField3' type='xs:string' minOccurs='1' maxOccurs='1' />
                              </xs:sequence>
                            </xs:complexType>
                          </xs:element>
                        </xs:schema>""")
    }

    def 'Structure can have a list of Attributes which default type is string'() {
        expect:
        SchemaTestBuilder.build('test', 'Attribute', 0, xlsxFile)
        .compareXML("""<?xml version='1.0' encoding='utf-8'?>
                       <xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                         <xs:element name='Attribute'>
                           <xs:complexType>
                             <xs:attribute name="stringAttr1" type="xs:string"/>
                             <xs:attribute name="stringAttr2" type="xs:string"/>
                           </xs:complexType>
                         </xs:element>
                       </xs:schema>""")
    }

    def 'Structure can define anyField with defaults minOccurs=0 and processContents=lax'() {
        expect:
        SchemaTestBuilder.build('test', 'AnyField', 0, xlsxFile)
        .compareXML("""<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                          <xs:element name='AnyField'>
                            <xs:complexType>
                              <xs:sequence>
                                <xs:element name='stringField1' type='xs:string' minOccurs='1' maxOccurs='1' />
                                <xs:any minOccurs='0' processContents='lax'/>
                                <xs:element name='stringField2' type='xs:string' minOccurs='1' maxOccurs='1' />
                              </xs:sequence>
                            </xs:complexType>
                          </xs:element>
                        </xs:schema>""")
    }

    def expectdPatientDetails = 
            """<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                 <xs:element name="PatientDetails">
                   <xs:annotation>
                     <xs:documentation>This is the Schema for Basic Tutorial</xs:documentation>
                     <xs:appinfo>
                       <dynamicForms>
                         <width>100%</width>
                       </dynamicForms>
                     </xs:appinfo>
                   </xs:annotation>
                   <xs:complexType>
                   <xs:sequence>
                     <xs:element name='FullName' type='xs:string' minOccurs='1' maxOccurs='1'>
                       <xs:annotation>
                         <xs:appinfo>
                           <dynamicForms>
                             <disabled>true</disabled>
                           </dynamicForms>
                         </xs:appinfo>
                       </xs:annotation>
                     </xs:element>
                     <xs:element name='DateOfBirth' type='xs:date' minOccurs='1' maxOccurs='1'>
                       <xs:annotation>
                         <xs:documentation>DateOfBirth docs</xs:documentation>
                       </xs:annotation>
                      </xs:element>
                      <xs:element minOccurs="1" maxOccurs="1" name="Gender">
                        <xs:annotation>
                          <xs:appinfo>
                            <dynamicForms>
                              <mask>999.99</mask>
                              <additional>
                                <updateScriptRef>Script:0</updateScriptRef>
                              </additional>
                            </dynamicForms>
                          </xs:appinfo>
                         </xs:annotation>
                         <xs:simpleType>
                           <xs:restriction base="xs:string">
                             <xs:enumeration value="male" />
                             <xs:enumeration value="female" />
                           </xs:restriction>
                         </xs:simpleType>
                       </xs:element>
                       <xs:element name='Weight' minOccurs='1' maxOccurs='1'>
                         <xs:annotation>
                           <xs:appinfo>
                             <dynamicForms>
                               <placeholder>999.99</placeholder>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
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
                     <xs:attribute name="InsuranceNumber" type="xs:string" default= "123456789ABC"/>
                   </xs:complexType>
                 </xs:element>
               </xs:schema>"""

    def 'Complex example using PatientDetails from Basic Tutorial'() {
        expect:
        SchemaTestBuilder.build('test', 'PatientDetails', 0, xlsxFile)
        .compareXML(expectdPatientDetails)
    }

    def 'Complex example using PatientDetails from Basic Tutorial - CSV'() {
        when:
        def actual = SchemaTestBuilder.build('test', 'PatientDetails', 0, "src/test/data/CsvSchemaBuilderStruct/PatientDetails.csv")

        then:
        KernelXMLUtility.compareXML(expectdPatientDetails, actual.schema.schemaData)
    }
}
