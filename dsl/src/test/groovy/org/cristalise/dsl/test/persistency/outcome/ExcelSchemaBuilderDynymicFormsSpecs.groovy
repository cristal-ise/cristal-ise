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

import spock.lang.Ignore
import spock.lang.Specification


/**
 *
 */
class ExcelSchemaBuilderDynymicFormsSpecs extends Specification implements CristalTestSetup {

    def setup()   {}
    def cleanup() {}

    def xlsxFile = 'src/test/data/ExcelSchemaBuilderDynamicForms.xlsx'

    def 'Field can specify dynamicForms.mask'() {
        expect:
        SchemaTestBuilder.build('test', 'Mask', 0, xlsxFile)
        .compareXML(
            """<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                 <xs:element name="Mask">
                   <xs:complexType>
                     <xs:sequence>
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
                     </xs:sequence>
                   </xs:complexType>
                 </xs:element>
               </xs:schema>""")
    }

    //@Ignore('God knows what wrong with this test')
    def 'Field can specify dynamicForms.placeHolder'() {
        expect:
        SchemaTestBuilder.build('test', 'PlaceHolder', 0, xlsxFile)
        .compareXML(
            """<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                 <xs:element name="PlaceHolder">
                   <xs:complexType>
                     <xs:sequence>
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
                     </xs:sequence>
                   </xs:complexType>
                 </xs:element>
               </xs:schema>""")
    }

    def 'Field can specify dynamicForms.showSeconds and dynamicForms.hideOnDateTimeSelect'() {
        expect:
        SchemaTestBuilder.build('test', 'ShowSeconds', 0, xlsxFile)
        .compareXML(
            """<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                 <xs:element name="ShowSeconds">
                   <xs:complexType>
                     <xs:sequence>
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
                     </xs:sequence>
                   </xs:complexType>
                 </xs:element>
               </xs:schema>""")
    }

    def 'Field can specify dynamicForms.inputType using htmlAccept'() {
      expect: 
      SchemaTestBuilder.build('test', 'HtmlAccept', 0, xlsxFile)
      .compareXML(
        '''<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
          <xs:element name="HtmlAccept">
            <xs:complexType>
              <xs:sequence>
                <xs:element name='stringField' type='xs:string' minOccurs='1' maxOccurs='1'>
                  <xs:annotation>
                    <xs:appinfo>
                      <dynamicForms>
                        <inputType>file</inputType>
                        <accept>.xlsx, .xlsm, .xlsb, .xltx</accept>
                      </dynamicForms>
                    </xs:appinfo>
                  </xs:annotation>
                </xs:element>
              </xs:sequence>
            </xs:complexType>
          </xs:element>
        </xs:schema>'''
      )
    }

    def 'Field can specify dynamicForms.warning using pattern or expression with message'() {
        expect:
        SchemaTestBuilder.build('test', 'Warning', 0, xlsxFile)
        .compareXML(
            '''<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                 <xs:element name="Warning">
                   <xs:complexType>
                     <xs:sequence>
                       <xs:element name='decimalField' type='xs:decimal' minOccurs='1' maxOccurs='1'>
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
                       <xs:element name='dateField' type='xs:date' minOccurs='1' maxOccurs='1'>
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
                     </xs:sequence>
                   </xs:complexType>
                 </xs:element>
               </xs:schema>''')
    }

    def 'Field can specify dynamicForms.precision and dynamicForms.scale'() {
        expect:
        SchemaTestBuilder.build('test', 'PrecisionScale', 0, xlsxFile)
        .compareXML(
            """<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                 <xs:element name="PrecisionScale">
                   <xs:complexType>
                     <xs:sequence>
                       <xs:element name='decimalField' type='xs:decimal' minOccurs='1' maxOccurs='1'>
                         <xs:annotation>
                           <xs:appinfo>
                             <dynamicForms>
                               <precision>5-</precision>
                               <scale>2</scale>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                     </xs:sequence>
                   </xs:complexType>
                 </xs:element>
               </xs:schema>""")
    }
    
    def 'Field can specify dynamicForms grid properties'() {
        expect:
        SchemaTestBuilder.build('test', 'GridProperties', 0, xlsxFile)
        .compareXML(
            """<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                  <xs:element name='GridProperties'>
                    <xs:complexType>
                      <xs:sequence>
                        <xs:element name='stringField' type='xs:string' minOccurs='1' maxOccurs='1'>
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
                      </xs:sequence>
                    </xs:complexType>
                  </xs:element>
                </xs:schema>""")
    }
    
    def 'Field can specify a list of dynamicForms.updateFields'() {
        expect:
        SchemaTestBuilder.build('test', 'UpdateFields', 0, xlsxFile)
        .compareXML(
            '''<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                 <xs:element name="UpdateFields">
                   <xs:complexType>
                     <xs:sequence>
                       <xs:element name='decimalField' type='xs:decimal' minOccurs='1' maxOccurs='1'>
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
                     </xs:sequence>
                   </xs:complexType>
                 </xs:element>
               </xs:schema>''')
    }

    def 'Field can specify dynamicForms.autoComplete'() {
        expect:
        SchemaTestBuilder.build('test', 'AutoComplete', 0, xlsxFile)
        .compareXML(
            '''<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                 <xs:element name="AutoComplete">
                   <xs:complexType>
                     <xs:sequence>
                       <xs:element name='decimalField' type='xs:decimal' minOccurs='1' maxOccurs='1'>
                         <xs:annotation>
                           <xs:appinfo>
                             <dynamicForms>
                               <autoComplete>on</autoComplete>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                     </xs:sequence>
                   </xs:complexType>
                 </xs:element>
               </xs:schema>''')
	}

    def 'Field can specify arbitrary fields in dynamicForms.additional'() {
        expect:
        SchemaTestBuilder.build('test', 'Additional', 0, xlsxFile)
        .compareXML(
            '''<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                 <xs:element name="Additional">
                   <xs:complexType>
                     <xs:sequence>
                       <xs:element name='decimalField' type='xs:decimal' minOccurs='1' maxOccurs='1'>
                         <xs:annotation>
                           <xs:appinfo>
                             <dynamicForms>
                               <additional>
                                 <updloadForm>period</updloadForm>
                                 <selectionFilter>*.*</selectionFilter>
                               </additional>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                     </xs:sequence>
                   </xs:complexType>
                 </xs:element>
               </xs:schema>''')
    }

    def 'Field can specify dynamicForms.updateScriptRef using String'() {
        expect:
        SchemaTestBuilder.build('test', 'UpdateScriptRef', 0, xlsxFile)
        .compareXML(
            '''<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                 <xs:element name="UpdateScriptRef">
                   <xs:complexType>
                     <xs:sequence>
                       <xs:element name='decimalField' type='xs:decimal' minOccurs='1' maxOccurs='1'>
                         <xs:annotation>
                           <xs:appinfo>
                             <dynamicForms>
                               <additional>
                                 <updateScriptRef>Script:0</updateScriptRef>
                               </additional>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                     </xs:sequence>
                   </xs:complexType>
                 </xs:element>
               </xs:schema>''')
    }

    def 'Struct can specify dynamicForms properties in appInfo'() {
        expect:
        SchemaTestBuilder.build('test', 'StructDynamicForms', 0, xlsxFile)
        .compareXML(
            """<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                  <xs:element name="StructDynamicForms">
                    <xs:annotation>
                      <xs:appinfo>
                        <dynamicForms>
                          <width>100%</width>
                          <label>testLabel</label>
                          <container>ui-g-12</container>
                        </dynamicForms>
                      </xs:appinfo>
                    </xs:annotation>
                    <xs:complexType />
                  </xs:element>
                </xs:schema>""")
    }
}