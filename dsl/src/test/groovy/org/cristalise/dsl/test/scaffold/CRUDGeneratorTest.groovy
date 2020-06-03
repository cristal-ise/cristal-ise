package org.cristalise.dsl.test.scaffold

import org.codehaus.groovy.control.CompilerConfiguration
import org.cristalise.dsl.scaffold.CRUDGenerator
import org.junit.Test

import groovy.transform.CompileStatic

@CompileStatic
class CRUDGeneratorTest {

    @Test
    void generateCRUDItemTest() {
        Map<String, Object> inputs = [
            item:            'TestItem',
            rootDir:         'src/test',
            resourceRoot:    'src/test/resources/org/cristalise/dsl/test/resources/',
            moduleName:      'DSL Test',
            moduleNs:        'dsl', 
            version:         0,
            moduleXmlDir:    'src/test/resources/META-INF/cristal',
            appPackage:      'org.cristalise.dsl.test',
            resourceURL:     'org/cristalise/dsl/test/resources/',
            useConstructor:  false,
            isAgent:         false,
            generatedName:   false,
            moduleFiles:     ['Factory.groovy', 'State.groovy', 'TestItem.groovy']
        ]

        new CRUDGenerator().generate(inputs, false, true)

        inputs.with {
            item = 'TestItemUseConstructor'
            useConstructor = true
            ((List)moduleFiles).add('TestItemUseConstructor.groovy')
        }

        new CRUDGenerator().generate(inputs, false, false)

        inputs.with {
            item = 'TestAgentUseConstructor'
            isAgent = true
            ((List)moduleFiles).add('TestAgentUseConstructor.groovy')
        }

        new CRUDGenerator().generate(inputs, false, false)

        inputs.with {
            item = 'TestAgent'
            useConstructor = false
            ((List)moduleFiles).add('TestAgent.groovy')
        }

        new CRUDGenerator().generate(inputs, false, false)

        inputs.with {
            item = 'TestItemGeneratedName'
            isAgent = false
            generatedName = true
            ((List)moduleFiles).add('TestItemGeneratedName.groovy')
        }

        new CRUDGenerator().generate(inputs, false, false)

        inputs.with {
            item = 'TestItemUseConstructorGeneratedName'
            useConstructor = true
            ((List)moduleFiles).add('TestItemUseConstructorGeneratedName.groovy')
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
