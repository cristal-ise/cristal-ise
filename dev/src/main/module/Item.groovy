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

import org.cristalise.kernel.collection.BuiltInCollections

Item(name: 'ScriptFactory', version: 0, folder: SCRIPT_RESOURCE.typeRoot, workflow: $scriptFactoryWf_CompositeActivityDef) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('LocalObjectType': 'Script')

    Outcome($script_PropertyDescriptionList)
    Outcome(schema: 'Script', version: '0', viewname: 'last', path: 'boot/SC/New_0.xml')

    Dependency(WORKFLOW) {
        Member(itemPath: '/desc/ActivityDesc/kernel/ManageScript') {
            Property('Version': 0)
        }
    }
    DependencyDescription(INCLUDE) {}
}

Item(name: 'QueryFactory', version: 0, folder: QUERY_RESOURCE.typeRoot, workflow: $queryFactoryWf_CompositeActivityDef) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('LocalObjectType': 'Query')

    Outcome($query_PropertyDescriptionList)
    Outcome(schema: 'Query', version: '0', viewname: 'last', path: 'boot/query/New_0.xml')

    Dependency(WORKFLOW) {
        Member(itemPath: '/desc/ActivityDesc/kernel/ManageQuery') {
            Property('Version': 0)
        }
    }
}

Item(name: 'SchemaFactory', version: 0, folder: SCHEMA_RESOURCE.typeRoot, workflow: $schemaFactoryWf_CompositeActivityDef) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('LocalObjectType': 'Schema')

    Outcome($schema_PropertyDescriptionList)
    Outcome(schema: 'Schema', version: '0', viewname: 'last', path: 'boot/OD/New_0.xsd')

    Dependency(WORKFLOW) {
        Member(itemPath: '/desc/ActivityDesc/kernel/ManageSchema') {
            Property('Version': 0)
        }
    }
}

Item(name: 'StateMachineFactory', version: 0, folder: STATE_MACHINE_RESOURCE.typeRoot, workflow: $stateMachineFactoryWf_CompositeActivityDef) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('LocalObjectType': 'StateMachine')

    Outcome($stateMachine_PropertyDescriptionList)

    Dependency(WORKFLOW) {
        Member(itemPath: '/desc/ActivityDesc/kernel/ManageStateMachine') {
            Property('Version': 0)
        }
    }
}

Item(name: 'ElementaryActivityDefFactory', version: 0, folder: ELEM_ACT_DESC_RESOURCE.typeRoot, workflow: $elementaryActivityFactory_CompositeActivityDef) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('LocalObjectType': 'ElementaryActivityDef')

    Outcome($elementaryActivityDesc_PropertyDescriptionList)

    Dependency(WORKFLOW) {
        Member(itemPath: '/desc/ActivityDesc/kernel/ManageElementaryActDef') {
            Property('Version': 0)
        }
    }
    DependencyDescription(SCHEMA) {
        Member($schema_PropertyDescriptionList)
    }
    DependencyDescription(SCRIPT) {
        Member($script_PropertyDescriptionList)
    }
    DependencyDescription(QUERY) {
        Member($query_PropertyDescriptionList)
    }
    DependencyDescription(STATE_MACHINE) {
        Member($stateMachine_PropertyDescriptionList)
    }
}

Item(name: 'ActivityDefType', version: 0, folder: ACTIVITY_DESC_RESOURCE.typeRoot, workflow: 'NoWorkflow', workflowVer: 0) {
    InmutableProperty('Type': 'Type')
    Outcome($activityDesc_PropertyDescriptionList)
}

Item(name: 'CompositeActivityDefFactory', version: 0, folder: COMP_ACT_DESC_RESOURCE.typeRoot, workflow: $compositeActivityFactory_CompositeActivityDef) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('LocalObjectType': 'CompositeActivityDef')

    Outcome($compositeActivityDesc_PropertyDescriptionList)

    Dependency(WORKFLOW) {
        Member(itemPath: '/desc/ActivityDesc/kernel/ManageCompositeActDef') {
            Property('Version': 0)
        }
    }
    DependencyDescription(ACTIVITY) {
        Member($activityDesc_PropertyDescriptionList)
    }
    DependencyDescription(SCHEMA) {
        Member($schema_PropertyDescriptionList)
    }
    DependencyDescription(SCRIPT) {
        Member($script_PropertyDescriptionList)
    }
    DependencyDescription(QUERY) {
        Member($query_PropertyDescriptionList)
    }
    DependencyDescription(STATE_MACHINE) {
        Member($stateMachine_PropertyDescriptionList)
    }
}

Item(name: 'DescriptionFactory', version: 0, folder: $descDevContext_DomainContext, workflow: $itemDescriptionFactoryWf_CompositeActivityDef) {
    InmutableProperty('Type': 'Factory')

    Outcome($itemDescription_PropertyDescriptionList)

    Dependency(WORKFLOW) {
        Member($itemDescriptionWf_CompositeActivityDef) {
            Property('Version': 0)
        }
    }
    DependencyDescription(WORKFLOW_PRIME) {
        Member($compositeActivityDesc_PropertyDescriptionList)
    }
}

Item(name: 'ModuleFactory', version: 0, folder: MODULE_RESOURCE.typeRoot, workflow: $moduleFactory_CompositeActivityDef) {
    InmutableProperty('Type': 'Factory')

    Outcome($module_PropertyDescriptionList)

    Dependency(WORKFLOW) {
        Member(itemPath: '/desc/ActivityDesc/kernel/ManageModule') {
            Property('Version': 0)
        }
    }
    DependencyDescription(CONTENTS) {}
}

Item(name: 'AgentFactory', version: 0, folder: $descDevContext_DomainContext, workflow: $agentFactoryWf_CompositeActivityDef) {
    InmutableProperty('Type': 'Factory')

    Outcome($agent_PropertyDescriptionList)

    Dependency(WORKFLOW) {
        Member(itemPath: '/desc/ActivityDesc/kernel/ManageAgent') {
            Property('Version': 0)
        }
    }
}

Item(name: 'PropertyDescriptionFactory', version: 0, folder: PROPERTY_DESC_RESOURCE.typeRoot, workflow: $propertyDescriptionFactoryWf_CompositeActivityDef) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('LocalObjectType': 'PropertyDescription')

    Outcome($propertyDescription_PropertyDescriptionList)

    Dependency(WORKFLOW) {
        Member(itemPath: '/desc/ActivityDesc/kernel/ManagePropertyDesc') {
            Property('Version': 0)
        }
    }
}

Item(name: 'AgentDescFactory', version: 0, folder: AGENT_DESC_RESOURCE.typeRoot, workflow: $agentDescFactoryWf_CompositeActivityDef) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('LocalObjectType': 'AgentDesc')

    Outcome($agentDesc_PropertyDescriptionList)

    Dependency(WORKFLOW) {
        Member(itemPath: '/desc/ActivityDesc/kernel/ManageAgentDesc') {
            Property('Version': 0)
        }
    }
}

Item(name: 'ItemDescFactory', version: 0, folder: ITEM_DESC_RESOURCE.typeRoot, workflow: $itemDescFactoryWf_CompositeActivityDef) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('LocalObjectType': 'ItemDesc')

    Outcome($itemDesc_PropertyDescriptionList)

    Dependency(WORKFLOW) {
        Member(itemPath: '/desc/ActivityDesc/kernel/ManageItemDesc') {
            Property('Version': 0)
        }
    }
}

Item(name: 'RoleDescFactory', version: 0, folder: ROLE_DESC_RESOURCE.typeRoot, workflow: $roleDescFactoryWf_CompositeActivityDef) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('LocalObjectType': 'RoleDesc')

    Outcome($roleDesc_PropertyDescriptionList)

    Dependency(WORKFLOW) {
        Member(itemPath: '/desc/ActivityDesc/kernel/ManageRoleDesc') {
            Property('Version': 0)
        }
    }
}

Item(name: 'DomainContextFactory', version: 0, folder: DOMAIN_CONTEXT_RESOURCE.typeRoot, workflow: $crudFactory_Workflow_CompositeActivityDef) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('Root': '/desc/DomainContext')
    InmutableProperty('SubFolder': 'dev')
    InmutableProperty('UpdateSchema': 'DomainContext:0')

    Outcome(schema: 'PropertyDescription', version: '0', viewname: 'last', path: 'boot/property/DomainContext_0.xml')

    Dependency(WORKFLOW) {
        Member(itemPath: '/desc/ActivityDesc/kernel/ManageDomainContext') {
            Property('Version': 0)
        }
    }

    Dependency(MASTER_SCHEMA) {
        Member(itemPath: '/desc/Schema/kernel/DomainContext') {
            Property('Version': 0)
        }
    }

//    Dependency(AGGREGATE_SCRIPT) {
//        Member(itemPath: $release_Aggregate_Script) {
//            Property('Version': 0)
//        }
//    }
}
