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
import org.cristalise.dev.dsl.item.CRUDItem
import org.cristalise.dev.scaffold.CRUDGenerator
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.Status
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic @Slf4j
class CRUDGeneratorTest {
    static Git gitRepo
    CRUDGenerator generator

    @BeforeClass
    public static void setup() throws Exception {
        gitRepo = Git.open(new File('..'));
    }

    @Before
    public void before() {
        generator = new CRUDGenerator(
            rootDir:         'src/test',
            resourceRootDir: 'src/test/resources/org/cristalise/devtest/resources/',
            moduleXmlDir:    'src/test/resources/META-INF/cristal',
        )
    }

    @AfterClass
    public static void teardown() {}

    public boolean checkGitStatus() {
        Status gitStatus = gitRepo.status().call()
        if (!gitStatus.isClean()) gitStatus.getModified().each { log.info('checkGitStatus() - changed file:{}', it) }
        return gitStatus.isClean()
    } 

    @Test
    void generateTestItem() throws Exception {
        Map<String, Object> inputs = [
            item:           new CRUDItem(name: 'TestItem'),
            version:        0,
            moduleNs:       'devtest',
            useConstructor: false,
            isAgent:        false,
            generatedName:  false,
            inputFile:      null,
            moduleFiles:    ['TestItem.groovy']
        ]

        generator.generateItemDSL(inputs)
    }

    @Test
    void generateTestItemExcel() throws Exception {
        Map<String, Object> inputs = [
            item:           new CRUDItem(name: 'TestItemExcel'),
            version:        0,
            moduleNs:       'devtest',
            useConstructor: false,
            isAgent:        false,
            generatedName:  false,
            inputFile:      'TestItemExcel.xlsx'
        ]

        generator.generateItemDSL(inputs)
    }

    @Test
    void generateTestItemUseConstructor() throws Exception {
        Map<String, Object> inputs = [
            item:           new CRUDItem(name: 'TestItemUseConstructor'),
            version:        0,
            moduleNs:       'devtest',
            useConstructor: true,
            isAgent:        false,
            generatedName:  false,
            inputFile:      null
        ]

        generator.generateItemDSL(inputs)
    }

    
    @Test
    void generateTestAgentUseConstructor() throws Exception {
        Map<String, Object> inputs = [
            item:           new CRUDItem(name: 'TestAgentUseConstructor'),
            version:        0,
            moduleNs:       'devtest',
            useConstructor: true,
            isAgent:        true,
            generatedName:  false,
            inputFile:      null
        ]

        generator.generateItemDSL(inputs)
    }

    @Test
    void generateTestAgent() throws Exception {
        Map<String, Object> inputs = [
            item:           new CRUDItem(name: 'TestAgent'),
            version:        0,
            moduleNs:       'devtest',
            useConstructor: false,
            isAgent:        true,
            generatedName:  false,
            inputFile:      null
        ]

        generator.generateItemDSL(inputs)
    }

    @Test
    void generateTestItemGeneratedName() throws Exception {
        Map<String, Object> inputs = [
            item:           new CRUDItem(name: 'TestItemGeneratedName'),
            version:        0,
            moduleNs:       'devtest',
            useConstructor: false,
            isAgent:        false,
            generatedName:  true,
            inputFile:      null
        ]

        generator.generateItemDSL(inputs)
    }

    @Test
    void generateTestItemUseConstructorGeneratedName() throws Exception {
        Map<String, Object> inputs = [
            item:           new CRUDItem(name: 'TestItemUseConstructorGeneratedName'),
            version:        0,
            moduleNs:       'devtest',
            useConstructor: true,
            isAgent:        false,
            generatedName:  true,
            inputFile:      null
        ]

        generator.generateItemDSL(inputs)
    }

    @Test
    void generateCRUDModule() throws Exception {
        def generator  = new CRUDGenerator(rootDir: 'src/test')
        def scriptText = new File('src/test/data/CRUDTestModule.groovy').text

        generator.generateCRUDModule(scriptText)
    }

    @Test
    void generateModule() throws Exception {
        Map<String, Object> inputs = [
            moduleName:  'DEV Scaffold Test module',
            version:     0,
            resourceURL: 'org/cristalise/devtest/resources/',
            moduleNs:    'devtest',
            inputFile:   null
        ]

        generator.generateModuleDSL(inputs)

        CompilerConfiguration cc = new CompilerConfiguration()
        cc.setScriptBaseClass(DelegatingScript.class.getName())

        GroovyShell shell = new GroovyShell(this.class.classLoader, new Binding(), cc)
        def scriptFile = new File(inputs['rootDir'].toString()+'/module/Module.groovy')
        DelegatingScript script = (DelegatingScript) shell.parse(scriptFile)

        script.setDelegate(this)
        script.run()

        // assert checkGitStatus()
    }
}
