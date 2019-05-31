Schema('SimpleElectonicSignature', 0) {
    struct(name: 'SimpleElectonicSignature', documentation: "Simple from to provide electronic signature") {
        field(name:'AgentName', type: 'string')
        field(name:'Password',  type: 'string'/*, minLength: 10*/) { dynamicForms(inputType: 'password') }
        field(name:'Comment',   type: 'string'/*, minLength: 10*/)

        struct(name: 'Context', documentation: "The context of Item and Actitiy of the Electronic Signature", multiplicity: '1..1') {
            field(name:'ItemPath', type: 'string')
            field(name:'EventID', type: 'integer')
            field(name:'ActivityType', type: 'string')
            field(name:'ActivityName', type: 'string')
            field(name:'StepPath', type: 'string')
        }
    }
}
