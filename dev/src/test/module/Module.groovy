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


    
    include(moduleDir+'/Car/Car.groovy')

    
    include(moduleDir+'/ClubMember/ClubMember.groovy')

    
    include(moduleDir+'/Motorcycle/Motorcycle.groovy')

    
    include(moduleDir+'/TestAgent/TestAgent.groovy')

    
    include(moduleDir+'/TestAgentUseConstructor/TestAgentUseConstructor.groovy')

    
    include(moduleDir+'/TestItem/TestItem.groovy')

    
    include(moduleDir+'/TestItemExcel/TestItemExcel.groovy')

    
    include(moduleDir+'/TestItemGeneratedName/TestItemGeneratedName.groovy')

    
    include(moduleDir+'/TestItemUseConstructor/TestItemUseConstructor.groovy')

    
    include(moduleDir+'/TestItemUseConstructorGeneratedName/TestItemUseConstructorGeneratedName.groovy')

}
