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

import org.cristalise.dsl.property.PropertyDescriptionBuilder
import org.cristalise.dsl.test.builders.SchemaTestBuilder
import org.cristalise.kernel.test.utils.CristalTestSetup
import org.cristalise.kernel.test.utils.KernelXMLUtility

import spock.lang.Specification


/**
 *
 */
class SchemaBuilderModelSpecs extends Specification implements CristalTestSetup {

    def setup()   { loggerSetup()    }
    def cleanup() { cristalCleanup() }


    def 'Field can specify the item type it references using String'() {
        expect:
        SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData', useSequence: true) {
                field(name: 'ItemRef') { model(referencedItemType: 'UnitTest') }
            }
        }.compareXML(
            """<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                 <xs:element name='TestData'>
                   <xs:complexType>
                     <xs:sequence>
                       <xs:element name='ItemRef' type='xs:string' minOccurs='1' maxOccurs='1'>
                         <xs:annotation>
                           <xs:appinfo>
                             <model>
                               <referencedItemType>UnitTest</referencedItemType>
                             </model>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                     </xs:sequence>
                   </xs:complexType>
                 </xs:element>
               </xs:schema>""")
    }

    def 'Field can specify the item type it references using PropertyDescriptionList'() {
        when:
        def unitTestPropDesc = PropertyDescriptionBuilder.build {
            PropertyDesc(name: "Type", defaultValue: "UnitTest", isMutable: false, isClassIdentifier: true)
        }

        def schema = SchemaTestBuilder.build('Test', 'TestData', 0) {
            struct(name: 'TestData', useSequence: true) {
                field(name: 'ItemRef') { model(referencedItemType: unitTestPropDesc) }
            }
        }

        then:
        KernelXMLUtility.compareXML(
            """<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                 <xs:element name='TestData'>
                   <xs:complexType>
                     <xs:sequence>
                       <xs:element name='ItemRef' type='xs:string' minOccurs='1' maxOccurs='1'>
                         <xs:annotation>
                           <xs:appinfo>
                             <model>
                               <referencedItemType>UnitTest</referencedItemType>
                             </model>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                     </xs:sequence>
                   </xs:complexType>
                 </xs:element>
               </xs:schema>""",
            schema.getSchema().getXSD()
            )
    }
}