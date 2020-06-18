Workflow('CompositeActivityFactory', 0) {
  ElemActDef($createNewLocalObjectDef_ActivityDef)
}

Workflow('ElementaryActivityFactory', 0) {
    ElemActDef($createNewLocalObjectDef_ActivityDef)
}

Workflow('SchemaFactoryWf', 0) {
    ElemActDef('EditDefinition', 0)
    ElemActDef($createNewLocalObjectDef_ActivityDef)
}

Workflow('ScriptFactoryWf', 0) {
    ElemActDef('EditDefinition', 0)
    ElemActDef($createNewLocalObjectDef_ActivityDef)
}

Workflow('QueryFactoryWf', 0) {
    ElemActDef('EditDefinition', 0)
    ElemActDef($createNewLocalObjectDef_ActivityDef)
}

Workflow('StateMachineFactoryWf', 0) {
    ElemActDef($createNewLocalObjectDef_ActivityDef)
}

Workflow('ItemDescriptionWf', 0) {
    ElemActDef($editPropertyDescription_ActivityDef)
    ElemActDef($setInstanceWorkflow_ActivityDef)
    ElemActDef($createItem_ActivityDef)
    ElemActDef($defineNewCollection_ActivityDef)
}

Workflow('ItemDescriptionFactoryWf', 0) {
    ElemActDef($createItem_ActivityDef)
}

Workflow('ReadOnlyItemDescriptionWf', 0) {
    ElemActDef($createItem_ActivityDef)
}

Workflow('ModuleFactory', 0) {
    ElemActDef($createItem_ActivityDef)
}

Workflow('AgentFactoryWf', 0) {
    ElemActDef($createAgent_ActivityDef)
}