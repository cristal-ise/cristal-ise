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
class SchemaBuilderDynymicFormsSpecs extends Specification implements CristalTestSetup {

    def setup()   { loggerSetup()    }
    def cleanup() { cristalCleanup() }

    def 'Field can specify dynamicForms.mask'() {
        expect:
        SchemaTestBuilder.build('test', 'PatientDetails', 0) {
            struct(name: 'PatientDetails') {
                field(name: 'Weight',      type: 'decimal') {dynamicForms(mask : '999.99')}
                field(name: 'DateOfBirth', type: 'date')    {dynamicForms(mask : '99/99/9999')}
            }
        }.compareXML(
            """<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                 <xs:element name="PatientDetails">
                   <xs:complexType>
                     <xs:all minOccurs="0">
                       <xs:element name='Weight' type='xs:decimal' minOccurs='1' maxOccurs='1'>
                         <xs:annotation>
                           <xs:appinfo>
                             <dynamicForms>
                               <mask>999.99</mask>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                       <xs:element name='DateOfBirth' type='xs:date' minOccurs='1' maxOccurs='1'>
                         <xs:annotation>
                           <xs:appinfo>
                             <dynamicForms>
                               <mask>99/99/9999</mask>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                     </xs:all>
                   </xs:complexType>
                 </xs:element>
               </xs:schema>""")
    }
    
    def 'Field can specify dynamicForms.placeHolder'() {
        expect:
        SchemaTestBuilder.build('test', 'PatientDetails', 0) {
            struct(name: 'PatientDetails') {
                field(name: 'Weight',      type: 'decimal') {dynamicForms(placeholder : '999.99')}
                field(name: 'DateOfBirth', type: 'date')    {dynamicForms(placeholder : '99/99/9999')}
            }
        }.compareXML(
            """<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                 <xs:element name="PatientDetails">
                   <xs:complexType>
                     <xs:all minOccurs="0">
                       <xs:element name='Weight' type='xs:decimal' minOccurs='1' maxOccurs='1'>
                         <xs:annotation>
                           <xs:appinfo>
                             <dynamicForms>
                               <placeholder>999.99</placeholder>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                       <xs:element name='DateOfBirth' type='xs:date' minOccurs='1' maxOccurs='1'>
                         <xs:annotation>
                           <xs:appinfo>
                             <dynamicForms>
                               <placeholder>99/99/9999</placeholder>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                     </xs:all>
                   </xs:complexType>
                 </xs:element>
               </xs:schema>""")
    }

    def 'Field can specify dynamicForms.showSeconds'() {
        expect:
        SchemaTestBuilder.build('test', 'Employee', 0) {
            struct(name: 'Employee') {
                field(name: 'startOfShift',         type: 'time') {dynamicForms(showSeconds : true)}
                field(name: 'startOfShiftNoSecods', type: 'time')
                field(name: 'signatureTS',          type: 'dateTime') {dynamicForms(showSeconds : true)}
                field(name: 'signatureTSNoSeconds', type: 'dateTime')
            }
        }.compareXML(
            """<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                 <xs:element name="Employee">
                   <xs:complexType>
                     <xs:all minOccurs="0">
                       <xs:element name='startOfShift' type='xs:time' minOccurs='1' maxOccurs='1'>
                         <xs:annotation>
                           <xs:appinfo>
                             <dynamicForms>
                               <showSeconds>true</showSeconds>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                       <xs:element name='startOfShiftNoSecods' type='xs:time' minOccurs='1' maxOccurs='1' />
                       <xs:element name='signatureTS' type='xs:dateTime' minOccurs='1' maxOccurs='1'>
                         <xs:annotation>
                           <xs:appinfo>
                             <dynamicForms>
                               <showSeconds>true</showSeconds>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                       <xs:element name='signatureTSNoSeconds' type='xs:dateTime' minOccurs='1' maxOccurs='1' />
                     </xs:all>
                   </xs:complexType>
                 </xs:element>
               </xs:schema>""")
    }

    def 'Field can specify dynamicForms.outOfSpecs'() {
        expect:
        SchemaTestBuilder.build('test', 'PatientDetails', 0) {
            struct(name: 'PatientDetails') {
                field(name: 'Weight',      type: 'decimal') {
                    dynamicForms {
                        outOfSpecs (pattern: '^[0-9]{1,4}$', message: 'Value out of specification, has to be <= 9999')
                    }
                }
                field(name: 'DateOfBirth', type: 'date') {
                    dynamicForms(placeholder : '99/99/9999')
                }
            }
        }.compareXML(
            '''<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                 <xs:element name="PatientDetails">
                   <xs:complexType>
                     <xs:all minOccurs="0">
                       <xs:element name='Weight' type='xs:decimal' minOccurs='1' maxOccurs='1'>
                         <xs:annotation>
                           <xs:appinfo>
                             <dynamicForms>
                               <additional>
                                 <outOfSpecs>
                                   <pattern>^[0-9]{1,4}$</pattern>
                                   <message>Value out of specification, has to be &lt;= 9999</message>
                                 </outOfSpecs>
                               </additional>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                       <xs:element name='DateOfBirth' type='xs:date' minOccurs='1' maxOccurs='1'>
                         <xs:annotation>
                           <xs:appinfo>
                             <dynamicForms>
                               <placeholder>99/99/9999</placeholder>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                     </xs:all>
                   </xs:complexType>
                 </xs:element>
               </xs:schema>''')
    }
    
    def 'Struct can specify dynamicForms.width'() {
        expect:
        SchemaTestBuilder.build('test', 'Form', 0) {
            struct(name: 'Form') {
                dynamicForms(width: '100%')
                field(name:'stringField1')
            }
        }.compareXML(
            """<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                  <xs:element name="Form">
                    <xs:annotation>
                      <xs:appinfo>
                        <dynamicForms>
                          <width>100%</width>
                        </dynamicForms>
                      </xs:appinfo>
                    </xs:annotation>
                    <xs:complexType>
                      <xs:all minOccurs='0'>
                        <xs:element name='stringField1' type='xs:string' minOccurs='1' maxOccurs='1' />
                      </xs:all>
                    </xs:complexType>
                  </xs:element>
                </xs:schema>""")
    }

    def 'Field can specify dynamicForms.precision and dynamicForms.scale'() {
        expect:
        SchemaTestBuilder.build('test', 'PatientDetails', 0) {
            struct(name: 'PatientDetails') {
                field(name: 'Weight', type: 'decimal') { dynamicForms(precision: '5', scale: '2') }
            }
        }.compareXML(
            """<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                 <xs:element name="PatientDetails">
                   <xs:complexType>
                     <xs:all minOccurs="0">
                       <xs:element name='Weight' type='xs:decimal' minOccurs='1' maxOccurs='1'>
                         <xs:annotation>
                           <xs:appinfo>
                             <dynamicForms>
                               <precision>5</precision>
                               <scale>2</scale>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                     </xs:all>
                   </xs:complexType>
                 </xs:element>
               </xs:schema>""")
    }
}
