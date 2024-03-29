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
class ExcelSchemaBuilderListOfValuesSpecs extends Specification implements CristalTestSetup {

    def setup()   {}
    def cleanup() {}

    def xlsxFile = 'src/test/data/ExcelSchemaBuilderListOfValues.xlsx'

    def 'Field can specify listOfValues.scriptRef'() {
        expect:
        SchemaTestBuilder.build('test', 'ScriptRef', 0, xlsxFile)
        .compareXML(
            '''<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                 <xs:element name="ScriptRef">
                   <xs:complexType>
                     <xs:sequence>
                       <xs:element name='stringField' type='xs:string' minOccurs='1' maxOccurs='1'>
                         <xs:annotation>
                           <xs:appinfo>
                             <listOfValues>
                               <scriptRef>Script:0</scriptRef>
                             </listOfValues>
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
                     </xs:sequence>
                   </xs:complexType>
                 </xs:element>
               </xs:schema>''')
    }

    def 'Field can specify listOfValues.values'() {
        expect:
        SchemaTestBuilder.build('test', 'Values', 0, xlsxFile)
        .compareXML(
            '''<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                 <xs:element name="Values">
                   <xs:complexType>
                     <xs:sequence>
                       <xs:element name='stringField' type='xs:string' minOccurs='1' maxOccurs='1'>
                         <xs:annotation>
                           <xs:appinfo>
                             <listOfValues>
                               <values>v1,v2,v3</values>
                             </listOfValues>
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
                     </xs:sequence>
                   </xs:complexType>
                 </xs:element>
               </xs:schema>''')
    }
}