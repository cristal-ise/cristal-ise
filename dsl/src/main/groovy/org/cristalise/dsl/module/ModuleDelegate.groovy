/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.dsl.module

import static org.cristalise.dsl.lifecycle.definition.CompActDefBuilder.generateWorkflowSVG
import static org.cristalise.kernel.process.resource.BuiltInResources.PROPERTY_DESC_RESOURCE

import java.nio.file.Files
import java.nio.file.Path

import org.codehaus.groovy.control.CompilerConfiguration
import org.cristalise.dsl.entity.AgentBuilder
import org.cristalise.dsl.entity.AgentDelegate
import org.cristalise.dsl.entity.ItemBuilder
import org.cristalise.dsl.entity.ItemDelegate
import org.cristalise.dsl.entity.RoleBuilder
import org.cristalise.dsl.entity.RoleDelegate
import org.cristalise.dsl.lifecycle.definition.CompActDefBuilder
import org.cristalise.dsl.lifecycle.definition.CompActDefDelegate
import org.cristalise.dsl.lifecycle.definition.ElemActDefBuilder
import org.cristalise.dsl.lifecycle.definition.ElemActDefDelegate
import org.cristalise.dsl.lifecycle.stateMachine.StateMachineBuilder
import org.cristalise.dsl.lifecycle.stateMachine.StateMachineDelegate
import org.cristalise.dsl.persistency.outcome.SchemaBuilder
import org.cristalise.dsl.persistency.outcome.SchemaDelegate
import org.cristalise.dsl.property.PropertyDescriptionBuilder
import org.cristalise.dsl.property.PropertyDescriptionDelegate
import org.cristalise.dsl.querying.QueryBuilder
import org.cristalise.dsl.querying.QueryDelegate
import org.cristalise.dsl.scripting.ScriptBuilder
import org.cristalise.dsl.scripting.ScriptDelegate
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.entity.Job
import org.cristalise.kernel.entity.imports.ImportAgent
import org.cristalise.kernel.entity.imports.ImportItem
import org.cristalise.kernel.entity.imports.ImportRole
import org.cristalise.kernel.entity.proxy.AgentProxy
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.graph.layout.DefaultGraphLayoutGenerator
import org.cristalise.kernel.lifecycle.ActivityDef
import org.cristalise.kernel.lifecycle.CompositeActivityDef
import org.cristalise.kernel.lifecycle.instance.predefined.Erase
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine
import org.cristalise.kernel.persistency.outcome.Schema
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.process.module.Module
import org.cristalise.kernel.process.module.ModuleActivity
import org.cristalise.kernel.process.module.ModuleAgent
import org.cristalise.kernel.process.module.ModuleConfig
import org.cristalise.kernel.process.module.ModuleDescRef
import org.cristalise.kernel.process.module.ModuleImport
import org.cristalise.kernel.process.module.ModuleInfo
import org.cristalise.kernel.process.module.ModuleItem
import org.cristalise.kernel.process.module.ModulePropertyDescription
import org.cristalise.kernel.process.module.ModuleQuery
import org.cristalise.kernel.process.module.ModuleRole
import org.cristalise.kernel.process.module.ModuleSchema
import org.cristalise.kernel.process.module.ModuleScript
import org.cristalise.kernel.process.module.ModuleStateMachine
import org.cristalise.kernel.process.module.ModuleWorkflow
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
class ModuleDelegate implements BindingConvention {

    private final boolean generateResourceXml = Gateway.properties.getBoolean('DSL.Module.generateResourceXml', true)
    private final boolean generateModuleXml   = Gateway.properties.getBoolean('DSL.Module.generateModuleXml', true)
    private final boolean uploadChangedItems  = Gateway.properties.getBoolean('Gateway.clusteredVertx', true)

    String uploadAgentName = null
    String uploadAgentPwd = null

    Module module = null
    Module newModule = null
    Binding bindings

    String resourceRoot = './src/main/resources'
    String moduleDir    = './src/main/module/'

    private File resourceBootDir = null
    private File moduleXMLFile = null

    private IncludeHandler includeHandler = null

    public ModuleDelegate(Map<String, Object> args) {
        assert args.ns && args.name && args.version != null

        log.info('ModuleDelegate() - args:{}', args)

        inititalise(args)

        addToBingings(bindings, 'moduleNs',      newModule.ns as String)
        addToBingings(bindings, 'moduleVersion', newModule.info.version as String)

        if (moduleXMLFile.exists()) {
            module = (Module) Gateway.getMarshaller().unmarshall(moduleXMLFile.text)
            assert module.ns == newModule.ns
            assert module.name == newModule.name
        }

        createModuleResourceDirectoryStructure()

        boolean enableIncludeHandler = false;

        if (args.containsKey('enableIncludeHandler')) {
            enableIncludeHandler = args.enableIncludeHandler as boolean
        }

        if (enableIncludeHandler) {
            includeHandler = new IncludeHandler()
            includeHandler.captureModuleFileChanges(moduleDir)

            generateModuleXml = false
        }
    }

    private void createModuleResourceDirectoryStructure() {
        new FileTreeBuilder(new File(resourceRoot)).dir('boot') {
            for (def res : BuiltInResources.values()) {
                dir(res.getTypeCode())
            }
        }
    }

    private void inititalise(Map<String, Object> args) {
        newModule = new Module()
        newModule.info = new ModuleInfo()
        newModule.ns = args.ns
        newModule.name = args.name
        newModule.info.version = args.version

        if (args.bindings) bindings = (Binding) args.bindings
        else               bindings = new Binding()

        if (args.resourceRoot) resourceRoot = args.resourceRoot
        if (args.moduleDir)    moduleDir    = args.moduleDir
        resourceBootDir = new File("$resourceRoot/boot")

        String moduleXmlDir = args.moduleXmlDir ?: resourceRoot
        moduleXMLFile = new File("$moduleXmlDir/module.xml")

        if (args.userName)     uploadAgentName = args.userName
        if (args.userPassword) uploadAgentPwd  = args.userPassword
    }

    public ModuleDelegate(String ns, String n, int v, Binding b = null) {
        this('ns': ns, 'name': n, 'version': v, 'bindings': b)
    }

    private void handleInclude(String scriptFile) {
        log.info('include() - scriptFile:{}', scriptFile)

        CompilerConfiguration cc = new CompilerConfiguration()
        cc.setScriptBaseClass(DelegatingScript.class.getName())

        GroovyShell shell = new GroovyShell(this.class.classLoader, bindings, cc)
        DelegatingScript script = (DelegatingScript) shell.parse(new File(scriptFile))

        script.setDelegate(this)
        script.run()
    }

    public void mandatoryInclude(String scriptFile) {
        handleInclude scriptFile
    }

    public void include(String scriptFile) {
        if (!includeHandler || includeHandler.shallInclude(scriptFile)) {
            handleInclude scriptFile
        }
        else {
            log.info('include() - SKIPPING unchanged scriptFile:{}', scriptFile)
        }
    }

    public Schema Schema(String name, Integer version) {
        log.info('Schema() - name:{} version:{}', name, version)
        
        def schema = LocalObjectLoader.getSchema(name, version)
        addSchema(schema)
        return schema
    }

    public Schema Schema(String name, Integer version, @DelegatesTo(SchemaDelegate) Closure cl) {
        log.info('Schema() - name:{} version:{}', name, version)
        
        def sb = SchemaBuilder.build(newModule.ns, name, version, cl)

        if (generateResourceXml) sb.schema.export(null, resourceBootDir, true)

        sb.expressionScipts.each { script ->
            if (generateResourceXml) script.export(null, resourceBootDir, true)
            addScript(script)
        }
        addSchema(sb.schema)
        
        return sb.schema
    }

    public Schema Schema(String name, Integer version, File file) {
        log.info('Schema() - name:{} version:{}', name, version)

        def sb = SchemaBuilder.build(newModule.ns, name, version, file)

        if (generateResourceXml) sb.schema.export(null, resourceBootDir, true)
        addSchema(sb.schema)

        sb.expressionScipts.each { script ->
            if (generateResourceXml) script.export(null, resourceBootDir, true)
            addScript(script)
        }

        return sb.schema
    }

    public Query Query(String name, Integer version) {
        log.info('Query() - name:{} version:{}', name, version)
        
        def query = LocalObjectLoader.getQuery(name, version)
        addQuery(query)
        return query
    }

    public Query Query(String name, Integer version, @DelegatesTo(QueryDelegate) Closure cl) {
        log.info('Query() - name:{} version:{}', name, version)

        def query = QueryBuilder.build(newModule.ns, name, version, cl)
        if (generateResourceXml) query.export(null, resourceBootDir, true)
        addQuery(query)
        return query
    }

    public Script Script(String name, Integer version) {
        log.info('Script() - name:{} version:{}', name, version)
        
        def script = LocalObjectLoader.getScript(name, version)
        addScript(script)
        return script
    }

    public Script Script(String name, Integer version, @DelegatesTo(ScriptDelegate) Closure cl) {
        log.info('Script() - name:{} version:{}', name, version)

        def sb = ScriptBuilder.build(newModule.ns, name, version, cl)
        if (generateResourceXml) sb.script.export(null, resourceBootDir, true)
        addScript(sb.script)
        return sb.script
    }

    public StateMachine StateMachine(String name, Integer version) {
        log.info('StateMachine() - name:{} version:{}', name, version)

        def sm = LocalObjectLoader.getStateMachine(name, version)
        addStateMachine(sm)
        return sm
    }

    public StateMachine StateMachine(String name, Integer version, @DelegatesTo(StateMachineDelegate) Closure cl) {
        log.info('StateMachine() - name:{} version:{}', name, version)

        def sm = StateMachineBuilder.build(newModule.ns, name, version, cl).sm
        if (generateResourceXml) sm.export(null, resourceBootDir, true)
        addStateMachine(sm)
        return sm
    }

    public ActivityDef Activity(String name, Integer version) {
        log.info('Activity() - name:{} version:{}', name, version)

        def eaDef = LocalObjectLoader.getActDef(name, version)
        addActivityDef(eaDef)
        return eaDef
    }

    public ActivityDef Activity(String name, Integer version, @DelegatesTo(ElemActDefDelegate) Closure cl) {
        log.info('Activity() - name:{} version:{}', name, version)

        def eaDef = ElemActDefBuilder.build(name, version, cl)
        if (generateResourceXml) eaDef.export(null, resourceBootDir, true)
        addActivityDef(eaDef)
        return eaDef
    }

    /**
     * 
     * @param name
     * @param version
     * @return
     */
    public CompositeActivityDef Workflow(String name, Integer version) {
        log.info('Workflow() - name:{} version:{}', name, version)

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
    public CompositeActivityDef Workflow(String name, Integer version, @DelegatesTo(CompActDefDelegate) Closure cl) {
        return Workflow(name: name, version: version, generate: false, cl)
    }

    /**
     * 
     * @param args
     * @param cl
     * @return
     */
    public CompositeActivityDef Workflow(Map args, @DelegatesTo(CompActDefDelegate) Closure cl) {
        log.info('Workflow() - name:{} version:{}', args.name, args.version)

        def caDef = CompActDefBuilder.build(args, cl)

        if (args?.generate) {
            if (generateResourceXml) {
                DefaultGraphLayoutGenerator.layoutGraph(caDef.childrenGraphModel)
                //do not rebuild during export, because LocalObjectLoader will not find new actDefs declared in DSL
                caDef.export(null, resourceBootDir, true, false)

                if (log.isDebugEnabled()) generateWorkflowSVG('target', caDef)
            }

            assert caDef.verify(), args
        }
        else {
            // since the workflow was not generated the XML file must exist
            File caDir = new File(resourceBootDir, 'CA')
            assert caDir.exists(), "Directory '$caDir' must exists"

            String caFileName = ""+args.name + (args.version == null ? "" : "_" + args.version) + ".xml"
            File caXmlFile = new File(caDir, caFileName)
            assert caXmlFile.exists(), "File '$caXmlFile' must exists"
        }

        addCompositeActivityDef(caDef)
        return caDef
    }

    /**
     * Generates xml files for the define property description values.
     * @param cl
     */
    public void PropertyDescriptionList(@DelegatesTo(PropertyDescriptionDelegate) Closure cl) {
        def propDescList = PropertyDescriptionBuilder.build(cl)
        def type = propDescList.list.find { it.isClassIdentifier && it.name == 'Type' }

        FileStringUtility.string2File(
            new File(new File(resourceBootDir.path+"/"+PROPERTY_DESC_RESOURCE.typeCode), "${type.defaultValue}.xml"),
            XmlUtil.serialize(Gateway.getMarshaller().marshall(propDescList))
        )
    }

    /**
     * 
     * @param name
     * @param version
     * @param cl
     * @return
     */
    public PropertyDescriptionList PropertyDescriptionList(String name, Integer version, @DelegatesTo(PropertyDescriptionDelegate) Closure cl) {
        log.info('PropertyDescriptionList() - name:{} version:{}', name, version)

        def propDescList = PropertyDescriptionBuilder.build(newModule.ns, name, version, cl)
        if (generateResourceXml) propDescList.export(null, resourceBootDir, true)
        addPropertyDescriptionList(propDescList)

        return propDescList
    }

    /**
     * Collects agent and add to module.xml, or update the definition it is already existing.
     * @param name
     * @param password
     * @param cl
     */
    public ImportAgent Agent(Map args, @DelegatesTo(AgentDelegate) Closure cl) {
        log.info('Agent() - name:{} version:{}', args.name, args.version)

        args.ns = newModule.ns
        def agent = AgentBuilder.build(args, cl)
        agent.roles.each { it.jobList = null }

        if (Gateway.getProperties().getBoolean('DSL.Module.generateAllResourceItems', true)) {
            if (generateResourceXml) agent.export(null, resourceBootDir, true)
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
    public ImportItem Item(Map args, @DelegatesTo(ItemDelegate) Closure cl) {
        log.info('Item() - name:{} version:{}', args.name, args.version)

        args.ns = newModule.ns
        def item = ItemBuilder.build(args, cl)
        item.properties.removeAll { it.value == args.name }

        if (Gateway.getProperties().getBoolean('DSL.Module.generateAllResourceItems', true)) {
            if (generateResourceXml) item.export(null, resourceBootDir, true)
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
    public List<ImportRole> Roles(@DelegatesTo(RoleDelegate) Closure cl) {
        log.info('Roles()')

        def importRoles = RoleBuilder.build(newModule.ns, cl)

        importRoles.each { role ->
            if (Gateway.getProperties().getBoolean('DSL.Module.generateAllResourceItems', true)) {
                if (generateResourceXml) role.export(null, resourceBootDir, true)
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


    private void generateModuleXML() {
        log.info('generateModuleXML()')

        def oldModuleXML = XmlUtil.serialize(Gateway.getMarshaller().marshall(module))
        def newModuleXML = XmlUtil.serialize(Gateway.getMarshaller().marshall(newModule))

        KernelXMLUtility.compareXML(oldModuleXML, newModuleXML)

        FileStringUtility.string2File(moduleXMLFile, newModuleXML)
    }

    private void uploadChangedItems() {
        log.info('uploadChangedItems()')
        
        def agent = Gateway.getSecurityManager().authenticate(uploadAgentName, uploadAgentPwd, null)
        def uploader = new ResourceUpdateHandler(agent, newModule.ns)
        uploader.updateChanges("${resourceRoot}/boot")
    }

    public void processClosure(Closure cl) {
        assert cl
        assert newModule.name

        cl.delegate = this
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()

        if (generateModuleXml) generateModuleXML()
        if (uploadChangedItems) uploadChangedItems()
    }



    /**
     * Validate if the StateMachine is existing or not.  If not it will be added to the module's resources.
     *
     * @param sm
     */
    private void addStateMachine(StateMachine sm) {
        addToBingings(bindings, sm)

        ModuleStateMachine moduleSm = new ModuleStateMachine()

        moduleSm.setVersion(sm.version)
        moduleSm.setName(sm.name)
        moduleSm.setNamespace(sm.namespace)
        
        updateImports(moduleSm)
    }

    /**
     * Validate if the Query is existing or not.  If not it will be added to the module's resources.
     *
     * @param obj
     */
    private void addQuery(Query query) {
        addToBingings(bindings, query)

        ModuleQuery moduleQuery = new ModuleQuery()

        moduleQuery.setVersion(query.version)
        moduleQuery.setName(query.name)
        moduleQuery.setNamespace(query.namespace)

        updateImports(moduleQuery)
    }

    /**
     * Validate if the Schema is existing or not.  If not it will be added to the module's resources.
     *
     * @param obj
     */
    private void addSchema(Schema schema) {
        addToBingings(bindings, schema)

        ModuleSchema moduleSchema = new ModuleSchema()

        moduleSchema.setVersion(schema.version)
        moduleSchema.setName(schema.name)
        moduleSchema.setNamespace(schema.namespace)

        updateImports(moduleSchema)
    }

    /**
     * Validate if the Script is existing or not.  If not it will be added to the module's resources.
     *
     * @param obj
     */
    private void addScript(Script script) {
        addToBingings(bindings, script)

        ModuleScript moduleScript = new ModuleScript()

        moduleScript.setVersion(script.version)
        moduleScript.setName(script.name)
        moduleScript.setNamespace(script.namespace)

        updateImports(moduleScript)
    }

    /**
     * Validate if the activity is existing and has been updated.  If not existing it will be added, if updated it will update the details.
     *
     * @param obj
     */
    private void addActivityDef(ActivityDef actDef) {
        addToBingings(bindings, actDef)

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
        addToBingings(bindings, caDef)

        ModuleWorkflow moduleWf = new ModuleWorkflow()
        moduleWf.setVersion(caDef.version)
        moduleWf.setName(caDef.name)

        if (caDef.refChildActDef) {
            caDef.refChildActDef.each {
                ActivityDef act = ActivityDef.cast(it)
                moduleWf.activities.add(new ModuleDescRef(act.name, null/*act.itemID*/, act.version))
            }
        }

        //Do not add 'CompositeActivity' StateMachine
        if (caDef.stateMachine && caDef.stateMachine.name != StateMachine.getDefaultStateMachine('Composite')) {
            moduleWf.setStateMachine( new ModuleDescRef(caDef.stateMachine.name, null, caDef.stateMachine.version))
        }

        updateImports(moduleWf)
    }

    private void addPropertyDescriptionList(PropertyDescriptionList pdl) {
        addToBingings(bindings, pdl)

        def modulePropDesc = new ModulePropertyDescription()
        modulePropDesc.setVersion(pdl.version)
        modulePropDesc.setName(pdl.name)
        modulePropDesc.setNamespace(pdl.namespace)

        updateImports(modulePropDesc)
    }

    private void addImportAgent(ImportAgent agent) {
        addToBingings(bindings, agent)

        def moduleAgent = new ModuleAgent()
        moduleAgent.setName(agent.name)
        moduleAgent.setVersion(agent.version)
        moduleAgent.setNamespace(agent.namespace)

        updateImports(moduleAgent)
    }

    private void addImportItem(ImportItem item) {
        addToBingings(bindings, item)

        def moduleItem = new ModuleItem()
        moduleItem.setName(item.name)
        moduleItem.setVersion(item.version)
        moduleItem.setNamespace(item.namespace)

        updateImports(moduleItem)
    }

    private void addImportRole(ImportRole role) {
        addToBingings(bindings, role)

        def moduleRole = new ModuleRole()
        moduleRole.setName(role.name)
        moduleRole.setVersion(role.version)
        moduleRole.setNamespace(role.namespace)
        
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
