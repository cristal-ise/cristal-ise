Workflow('CompositeActivityFactory', 0) {
  ElemActDef($CreateNewLocalObjectDefActivity)
}

Workflow('ElementaryActivityFactory', 0) {
    ElemActDef($CreateNewLocalObjectDefActivity)
}

Workflow('SchemaFactoryWf', 0) {
    ElemActDef('EditDefinition', 0)
    ElemActDef($CreateNewLocalObjectDefActivity)
}

Workflow('ScriptFactoryWf', 0) {
    ElemActDef('EditDefinition', 0)
    ElemActDef($CreateNewLocalObjectDefActivity)
}

Workflow('QueryFactoryWf', 0) {
    ElemActDef('EditDefinition', 0)
    ElemActDef($CreateNewLocalObjectDefActivity)
}

Workflow('StateMachineFactoryWf', 0) {
    ElemActDef($CreateNewLocalObjectDefActivity)
}

Workflow('ItemDescriptionWf', 0) {
    ElemActDef($EditPropertyDescriptionActivity)
    ElemActDef($SetInstanceWorkflowActivity)
    ElemActDef($CreateItemActivity)
    ElemActDef($DefineNewCollectionActivity)
}

Workflow('ItemDescriptionFactoryWf', 0) {
    ElemActDef($CreateItemActivity)
}

Workflow('ReadOnlyItemDescriptionWf', 0) {
    ElemActDef($CreateItemActivity)
}

Workflow('ModuleFactory', 0) {
    ElemActDef($CreateItemActivity)
}

Workflow('AgentFactoryWf', 0) {
    ElemActDef($CreateItemActivity)
}