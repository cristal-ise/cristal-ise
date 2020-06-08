Schema(name: 'Synopsys', folder: 'tutorial', version: -1) {
    
}

//This could be defined in a CSV/Excel/Google document as well
def roleAgentMap = [Hematologist: HemaLabor,
                    Internist:    UrinLabor,
                    Cardiologist: Sam,
                    Optometrist:  Steve]

roleAgentMap.each { role, agent ->
    Role(name: role, folder: 'tutorial')
    Agent(name: agent, pwd: 'test', folder: 'tutorial' roles: [role])
}

//This could be defined in a CSV/Excel/Google document as well
def actNameRoleMap = ['Blood Biochemical Analysis':    'Hematologist',
                      'Urinalysis':                    'Internist',
                      'ECG':                           'Cardiologist',
                      'Comprehensive Eye Examination': 'Opthometrist']

actNameRoleMap.each { name, role ->
    ElementaryActivityDefinition(name: name, folder: 'tutorial', version: -1) {
        schema        = 'Synopsys'
        schemaVersion = -1
        role          = role
    }
}

CompositeActivityDefinition(name: 'Protocol', folder: 'tutorial', version: -1) {
    AndSplit { //AndSplit has a list of Blocks and Loop is a Block
        Loop(condition: 'javascript: true') { ElemAct('Blood Biochemical Analysis') } 
        Loop(condition: 'javascript: true') { ElemAct('Urinalysis') }
        Loop(condition: 'javascript: true') { ElemAct('ECG') }
        Loop(condition: 'javascript: true') { ElemAct('Comprehensive Eye Examination') }
    }
}

Definition(name: 'Patient', folder: 'tutorial') {
    String name
    final String type = 'Patient'
    String NationalInsurenaceNumber

    Workflow = 'Protocol:0' //name:version
}
