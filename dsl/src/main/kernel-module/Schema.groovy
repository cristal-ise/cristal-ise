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
        struct(name:'Property', multiplicity: '0..*') {
            field(name: 'name', type: 'string')
            field(name: 'module', type: 'string', multiplicity: '0..1', documentation: 'The module in which the System Property was defined')
            field(name: 'readOnly', type: 'boolean', default: false, documentation: 'Specify if the Property can be dinamically overridden')
            field(name: 'description', type: 'string', multiplicity: '0..1')
            field(name: 'value', type: 'string', multiplicity: '0..1') {
                attribute(name: 'setInConfigFiles', type: 'boolean', multiplicity: '0..1') //Indicates if the value was set in config files
            }
        }
    }
}
