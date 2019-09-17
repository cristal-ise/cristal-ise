def newInstanceDetails = Schema('Factory_NewInstanceDetails', 0) {
    struct(name: 'Factory_NewInstanceDetails', useSequence: true) {
        field(name: 'Name',      type: 'string',  documentation: 'The user given Name of the new instance')
        field(name: 'ID',        type: 'string',  documentation: 'The generated ID of the new instance')    { dynamicForms (hidden: true, required:false) }
        field(name: 'LastCount', type: 'integer', documentation: 'The last number used to generate the ID') { dynamicForms (hidden: true, required:false) }
        struct(name: 'SchemaInitialise', useSequence: true, multiplicity: '0..1') {
            anyField()
        }
    }
}

def instantiateItem = Script("Factory_InstantiateItem", 0) {
    script('groovy', 'src/main/script/SC/Factory_InstantiateItem.groovy')
}

changeName = Script("Entity_ChangeName", 0) {
    script('groovy', 'src/main/script/SC/Entity_ChangeName.groovy')
}

def createItem = Activity('Factory_CreateItem', 0) {
    Property('OutcomeInit': 'Empty')

    Schema(newInstanceDetails)
    Script(instantiateItem)
}

Workflow('FactoryLifecycle', 0) {
    ElemActDef(createItem)
}
