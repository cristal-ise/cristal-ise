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
package org.cristalise.dsl.test.lifecycle.definition

import org.cristalise.dsl.lifecycle.definition.ElemActDefBuilder
import org.cristalise.kernel.test.utils.CristalTestSetup

import spock.lang.Specification


/**
 *
 */
class ElemActDefBuilderSpecs extends Specification implements CristalTestSetup {
    
    def defaultActProps = [StateMachineName: "Default", StateMachineVersion: "0", Breakpoint: false, Description: '', 'Agent Role': '', 'Agent Name': '', Viewpoint: '', OutcomeInit: '']

    def setup()   {}
    def cleanup() {}

    def 'ElemActDef has a set of default Properties'() {
        when:
        def eaDef = ElemActDefBuilder.build(module: 'test', name: 'EADef', version: 0) {}

        then:
        eaDef.name == 'EADef'
        eaDef.version == 0

        eaDef.getProperties() == defaultActProps
        eaDef.properties.getAbstract().size() == 0
    }

    def 'ElemActDef can change default Properties'() {
        when:
        def eaDef = ElemActDefBuilder.build(module: 'test', name: 'EADef', version: 0) {
            Property(SchemaType: 'dummy')
        }
        defaultActProps.SchemaType = 'dummy'

        then:
        eaDef.name == 'EADef'
        eaDef.version == 0

        eaDef.getProperties() == defaultActProps
        eaDef.properties.getAbstract().size() == 0
    }

    def 'ElemActDef can add new Properties - concrete or abstract'() {
        when:
        def eaDef = ElemActDefBuilder.build(module: 'test', name: 'EADef', version: 0) {
            Property(concreteProp: 'dummy')
            AbstractProperty(abstractProp: 'dummy')
        }
        defaultActProps.concreteProp = 'dummy'
        defaultActProps.abstractProp = 'dummy'

        then:
        eaDef.name == 'EADef'
        eaDef.version == 0

        eaDef.getProperties() == defaultActProps
        eaDef.properties.getAbstract() == ["abstractProp"]
    }
}
