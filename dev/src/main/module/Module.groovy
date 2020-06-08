@BaseScript(org.cristalise.dsl.module.ModuleScriptBase)
import groovy.transform.BaseScript
import groovy.transform.SourceURI

@SourceURI
URI scriptUri

setModuleDir scriptUri

setConfig  'src/test/conf/client.conf'
setConnect 'src/test/conf/server.clc'


Module(ns: 'dev', name: 'DSL Test', version: 0) {

    Info(description: 'CRISTAL-iSE Development Items to implement CRUD functionality.', version: '0'){
    }

    Url('org/cristalise/dev/resources/')

    include(moduleDir+'/Factory.groovy')
}
