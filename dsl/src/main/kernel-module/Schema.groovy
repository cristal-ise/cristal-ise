Schema('SimpleElectonicSignature', 0) {
    struct(name: 'SimpleElectonicSignature', documentation: 'Minimum form to provide electronic signature') {
        field(name:'AgentName', type: 'string')
        field(name:'Password',  type: 'string') { dynamicForms(inputType: 'password') }

        struct(name: 'ExecutionContext', documentation: 'The context of Item and Actitiy of the Electronic Signature', multiplicity: '1..1') {
            field(name:'ItemPath',     type: 'string') { dynamicForms(hidden: true) }
            field(name:'SchemaName',   type: 'string') { dynamicForms(hidden: true) }
            field(name:'SchemaVersion',type: 'string') { dynamicForms(hidden: true) }
            field(name:'ActivityType', type: 'string') { dynamicForms(hidden: true) }
            field(name:'ActivityName', type: 'string') { dynamicForms(hidden: true) }
            field(name:'StepPath',     type: 'string') { dynamicForms(hidden: true) }
        }
    }
}

Schema('SystemProperties', 0) {
    struct(name: 'SystemProperties', useSequence: true) {
        field(name: 'ProcessName', type: 'string')  {
            dynamicForms(disabled: true)
        }
        struct(name:'Property', multiplicity: '0..*') {
            field(name: 'Name', type: 'string')  {
                dynamicForms(disabled: true)
            }
            field(name: 'Module', type: 'string', multiplicity: '0..1', documentation: 'The module in which the System Property was defined') {
                dynamicForms(disabled: true)
            }
            field(name: 'ReadOnly', type: 'boolean', multiplicity: '0..1', documentation: 'Specify if the Property can be dynamically overridden') {
                dynamicForms(disabled: true)
            }
            field(name: 'Description', type: 'string', multiplicity: '0..1') {
                dynamicForms(disabled: true)
            }
            field(name: 'SetInConfigFiles', type: 'boolean', documentation: 'Indicates if the value was set in config files') {
                dynamicForms(disabled: true)
            }
            field(name: 'Value', type: 'string')
        }
    }
}

def level = ['OFF', 'ERROR', 'WARN', 'INFO', 'DEBUG', 'TRACE', 'ALL']

Schema('LoggerConfig', 0) {
    struct(name: 'LoggerConfig', useSequence: true) {
        field(name:'Root', type: 'string', values: level, multiplicity: '0..1')
        struct(name: 'Logger', multiplicity: '0..*', useSequence: true) {
            field(name:'Name', type: 'string')
            field(name:'Level', type: 'string', values: level)
        }
    }
}

Schema('ModuleChanges', 0) {
    struct(name: 'ModuleChanges', useSequence: true) {
        struct(name: 'ResourceChangeDetails', useSequence: true, multiplicity: '0..*') {
            field(name:'ResourceName', type: 'string')
            field(name:'ResourceVersion', type: 'string')
            field(name:'SchemaName', type: 'string')
            field(name:'ChangeType', type: 'string', values: ['IDENTICAL', 'NEW', 'UPDATED', 'OVERWRITTEN', 'SKIPPED', 'REMOVED'])
        }
    }
}

Schema('RoleDesc', 0) {
    struct(name: 'RoleDesc', useSequence: true) {
        field(name: 'Name', type: 'string',  multiplicity: '1..1')
        field(name: 'Version', type: 'integer', multiplicity: '0..1')
        field(name: 'Id', type: 'string',  multiplicity: '0..1', pattern: patternUuid, length: 36) {
            dynamicForms(disabled: true)
        }
        field(name: 'JobList', type: 'boolean', default: 'false', multiplicity: '1..1')

        struct(name:'Permission', multiplicity: '0..*', useSequence: true) {
            field(name: 'Domains', type: 'string', default: '*',  multiplicity: '1..1') {
                listOfValues(scriptRef: 'QueryPropertyDescriptions:0') //cannot use * 
            }
            field(name: 'Actions', type: 'string', default: '*', multiplicity: '1..1') {
                listOfValues(scriptRef: 'QueryActivityNames:0') //collects all ActNames from all Types selected for Domains
                dynamicForms(multiple: true)
            }
            field(name: 'Targets', type: 'string', default: '*', multiplicity: '1..1') {
                listOfValues(scriptRef: 'QueryItemsOfType:0') //
            }
        }
    }
}

Schema('AgentDesc', 0) {
    struct(name: 'AgentDesc', useSequence: true) {
        field(name: 'Name', type: 'string',  multiplicity: '1..1')
        field(name: 'Version', type: 'integer', multiplicity: '0..1')
        field(name: 'Id', type: 'string',  multiplicity: '0..1', pattern: patternUuid, length: 36) {
            dynamicForms(disabled: true)
        }
        field(name: 'Password', type: 'string',  multiplicity: '1..1') {
            dynamicForms(inputType: 'password')
        }
        field(name: 'InitialPath', type: 'string',  multiplicity: '0..1')
        field(name: 'Roles', type: 'string', multiplicity: '1..1') {
            listOfValues(scriptRef: 'QueryRoles:0')
        }

        struct(name:'Property', multiplicity: '0..*', useSequence: true) {
            field(name: 'Name', type: 'string',  multiplicity: '1..1')
            field(name: 'Mutable', type: 'boolean', default: 'true', multiplicity: '0..1')
            field(name: 'Value', type: 'string', multiplicity: '1..1')
        }
    }
}

