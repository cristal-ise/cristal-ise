package org.cristalise.dsl.test.scaffold

import org.codehaus.groovy.control.CompilerConfiguration
import org.cristalise.dsl.scaffold.CRUDGenerator
import org.junit.Test

class CRUDGeneratorTest {

    @Test
    void generateCRUDItemTest() {
        def inputs = [
            rootDir:         'src/test', 
            moduleName:      'DSL Test', 
            moduleNs:        'testns', 
            moduleVersion:   0,
            resourcePackage: 'org.cristalise.test',
            item:            'TestItem'
        ]

        new CRUDGenerator().generate(inputs, true, true)

        CompilerConfiguration cc = new CompilerConfiguration()
        cc.setScriptBaseClass(DelegatingScript.class.getName())

        GroovyShell shell = new GroovyShell(this.class.classLoader, new Binding(), cc)
        DelegatingScript script = (DelegatingScript) shell.parse(new File(inputs.rootDir+'/module/Module.groovy'))

        script.setDelegate(this)
        script.run()
    }
}
