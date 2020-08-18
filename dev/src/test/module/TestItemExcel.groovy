import static org.cristalise.kernel.collection.BuiltInCollections.AGGREGATE_SCRIPT
import static org.cristalise.kernel.collection.BuiltInCollections.MASTER_SCHEMA
import static org.cristalise.kernel.collection.BuiltInCollections.SCHEMA_INITIALISE

// this is defined in CrudState.groovy of the dev module
def states = ['ACTIVE', 'INACTIVE']

/**
 * TestItemExcel Item
 */

def xlsxFile = new File(moduleDir+'/TestItemExcel.xlsx')

def TestItemExcel         = Schema('TestItemExcel', 0, xlsxFile) 
def TestItemExcelDetails =  Schema('TestItemExcel_Details', 0, xlsxFile)



def TestItemExcelUpdateAct = Activity('TestItemExcel_Update', 0) {
    Property('OutcomeInit': 'Empty')
    Schema(TestItemExcelDetails)
    //Script('CrudEntity_ChangeName', 0)
}

def TestItemExcelAggregateScript = Script('TestItemExcel_Aggregate', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    output('TestItemExcelXML', 'java.lang.String')
    script('groovy', moduleDir+'/script/TestItemExcel_Aggregate.groovy')
}

def TestItemExcelQueryListScript = Script('TestItemExcel_QueryList', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    output('TestItemExcelMap', 'java.util.Map')
    script('groovy', moduleDir+'/script/TestItemExcel_QueryList.groovy')
}

Activity('TestItemExcel_Aggregate', 0) {
    Property('OutcomeInit': 'Empty')
    Property('Agent Role': 'UserCode')

    Schema(TestItemExcel)
    Script(TestItemExcelAggregateScript)
}

def TestItemExcelWf = Workflow('TestItemExcel_Workflow', 0) {
    ElemActDef(TestItemExcelUpdateAct)
    CompActDef('CrudState_Manage', 0)
}

def TestItemExcelPropDesc = PropertyDescriptionList('TestItemExcel', 0) {
    PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
    PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true,  defaultValue: 'TestItemExcel')
    PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: false, defaultValue: 'ACTIVE')
}

Item(name: 'TestItemExcelFactory', version: 0, folder: '/devtest', workflow: 'CrudFactory_Workflow', workflowVer: 0) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('Root': '/devtest/TestItemExcels')





    InmutableProperty('UpdateSchema': 'TestItemExcel_Details:0')


    Outcome(schema: 'PropertyDescription', version: '0', viewname: 'last', path: 'boot/property/TestItemExcel_0.xml')

    Dependency('workflow') {
        Member(itemPath: '/desc/ActivityDesc/devtest/TestItemExcel_Workflow') {
            Property('Version': 0)
        }
    }

    Dependency(MASTER_SCHEMA) {
        Member(itemPath: '/desc/Schema/devtest/TestItemExcel') {
            Property('Version': 0)
        }
    }

    Dependency(AGGREGATE_SCRIPT) {
        Member(itemPath: '/desc/Script/devtest/TestItemExcel_Aggregate') {
            Property('Version': 0)
        }
    }
}
