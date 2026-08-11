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
package org.cristalise.dsl.test.property

import org.cristalise.dsl.property.PropertyDescriptionBuilder
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.property.PropertyDescriptionList
import org.cristalise.kernel.test.utils.CristalTestSetup

import spock.lang.Specification


/**
 *
 */
class PropertyDescriptionBuilderSpecs extends Specification implements CristalTestSetup {
    
    def setup()   {}
    def cleanup() {}

    def 'PropertyDescription can be created specifying the name only, creates a mutable Property'() {
        when:
        PropertyDescriptionList propDescList = PropertyDescriptionBuilder.build { 
            PropertyDesc("Name")
        }

        then:
        propDescList
        propDescList.list.size() == 1
        propDescList.list[0]
        propDescList.list[0].name == "Name"
        propDescList.list[0].isMutable
        propDescList.list[0].isClassIdentifier == false
        propDescList.list[0].isTransitive() == false
        propDescList.list[0].defaultValue == null
    }

    def 'Create list of PropertyDescriptions using the full specification'() {
        when:
        PropertyDescriptionList propDescList = PropertyDescriptionBuilder.build { 
            PropertyDesc("Name")
            PropertyDesc(name: "Type", defaultValue: "ElemActDesc", isMutable: false, isClassIdentifier: false, isTransitive: true)
        }

        then:
        propDescList
        propDescList.list.size() == 2
        propDescList.list[0].name == "Name"
        propDescList.list[1].name == "Type"
        propDescList.list[1].isMutable == false
        propDescList.list[1].isClassIdentifier == false
        propDescList.list[1].transitive == true
        propDescList.list[1].defaultValue == "ElemActDesc"
    }

    def 'PropertyDescription defaultValue must be String type, throws InvalidDataException'() {
        when:
        PropertyDescriptionList propDescList = PropertyDescriptionBuilder.build { 
            PropertyDesc(name: "Multiplicity", defaultValue: 1000)
        }

        then:
        thrown(InvalidDataException)
    }
}
