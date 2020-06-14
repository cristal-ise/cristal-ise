/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
package org.cristalise.dsl.module

import static org.cristalise.kernel.process.resource.BuiltInResources.PROPERTY_DESC_RESOURCE

import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.codehaus.groovy.control.CompilerConfiguration
import org.cristalise.dsl.entity.AgentBuilder
import org.cristalise.dsl.entity.ItemBuilder
import org.cristalise.dsl.entity.RoleBuilder
import org.cristalise.dsl.lifecycle.definition.CompActDefBuilder
import org.cristalise.dsl.lifecycle.definition.ElemActDefBuilder
import org.cristalise.dsl.lifecycle.stateMachine.StateMachineBuilder
import org.cristalise.dsl.persistency.outcome.SchemaBuilder
import org.cristalise.dsl.property.PropertyDescriptionBuilder
import org.cristalise.dsl.querying.QueryBuilder
import org.cristalise.dsl.scripting.ScriptBuilder
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.entity.imports.ImportAgent
import org.cristalise.kernel.entity.imports.ImportItem
import org.cristalise.kernel.entity.imports.ImportRole
import org.cristalise.kernel.graph.layout.DefaultGraphLayoutGenerator
import org.cristalise.kernel.lifecycle.ActivityDef
import org.cristalise.kernel.lifecycle.CompositeActivityDef
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine
import org.cristalise.kernel.persistency.outcome.Schema
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.process.module.*
import org.cristalise.kernel.process.resource.BuiltInResources
import org.cristalise.kernel.property.PropertyDescriptionList
import org.cristalise.kernel.querying.Query
import org.cristalise.kernel.scripting.Script
import org.cristalise.kernel.test.utils.KernelXMLUtility
import org.cristalise.kernel.utils.FileStringUtility
import org.cristalise.kernel.utils.LocalObjectLoader

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import groovy.xml.XmlUtil

/**
 *
 */
@CompileStatic @Slf4j
class ModuleDelegate {

    Module module = null
    Module newModule = null
    Binding bindings

    String resourceRoot = 'src/main/resources'
    String exportRoot   = 'src/main/script/'
    String moduleDir    = 'src/main/script/'
    String moduleXmlDir = null

    private File resourceBootDir = null
    private File moduleXMLFile = null

    public ModuleDelegate(Map<String, Object> args) {
        assert args.ns && args.name && args.version != null

        newModule = new Module()
        newModule.info = new ModuleInfo()
        newModule.ns = args.ns
        newModule.name = args.name
        newModule.info.version = args.version

        if (args.bindings) bindings = (Binding) args.bindings
        else               bindings = new Binding()

        if (args.resourceRoot) resourceRoot = args.resourceRoot
        if (args.exportRoot)   exportRoot   = args.exportRoot
        if (args.moduleDir)    moduleDir    = args.moduleDir
        if (args.moduleXmlDir) moduleXmlDir = args.moduleXmlDir

        if (!moduleXmlDir) moduleXmlDir = resourceRoot

        moduleXMLFile = new File("$moduleXmlDir/module.xml")

        if (moduleXMLFile.exists()) {
            module = (Module) Gateway.getMarshaller().unmarshall(moduleXMLFile.text)
            assert module.ns == newModule.ns
            assert module.name == newModule.name
        }

        new FileTreeBuilder(new File(resourceRoot)).dir('boot') {
            for (def res : BuiltInResources.values()) {
                dir(res.getTypeCode())
            }
        }
        resourceBootDir = new File("$resourceRoot/boot")
    }

    public ModuleDelegate(String ns, String n, int v, String resRoot, String expRoot, String modDir, Binding b = null) {
        this('ns': ns, 'name': n, 'version': v, 'resourceRoot': resRoot, 'exportRoot': expRoot, 'moduleDir': modDir, 'bindings': b)
    }

    public ModuleDelegate(String ns, String n, int v, Binding b = null) {
        this('ns': ns, 'name': n, 'version': v, 'bindings': b)
    }

    public include(String scriptFile) {
        CompilerConfiguration cc = new CompilerConfiguration()
        cc.setScriptBaseClass(DelegatingScript.class.getName())

        GroovyShell shell = new GroovyShell(this.class.classLoader, bindings, cc)
        DelegatingScript script = (DelegatingScript) shell.parse(new File(scriptFile))

        script.setDelegate(this)
        script.run()
    }

    public Schema Schema(String name, Integer version) {
        def schema = LocalObjectLoader.getSchema(name, version)
        addSchema(schema)
        return schema
    }

    public Schema Schema(String name, Integer version, Closure cl) {
        def schema = SchemaBuilder.build(name, version, cl)
        schema.export(null, resourceBootDir, true)
        addSchema(schema)
        return schema
    }

    public Schema Schema(String name, Integer version, File file) {
        def schema = SchemaBuilder.build(name, version, file)
        schema.export(null, resourceBootDir, true)
        addSchema(schema)
        return schema
    }

    public Query Query(String name, Integer version) {
        def query = LocalObjectLoader.getQuery(name, version)
        addQuery(query)
        return query
    }

    public Query Query(String name, Integer version, Closure cl) {
        def query = QueryBuilder.build(newModule.name, name, version, cl)
        query.export(null, resourceBootDir, true)
        addQuery(query)
        return query
    }

    public Script Script(String name, Integer version) {
        def script = LocalObjectLoader.getScript(name, version)
        addScript(script)
        return script
    }

    public Script Script(String name, Integer version, Closure cl) {
        def script = ScriptBuilder.build(name, version, cl)
        script.export(null, resourceBootDir, true)
        addScript(script)
        return script
    }

    public StateMachine StateMachine(String name, Integer version) {
        def sm = LocalObjectLoader.getStateMachine(name, version)
        addStateMachine(sm)
        return sm
    }

    public StateMachine StateMachine(String name, Integer version, Closure cl) {
        def sm = StateMachineBuilder.build("", name, version, cl).sm
        sm.export(null, resourceBootDir, true)
        addStateMachine(sm)
        return sm
    }

    public ActivityDef Activity(String name, Integer version) {
        def eaDef = LocalObjectLoader.getActDef(name, version)
        addActivityDef(eaDef)
        return eaDef
    }

    public ActivityDef Activity(String name, Integer version, Closure cl) {
        def eaDef = ElemActDefBuilder.build(name, version, cl)
        eaDef.export(null, resourceBootDir, true)
        addActivityDef(eaDef)
        return eaDef
    }

    public CompositeActivityDef Workflow(String name, Integer version) {
        def caDef = LocalObjectLoader.getCompActDef(name, version)
        addCompositeActivityDef(caDef)
        return caDef
    }

    /**
     * 
     * @param name
     * @param version
     * @param cl
     * @return
     */
    public CompositeActivityDef Workflow(String name, Integer version, Closure cl) {
        return Workflow(name: name, version: version, generate: false, cl)
    }

    /**
     * Enable export if workflow needs to be generated, e.g. caDef.export(imports, new File(exportRoot), true)
     * 
     * @param args
     * @param cl
     * @return
     */
    public CompositeActivityDef Workflow(Map args, Closure cl) {
        def caDef = CompActDefBuilder.build((String)args.name, (Integer)args.version, cl)

        if (args?.generate) {
            DefaultGraphLayoutGenerator.layoutGraph(caDef.childrenGraphModel)
            caDef.export(null, resourceBootDir, true)
        }

        addCompositeActivityDef(caDef)
        return caDef
    }

    /**
     * Generates xml files for the define property description values.
     * @param cl
     */
    public void PropertyDescriptionList(Closure cl) {
        def propDescList = PropertyDescriptionBuilder.build(cl)
        def type = propDescList.list.find { it.isClassIdentifier && it.name == 'Type' }

        FileStringUtility.string2File(
            new File(new File(resourceBootDir.path+"/"+PROPERTY_DESC_RESOURCE.typeCode), "${type.defaultValue}.xml"),
            XmlUtil.serialize(Gateway.getMarshaller().marshall(propDescList))
        )
    }

    public PropertyDescriptionList PropertyDescriptionList(String name, Integer version, Closure cl) {
        def propDescList = PropertyDescriptionBuilder.build(name, version, cl)
        propDescList.export(null, resourceBootDir, true)
        addPropertyDescriptionList(propDescList)

        return propDescList
    }

    /**
     * Collects agent and add to module.xml, or update the definition it is already existing.
     * @param name
     * @param password
     * @param cl
     */
    public ImportAgent Agent(Map args, Closure cl) {
        def agent = AgentBuilder.build(args, cl)
        agent.roles.each { it.jobList = null }

        if (Gateway.getProperties().getBoolean('DSL.Module.generateAllResourceItems', true)) {
            agent.export(null, resourceBootDir, true)
            addImportAgent(agent)
        }
        else {
            //Original functionality: XML of ImportAgent is added to the module.xml
            updateImports(agent)
        }

        return agent
    }

    /**
     * Collects items define in the groovy scripts and add to module.xml
     * or update the definition it is already existing.
     * @param args
     * @param cl
     */
    public ImportItem Item(Map args, Closure cl) {
        def item = ItemBuilder.build(args, cl)
        item.properties.removeAll { it.value == args.name }

        if (Gateway.getProperties().getBoolean('DSL.Module.generateAllResourceItems', true)) {
            item.export(null, resourceBootDir, true)
            addImportItem(item)
        }
        else {
            //Original functionality: XML of ImportItem is added to the module.xml
            updateImports(item)
        }

        return item
    }

    /**
     * Collects define roles and add/update on module.xml.
     * @param cl
     */
    public List<ImportRole> Roles(Closure cl) {
        def importRoles = RoleBuilder.build(cl)

        importRoles.each { role ->
            if (Gateway.getProperties().getBoolean('DSL.Module.generateAllResourceItems', true)) {
                role.export(null, resourceBootDir, true)
                addImportRole(role)
            }
            else {
                //Original functionality: XML of ImportRole is added to the module.xml
                updateImports(role)
            }
        }

        return importRoles
    }

    /**
     * Collects define config and add/update on module.xml.
     * @param attr
     */
    public void Config(Map attr) {
        def config = new ModuleConfig((String) attr.name, (String) attr.value, (String) attr.target ? (String) attr.target : null)
        newModule.config.add(config)
    }

    /**
     * Collects define info and add/update on module.xml.
     * @param attr
     */
    public void Info(Map attr, Closure<String[]> cl) {
        assert attr

        if (!newModule.info) newModule.info = new ModuleInfo()

        if (attr.description) newModule.info.desc          = (String) attr.description
        if (attr.version)     newModule.info.version       = (String) attr.version
        if (attr.kernel)      newModule.info.kernelVersion = (String) attr.kernel

        if (cl) {
            def dependencies = cl()

            if (dependencies) newModule.info.dependency.addAll(dependencies)
        }
    }

    /**
     * Sets the module's resourceUrl value.
     * @param url
     * @return
     */
    public Url(String url){
        assert url
        newModule.resURL = url
    }

    public void processClosure(Closure cl) {
        assert cl
        assert newModule.name

        cl.delegate = this
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()

        if (Gateway.properties.getBoolean('DSL.GenerateModuleXml', true)) {
            def oldModuleXML = XmlUtil.serialize(Gateway.getMarshaller().marshall(module))
            def newModuleXML = XmlUtil.serialize(Gateway.getMarshaller().marshall(newModule))

            KernelXMLUtility.compareXML(oldModuleXML, newModuleXML)

            FileStringUtility.string2File(moduleXMLFile, newModuleXML)
        }

    }

    /**
     * Validate if the StateMachine is existing or not.  If not it will be added to the module's resources.
     *
     * @param sm
     */
    private void addStateMachine(StateMachine sm) {
        ModuleStateMachine moduleSm = new ModuleStateMachine()

        moduleSm.setVersion(sm.version)
        moduleSm.setName(sm.name)

        updateImports(moduleSm)
    }

    /**
     * Validate if the Query is existing or not.  If not it will be added to the module's resources.
     *
     * @param obj
     */
    private void addQuery(Query query) {
        ModuleQuery moduleQuery = new ModuleQuery()

        moduleQuery.setVersion(query.version)
        moduleQuery.setName(query.name)

        updateImports(moduleQuery)
    }

    /**
     * Validate if the Schema is existing or not.  If not it will be added to the module's resources.
     *
     * @param obj
     */
    private void addSchema(Schema schema) {
        ModuleSchema moduleSchema = new ModuleSchema()

        moduleSchema.setVersion(schema.version)
        moduleSchema.setName(schema.name)

        updateImports(moduleSchema)
    }

    /**
     * Validate if the Script is existing or not.  If not it will be added to the module's resources.
     *
     * @param obj
     */
    private void addScript(Script script) {
        ModuleScript moduleScript = new ModuleScript()

        moduleScript.setVersion(script.version)
        moduleScript.setName(script.name)

        updateImports(moduleScript)
    }

    /**
     * Validate if the activity is existing and has been updated.  If not existing it will be added, if updated it will update the details.
     *
     * @param obj
     */
    private void addActivityDef(ActivityDef actDef) {
        ModuleActivity moduleAct = new ModuleActivity()

        moduleAct.setVersion(actDef.version)
        moduleAct.setName(actDef.name)

        if (actDef.script) moduleAct.setScript(new ModuleDescRef(actDef.script.name, null, actDef.script.version))
        if (actDef.schema) moduleAct.setSchema(new ModuleDescRef(actDef.schema.name, null, actDef.schema.version))
        if (actDef.query)  moduleAct.setQuery( new ModuleDescRef(actDef.query.name,  null, actDef.query.version))

        //Do not add 'Default' StateMachine
        if (actDef.stateMachine && actDef.stateMachine.name != StateMachine.getDefaultStateMachine('Elementary')) {
            moduleAct.setStateMachine( new ModuleDescRef(actDef.stateMachine.name, null, actDef.stateMachine.version))
        }

        updateImports(moduleAct)
    }

    /**
     *
     * @param caDef
     */
    private void addCompositeActivityDef(CompositeActivityDef caDef) {
        ModuleWorkflow moduleWf = new ModuleWorkflow()
        moduleWf.setVersion(caDef.version)
        moduleWf.setName(caDef.name)

        if (caDef.refChildActDef) {
            caDef.refChildActDef.each {
                ActivityDef act = ActivityDef.cast(it)
                moduleWf.activities.add(new ModuleDescRef(act.name, act.itemID, act.version))
            }
        }

        //Do not add 'CompositeActivity' StateMachine
        if (caDef.stateMachine && caDef.stateMachine.name != StateMachine.getDefaultStateMachine('Composite')) {
            moduleWf.setStateMachine( new ModuleDescRef(caDef.stateMachine.name, null, caDef.stateMachine.version))
        }

        updateImports(moduleWf)
    }

    private void addPropertyDescriptionList(PropertyDescriptionList pdl) {
        def modulePropDesc = new ModulePropertyDescription()
        modulePropDesc.setVersion(pdl.version)
        modulePropDesc.setName(pdl.name)

        updateImports(modulePropDesc)
    }

    private void addImportAgent(ImportAgent agent) {
        def moduleAgent = new ModuleAgent()
        moduleAgent.setName(agent.name)
        moduleAgent.setVersion(agent.version)

        updateImports(moduleAgent)
    }

    private void addImportItem(ImportItem item) {
        def moduleItem = new ModuleItem()
        moduleItem.setName(item.name)
        moduleItem.setVersion(item.version)

        updateImports(moduleItem)
    }

    private void addImportRole(ImportRole role) {
        def moduleRole = new ModuleRole()
        moduleRole.setName(role.name)
        moduleRole.setVersion(role.version)

        updateImports(moduleRole)
    }

    private void updateImports(ModuleImport mImport) {
        int index = newModule.imports.list.findIndexOf {
            ModuleImport mi -> (mi.name == mImport.name) && (mi.getClass() == mImport.getClass())
        }

        if (index > -1 && Gateway.properties.getBoolean('DSL.GenerateModuleXml', true)) {
            def msg = "Cannot update existing import:$mImport.name, class:${mImport.getClass().getSimpleName()}"

            if (Gateway.properties.getBoolean('DSL.ModuleImport.strictUpdate', true)) throw new InvalidDataException(msg)
            else                                                                      log.warn(msg)
        }

        if (index > -1) newModule.imports.list.set(index, mImport)
        else            newModule.imports.list.add(mImport)
    }
}
