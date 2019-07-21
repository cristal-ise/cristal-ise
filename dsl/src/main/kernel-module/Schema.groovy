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

def level = ['OFF', 'ERROR', 'WARN', 'INFO', 'DEBUG', 'TRACE', 'ALL']

Schema('LoggerConfig', 0) {
    struct(name: 'LoggerConfig', useSequence: true) {
        field(name:'Root', type: 'string', values: level)
        struct(name: 'Logger', multiplicity: '0..*', useSequence: true) {
            field(name:'Class', type: 'string')
            field(name:'Level', type: 'string', values: level)
        }
    }
}
