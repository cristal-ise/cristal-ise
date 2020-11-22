import java.nio.file.Paths

@BaseScript(org.cristalise.dsl.module.ModuleScriptBase)
import groovy.transform.BaseScript
import groovy.transform.SourceURI

@SourceURI
URI scriptUri

setModuletDir scriptUri

setConfig  'src/main/bin/client.conf'
setConnect 'src/main/bin/integTest.clc'

setResourceRoot Paths.get(scriptUri).parent.toString()+'/resources'

Module(ns: 'integTest', name: 'IntegrationTest', version: 0) {

    Info(description: 'CRISTAL-iSE Items for testing', version: '0'){
        // provide dependencies here. e.g. dependencies: ['dependency1', 'dependency1' ... ]
    }

    Url('org/cristalise/testing/resources/')
 
    include(moduleDir+'/Patient.groovy')
 }
