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
package org.cristalise.dsl.test.entity

import org.cristalise.dsl.entity.DomainContextBuilder
import org.cristalise.kernel.test.utils.CristalTestSetup
import spock.lang.Specification

class DomainContextBuilderSpecs extends Specification implements CristalTestSetup {
    
    def setup()   {}
    def cleanup() {}

    def "Build a list of DomainContext with namespace"() {
        when:
        def dcList = DomainContextBuilder.build('ttt') {
            DomainContext('/desc/PropertyDesc/ttt')
            DomainContext('/ttt/Doctor', 2)
        }

        then:
        dcList[0].name == "DescPropertyDescTttContext"
        dcList[0].namespace == "ttt"
        dcList[0].version == 0

        dcList[1].name == "TttDoctorContext"
        dcList[1].namespace == "ttt"
        dcList[1].version == 2
    }
}
