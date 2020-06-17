Workflow('CompositeActivityFactory', 0) {
  ElemActDef($CreateNewLocalObjectDef_ActivityDef)
}

Workflow('ElementaryActivityFactory', 0) {
    ElemActDef($CreateNewLocalObjectDef_ActivityDef)
}

Workflow('SchemaFactoryWf', 0) {
    ElemActDef('EditDefinition', 0)
    ElemActDef($CreateNewLocalObjectDef_ActivityDef)
}

Workflow('ScriptFactoryWf', 0) {
    ElemActDef('EditDefinition', 0)
    ElemActDef($CreateNewLocalObjectDef_ActivityDef)
}

Workflow('QueryFactoryWf', 0) {
    ElemActDef('EditDefinition', 0)
    ElemActDef($CreateNewLocalObjectDef_ActivityDef)
}

Workflow('StateMachineFactoryWf', 0) {
    ElemActDef($CreateNewLocalObjectDef_ActivityDef)
}

Workflow('ItemDescriptionWf', 0) {
    ElemActDef($EditPropertyDescription_ActivityDef)
    ElemActDef($SetInstanceWorkflow_ActivityDef)
    ElemActDef($CreateItem_ActivityDef)
    ElemActDef($DefineNewCollection_ActivityDef)
}

Workflow('ItemDescriptionFactoryWf', 0) {
    ElemActDef($CreateItem_ActivityDef)
}

Workflow('ReadOnlyItemDescriptionWf', 0) {
    ElemActDef($CreateItem_ActivityDef)
}

Workflow('ModuleFactory', 0) {
    ElemActDef($CreateItem_ActivityDef)
}

Workflow('AgentFactoryWf', 0) {
    ElemActDef($CreateAgent_ActivityDef)
}