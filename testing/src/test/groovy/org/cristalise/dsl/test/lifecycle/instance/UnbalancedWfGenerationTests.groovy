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
package org.cristalise.dsl.test.lifecycle.instance;

import static org.junit.Assert.*

import org.cristalise.test.CristalTestSetup
import org.junit.After
import org.junit.Before
import org.junit.Test


class UnbalancedWfGenerationTests implements CristalTestSetup {

    WorkflowTestBuilder wfBuilder

    @Before
    public void setup() {
        inMemorySetup()
        wfBuilder = new WorkflowTestBuilder()
    }

    @After
    public void cleanup() {
        //println Gateway.getMarshaller().marshall(wfBuilder.wf)
        cristalCleanup()
    }


    @Test
    public void 'Unbalanced first-second-third-last'() {
        wfBuilder.build {
            connect ElemAct: 'first' to ElemAct: 'second' to ElemAct: 'third' to ElemAct: 'last'
            setFirst('first')
        }

        assert wfBuilder.verify()

        wfBuilder.checkSequence('first', 'second', 'third', 'last')
    }

}
