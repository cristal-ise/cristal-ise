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

import static org.cristalise.dev.scaffold.CRUDItemCreator.UpdateMode.ERASE
import org.cristalise.dev.dsl.DevItemDSL
import org.cristalise.dev.scaffold.DevItemCreator
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.utils.CristalTestSetup
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

@CompileStatic
@TestInstance(Lifecycle.PER_CLASS)
class BasicDescriptionTests extends DevItemDSL implements CristalTestSetup {

    Properties props = new Properties()
    String folder = "devtest"

    @BeforeAll
    public void setup() {
        props.put('Resource.moduleUseFileNameWithVersion', 'dev,devtest')

        inMemoryServer(props)

        agent = Gateway.getAgentProxy('devtest')
        creator = new DevItemCreator(folder, ERASE, agent)
    }

    @CompileDynamic
    private ItemProxy setupPatient() {
        def schema = Schema('PatientDetails', folder) {
            struct(name: 'PatientDetails') {
                attribute(name: 'InsuranceNumber', type: 'string', default: '123456789ABC')
                field(name: 'DateOfBirth', type: 'date')
                field(name: 'Gender',      type: 'string', values: ['male', 'female'])
                field(name: 'Weight',      type: 'decimal') { unit(values: ['g', 'kg'], default: 'kg') }
            }
        }

        def ea = ElementaryActivityDef('Patient_Update', folder) {
            Property(OutcomeInit: "Empty")
            Schema(schema)
        }

        def wf = CompositeActivityDef('Patient_Workflow', folder) {
            Layout {
                Act('Update',  ea)
            }
        }

        return DescriptionItem('PatientDescription', folder) {
            PropertyDesc(name: 'Type', defaultValue: 'Patient', isMutable: false, isClassIdentifier: true)
            Workflow(wf)
        }
    }

    @Test @Disabled('LocalObjectLoader does not work with current in-memory persistency')
    void test() {
        assert setupPatient()
    }

}
