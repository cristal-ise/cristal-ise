Schema('SimpleElectonicSignature', 0) {
    struct(name: 'SimpleElectonicSignature', documentation: "Minimum form to provide electronic signature") {
        field(name:'AgentName', type: 'string')
        field(name:'Password',  type: 'string') { dynamicForms(inputType: 'password') }

        struct(name: 'ExecutionContext', documentation: "The context of Item and Actitiy of the Electronic Signature", multiplicity: '1..1') {
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
    struct(name: 'SystemProperties', useSequence: true, documentation: 'blabla') {
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

def uuidPattern = '[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}'

def fieldKeyValuePair = {
    field(name:' KeyValuePair', type: 'string',  multiplicity: '0..*') {
        attribute(name: 'Key',        type: 'string',  multiplicity: '1..1')
        attribute(name: 'isAbstract', type: 'boolean', multiplicity: '1..1')
        attribute(name: 'Integer',    type: 'integer', multiplicity: '0..1')
        attribute(name: 'String',     type: 'string',  multiplicity: '0..1')
        attribute(name: 'Float',      type: 'decimal', multiplicity: '0..1')
        attribute(name: 'Boolean',    type: 'boolean', multiplicity: '0..1')
    }
}

Schema('Dependency', 0) {
    struct(name: 'Dependency', useSequence: true) {
        attribute(name: 'CollectionName', type: 'string', multiplicity: '1..1')
        attribute(name: 'ClassProps',     type: 'string', multiplicity: '1..1')
        struct(name: 'CollectionMemberList', useSequence: true, multiplicity: '1..1') {
            struct(name: 'DependencyMember', useSequence: true, multiplicity: '0..*') {
                attribute(name: 'ChildUUID',  type: 'string', pattern: uuidPattern, length : 36)
                attribute(name: 'ID',         type: 'integer')
                attribute(name: 'ClassProps', type: 'string')

                struct(name: 'Properties', useSequence: true,  multiplicity: '1..*', fieldKeyValuePair)
            }
        }
        struct(name: 'Properties', useSequence: true, multiplicity: '1..*', fieldKeyValuePair)
    }
}
