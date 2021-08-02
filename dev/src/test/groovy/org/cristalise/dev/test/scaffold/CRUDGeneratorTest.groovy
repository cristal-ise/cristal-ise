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
import org.cristalise.kernel.process.Gateway
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.Status
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic @Slf4j
class CRUDGeneratorTest {
    static Git gitRepo

    @BeforeClass
    public static void setup() throws Exception {
        gitRepo = Git.open(new File('../.git'));
    }

    @AfterClass
    public static void teardown() {}

    public boolean getCheckGitStatus() {
        Status gitStatus = gitRepo.status().call()
        //gitStatus.getModified().each { log.info it }
        return gitStatus.isClean()
    } 

    @Test
    void generateCRUDItemTest() throws Exception {
        def generator = new CRUDGenerator(
            rootDir:         'src/test',
            resourceRootDir: 'src/test/resources/org/cristalise/devtest/resources/',
            moduleXmlDir:    'src/test/resources/META-INF/cristal',
        )

        Map<String, Object> inputs = [
            item:           'TestItem',
            version:        0,
            moduleNs:       'devtest',
            useConstructor: false,
            isAgent:        false,
            generatedName:  false,
            inputFile:      null,
            moduleFiles:    ['TestItem.groovy']
        ]

        generator.generateCRUDItem(inputs)

        inputs.with {
            item = 'TestItemExcel'
            inputFile = 'TestItemExcel.xlsx'
            ((List)moduleFiles).add('TestItemExcel.groovy')
        }

        generator.generateCRUDItem(inputs)

        inputs.with {
            inputFile = null
            item = 'TestItemUseConstructor'
            useConstructor = true
            ((List)moduleFiles).add('TestItemUseConstructor.groovy')
        }

        generator.generateCRUDItem(inputs)

        inputs.with {
            item = 'TestAgentUseConstructor'
            isAgent = true
            ((List)moduleFiles).add('TestAgentUseConstructor.groovy')
        }

        generator.generateCRUDItem(inputs)

        inputs.with {
            item = 'TestAgent'
            useConstructor = false
            ((List)moduleFiles).add('TestAgent.groovy')
        }

        generator.generateCRUDItem(inputs, false)

        inputs.with {
            item = 'TestItemGeneratedName'
            isAgent = false
            generatedName = true
            ((List)moduleFiles).add('TestItemGeneratedName.groovy')
        }

        generator.generateCRUDItem(inputs)

        inputs.with {
            moduleName     = 'DEV Scaffold Test module'
            resourceURL    = 'org/cristalise/devtest/resources/'
            item           = 'TestItemUseConstructorGeneratedName'
            useConstructor = true

            ((List)moduleFiles).add('TestItemUseConstructorGeneratedName.groovy')
        }

        generator.generateCRUDItem(inputs, true)

        assert getCheckGitStatus()

        CompilerConfiguration cc = new CompilerConfiguration()
        cc.setScriptBaseClass(DelegatingScript.class.getName())

        GroovyShell shell = new GroovyShell(this.class.classLoader, new Binding(), cc)
        def scriptFile = new File(inputs['rootDir'].toString()+'/module/Module.groovy')
        DelegatingScript script = (DelegatingScript) shell.parse(scriptFile)

        script.setDelegate(this)
        script.run()

        assert getCheckGitStatus()
    }

    @Test
    void generateCRUDModule() throws Exception {
        def generator  = new CRUDGenerator(rootDir: 'src/test')

        generator.genererateCRUDModule(new File('src/test/data/CRUDTestModule.groovy').text)
    }
}
