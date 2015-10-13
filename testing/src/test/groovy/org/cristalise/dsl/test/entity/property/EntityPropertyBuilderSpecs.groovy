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
package org.cristalise.dsl.test.entity.property

import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.test.CristalTestSetup

import spock.lang.Specification


/**
 *
 */
class EntityPropertyBuilderSpecs extends Specification implements CristalTestSetup {
    
    def setup()   { loggerSetup()    }
    def cleanup() { cristalCleanup() }

    def 'Mutable EntityProperty can be created from name only'() {
        when:
        def props = EntityPropertyTestBuilder.build {
            Property("Type")
        }

        then:
        props.list && props.list.size() == 1
        props.list[0].name == "Type"
        props.list[0].value == ""
        props.list[0].mutable == true
    }

    def 'Mutable EntityProperty can be created from name and value'() {
        when:
        def props = EntityPropertyTestBuilder.build {
            Property(Type: "patient")
        }

        then:
        props.list && props.list.size() == 1
        props.list[0].name == "Type"
        props.list[0].value == "patient"
        props.list[0].mutable == true
    }

    def 'Inmutable EntityProperty can be created from name and value'() {
        when:
        def props = EntityPropertyTestBuilder.build {
            InmutableProperty(Type: "patient")
        }

        then:
        props.list && props.list.size() == 1
        props.list[0].name == "Type"
        props.list[0].value == "patient"
        props.list[0].mutable == false
    }

    def 'Inmutable EntityProperty must have the value set'() {
        when:
        def props = EntityPropertyTestBuilder.build {
            InmutableProperty(Type: "")
        }

        then:
        InvalidDataException ex = thrown()
        ex.message == "IDL:org.cristalise.kernel/common/InvalidDataException:1.0  Inmutable EntityProperty 'Type' must have valid value"
    }

    def 'EntityProperty can only have String value'() {
        when:
        def props = EntityPropertyTestBuilder.build {
            Property(Type: new Object())
        }

        then:
        InvalidDataException ex = thrown()
        ex.message == "IDL:org.cristalise.kernel/common/InvalidDataException:1.0  EntityProperty 'Type' value must be String"
    }

    def 'EntityProperty Builder builds unlimited length of List keeping the order of declaration'() {
        when:
        def props = EntityPropertyTestBuilder.build {
            Property("Name")
            InmutableProperty(Type: "testing")
            Property('Date of Birth': 'today')
        }

        then:
        props.list && props.list.size() == 3

        props.list[0].name == "Name"
        props.list[0].value == ""
        props.list[0].mutable == true

        props.list[1].name == "Type"
        props.list[1].value == "testing"
        props.list[1].mutable == false

        props.list[2].name == "Date of Birth"
        props.list[2].value == "today"
        props.list[2].mutable == true
    }

    def 'The last accurance of the EntityProperty with same Name is kept'() {
        when:
        def props = EntityPropertyTestBuilder.build {
            Property("Type")
            Property("Zombi")
            Property(Type: "patient")
        }

        then:
        props.list && props.list.size() == 2

        props.list[0].name == "Zombi"
        props.list[0].value == ""
        props.list[0].mutable == true

        props.list[1].name == "Type"
        props.list[1].value == "patient"
        props.list[1].mutable == true
    }
}
