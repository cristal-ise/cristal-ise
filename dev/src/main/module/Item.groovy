import org.cristalise.kernel.collection.BuiltInCollections

Item(name: 'ScriptFactory', version: 0, folder: '/desc/dev', workflow: 'ScriptFactoryWf', workflowVer: 0) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('LocalObjectType': 'Script')
    Outcome($script_PropertyDescriptionList)
    Outcome(schema: 'Script', version: '0', viewname: 'last', path: 'boot/SC/New_0.xml')

    Dependency(BuiltInCollections.WORKFLOW) {
        Member(itemPath: '/desc/ActivityDesc/kernel/ManageScript') {
            Property('Version': 0)
        }
    }
    DependencyDescription(BuiltInCollections.INCLUDE) {}
}

Item(name: 'QueryFactory', version: 0, folder: '/desc/dev', workflow: 'QueryFactoryWf', workflowVer: 0) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('LocalObjectType': 'Query')
    Outcome($query_PropertyDescriptionList)
    Outcome(schema: 'Query', version: '0', viewname: 'last', path: 'boot/query/New_0.xml')

    Dependency(BuiltInCollections.WORKFLOW) {
        Member(itemPath: '/desc/ActivityDesc/kernel/ManageQuery') {
            Property('Version': 0)
        }
    }
}

Item(name: 'SchemaFactory', version: 0, folder: '/desc/dev', workflow: 'SchemaFactoryWf', workflowVer: 0) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('LocalObjectType': 'Schema')
    Outcome($schema_PropertyDescriptionList)
    Outcome(schema: 'Schema', version: '0', viewname: 'last', path: 'boot/OD/New_0.xsd')

    Dependency(BuiltInCollections.WORKFLOW) {
        Member(itemPath: '/desc/ActivityDesc/kernel/ManageSchema') {
            Property('Version': 0)
        }
    }
}

Item(name: 'StateMachineFactory', version: 0, folder: '/desc/dev', workflow: 'StateMachineFactoryWf', workflowVer: 0) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('LocalObjectType': 'StateMachine')
    Outcome($stateMachine_PropertyDescriptionList)

    Dependency(BuiltInCollections.WORKFLOW) {
        Member(itemPath: '/desc/ActivityDesc/kernel/ManageStateMachine') {
            Property('Version': 0)
        }
    }
}

Item(name: 'ElementaryActivityDefFactory', version: 0, folder: '/desc/dev', workflow: 'ElementaryActivityFactory', workflowVer: 0) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('LocalObjectType': 'ElementaryActivityDef')
    Outcome($elementaryActivityDesc_PropertyDescriptionList)

    Dependency(BuiltInCollections.WORKFLOW) {
        Member(itemPath: '/desc/ActivityDesc/kernel/ManageElementaryActDef') {
            Property('Version': 0)
        }
    }
    DependencyDescription(BuiltInCollections.SCHEMA) {
        Member(itemPath: '/desc/dev/SchemaFactory')
    }
    DependencyDescription(BuiltInCollections.SCRIPT) {
        Member(itemPath: '/desc/dev/ScriptFactory')
    }
    DependencyDescription(BuiltInCollections.QUERY) {
        Member(itemPath: '/desc/dev/QueryFactory')
    }
    DependencyDescription(BuiltInCollections.STATE_MACHINE) {
        Member(itemPath: '/desc/dev/StateMachineFactory')
    }
}

Item(name: 'ActivityDefType', version: 0, folder: '/desc/dev', workflow: 'NoWorkflow', workflowVer: 0) {
    InmutableProperty('Type': 'Type')
    Outcome($activityDesc_PropertyDescriptionList)
}

Item(name: 'CompositeActivityDefFactory', version: 0, folder: '/desc/dev', workflow: 'CompositeActivityFactory', workflowVer: 0) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('LocalObjectType': 'CompositeActivityDef')
    Outcome($compositeActivityDesc_PropertyDescriptionList)

    Dependency(BuiltInCollections.WORKFLOW) {
        Member(itemPath: '/desc/ActivityDesc/kernel/ManageCompositeActDef') {
            Property('Version': 0)
        }
    }
    DependencyDescription(BuiltInCollections.ACTIVITY) {
        Member(itemPath: '/desc/dev/ActivityDefType')
    }
    DependencyDescription(BuiltInCollections.SCHEMA) {
        Member(itemPath: '/desc/dev/SchemaFactory')
    }
    DependencyDescription(BuiltInCollections.SCRIPT) {
        Member(itemPath: '/desc/dev/ScriptFactory')
    }
    DependencyDescription(BuiltInCollections.QUERY) {
        Member(itemPath: '/desc/dev/QueryFactory')
    }
    DependencyDescription(BuiltInCollections.STATE_MACHINE) {
        Member(itemPath: '/desc/dev/StateMachineFactory')
    }
}

Item(name: 'DescriptionFactory', version: 0, folder: '/desc/dev', workflow: 'ItemDescriptionFactoryWf', workflowVer: 0) {
    InmutableProperty('Type': 'Factory')
    Outcome($itemDescription_PropertyDescriptionList)

    Dependency(BuiltInCollections.WORKFLOW) {
        Member(itemPath: '/desc/ActivityDesc/dev/ItemDescriptionWf') {
            Property('Version': 0)
        }
    }
    DependencyDescription(BuiltInCollections.WORKFLOW_PRIME) {
        Member(itemPath: '/desc/dev/CompositeActivityDefFactory')
    }
}

Item(name: 'ModuleFactory', version: 0, folder: '/desc/dev', workflow: 'ModuleFactory', workflowVer: 0) {
    InmutableProperty('Type': 'Factory')
    Outcome($module_PropertyDescriptionList)

    Dependency(BuiltInCollections.WORKFLOW) {
        Member(itemPath: '/desc/ActivityDesc/kernel/ManageModule') {
            Property('Version': 0)
        }
    }
    DependencyDescription(BuiltInCollections.CONTENTS) {}
}

Item(name: 'AgentFactory', version: 0, folder: '/desc/dev', workflow: 'AgentFactoryWf', workflowVer: 0) {
    InmutableProperty('Type': 'Factory')
    Outcome($agent_PropertyDescriptionList)

    Dependency(BuiltInCollections.WORKFLOW) {
        Member(itemPath: '/desc/ActivityDesc/kernel/ManageAgent') {
            Property('Version': 0)
        }
    }
}
