package org.cristalise.dsl.test.scaffold

import org.codehaus.groovy.control.CompilerConfiguration
import org.cristalise.dsl.scaffold.CRUDGenerator
import org.junit.Test

import groovy.transform.CompileStatic

@CompileStatic
class CRUDGeneratorTest {

    @Test
    void generateCRUDItemTest() {
        def inputs = [
            rootDir:         'src/test', 
            moduleName:      'DSL Test', 
            moduleNs:        'testns', 
            moduleVersion:   0,
            resourcePackage: 'org.cristalise.test',
            item:            'TestItem',
            useConstructor:  false,
            isAgent:         false,
            moduleFiles:     ['Factory.groovy', 'State.groovy', 'TestAgent.groovy', 'TestItem.groovy']
        ]

        new CRUDGenerator().generate(inputs, true, true)

        inputs.with {
            item = 'TestAgent'
            isAgent = true
            useConstructor = true
        }

        new CRUDGenerator().generate(inputs, true, false)

        CompilerConfiguration cc = new CompilerConfiguration()
        cc.setScriptBaseClass(DelegatingScript.class.getName())

        GroovyShell shell = new GroovyShell(this.class.classLoader, new Binding(), cc)
        def scriptFile = new File(inputs['rootDir'].toString()+'/module/Module.groovy')
        DelegatingScript script = (DelegatingScript) shell.parse(scriptFile)

        script.setDelegate(this)
        script.run()
    }
}
