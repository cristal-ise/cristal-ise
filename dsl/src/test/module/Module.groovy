@BaseScript(org.cristalise.dsl.module.ModuleScriptBase)
import groovy.transform.BaseScript
import groovy.transform.SourceURI

@SourceURI
URI scriptUri

setModuletDir scriptUri

setConfig  'src/test/conf/client.conf'
setConnect 'src/test/conf/server.clc'

setResourceRoot 'src/test/resources'

Module(ns: 'testns', name: 'DSL Test', version: 0) {

    Info(description: 'DSL Test CRISTAL-iSE module', version: '0'){
        // provide dependencies here. e.g. dependencies: ['dependency1', 'dependency1' ... ]
    }

    Url('org.cristalise.test/resources/')

    Config(name: 'Module.debug', value: true)

    Roles {
        Role(name: 'Admin', jobList: false) {
            Permission('*')
        }
    }

    Agent(name: 'TestAdmin', password: 'test', folder:'/testns/Agents') {
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
