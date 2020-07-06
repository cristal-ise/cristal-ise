Schema('CrudFactory_NewInstanceDetails', 0) {
    struct(name: 'CrudFactory_NewInstanceDetails', useSequence: true) {
        field(name: 'Name',      type: 'string',  documentation: 'The Name of the new instance, it can be generated')
        field(name: 'LastCount', type: 'integer', documentation: 'The last number used to generate the ID', multiplicity: '0..1') {
            dynamicForms (hidden: true)
        }
        struct(name: 'SchemaInitialise', useSequence: true, multiplicity: '0..1') {
            dynamicForms (hidden: true)
            anyField()
        }
    }
}

Script("CrudFactory_InstantiateItem", 0) {
    script('groovy', moduleDir+'/script/CrudFactory_InstantiateItem.groovy')
}

Script("CrudEntity_ChangeName", 0) {
    script('groovy', moduleDir+'/script/CrudEntity_ChangeName.groovy')
}

Activity('CrudFactory_InstantiateItem', 0) {
    Property('OutcomeInit': 'Empty')

    Schema($crudFactory_NewInstanceDetails_Schema)
    Script($crudFactory_InstantiateItem_Script)
}

Workflow('CrudFactory_Workflow', 0) {
    ElemActDef($crudFactory_InstantiateItem_ActivityDef)
}
