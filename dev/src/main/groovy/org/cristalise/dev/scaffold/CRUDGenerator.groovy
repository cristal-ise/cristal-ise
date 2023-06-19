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

import static org.cristalise.dsl.SystemProperties.DSL_Module_BindingConvention_variablePrefix

import org.apache.commons.lang3.StringUtils
import org.cristalise.dev.dsl.item.CRUDItem
import org.cristalise.dev.dsl.module.CRUDModuleDelegate
import org.cristalise.dsl.SystemProperties
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.process.resource.BuiltInResources
import org.cristalise.kernel.utils.FileStringUtility
import org.mvel2.integration.impl.MapVariableResolverFactory
import org.mvel2.templates.CompiledTemplate
import org.mvel2.templates.SimpleTemplateRegistry
import org.mvel2.templates.TemplateCompiler
import org.mvel2.templates.TemplateRegistry
import org.mvel2.templates.TemplateRuntime

import groovy.cli.picocli.CliBuilder
import groovy.cli.picocli.OptionAccessor
import groovy.io.FileType
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

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
        'item_aggregate_groovy.tmpl',
        'item_dependencies_groovy.tmpl',
        'item_groovy.tmpl',
        'item_queryList_groovy.tmpl',
        'module_groovy.tmpl',
        'commonDefs_groovy.tmpl',
        'item_toolbarMain_json.tmpl',
        'item_itemCollection_json.tmpl',
        'item_itemCollectionList_json.tmpl',
        'item_toolbarCollection_json.tmpl',
        'resourcesMenu_json.tmpl',
        'lookupNames_json.tmpl',
        'item_masterOutcomeView_json.tmpl',
        'item_itemList_json.tmpl',
    ]

    static final String templateRoot = '/org/cristalise/dev/resources/templates/'

    TemplateRegistry templateRegistry = new SimpleTemplateRegistry()

    String rootDir
    String resourceRootDir
    String moduleXmlDir

    String webuiDir

    public CRUDGenerator(Map<String, String> args) {
        assert args && args.rootDir, 'Please specify rootDir'

        rootDir         = args.rootDir
        resourceRootDir = args?.resourceRootDir
        moduleXmlDir    = args?.moduleXmlDir
        webuiDir        = args?.webuiDir

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
        String prefix = DSL_Module_BindingConvention_variablePrefix.getString()

        inputs.rootDir = rootDir
        inputs.prefix = prefix
        if (inputs.item) {
            inputs.itemVar = prefix + StringUtils.uncapitalize(inputs.item as String)
            inputs.itemPackage = StringUtils.uncapitalize(inputs.item as String)
        }
        inputs.resourceRootDir = resourceRootDir
        if (moduleXmlDir) inputs.moduleXmlDir = moduleXmlDir

        def packageDir = inputs.rootPackage.toString().replace('.', '/')
        inputs.resourceURL = "${packageDir}/resources/"

        if (!inputs.containsKey('generateProperty')) inputs.generateProperty = false
    }

    /**
     * Triggers the generation of the DSL files of an Item based on MVEL2 templates.
     * Module.groovy is not generated by this method.
     * 
     * @param inputs the inputs to the MVEL2 templates
     */
    public void generateItemDSL(Map<String, Object> inputs) {
        setInputs(inputs)

        assert inputs['item'], "Specify input called 'item'"
        def item = (CRUDItem) inputs['item']

        if (inputs['generatedName']) {
            if (!inputs['idPrefix'])    inputs['idPrefix']    = 'ID'
            if (!inputs['leftPadSize']) inputs['leftPadSize'] = '6'
        }

        new FileTreeBuilder(new File(resourceRootDir)).dir('boot') {
            for (def res : BuiltInResources.values()) {
                dir(res.getTypeCode())
            }
        }

        def packageDir = inputs.rootPackage.toString().replace('.', '/')

        def itemDir = new File("${rootDir}/module/${packageDir}/${item.name.uncapitalize()}")
        def scriptDir = new File("${itemDir}/script")

        scriptDir.mkdirs()

        new File(itemDir,   "${item.name}.groovy")          .write(mvelGenerate('item_groovy.tmpl',           inputs));
        new File(scriptDir, "${item.name}_Aggregate.groovy").write(mvelGenerate('item_aggregate_groovy.tmpl', inputs));
        new File(scriptDir, "${item.name}_QueryList.groovy").write(mvelGenerate('item_queryList_groovy.tmpl', inputs));
    }

    /**
     * 
     * @param menu
     * @param lookupNames
     * @param inputs
     */
    public void generateWebuiConfigs(JsonArray menu, JsonArray lookupNames, Map<String, Object> inputs) {
        setInputs(inputs)

        assert inputs['item'], "Specify input called 'item'"
        def item = (CRUDItem) inputs['item']

        menu.add(       new JsonObject(mvelGenerate('resourcesMenu_json.tmpl', inputs)))
        lookupNames.add(new JsonObject(mvelGenerate('lookupNames_json.tmpl', inputs)))

        def configRootDir = new File(webuiDir ?: rootDir+'/module/webui')
        configRootDir.mkdirs()
        new File(configRootDir, 'lookupNames.json')  .write(new JsonObject().put('column', lookupNames).encodePrettily())
        new File(configRootDir, 'resourcesMenu.json').write(new JsonObject().put('column', menu       ).encodePrettily())

        def configItemDir = new File("${configRootDir.getPath()}/${item.name}")
        configItemDir.mkdirs()
        JsonObject toolbarMain       = new JsonObject(mvelGenerate('item_toolbarMain_json.tmpl', inputs))
        JsonObject masterOutcomeView = new JsonObject(mvelGenerate('item_masterOutcomeView_json.tmpl', inputs))
        JsonObject itemList          = new JsonObject(mvelGenerate('item_itemList_json.tmpl', inputs))
        JsonArray  itemCollection    = new JsonArray (mvelGenerate('item_itemCollection_json.tmpl', inputs))

        new File(configItemDir, 'toolbarMain.json')      .write(toolbarMain.encodePrettily())
        new File(configItemDir, 'masterOutcomeView.json').write(masterOutcomeView.encodePrettily())
        new File(configItemDir, 'itemList.json')         .write(masterOutcomeView.encodePrettily())
        new File(configItemDir, 'itemCollection.json')   .write(itemCollection.encodePrettily())

        item.dependencies.values().each { currentDependency ->
            inputs.put('currentDependency', currentDependency)
            JsonObject toolbarCollection  = new JsonObject(mvelGenerate('item_toolbarCollection_json.tmpl', inputs))
            JsonObject itemCollectionList = new JsonObject(mvelGenerate('item_itemCollectionList_json.tmpl', inputs))
            new File(configItemDir, "toolbar${currentDependency.name}.json").write(toolbarCollection.encodePrettily())
            new File(configItemDir, "itemCollection${currentDependency.name}.json").write(itemCollectionList.encodePrettily())
        }
    }

    private boolean checkIncludeRequiredInModule(File file) {
        if (file.name.endsWith('.groovy')) {
            if (file.path.contains('/script') || ['Module.groovy', 'CommonDefs.groovy'].contains(file.name)) {
                return false
            }
            else {
                return true
            }
        }
        return false
    }

    /**
     * 
     * @param inputs
     * @return
     */
    public generateModuleDSL(Map<String, Object> inputs) {
        setInputs(inputs)

        def packageDir = inputs.rootPackage.toString().replace('.', '/')
        def moduleDir = new File("${rootDir}/module/${packageDir}")

        moduleDir.mkdirs()

        if (!inputs['moduleFiles']) {
            inputs['moduleFiles'] = []

            moduleDir.eachFileRecurse(FileType.FILES) { file ->
                if (checkIncludeRequiredInModule(file)) {
                    ((List)inputs['moduleFiles']).add(file.name)
                }
            }
            ((List)inputs['moduleFiles']).sort()
        }

        log.info('generateModuleDSL() - files:{}', inputs['moduleFiles'])

        new File(moduleDir, 'CommonDefs.groovy').write(mvelGenerate('commonDefs_groovy.tmpl', inputs));
        new File(moduleDir, 'Module.groovy')    .write(mvelGenerate('module_groovy.tmpl',     inputs));

        if (inputs.puml ) new File(moduleDir, "Module.puml").write(inputs.puml as String)
    }

    /**
     * Generates a String based on the MVEL2 template
     * 
     * @param templName MVEL2 template to be used, shall be available on the classpath
     * @param inputs needed for the generation
     * @return generated String
     */
    private String mvelGenerate(String templName, Map inputs) {
        CompiledTemplate expr = templateRegistry.getNamedTemplate(templName)
        return (String)TemplateRuntime.execute(expr, null, new MapVariableResolverFactory(inputs), templateRegistry);
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

        JsonArray menu = new JsonArray()
        JsonArray lookupNames = new JsonArray()

        crudModule.items.values().each { def item ->
            log.info('generateCRUDModule() - generating item:{}', item.name)

            def inputs = [
                item:           item,
                version:        0,
                moduleNs:       crudModule.namespace,
                rootPackage:    crudModule.rootPackage,
                useConstructor: false,
                isAgent:        false,
                generatedName:  false,
                inputFile:      null
            ]

            generateItemDSL(inputs)

            if (crudModule.webuiConfigs) generateWebuiConfigs(menu, lookupNames, inputs)
        }

        if (crudModule.generateModule) {
            def inputs = [
                moduleName:   crudModule.name,
                version:      0,
                rootPackage:  crudModule.rootPackage,
                moduleNs:     crudModule.namespace,
                inputFile:    null,
                moduleXmlDir: null,
                puml:         crudModule.plantUml,
            ]

            generateModuleDSL(inputs)
        }
    }

    @CompileDynamic
    public static void main(String[] args) {
        def cli = new CliBuilder(usage: 'CrudGenerator -[options] [excelfile]')
        cli.width = 100

        cli.with {
            h longOpt: 'help', 'Show usage information'
            r longOpt: 'rootDir',    args: 1, argName: 'root',  'Root directory'
            w longOpt: 'webuiDir',   args: 1, argName: 'webui', 'Directory for webui config files'
            t longOpt: 'itemTypes',  args: 1, argName: 'types', 'Comma separated list of Item types (e.g. Site, Product)'
            m longOpt: 'moduleFile', args: 1, argName: 'file',  'File containing the definition of CRUD Items'
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

            def generator  = new CRUDGenerator(rootDir: rootDir, webuiDir: (options.w ? (String)options.w : null))

            generator.generateCRUDModule(scriptText)
        }
        else {
            println "Please provide itemTypes or moduleFile"
            cli.usage()
            return
        }
    }
}
