@BaseScript(org.cristalise.dsl.module.ModuleScriptBase)
import groovy.transform.BaseScript
import groovy.transform.SourceURI

@SourceURI
URI scriptUri

setModuleDir scriptUri

Module(ns: 'dev', name: 'CristaliseDev', version: 0) {

    Info(description: 'CRISTAL-iSE Development Items to implement CRUD functionality.', version: '0') {
    }

    Url('org/cristalise/dev/resources/')

    include(moduleDir+'/Schema.groovy')
    include(moduleDir+'/Script.groovy')
    include(moduleDir+'/Activity.groovy')
    include(moduleDir+'/Workflow.groovy')
}
