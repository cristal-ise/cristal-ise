@BaseScript(org.cristalise.dsl.module.ModuleScriptBase)
import groovy.transform.BaseScript
import groovy.transform.SourceURI

@SourceURI
URI scriptUri

setModuleDir scriptUri

setConfig  'src/test/conf/client.conf'
setConnect 'src/test/conf/server.clc'

setResourceRoot  'src/test/resources/org/cristalise/dsl/test/resources/' 
 setModuleXmlDir 'src/test/resources/META-INF/cristal' 

Module(ns: 'dsl', name: 'DSL Test', version: 0) {

    Info(description: 'DSL Test CRISTAL-iSE module', version: '0'){
        // provide dependencies here. e.g. dependencies: ['dependency1', 'dependency1' ... ]
    }

    Url('org/cristalise/dsl/test/resources/')

    Agent(name: 'dsl', password: 'test', folder:'/dsl/Agents') {
        Roles {
            Role(name: 'Admin')
        }
    }

 
    include(moduleDir+'/Factory.groovy')
 
    include(moduleDir+'/State.groovy')
 
    include(moduleDir+'/TestItem.groovy')
 
    include(moduleDir+'/TestItemUseConstructor.groovy')
 
    include(moduleDir+'/TestAgentUseConstructor.groovy')
 
    include(moduleDir+'/TestAgent.groovy')
 
    include(moduleDir+'/TestItemGeneratedName.groovy')
 
    include(moduleDir+'/TestItemUseConstructorGeneratedName.groovy')

}
