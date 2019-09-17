package org.cristalise.dsl.test.scaffold

import org.codehaus.groovy.control.CompilerConfiguration
import org.cristalise.dsl.scaffold.CRUDGenerator
import org.cristalise.kernel.utils.Logger
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
            useConstructor:  false
        ]

        new CRUDGenerator().generate(inputs, true, true)

        CompilerConfiguration cc = new CompilerConfiguration()
        cc.setScriptBaseClass(DelegatingScript.class.getName())

        GroovyShell shell = new GroovyShell(this.class.classLoader, new Binding(), cc)
        DelegatingScript script = (DelegatingScript) shell.parse(new File((String)inputs.rootDir+'/module/Module.groovy'))

        script.setDelegate(this)
        script.run()
    }
}
