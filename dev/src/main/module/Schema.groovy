Schema('ChooseWorkflow', 0) {
    struct(name: 'ChooseWorkflow', useSequence: true) {
        field(name: 'WorkflowDefinitionName', type: 'string', documentation: 'Give the name of the composite activity description that you would like new instance of this description to run')
        field(name: 'WorkflowDefinitionVersion', type: 'string', documentation: 'Give the version of this activity that you would like to use.')
    }
}

Schema('New', 0, new File('src/main/resources/boot/OD/New_0.xsd'))

Schema('NewAgent', 0) {
    struct(name: 'NewAgent', useSequence: true) {
        field(name: 'Name', type: 'string', documentation: 'Please give a name for your new Agent.')
        field(name: 'SubFolder', type: 'string', documentation: 'If you want to store your object in a subfolder, give the subpath here.')
        field(name: 'InitialRoles', type: 'string', documentation: 'Comma separated list of Roles.')
        field(name: 'Password', type: 'string', multiplicity: '0..1', documentation: 'Initial password (optional).')
    }
}

Schema('NewCollection', 0) {
    struct(name: 'NewCollection', useSequence: true) {
        field(name: 'Name', type: 'string')
        field(name: 'Type', type: 'string', values: ['Dependency', 'Aggregation'])
    }
}

Schema('NewDevObjectDef', 0) {
    struct(name: 'NewDevObjectDef', useSequence: true) {
        field(name: 'ObjectName', type: 'string', documentation: 'Please give a name for your new object.')
        field(name: 'SubFolder',  type: 'string', documentation: 'If you want to store your object in a subfolder, give the subpath here.')
    }
}
