/**
 * This file is part of the CRISTAL-iSE Development Module.
 * Copyright (c) 2001-2017 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.dev.test

import static org.junit.Assert.*

import org.cristalise.dev.dsl.DevXMLUtility
import org.cristalise.kernel.test.utils.KernelXMLUtility
import org.junit.Test

import groovy.transform.CompileStatic

@CompileStatic
class DevXMLUtilityTest {

    @Test
    public void 'Convert Map containig Maps and Lists to XML'() {
        def actual = DevXMLUtility.recordToXML(
            'Batch', [
                Name: 'ID-12345',
                ParentBatch: null,
                Status: [Quality: 'Producion', State: 'Verified'],
                SignedBy: 'User12',
                Workorders: ['WO-12', 'WO-53', null],
                Imported: true,
                ShippmentPerCountry: [
                    [Country: 'Switzerland', boxes: 100],
                    [Country: 'France', boxes: 22],
                    [Country: 'US', containers: [[Type: 'Large', boxes: 111], [Type: 'Medium', boxes: 36]]],
                ],
            ]
        )

        def expected ='''
               <Batch>
                 <Name>ID-12345</Name>
                 <ParentBatch />
                 <Status>
                   <Quality>Producion</Quality>
                   <State>Verified</State>
                 </Status>
                 <SignedBy>User12</SignedBy>
                 <Workorders>WO-12</Workorders>
                 <Workorders>WO-53</Workorders>
                 <Workorders />
                 <Imported>true</Imported>
                 <ShippmentPerCountry>
                   <Country>Switzerland</Country>
                   <boxes>100</boxes>
                 </ShippmentPerCountry>
                 <ShippmentPerCountry>
                   <Country>France</Country>
                   <boxes>22</boxes>
                 </ShippmentPerCountry>
                 <ShippmentPerCountry>
                   <Country>US</Country>
                   <containers>
                     <Type>Large</Type>
                     <boxes>111</boxes>
                   </containers>
                   <containers>
                     <Type>Medium</Type>
                     <boxes>36</boxes>
                   </containers>
                 </ShippmentPerCountry>
               </Batch>'''

        assert KernelXMLUtility.compareXML(expected, actual)
    }

}
