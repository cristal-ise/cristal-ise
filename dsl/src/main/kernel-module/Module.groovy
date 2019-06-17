import java.nio.file.Paths

@BaseScript(org.cristalise.dsl.module.ModuleScriptBase)
import groovy.transform.BaseScript
import groovy.transform.SourceURI

@SourceURI
URI scriptUri

setModuletDir scriptUri

setConfig  'src/test/conf/client.conf'
setConnect 'src/test/conf/server.clc'

setResourceRoot Paths.get(scriptUri).parent.toString()+'/resources'


Module(ns: 'kernel', name: 'cristal-ise kernel', version: 0) {
    include(moduleDir+'/Schema.groovy')
}