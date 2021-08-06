@BaseScript(org.cristalise.dsl.module.ModuleScriptBase)
import groovy.transform.BaseScript
import groovy.transform.SourceURI

@SourceURI
URI scriptUri

setModuleDir scriptUri

setConfig  'src/test/conf/client.conf'
setConnect 'src/test/conf/server.clc'

setResourceRoot  'src/test/resources/org/cristalise/devtest/resources/' 
 setModuleXmlDir 'src/test/resources/META-INF/cristal' 

Module(ns: 'devtest', name: 'DEV Scaffold Test module', version: 0) {

    Info(description: 'DEV Scaffold Test module CRISTAL-iSE module', version: '0'){
        Dependencies:['CristaliseDev']
    }

    Url('org/cristalise/devtest/resources/')

    Agent(name: 'devtest', version: 0, password: 'test', folder:'/devtest/Agents') {
        Roles {
            Role(name: 'Admin')
        }
    }

    include(moduleDir+'/Car.groovy')
    include(moduleDir+'/ClubMember.groovy')
    include(moduleDir+'/TestAgent.groovy')
    include(moduleDir+'/TestAgentUseConstructor.groovy')
    include(moduleDir+'/TestItem.groovy')
    include(moduleDir+'/TestItemExcel.groovy')
    include(moduleDir+'/TestItemGeneratedName.groovy')
    include(moduleDir+'/TestItemUseConstructor.groovy')
    include(moduleDir+'/TestItemUseConstructorGeneratedName.groovy')
}
