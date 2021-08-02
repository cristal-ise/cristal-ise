import static org.cristalise.kernel.collection.BuiltInCollections.AGGREGATE_SCRIPT
import static org.cristalise.kernel.collection.BuiltInCollections.MASTER_SCHEMA
import static org.cristalise.kernel.collection.BuiltInCollections.SCHEMA_INITIALISE
import static org.cristalise.kernel.collection.BuiltInCollections.WORKFLOW

// this is defined in CrudState.groovy of the dev module
def states = ['ACTIVE', 'INACTIVE']

/**
 * ClubMember Item
 */

def xlsxFile = new File(moduleDir+'/ClubMember.xslx')

Schema('ClubMember', 0, xlsxFile)
Schema('ClubMember_Details', 0, xlsxFile)



Activity('ClubMember_Update', 0) {
    Property('OutcomeInit': 'Empty')
    Schema($clubMember_Details_Schema)
    //Script('CrudEntity_ChangeName', 0)
}

Script('ClubMember_Aggregate', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    output('ClubMemberXML', 'java.lang.String')
    script('groovy', moduleDir+'/script/ClubMember_Aggregate.groovy')
}

Script('ClubMember_QueryList', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    output('ClubMemberMap', 'java.util.Map')
    script('groovy', moduleDir+'/script/ClubMember_QueryList.groovy')
}

Activity('ClubMember_Aggregate', 0) {
    Property('OutcomeInit': 'Empty')
    Property('Agent Role': 'UserCode')

    Schema($clubMember_Schema)
    Script($clubMember_Aggregate_Script)
}

Workflow('ClubMember_Workflow', 0) {
    ElemActDef($clubMember_Update_ActivityDef)
    CompActDef('CrudState_Manage', 0)
}

PropertyDescriptionList('ClubMember', 0) {
    PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
    PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true,  defaultValue: 'ClubMember')
    PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: false, defaultValue: 'ACTIVE')
}

Item(name: 'ClubMemberFactory', version: 0, folder: '/test', workflow: 'CrudFactory_Workflow', workflowVer: 0) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('Root': '/test/ClubMembers')





    InmutableProperty('UpdateSchema': 'ClubMember_Details:0')


    Outcome(schema: 'PropertyDescription', version: '0', viewname: 'last', path: 'boot/property/ClubMember_0.xml')

    Dependency(WORKFLOW) {
        Member(itemPath: $clubMember_Workflow_CompositeActivityDef) {
            Property('Version': 0)
        }
    }

    Dependency(MASTER_SCHEMA) {
        Member(itemPath: $clubMember_Schema) {
            Property('Version': 0)
        }
    }

    Dependency(AGGREGATE_SCRIPT) {
        Member(itemPath: $clubMember_Aggregate_Script) {
            Property('Version': 0)
        }
    }
}
