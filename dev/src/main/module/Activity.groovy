Activity('CreateNewLocalObjectDef', 0) {
    Property(Description: 'Create a new C2KLocalObject Definition')
    AbstractProperty(NewType: '')

    Schema($newDevObjectDef_Schema)
    Script($localObjectDefCreator_Script)
}

Activity('CreateAgent', 0) {
    Property(Description: 'Create a new Agent from its Description')

    Schema($newAgent_Schema)
    Script($instantiateAgent_Script)
}

Activity('CreateItem', 0) {
    Property(Description: 'Create a new Item from its Description')

    Schema($newDevObjectDef_Schema)
    Script($instantiateItem_Script)
}

Activity('DefineNewCollection', 0) {
    Property(Description: '')

    Schema($newCollection_Schema)
    Script($collDescCreator_Script)
}

Activity('EditPropertyDescription', 0) {
    Property(Description: 'Set the initial properties for new instances.')

    Schema('PropertyDescription', 0)
}

Activity('SetInstanceWorkflow', 0) {
    Property(Description: 'Choose a CompositeActivityDefinition to use for the workflow of new instances')

    Schema($chooseWorkflow_Schema)
    Script($setWorkflow_Script)
}
