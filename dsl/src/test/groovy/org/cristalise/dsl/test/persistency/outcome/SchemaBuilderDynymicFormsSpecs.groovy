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
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.scripting.Script
import org.cristalise.kernel.test.utils.CristalTestSetup

import spock.lang.Specification


/**
 *
 */
class SchemaBuilderDynymicFormsSpecs extends Specification implements CristalTestSetup {

    def setup()   {}
    def cleanup() {}

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
                       <xs:element name='PredefinedSteps' type='xs:anyType'  minOccurs='0' maxOccurs='1'>
                         <xs:annotation>
                           <xs:appinfo>
                             <dynamicForms>
                               <hidden>true</hidden>
                               <required>false</required>
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
                       <xs:element name='PredefinedSteps' type='xs:anyType'  minOccurs='0' maxOccurs='1'>
                         <xs:annotation>
                           <xs:appinfo>
                             <dynamicForms>
                               <hidden>true</hidden>
                               <required>false</required>
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
                field(name: 'startOfShift',          type: 'time') {dynamicForms(showSeconds: true, hideOnDateTimeSelect: true)}
                field(name: 'startOfShiftNoSeconds', type: 'time')
                field(name: 'signatureTS',           type: 'dateTime') {dynamicForms(showSeconds: true, hideOnDateTimeSelect: true)}
                field(name: 'signatureTSNoSeconds',  type: 'dateTime')
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
                               <hideOnDateTimeSelect>true</hideOnDateTimeSelect>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                       <xs:element name='startOfShiftNoSeconds' type='xs:time' minOccurs='1' maxOccurs='1' />
                       <xs:element name='signatureTS' type='xs:dateTime' minOccurs='1' maxOccurs='1'>
                         <xs:annotation>
                           <xs:appinfo>
                             <dynamicForms>
                               <showSeconds>true</showSeconds>
                               <hideOnDateTimeSelect>true</hideOnDateTimeSelect>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                       <xs:element name='signatureTSNoSeconds' type='xs:dateTime' minOccurs='1' maxOccurs='1' />
                       <xs:element name='PredefinedSteps' type='xs:anyType'  minOccurs='0' maxOccurs='1'>
                         <xs:annotation>
                           <xs:appinfo>
                             <dynamicForms>
                               <hidden>true</hidden>
                               <required>false</required>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                     </xs:all>
                   </xs:complexType>
                 </xs:element>
               </xs:schema>""")
    }

    def 'Field can specify dynamicForms.accept using htmlAccept'() {
      expect: 
      SchemaTestBuilder.build('test', 'PatientDetails', 0) {
        struct(name: 'PatientDetails') {
          field(name: 'Record', type: 'string') {
            dynamicForms(
              inputType: 'file',
              htmlAccept: '.xlsx, .xlsm, .xlsb, .xltx, .xltm, .xls, .xlt, .xml, .xlam, .xla, .xlw, .xlr'
            )
          }
        }
      }.compareXML(
        '''<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
          <xs:element name="PatientDetails">
            <xs:complexType>
              <xs:all minOccurs="0">
                <xs:element name='Record' type='xs:string' minOccurs='1' maxOccurs='1'>
                  <xs:annotation>
                    <xs:appinfo>
                      <dynamicForms>
                      <inputType>file</inputType>
                      <accept>.xlsx, .xlsm, .xlsb, .xltx, .xltm, .xls, .xlt, .xml, .xlam, .xla, .xlw, .xlr</accept>
                    </dynamicForms>
                    </xs:appinfo>
                  </xs:annotation>
                </xs:element>
                       <xs:element name='PredefinedSteps' type='xs:anyType'  minOccurs='0' maxOccurs='1'>
                         <xs:annotation>
                           <xs:appinfo>
                             <dynamicForms>
                               <hidden>true</hidden>
                               <required>false</required>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
              </xs:all>
            </xs:complexType>
          </xs:element>
        </xs:schema>'''
      )
    }

    def 'Field can specify dynamicForms.warning using pattern or expression'() {
        expect:
        SchemaTestBuilder.build('test', 'PatientDetails', 0) {
            struct(name: 'PatientDetails') {
                field(name: 'Weight',      type: 'decimal') {
                    dynamicForms {
                        warning (pattern: '^[0-9]{1,4}$', message: 'Value out of specification, has to be <= 9999')
                    }
                }
                field(name: 'DateOfBirth', type: 'date') {
                    dynamicForms {
                        warning (expression: "var m = moment('2015-11-32', 'YYYY-MM-DD'); m.isValid();", message: 'Date is invalid')
                    }
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
                                 <warning>
                                   <pattern>^[0-9]{1,4}$</pattern>
                                   <message>Value out of specification, has to be &lt;= 9999</message>
                                 </warning>
                               </additional>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                       <xs:element name='DateOfBirth' type='xs:date' minOccurs='1' maxOccurs='1'>
                         <xs:annotation>
                           <xs:appinfo>
                             <dynamicForms>
                               <additional>
                                 <warning>
                                   <message>Date is invalid</message>
                                   <expression><![CDATA[ var m = moment('2015-11-32', 'YYYY-MM-DD'); m.isValid();]]></expression>
                                 </warning>
                               </additional>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                       <xs:element name='PredefinedSteps' type='xs:anyType'  minOccurs='0' maxOccurs='1'>
                         <xs:annotation>
                           <xs:appinfo>
                             <dynamicForms>
                               <hidden>true</hidden>
                               <required>false</required>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                     </xs:all>
                   </xs:complexType>
                 </xs:element>
               </xs:schema>''')
    }
    
    def 'Struct can specify dynamicForms properties in appInfo'() {
        expect:
        SchemaTestBuilder.build('test', 'FormAppInfo', 0) {
            struct(name: 'Form') {
                dynamicForms(width: '100%', label: 'testLabel', container: 'ui-g-12', hidden: true, required: false)
                field(name:'stringField1')
            }
        }.compareXML(
            """<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                  <xs:element name="Form">
                    <xs:annotation>
                      <xs:appinfo>
                        <dynamicForms>
                          <width>100%</width>
                          <label>testLabel</label>
                          <container>ui-g-12</container>
                          <hidden>true</hidden>
                          <required>false</required>
                        </dynamicForms>
                      </xs:appinfo>
                    </xs:annotation>
                    <xs:complexType>
                      <xs:all minOccurs='0'>
                        <xs:element name='stringField1' type='xs:string' minOccurs='1' maxOccurs='1' />
                        <xs:element name='PredefinedSteps' type='xs:anyType'  minOccurs='0' maxOccurs='1'>
                          <xs:annotation>
                            <xs:appinfo>
                              <dynamicForms>
                                <hidden>true</hidden>
                                <required>false</required>
                              </dynamicForms>
                            </xs:appinfo>
                          </xs:annotation>
                        </xs:element>
                      </xs:all>
                    </xs:complexType>
                  </xs:element>
                </xs:schema>""")
    }

    def 'Field can specify dynamicForms.precision and dynamicForms.scale'() {
        expect:
        SchemaTestBuilder.build('test', 'PatientDetails', 0) {
            struct(name: 'PatientDetails') {
                field(name: 'Weight', type: 'decimal') { dynamicForms(precision: '5-', scale: '2') }
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
                               <precision>5-</precision>
                               <scale>2</scale>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                       <xs:element name='PredefinedSteps' type='xs:anyType'  minOccurs='0' maxOccurs='1'>
                         <xs:annotation>
                           <xs:appinfo>
                             <dynamicForms>
                               <hidden>true</hidden>
                               <required>false</required>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                     </xs:all>
                   </xs:complexType>
                 </xs:element>
               </xs:schema>""")
    }
    
    def 'Field can specify dynamicForms grid properties'() {
        expect:
        SchemaTestBuilder.build('test', 'FormGrid', 0) {
            struct(name: 'testForm') {
                field(name:'testField', type: 'string', multiplicity: '0..1') {dynamicForms(container: 'ui-g-12', control: 'ui-g-10', labelGrid: 'ui-g-2')}
            }
        }.compareXML(
            """<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                  <xs:element name='testForm'>
                    <xs:complexType>
                      <xs:all minOccurs='0'>
                        <xs:element name='testField' type='xs:string' minOccurs='0' maxOccurs='1'>
                          <xs:annotation>
                            <xs:appinfo>
                              <dynamicForms>
                                <container>ui-g-12</container>
                                <control>ui-g-10</control>
                                <labelGrid>ui-g-2</labelGrid>
                              </dynamicForms>
                            </xs:appinfo>
                          </xs:annotation>
                        </xs:element>
                        <xs:element name='PredefinedSteps' type='xs:anyType'  minOccurs='0' maxOccurs='1'>
                          <xs:annotation>
                            <xs:appinfo>
                              <dynamicForms>
                                <hidden>true</hidden>
                                <required>false</required>
                              </dynamicForms>
                            </xs:appinfo>
                          </xs:annotation>
                        </xs:element>
                      </xs:all>
                    </xs:complexType>
                  </xs:element>
                </xs:schema>""")
    }
    
    def 'Field can specify a list of dynamicForms.updateFields'() {
        expect:
        SchemaTestBuilder.build('test', 'PatientDetails', 0) {
            struct(name: 'PatientDetails') {
                field(name: 'Weight',      type: 'decimal') {
                    dynamicForms(updateFields: ['Field1','Field2'])
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
                                  <updateFields>Field1,Field2</updateFields>
                                </additional>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                       <xs:element name='PredefinedSteps' type='xs:anyType'  minOccurs='0' maxOccurs='1'>
                         <xs:annotation>
                           <xs:appinfo>
                             <dynamicForms>
                               <hidden>true</hidden>
                               <required>false</required>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                     </xs:all>
                   </xs:complexType>
                 </xs:element>
               </xs:schema>''')
    }

    def 'Field can specify dynamicForms.autoComplete'() {
        expect:
        SchemaTestBuilder.build('test', 'PatientDetails', 0) {
            struct(name: 'PatientDetails') {
                field(name: 'Weight',      type: 'decimal') {
                    dynamicForms(autoComplete: 'on')
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
                               <autoComplete>on</autoComplete>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                       <xs:element name='PredefinedSteps' type='xs:anyType'  minOccurs='0' maxOccurs='1'>
                         <xs:annotation>
                           <xs:appinfo>
                             <dynamicForms>
                               <hidden>true</hidden>
                               <required>false</required>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                     </xs:all>
                   </xs:complexType>
                 </xs:element>
               </xs:schema>''')
	}

    def 'Field can specify dynamicForms.additional'() {
        expect:
        SchemaTestBuilder.build('test', 'PatientDetails', 0) {
            struct(name: 'PatientDetails') {
                field(name: 'Weight', type: 'decimal') {
                    dynamicForms(autoComplete: 'on') {
                        additional(editable: true, updateScriptRef: 'Script:0')
                    }
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
                               <autoComplete>on</autoComplete>
                               <additional>
                                 <editable>true</editable>
                                 <updateScriptRef>Script:0</updateScriptRef>
                               </additional>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                       <xs:element name='PredefinedSteps' type='xs:anyType'  minOccurs='0' maxOccurs='1'>
                         <xs:annotation>
                           <xs:appinfo>
                             <dynamicForms>
                               <hidden>true</hidden>
                               <required>false</required>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                     </xs:all>
                   </xs:complexType>
                 </xs:element>
               </xs:schema>''')
    }

    def 'Field can specify dynamicForms.updateScriptRef using String'() {
        expect:
        SchemaTestBuilder.build('test', 'PatientDetails', 0) {
            struct(name: 'PatientDetails') {
                field(name: 'Weight', type: 'decimal') {
                    dynamicForms(autoComplete: 'on', updateScriptRef: 'Script:0')
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
                               <autoComplete>on</autoComplete>
                               <additional>
                                 <updateScriptRef>Script:0</updateScriptRef>
                               </additional>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                       <xs:element name='PredefinedSteps' type='xs:anyType'  minOccurs='0' maxOccurs='1'>
                         <xs:annotation>
                           <xs:appinfo>
                             <dynamicForms>
                               <hidden>true</hidden>
                               <required>false</required>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                     </xs:all>
                   </xs:complexType>
                 </xs:element>
               </xs:schema>''')
    }

    def 'Field can specify dynamicForms.updateScriptRef using Script object'() {
        expect:
        def script = new Script("Script", 0, new ItemPath(), "<cristalscript><script language='javascript' name='Script'><![CDATA[;]]></script></cristalscript>", true);

        SchemaTestBuilder.build('test', 'PatientDetails', 0) {
            struct(name: 'PatientDetails') {
                field(name: 'Weight', type: 'decimal') {
                    dynamicForms(autoComplete: 'on', updateScriptRef: script)
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
                               <autoComplete>on</autoComplete>
                               <additional>
                                 <updateScriptRef>Script:0</updateScriptRef>
                               </additional>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                       <xs:element name='PredefinedSteps' type='xs:anyType'  minOccurs='0' maxOccurs='1'>
                         <xs:annotation>
                           <xs:appinfo>
                             <dynamicForms>
                               <hidden>true</hidden>
                               <required>false</required>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                     </xs:all>
                   </xs:complexType>
                 </xs:element>
               </xs:schema>''')
    }
}