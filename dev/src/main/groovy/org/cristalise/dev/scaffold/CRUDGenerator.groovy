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

import org.apache.commons.lang3.StringUtils
import org.cristalise.dev.dsl.item.CRUDItem
import org.cristalise.dev.dsl.module.CRUDModuleDelegate
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.process.resource.BuiltInResources
import org.cristalise.kernel.utils.FileStringUtility
import org.mvel2.integration.VariableResolverFactory
import org.mvel2.integration.impl.MapVariableResolverFactory
import org.mvel2.templates.CompiledTemplate
import org.mvel2.templates.SimpleTemplateRegistry
import org.mvel2.templates.TemplateCompiler
import org.mvel2.templates.TemplateRegistry
import org.mvel2.templates.TemplateRuntime

import groovy.cli.commons.CliBuilder
import groovy.cli.commons.OptionAccessor
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * Class generating the following DSL files. 
 *
 * <pre>
 * - ${root}/module/${item}.groovy
 * - ${root}/module/script/${item}_Aggregate.groovy
 * - ${root}/module/script/${item}_QueryList.groovy
 * - ${root}/module/Module.groovy (optional)
 * </pre>
 * 
 * Inputs could be like these using the groovy map literal:
 * <pre>
 * [
 *   rootDir: 'src/test',
 *   moduleName: 'Test Module',
 *   moduleNs: 'testns',
 *   moduleVersion: 0,
 *   item: crudItem,
 *   useConstructor: false
 * ]
 * </pre>
 */
@CompileStatic @Slf4j
class CRUDGenerator {

    static List<String> templates = [
        'item_addToDependency_groovy.tmpl',
        'item_aggregate_groovy.tmpl',
        'item_dependencies_groovy.tmpl',
        'item_groovy.tmpl',
        'item_queryList_groovy.tmpl',
        'item_removeFromDependency_groovy.tmpl',
        'module_groovy.tmpl'
    ]

    static final String templateRoot = '/org/cristalise/dev/resources/templates/'

    TemplateRegistry templateRegistry = new SimpleTemplateRegistry()

    String rootDir
    String resourceRootDir
    String moduleXmlDir

    public CRUDGenerator(Map<String, String> args) {
        assert args && args.rootDir, 'Please specify rootDir'

        rootDir         = args.rootDir
        resourceRootDir = args?.resourceRootDir
        moduleXmlDir    = args?.moduleXmlDir

        if (!resourceRootDir) resourceRootDir = "${rootDir}/resources"

        templates.each { templName ->
            log.debug('compiling MVEL template:{}', templName)

            String templStr = FileStringUtility.url2String(this.getClass().getResource(templateRoot + templName))
            CompiledTemplate expr = TemplateCompiler.compileTemplate(templStr);

            if (expr) templateRegistry.addNamedTemplate(templName, expr)
            else      log.error('ctor() -could not compile MVEL template:{}', templName)
        }
    }

    private setInputs(Map inputs) {
        assert inputs

        //String prefix = BindingConvention.variablePrefix -- DOES NOT WORK!??!?
        String prefix = Gateway.getProperties().getString('DSL.Module.BindingConvention.variablePrefix', '$')

        inputs.rootDir = rootDir
        inputs.prefix = prefix
        if (inputs.item) inputs.itemVar = prefix + StringUtils.uncapitalize(inputs.item as String)
        inputs.resourceRootDir = resourceRootDir
        if (moduleXmlDir) inputs.moduleXmlDir = moduleXmlDir

        if (!inputs.containsKey('generateProperty')) inputs.generateProperty = false
        if (!inputs.containsKey('useCrudDependency')) inputs.useCrudDependency = false
    }

    /**
     * Trigger the generation of the DSL files. It is based on MVEL2 templating. Module.groovy is not generated.
     * 
     * @param inputs the inputs to the MVEL2 templates
     */
    public void generateItemDSL(Map<String, Object> inputs) {
        setInputs(inputs)

        assert inputs['item'], "Specify input called 'item'"
        def item = (CRUDItem) inputs['item']

        if (inputs['generatedName']) {
            if (!inputs['idPrefix'])    inputs['idPrefix'] = 'ID'
            if (!inputs['leftPadSize']) inputs['leftPadSize'] = '6'
        }

        new FileTreeBuilder(new File(resourceRootDir)).dir('boot') {
            for (def res : BuiltInResources.values()) {
                dir(res.getTypeCode())
            }
        }

        def moduleDir = new File("${rootDir}/module")
        def scriptDir = new File("${rootDir}/module/script")

        generateDSL(new File(moduleDir, "${item.name}.groovy"),           'item_groovy.tmpl',           inputs)
        generateDSL(new File(scriptDir, "${item.name}_Aggregate.groovy"), 'item_aggregate_groovy.tmpl', inputs)
        generateDSL(new File(scriptDir, "${item.name}_QueryList.groovy"), 'item_queryList_groovy.tmpl', inputs)

        item.dependencies.each { name, dependency ->
            if (dependency.originator) {
                inputs['currentDependency'] = dependency
                def scriptFile = new File(scriptDir, "${item.name}_AddTo${dependency.name}.groovy")
                generateDSL(scriptFile, 'item_addToDependency_groovy.tmpl', inputs)
                scriptFile = new File(scriptDir, "${item.name}_RemoveFrom${dependency.name}.groovy")
                generateDSL(scriptFile, 'item_removeFromDependency_groovy.tmpl', inputs)
            }
        }
    }

    /**
     * 
     * @param inputs
     * @return
     */
    public generateModuleDSL(Map<String, Object> inputs) {
        setInputs(inputs)

        def moduleDir = new File("${rootDir}/module")

        if (!inputs['moduleFiles']) {
            inputs['moduleFiles'] = []

            moduleDir.eachFileMatch(~/.*.groovy/) { file ->
                if (file.name != 'Module.groovy') ((List)inputs['moduleFiles']).add(file.name)
            }
            ((List)inputs['moduleFiles']).sort()
        }

        log.info('generateModuleDSL() - files:{}', inputs['moduleFiles'])

        generateDSL(new File(moduleDir, 'Module.groovy'), 'module_groovy.tmpl', inputs)
    }

    /**
     * Generates and saves a single DSL file
     * 
     * @param file the File instance to be saved
     * @param templName the MVEL" template to be used, available on the classpath
     * @param vars the inputs needed for the generation
     */
    private void generateDSL(File file, String templName, Map vars) {
        CompiledTemplate expr = templateRegistry.getNamedTemplate(templName)
        file.write((String)TemplateRuntime.execute(expr, null, new MapVariableResolverFactory(vars), templateRegistry))
    }

    @CompileDynamic
    private static void genererateTypes(CliBuilder cli, OptionAccessor options) {
        if (!options.n) {
            println "Please provide the namespace (e.g. integTest)"
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
        def isAgent   = options.arguments() as Boolean

        def generator = new CRUDGenerator(rootDir: rootDir)

        items.split(',').each { itemType ->
            def item = new CRUDItem(itemType.trim())
            log.info('genererateTypes() - generating item:{}', item.name)

            Map<String, Object> inputs = [
                item:           item,
                version:        0,
                moduleNs:       ns,
                useConstructor: false,
                isAgent:        isAgent,
                generatedName:  false,
                inputFile:      inputFile
            ]

            generator.generateItemDSL(inputs)
        }
    }

    public void generateCRUDModule(String scriptText) {
        def crudModule = new CRUDModuleDelegate(null).processText(scriptText)
        assert crudModule

        crudModule.items.values().each { def item ->
            log.info('generateCRUDModule() - generating item:{}', item.name)

            def inputs = [
                item:           item,
                version:        0,
                moduleNs:       crudModule.namespace,
                useConstructor: false,
                isAgent:        false,
                generatedName:  false,
                inputFile:      null
            ]

            generateItemDSL(inputs as Map)
        }

        if (crudModule.generateDSL) {
            def inputs = [
                moduleName:   crudModule.name,
                version:      0,
                resourceURL:  crudModule.resourceURL,
                moduleNs:     crudModule.namespace,
                inputFile:    null,
                moduleXmlDir: null
            ]

            generateModuleDSL(inputs)
        }
    }

    @CompileDynamic
    public static void main(String[] args) {
        def cli = new CliBuilder(usage: 'CrudGenerator -[rtnh] [excelfile]')
        cli.width = 100

        cli.with {
            h longOpt: 'help', 'Show usage information'
            r longOpt: 'rootDir',    args: 1, argName: 'root',  'Root directory'
            t longOpt: 'itemTypes',  args: 1, argName: 'types', 'Comma separated list of Item types (e.g. Site, Product)'
            m longOpt: 'moduleFile', args: 1, argName: 'file',  'Files containing the definition of crud Items'
            n longOpt: 'moduleNs',   args: 1, argName: 'ns',    'Module namespace'
            a longOpt: 'agent',                                 'Generated Item(s) is an Agent'
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

        if (options.t && options.m) {
            println "Please provide either itemTypes or moduleFile"
            cli.usage()
            return
        }

        if (options.t) {
            genererateTypes(cli, options)
        }
        else if (options.m) {
            def rootDir    = (String)options.r
            def moduleFile = (String)options.m
            def scriptText = new File(moduleFile).text
    
            def generator  = new CRUDGenerator(rootDir: rootDir)

            generator.generateCRUDModule(scriptText)
        }
        else {
            println "Please provide itemTypes or moduleFile"
            cli.usage()
            return
        }
    }
}
