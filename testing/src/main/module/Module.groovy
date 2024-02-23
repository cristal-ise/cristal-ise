import java.nio.file.Paths

@BaseScript(org.cristalise.dsl.module.ModuleScriptBase)
import groovy.transform.BaseScript
import groovy.transform.SourceURI

@SourceURI
URI scriptUri

setModuleDir scriptUri

setConfig  'src/main/bin/dsl.conf'
setConnect 'src/main/bin/integTest.clc'

setResourceRoot 'src/main/resources/org/cristalise/testing/resources'

Module(ns: 'integTest', name: 'IntegrationTest', version: 0) {

    Info(description: 'CRISTAL-iSE Items for testing', version: '0'){
        // provide dependencies here. e.g. dependencies: ['dependency1', 'dependency1' ... ]
    }

    Url('org/cristalise/testing/resources/')
 
    PropertyDescriptionList('Doctor', 0) {
        PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
        PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true,  defaultValue: 'Doctor')
        PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: false, defaultValue: 'ACTIVE')
    }
    
    PropertyDescriptionList('Patient', 0) {
        PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
        PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true,  defaultValue: 'Patient')
        PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: false, defaultValue: 'ACTIVE')
    }
    
    include(moduleDir+'/Patient.groovy')
    include(moduleDir+'/Doctor.groovy')
 }
