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
