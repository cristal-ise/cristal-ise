@BaseScript(org.cristalise.dsl.module.ModuleScriptBase)
import groovy.transform.BaseScript
import groovy.transform.SourceURI

@SourceURI
URI scriptUri

setModuleDir scriptUri

Module(ns: 'dev', name: 'CristaliseDev', version: 0) {
    Info(description: 'CRISTAL-iSE Development Items to implement CRUD functionality.', version: '${version}') {
    }
    Config(name: 'OutcomeInit.Dev',               value: 'org.cristalise.dev.DevObjectOutcomeInitiator')
    Config(name: 'OverrideScriptLang.javascript', value: 'rhino')
    Config(name: 'OverrideScriptLang.JavaScript', value: 'rhino')
    Config(name: 'OverrideScriptLang.js',         value: 'rhino')
    Config(name: 'OverrideScriptLang.JS',         value: 'rhino')
    Config(name: 'OverrideScriptLang.ECMAScript', value: 'rhino')
    Config(name: 'OverrideScriptLang.ecmascript', value: 'rhino')

    Url('org/cristalise/dev/resources/')

    include(moduleDir+'/Property.groovy')
    include(moduleDir+'/Schema.groovy')
    include(moduleDir+'/Script.groovy')
    include(moduleDir+'/Activity.groovy')
    include(moduleDir+'/Workflow.groovy')
    include(moduleDir+'/Item.groovy')
}
