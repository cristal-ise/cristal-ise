import static org.cristalise.kernel.collection.BuiltInCollections.AGGREGATE_SCRIPT
import static org.cristalise.kernel.collection.BuiltInCollections.MASTER_SCHEMA
import static org.cristalise.kernel.collection.BuiltInCollections.SCHEMA_INITIALISE
import static org.cristalise.kernel.collection.BuiltInCollections.WORKFLOW

// this is defined in CrudState.groovy of the dev module
def states = ['ACTIVE', 'INACTIVE']

/**
 * Car Item
 */

Schema('Car', 0) {
    struct(name:' Car', documentation: 'Car aggregated data') {
        field(name: 'Name',        type: 'string')
        field(name: 'State',       type: 'string', values: states)
        field(name: 'Description', type: 'string')
    }
}

Schema('Car_Details', 0) {
    struct(name: 'Car_Details') {

        field(name: 'Name', type: 'string')

        field(name: 'Description', type: 'string')
    }
}


Activity('Car_Update', 0) {
    Property('OutcomeInit': 'Empty')
    Schema($car_Details_Schema)
    //Script('CrudEntity_ChangeName', 0)
}

Script('Car_Aggregate', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    output('CarXML', 'java.lang.String')
    script('groovy', moduleDir+'/script/Car_Aggregate.groovy')
}

Script('Car_QueryList', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    output('CarMap', 'java.util.Map')
    script('groovy', moduleDir+'/script/Car_QueryList.groovy')
}

Activity('Car_Aggregate', 0) {
    Property('OutcomeInit': 'Empty')
    Property('Agent Role': 'UserCode')

    Schema($car_Schema)
    Script($car_Aggregate_Script)
}

Workflow('Car_Workflow', 0) {
    ElemActDef($car_Update_ActivityDef)
    CompActDef('CrudState_Manage', 0)
}

PropertyDescriptionList('Car', 0) {
    PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
    PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true,  defaultValue: 'Car')
    PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: false, defaultValue: 'ACTIVE')
}

Item(name: 'CarFactory', version: 0, folder: '/test', workflow: 'CrudFactory_Workflow', workflowVer: 0) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('Root': '/test/Cars')





    InmutableProperty('UpdateSchema': 'Car_Details:0')


    Outcome(schema: 'PropertyDescription', version: '0', viewname: 'last', path: 'boot/property/Car_0.xml')

    Dependency(WORKFLOW) {
        Member(itemPath: $car_Workflow_CompositeActivityDef) {
            Property('Version': 0)
        }
    }

    Dependency(MASTER_SCHEMA) {
        Member(itemPath: $car_Schema) {
            Property('Version': 0)
        }
    }

    Dependency(AGGREGATE_SCRIPT) {
        Member(itemPath: $car_Aggregate_Script) {
            Property('Version': 0)
        }
    }
}
