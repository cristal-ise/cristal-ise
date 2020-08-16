/**
 * This file is part of the CRISTAL-iSE Development Module.
 * Copyright (c) 2001-2017 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
package org.cristalise.dev.test.scaffold

import org.codehaus.groovy.control.CompilerConfiguration
import org.cristalise.dev.scaffold.CRUDGenerator
import org.junit.Test

import groovy.transform.CompileStatic

@CompileStatic
class CRUDGeneratorTest {

    @Test
    void generateCRUDItemTest() {
        Map<String, Object> inputs = [
            item:            'TestItem',
            rootDir:         'src/test',
            resourceRoot:    'src/test/resources/org/cristalise/devtest/resources/',
            moduleName:      'DEV Scaffold Test module',
            moduleNs:        'devtest', 
            version:         0,
            moduleXmlDir:    'src/test/resources/META-INF/cristal',
            appPackage:      'org.cristalise.devtest',
            resourceURL:     'org/cristalise/devtest/resources/',
            useConstructor:  false,
            isAgent:         false,
            generatedName:   false,
            moduleFiles:     ['TestItem.groovy']
        ]

        new CRUDGenerator().generate(inputs, false)

        inputs.with {
            item = 'TestItemUseConstructor'
            useConstructor = true
            ((List)moduleFiles).add('TestItemUseConstructor.groovy')
        }

        new CRUDGenerator().generate(inputs, false)

        inputs.with {
            item = 'TestAgentUseConstructor'
            isAgent = true
            ((List)moduleFiles).add('TestAgentUseConstructor.groovy')
        }

        new CRUDGenerator().generate(inputs, false)

        inputs.with {
            item = 'TestAgent'
            useConstructor = false
            ((List)moduleFiles).add('TestAgent.groovy')
        }

        new CRUDGenerator().generate(inputs, false)

        inputs.with {
            item = 'TestItemGeneratedName'
            isAgent = false
            generatedName = true
            ((List)moduleFiles).add('TestItemGeneratedName.groovy')
        }

        new CRUDGenerator().generate(inputs, false)

        inputs.with {
            item = 'TestItemUseConstructorGeneratedName'
            useConstructor = true
            ((List)moduleFiles).add('TestItemUseConstructorGeneratedName.groovy')
        }

        new CRUDGenerator().generate(inputs, true)

        CompilerConfiguration cc = new CompilerConfiguration()
        cc.setScriptBaseClass(DelegatingScript.class.getName())

        GroovyShell shell = new GroovyShell(this.class.classLoader, new Binding(), cc)
        def scriptFile = new File(inputs['rootDir'].toString()+'/module/Module.groovy')
        DelegatingScript script = (DelegatingScript) shell.parse(scriptFile)

        script.setDelegate(this)
        script.run()
    }
}
