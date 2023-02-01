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
import static org.cristalise.kernel.collection.BuiltInCollections.*
import static org.cristalise.kernel.process.resource.BuiltInResources.*


Schema('ChooseWorkflow', 0) {
    struct(name: 'ChooseWorkflow', useSequence: true) {
        field(name: 'WorkflowDefinitionName', type: 'string', documentation: 'Give the name of the composite activity description that you would like new instance of this description to run')
        field(name: 'WorkflowDefinitionVersion', type: 'string', documentation: 'Give the version of this activity that you would like to use.')
    }
}

Schema('NewCollection', 0) {
    struct(name: 'NewCollection', useSequence: true) {
        field(name: 'Name', type: 'string')
        field(name: 'Type', type: 'string', values: ['Dependency', 'Aggregation'])
    }
}

Script('CollDescCreator', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    input('agent', 'org.cristalise.kernel.entity.proxy.AgentProxy')
    input('job', 'org.cristalise.kernel.entity.Job')
    output('errors', 'org.cristalise.kernel.scripting.ErrorInfo')
    script('groovy', "src/main/module/script/CollDescCreator.groovy")
}

Script('SetWorkflow', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    input('agent', 'org.cristalise.kernel.entity.proxy.AgentProxy')
    input('job', 'org.cristalise.kernel.entity.Job')
    output('errors', 'org.cristalise.kernel.scripting.ErrorInfo')
    script('groovy', "src/main/module/script/SetWorkflow.groovy")
}

Script('InstantiateItem', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    input('agent', 'org.cristalise.kernel.entity.proxy.AgentProxy')
    input('job', 'org.cristalise.kernel.entity.Job')
    output('errors', 'org.cristalise.kernel.scripting.ErrorInfo')
    script('javascript', "src/main/module/script/InstantiateItem.js")
}

Activity('CreateItem', 0) {
    Property(Description: 'Create a new Item from its Description')

    Schema($newDevObjectDef_Schema)
    Script($instantiateItem_Script)
}

Activity('DefineNewCollection', 0) {
    Property(Description: '')

    Schema($newCollection_Schema)
    Script($collDescCreator_Script)
}

Activity('EditPropertyDescription', 0) {
    Property(Description: 'Set the initial properties for new instances.')

    Schema('PropertyDescription', 0)
}

Activity('SetInstanceWorkflow', 0) {
    Property(Description: 'Choose a CompositeActivityDefinition to use for the workflow of new instances')

    Schema($chooseWorkflow_Schema)
    Script($setWorkflow_Script)
}

Workflow('Description_Workflow', 0) {
    Layout {
        Act('SetPropertyDescription', $editPropertyDescription_ActivityDef)
        Act('SetInstanceWorkflow', $setInstanceWorkflow_ActivityDef)

        AndSplit {
            LoopInfinitive {
                Act('EditPropertyDescription', $editPropertyDescription_ActivityDef)
            }
            LoopInfinitive {
                Act('ChangeInstanceWorkflow', $setInstanceWorkflow_ActivityDef)
            }
            LoopInfinitive {
                Act('CreateNewInstance', $createItem_ActivityDef)
            }
            LoopInfinitive {
                Act('DefineNewCollection', $defineNewCollection_ActivityDef)
            }
        }
    }
}

Item(name: 'DescriptionFactory', version: 0, folder: $descDevContext_DomainContext, workflow: $crudFactory_Workflow_CompositeActivityDef) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('Root': $descDevContext_DomainContext)
//    InmutableProperty('SubFolder': '????')
//    InmutableProperty('UpdateSchema'????:0')

    Outcome($itemDescription_PropertyDescriptionList)

    Dependency(WORKFLOW) {
        Member($description_Workflow_CompositeActivityDef) {
            Property('Version': 0)
        }
    }
    DependencyDescription(WORKFLOW_PRIME) {
        Member($compositeActivityDesc_PropertyDescriptionList)
    }
}
