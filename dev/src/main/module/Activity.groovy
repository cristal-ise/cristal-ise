Activity('CreateNewLocalObjectDef', 0) {
    Property(Description: 'Create a new C2KLocalObject Definition')
    AbstractProperty(NewType: '')

    Schema($NewDevObjectDefSchema)
    Script($LocalObjectDefCreatorScript)
}

Activity('CreateAgent', 0) {
    Property(Description: 'Create a new Agent from its Description')

    Schema($NewAgentSchema)
    Script($InstantiateAgentScript)
}

Activity('CreateItem', 0) {
    Property(Description: 'Create a new Item from its Description')

    Schema($NewDevObjectDefSchema)
    Script($InstantiateItemScript)
}

Activity('DefineNewCollection', 0) {
    Property(Description: '')

    Schema($NewCollectionSchema)
    Script($CollDescCreatorScript)
}

Activity('EditPropertyDescription', 0) {
    Property(Description: 'Set the initial properties for new instances.')

    Schema('PropertyDescription', 0)
}

Activity('SetInstanceWorkflow', 0) {
    Property(Description: 'Choose a CompositeActivityDefinition to use for the workflow of new instances')

    Schema($ChooseWorkflowSchema)
    Script($SetWorkflowScript)
}
