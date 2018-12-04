package org.cristalise.dsl.test.scaffold

import org.codehaus.groovy.control.CompilerConfiguration
import org.cristalise.dsl.scaffold.CRUDGenerator
import org.junit.Test

class CRUDGeneratorTest {

    @Test
    void generateCRUDItemTest( ) {
        def vars = [rootDir: 'src/test', moduleName: 'Test Module', moduleNs: 'testns', moduleVersion: 0, item: 'TestItem']
        new CRUDGenerator().generate(vars, true, true)

        CompilerConfiguration cc = new CompilerConfiguration()
        cc.setScriptBaseClass(DelegatingScript.class.getName())

        GroovyShell shell = new GroovyShell(this.class.classLoader, new Binding(), cc)
        DelegatingScript script = (DelegatingScript) shell.parse(new File('src/test/module/Module.groovy'))

        script.setDelegate(this)
        script.run()
    }
}
