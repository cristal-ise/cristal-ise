import java.nio.file.Paths

@BaseScript(org.cristalise.dsl.module.ModuleScriptBase)
import groovy.transform.BaseScript
import groovy.transform.SourceURI

@SourceURI
URI scriptUri

setModuleDir scriptUri

setConfig  'src/test/conf/client.conf'
setConnect 'src/test/conf/server.clc'

setResourceRoot Paths.get(scriptUri).parent.toString()+'/resources'


Module(ns: 'kernel', name: 'cristal-ise kernel', version: 0) {
    include(moduleDir+'/CommonTypes.groovy')
    include(moduleDir+'/Role.groovy')
    include(moduleDir+'/Script.groovy')
    include(moduleDir+'/Schema.groovy')
}