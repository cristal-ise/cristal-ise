Activity('CreateNewLocalObjectDef', 0) {
    Property(Description: 'Create a new C2KLocalObject Definition')
    AbstractProperty(NewType: '')

    Schema($NewDevObjectDef_Schema)
    Script($LocalObjectDefCreator_Script)
}

Activity('CreateAgent', 0) {
    Property(Description: 'Create a new Agent from its Description')

    Schema($NewAgent_Schema)
    Script($InstantiateAgent_Script)
}

Activity('CreateItem', 0) {
    Property(Description: 'Create a new Item from its Description')

    Schema($NewDevObjectDef_Schema)
    Script($InstantiateItem_Script)
}

Activity('DefineNewCollection', 0) {
    Property(Description: '')

    Schema($NewCollection_Schema)
    Script($CollDescCreator_Script)
}

Activity('EditPropertyDescription', 0) {
    Property(Description: 'Set the initial properties for new instances.')

    Schema('PropertyDescription', 0)
}

Activity('SetInstanceWorkflow', 0) {
    Property(Description: 'Choose a CompositeActivityDefinition to use for the workflow of new instances')

    Schema($ChooseWorkflow_Schema)
    Script($SetWorkflow_Script)
}
