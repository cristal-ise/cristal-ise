states = ['ACTIVE', 'INACTIVE']

Activity('CrudState_Activate', 0) {
    Property('ItemProperty.State': states[0])
}

Activity('CrudState_Deactivate', 0) {
    Property('ItemProperty.State': states[1])
}

def stateWf = Workflow('CrudState_Manage', 0) {
    ElemActDef($crudState_Activate_ActivityDef)
    ElemActDef($crudState_Deactivate_ActivityDef)
}
