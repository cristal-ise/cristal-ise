package org.cristalise.devtest

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

    include(moduleDir+'/CommonDefs.groovy')

    
    include(moduleDir+'/car/Car.groovy')

    
    include(moduleDir+'/clubMember/ClubMember.groovy')

    
    include(moduleDir+'/motorcycle/Motorcycle.groovy')

    
    include(moduleDir+'/testAgent/TestAgent.groovy')

    
    include(moduleDir+'/testAgentUseConstructor/TestAgentUseConstructor.groovy')

    
    include(moduleDir+'/testItem/TestItem.groovy')

    
    include(moduleDir+'/testItemExcel/TestItemExcel.groovy')

    
    include(moduleDir+'/testItemGeneratedName/TestItemGeneratedName.groovy')

    
    include(moduleDir+'/testItemUseConstructor/TestItemUseConstructor.groovy')

    
    include(moduleDir+'/testItemUseConstructorGeneratedName/TestItemUseConstructorGeneratedName.groovy')

}
