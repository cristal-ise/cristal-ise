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
package org.cristalise.dev.dsl

import groovy.transform.CompileStatic

import org.cristalise.dsl.lifecycle.instance.WorkflowBuilder
import org.cristalise.dsl.persistency.outcome.OutcomeBuilder;
import org.cristalise.dsl.property.PropertyDescriptionDelegate;
import org.cristalise.kernel.entity.imports.ImportItem
import org.cristalise.kernel.lifecycle.CompositeActivityDef;
import org.cristalise.kernel.lifecycle.instance.Workflow
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.property.PropertyDescriptionList

class DescriptionItemFactoryDelegate extends PropertyDescriptionDelegate {
    
    public String chooseWorkflowXML

    public void processClosure(Closure cl) {
        assert cl
        cl.delegate = this
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()
    }

    def Workflow(CompositeActivityDef caDef) {
        chooseWorkflowXML = OutcomeBuilder.build("ChooseWorkflow") {
            WorkflowDefinitionName(caDef.name)
            WorkflowDefinitionVersion(caDef.version)
        }
    }

    def Workflow(String name, String ver) {
        chooseWorkflowXML = OutcomeBuilder.build("ChooseWorkflow") {
            WorkflowDefinitionName(name)
            WorkflowDefinitionVersion(ver)
        }
    }
}
