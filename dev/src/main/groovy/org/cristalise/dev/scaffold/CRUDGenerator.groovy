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
package org.cristalise.dev.scaffold

import org.cristalise.kernel.process.resource.BuiltInResources
import org.cristalise.kernel.utils.FileStringUtility
import org.mvel2.templates.CompiledTemplate
import org.mvel2.templates.TemplateCompiler
import org.mvel2.templates.TemplateRuntime

import groovy.cli.commons.CliBuilder
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

/**
 * Class generating the following DSL files. 
 *
 * <pre>
 * - ${root}/module/${item}.groovy
 * - ${root}/module/script/${item}_Aggregate.groovy
 * - ${root}/module/script/${item}_QueryList.groovy
 * - ${root}/${resourceRoot}/CA/${item}_Workflow_0,xml
 * - ${root}/module/Module.groovy (optional)
 * </pre>
 * 
 * Inputs could be like these using the groovy map literal:
 * 
 * <pre>
 * [rootDir: 'src/test', moduleName: 'Test Module', moduleNs: 'testns', moduleVersion: 0, item: 'TestItem', useConstructor: false]
 * </pre>
 */
@CompileStatic
class CRUDGenerator {
    
    static final String templateRoot = '/org/cristalise/dev/resources/templates/'

    String rootDir
    String resourceRootDir
    String moduleXmlDir

    public CRUDGenerator(Map<String, String> args) {
        assert args && args.rootDir, 'Please specify rootDir'

        rootDir         = args.rootDir
        resourceRootDir = args?.resourceRootDir
        moduleXmlDir    = args?.moduleXmlDir

        if (!resourceRootDir) resourceRootDir = "${rootDir}/resources"
    }

    /**
     * Trigger the generation of the DSL files. It is based on MVEL2 templating. Module.groovy is not generated.
     * 
     * @param inputs the inputs to the MVEL2 templates
     * @param itemSpecificFactoryWf whether generate an Item specific Factory workflow or not
     */
    public void generate(Map<String, Object> inputs) {
        generate(inputs, false)
    }

    /**
     * Trigger the generation of the DSL files. It is based on MVEL2 templating.
     * 
     * @param inputs the inputs to the MVEL2 templates
     * @param generateModule whether the Module.groovy file should be generated or not
     * @param itemSpecificFactoryWf whether generate an Item specific Factory workflow or not
     */
    public void generate(Map<String, Object> inputs, boolean generateModule) {
        assert inputs

        inputs.rootDir = rootDir
        inputs.resourceRootDir = resourceRootDir
        if (moduleXmlDir) inputs.moduleXmlDir = moduleXmlDir

        new FileTreeBuilder(new File(resourceRootDir)).dir('boot') {
            for (def res : BuiltInResources.values()) {
                dir(res.getTypeCode())
            }
        }

        def moduleDir   = new File("${rootDir}/module")
        def scriptDir   = new File("${rootDir}/module/script")
        def workflowDir = new File("${resourceRootDir}/boot/CA")

        checkAndSetInputs(inputs)

        generateDSL(new File(moduleDir,   "${inputs.item}.groovy"),           'item_groovy.tmpl',           inputs)
        generateDSL(new File(scriptDir,   "${inputs.item}_Aggregate.groovy"), 'item_aggregate_groovy.tmpl', inputs)
        generateDSL(new File(scriptDir,   "${inputs.item}_QueryList.groovy"), 'item_queryList_groovy.tmpl', inputs)
        generateDSL(new File(workflowDir, "${inputs.item}_Workflow_0.xml"),   'item_workflow_xml.tmpl',     inputs)

        if (generateModule) generateModuleFiles(inputs, moduleDir)
    }

    private generateModuleFiles(Map<String, Object> inputs, File moduleDir) {
        if (!inputs['moduleFiles']) {
            inputs['moduleFiles'] = []

            moduleDir.eachFileMatch(~/.*.groovy/) { file ->
                if (file.name != 'Module.groovy') ((List)inputs['moduleFiles']).add(file.name)
            }
        }

        generateDSL(new File(moduleDir, 'Module.groovy'), 'module_groovy.tmpl', inputs)
    }

    private void checkAndSetInputs(Map inputs) {
        assert inputs['item'], "Specify input called 'item'"

        if(inputs['generatedName']) {
            if (!inputs['idPrefix'])    inputs['idPrefix'] = 'ID'
            if (!inputs['leftPadSize']) inputs['leftPadSize'] = '6'
        }
    }

    /**
     * Generates and saves a single DSL file
     * 
     * @param file the File instance to be saved
     * @param templName the MVEL" template to be used, available on the classpath
     * @param vars the inputs needed for the generation
     */
    private void generateDSL(File file, String templName, Map vars) {
        String templ = FileStringUtility.url2String(this.getClass().getResource(templateRoot + templName))
        CompiledTemplate expr = TemplateCompiler.compileTemplate(templ);
        file.write((String) TemplateRuntime.execute(expr, vars))
    }

    @CompileDynamic
    public static void main(String[] args) {
        def cli = new CliBuilder(usage: 'CrudGenerator -[rtnh] [excelfile]')
        cli.width = 100

        cli.with {
            h longOpt: 'help', 'Show usage information'
            r longOpt: 'rootDir',   args: 1, argName: 'root',  'Root directory'
            t longOpt: 'itemTypes', args: 1, argName: 'types', 'Comma separated list of Item types'
            n longOpt: 'moduleNs',  args: 1, argName: 'ns',    'Module namespace'
        }

        def options = cli.parse(args)

        // Show usage text when error or -h or --help option is used.
        if (!args || !options || options.h) {
            cli.usage(); return
        }

        if (!options.r) {
            println "Please provide the root directory"
            cli.usage()
            return
        }

        if (!options.t) {
            println "Please provide the comma separated list of Item types (i.e. Site, Product)"
            cli.usage()
            return
        }

        if (!options.n) {
            println "Please provide the namespace (i.e. limsdemo)"
            cli.usage()
            return
        }

        if (!options.arguments()) {
            println "Please provide input csv/excel file"
            cli.usage()
            return
        }

        def rootDir   = (String)options.r
        def items     = (String)options.t
        def ns        = (String)options.n
        def inputFile = options.arguments()[0]

        def generator = new CRUDGenerator(rootDir: rootDir)

        items.split(',').each { item ->
            Map<String, Object> inputs = [
                item:           item.trim(),
                version:        0,
                moduleNs:       ns,
                useConstructor: false,
                isAgent:        false,
                generatedName:  false,
                inputFile:      inputFile
            ]

            generator.generate(inputs)
        }
    }
}
