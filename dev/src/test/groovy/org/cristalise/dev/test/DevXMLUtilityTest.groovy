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
