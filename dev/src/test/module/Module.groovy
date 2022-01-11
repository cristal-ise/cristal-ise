@BaseScript(org.cristalise.dsl.module.ModuleScriptBase)
import groovy.transform.BaseScript
import groovy.transform.SourceURI

@SourceURI URI scriptUri

setModuleDir scriptUri

setConfig  'src/test/conf/client.conf'
setConnect 'src/test/conf/server.clc'

setResourceRoot  'src/test/resources/org/cristalise/devtest/resources/' 
 setModuleXmlDir 'src/test/resources/META-INF/cristal' 

Module(ns: 'devtest', name: 'DEV Scaffold Test module', version: 0) {

    Info(description: 'DEV Scaffold Test module', version: '0'){
        Dependencies:['CristaliseDev']
    }

    Url('org/cristalise/devtest/resources/')

    Agent(name: 'devtest', version: 0, password: 'test', folder:'/devtest/Agents') {
        Roles {
            Role(name: 'Admin')
        }
    }


    
    PropertyDescriptionList('Car', 0) {
        PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
        PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true,  defaultValue: 'Car')
        PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: false, defaultValue: 'ACTIVE')
    }

    
    PropertyDescriptionList('ClubMember', 0) {
        PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
        PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true,  defaultValue: 'ClubMember')
        PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: false, defaultValue: 'ACTIVE')
    }

    
    PropertyDescriptionList('Motorcycle', 0) {
        PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
        PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true,  defaultValue: 'Motorcycle')
        PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: false, defaultValue: 'ACTIVE')
    }

    
    PropertyDescriptionList('TestAgent', 0) {
        PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
        PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true,  defaultValue: 'TestAgent')
        PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: false, defaultValue: 'ACTIVE')
    }

    
    PropertyDescriptionList('TestAgentUseConstructor', 0) {
        PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
        PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true,  defaultValue: 'TestAgentUseConstructor')
        PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: false, defaultValue: 'ACTIVE')
    }

    
    PropertyDescriptionList('TestItem', 0) {
        PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
        PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true,  defaultValue: 'TestItem')
        PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: false, defaultValue: 'ACTIVE')
    }

    
    PropertyDescriptionList('TestItemExcel', 0) {
        PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
        PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true,  defaultValue: 'TestItemExcel')
        PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: false, defaultValue: 'ACTIVE')
    }

    
    PropertyDescriptionList('TestItemGeneratedName', 0) {
        PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
        PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true,  defaultValue: 'TestItemGeneratedName')
        PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: false, defaultValue: 'ACTIVE')
    }

    
    PropertyDescriptionList('TestItemUseConstructor', 0) {
        PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
        PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true,  defaultValue: 'TestItemUseConstructor')
        PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: false, defaultValue: 'ACTIVE')
    }

    
    PropertyDescriptionList('TestItemUseConstructorGeneratedName', 0) {
        PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
        PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true,  defaultValue: 'TestItemUseConstructorGeneratedName')
        PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: false, defaultValue: 'ACTIVE')
    }


    include(moduleDir+'/Car.groovy')
    include(moduleDir+'/ClubMember.groovy')
    include(moduleDir+'/Motorcycle.groovy')
    include(moduleDir+'/TestAgent.groovy')
    include(moduleDir+'/TestAgentUseConstructor.groovy')
    include(moduleDir+'/TestItem.groovy')
    include(moduleDir+'/TestItemExcel.groovy')
    include(moduleDir+'/TestItemGeneratedName.groovy')
    include(moduleDir+'/TestItemUseConstructor.groovy')
    include(moduleDir+'/TestItemUseConstructorGeneratedName.groovy')
}
