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

    include(moduleDir+'/State.groovy')
    include(moduleDir+'/TestItem.groovy')
}
