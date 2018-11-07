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

import java.util.concurrent.CopyOnWriteArrayList

import org.apache.commons.lang3.StringUtils
import org.codehaus.groovy.control.CompilerConfiguration
import org.cristalise.dsl.entity.AgentBuilder
import org.cristalise.dsl.entity.ItemBuilder
import org.cristalise.dsl.entity.RoleBuilder
import org.cristalise.dsl.lifecycle.definition.CompActDefBuilder
import org.cristalise.dsl.lifecycle.definition.ElemActDefBuilder
import org.cristalise.dsl.persistency.database.Database
import org.cristalise.dsl.persistency.database.DatabaseBuilder
import org.cristalise.dsl.persistency.outcome.SchemaBuilder
import org.cristalise.dsl.property.PropertyDescriptionBuilder
import org.cristalise.dsl.querying.QueryBuilder
import org.cristalise.dsl.scripting.ScriptBuilder
import org.cristalise.kernel.entity.imports.ImportAgent
import org.cristalise.kernel.entity.imports.ImportItem
import org.cristalise.kernel.entity.imports.ImportRole
import org.cristalise.kernel.lifecycle.ActivityDef
import org.cristalise.kernel.lifecycle.CompositeActivityDef
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine
import org.cristalise.kernel.persistency.outcome.Schema
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.process.module.*
import org.cristalise.kernel.querying.Query
import org.cristalise.kernel.scripting.Script
import org.cristalise.kernel.utils.DescriptionObject
import org.cristalise.kernel.utils.FileStringUtility
import org.cristalise.kernel.utils.LocalObjectLoader
import org.cristalise.kernel.utils.Logger

import groovy.transform.CompileStatic
import groovy.xml.XmlUtil

/**
 *
 */
@CompileStatic
class ModuleDelegate {

    Module module = null

    /**
     * Collects the generated resources.
     * Used by the {updateAndGenerateResource} method.
     */
    List<? extends DescriptionObject> resources

    Set<ImportAgent> agents
    Set<ImportItem> items
    Set<ImportRole> roles
    Set<ModuleConfig> configs
    List<ModuleImport> moduleImports

    Writer imports

    Binding bindings = new Binding()

    static final String MODULE_RESOURCE_ROOT = "src/main/resources"
    static final String MODULE_EXPORT_ROOT   = "$MODULE_RESOURCE_ROOT/boot"
    static final String PROPERTY_ROOT        = "${MODULE_EXPORT_ROOT}/property"
    static final String EXPORT_DB_ROOT       = "src/main/script/"
    static final String[] ATTRS_TO_REPLACE   = ["isAbstract=\"false\""]

    File moduleXMLFile = new File("$MODULE_RESOURCE_ROOT/module.xml")

    public ModuleDelegate(String ns, String n, int v) {
        resources = new ArrayList<>()
        moduleImports = new CopyOnWriteArrayList<>()

        if (moduleXMLFile.exists()) {
            module = (Module) Gateway.getMarshaller().unmarshall(moduleXMLFile.text)
            assert module.ns == ns
            assert module.name == n
        }
        else {
            module = new Module()
            module.ns = ns
            module.name = n
        }

        if (module.info == null) module.info = new ModuleInfo()
        module.info.version = Integer.toString(v)

        imports = new PrintWriter(System.out)
        agents = new HashSet<>()
        items = new HashSet<>()
        roles = new HashSet<>()
        configs = new HashSet<>()
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
        resources.add(schema)
        return schema
    }

    public Schema Schema(String name, Integer version, Closure cl) {
        def schema = SchemaBuilder.build(name, version, cl)
        schema.export(imports, new File(MODULE_EXPORT_ROOT), true)
        resources.add(schema)
        return schema
    }

    public Database Database(String name, Integer version, Closure cl) {
        def database = DatabaseBuilder.build(name, version, cl)
        database.export(new File(EXPORT_DB_ROOT))
        return database
    }

    public Query Query(String name, Integer version) {
        def query = LocalObjectLoader.getQuery(name, version)
        resources.add(query)
        return query
    }

    public Query Query(String name, Integer version, Closure cl) {
        def query = QueryBuilder.build(this.module.name, name, version, cl)
        query.export(imports, new File(MODULE_EXPORT_ROOT), true)
        resources.add(query)
        return query
    }

    public Script Script(String name, Integer version) {
        def script = LocalObjectLoader.getScript(name, version)
        resources.add(script)
        return script
    }

    public Script Script(String name, Integer version, Closure cl) {
        def script = ScriptBuilder.build(name, version, cl)
        script.export(imports, new File(MODULE_EXPORT_ROOT), true)
        resources.add(script)
        return script
    }

    public StateMachine StateMachine(String name, Integer version) {
        def sm = LocalObjectLoader.getStateMachine(name, version)
        resources.add(sm)
        return sm
    }

    public ActivityDef Activity(String name, Integer version) {
        def eaDef = LocalObjectLoader.getActDef(name, version)
        resources.add(eaDef)
        return eaDef
    }

    public ActivityDef Activity(String name, Integer version, Closure cl) {
        def eaDef = ElemActDefBuilder.build(name, version, cl)
        eaDef.export(imports, new File(MODULE_EXPORT_ROOT), true)
        resources.add(eaDef)
        return eaDef
    }

    public CompositeActivityDef Workflow(String name, Integer version) {
        def caDef = LocalObjectLoader.getCompActDef(name, version)
        resources.add(caDef)
        return caDef
    }

    /**
     * Enable export if workflow needs to be generated.
     * e.g. caDef.export(imports, new File(exportRoot), true)
     * @param name
     * @param version
     * @param cl
     * @return
     */
    public CompositeActivityDef Workflow(String name, Integer version, Closure cl) {
        def caDef = CompActDefBuilder.build(name, version, cl)
        resources.add(caDef)
        return caDef
    }

    /**
     * Generates xml files for the define property description values.
     * @param cl
     */
    public void PropertyDescriptionList(Closure cl) {
        def propDescList = PropertyDescriptionBuilder.build(cl)
        def type = propDescList.list.find {
            it.isClassIdentifier && it.name == 'Type'
        }
        FileStringUtility.string2File(new File(new File(PROPERTY_ROOT), "${type.defaultValue}.xml"), XmlUtil.serialize(Gateway.getMarshaller().marshall(propDescList)))
    }

    /**
     * Collects agent and add to module.xml, or update the definition it is already existing.
     * @param name
     * @param password
     * @param cl
     */
    public void Agent(Map args, Closure cl) {
        def agent = AgentBuilder.build((String) args.name, (String) args.password, cl)
        agent.setProperties(null)
        agent.roles.each {
            it.jobList = null
        }
        agents.add(agent)
    }

    /**
     * Collects items define in the groovy scripts and add to module.xml
     * or update the definition it is already existing.
     * @param args
     * @param cl
     */
    public void Item(Map args, Closure cl) {
        def item = ItemBuilder.build((String) args.name, (String) args.folder, (String) args.workflow, cl)
        item.properties.removeAll{
            it.value == args.name
        }
        items.add(item)
    }

    /**
     * Collects define roles and add/update on module.xml.
     * @param cl
     */
    public void Roles(Closure cl) {
        def importRoles = RoleBuilder.build(cl)
        roles.addAll(importRoles)
    }

    /**
     * Collects define config and add/update on module.xml.
     * @param attr
     */
    public void Config(Map attr){
        configs.add(new ModuleConfig((String) attr.name, (String) attr.value, (String) attr.target ? (String) attr.target : null))
    }

    /**
     * Collects define info and add/update on module.xml.
     * @param attr
     */
    public void Info(Map attr, Closure<String[]> cl) {
        assert attr

        if (!module.info) module.info = new ModuleInfo()

        if (attr.description) module.info.desc          = (String) attr.description
        if (attr.version)     module.info.version       = (String) attr.version
        if (attr.kernel)      module.info.kernelVersion = (String) attr.kernel

        if (cl) {
            def dependencies = cl()

            if (dependencies) module.info.dependency.addAll(dependencies)
        }
    }

    /**
     * Sets the module's resourceUrl value.
     * @param url
     * @return
     */
    public Url(String url){
        assert url
        module.resURL = url
    }

    public void processClosure(Closure cl) {
        assert cl
        assert module.name

        cl.delegate = this
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()

        imports.close()

        updateAndGenerateResource()
    }

    /**
     * Updates the the resources in the module.xml.
     * It compares the {@value #resources} with the {@value Module#imports} collection, if not found then it will be added.
     * The resource will be updated if it is existing but elements are the not same with the one in the module's imports.
     */
    private void updateAndGenerateResource() {
        moduleImports.clear()
        moduleImports.addAll(module.imports.list)

        resources.each { resource ->
            if      (resource instanceof CompositeActivityDef) addCompositeActivityDef(CompositeActivityDef.cast(resource))
            else if (resource instanceof ActivityDef)          addActivityDef(ActivityDef.cast(resource))
            else if (resource instanceof Script)               addScript(Script.cast(resource))
            else if (resource instanceof Schema)               addSchema(Schema.cast(resource))
            else if (resource instanceof Query)                addQuery(Query.cast(resource))
            else if (resource instanceof StateMachine)         addStateMachine(StateMachine.cast(resource))
        }

        agents.each { findAndUpdateResource(it, true) }
        items.each  { findAndUpdateResource(it, true) }
        roles.each  { findAndUpdateResource(it, true) }

        addConfigs()

        module.imports.list.clear()
        module.imports.list.addAll(moduleImports)

        if (moduleXMLFile.exists()) FileStringUtility.string2File(moduleXMLFile, XmlUtil.serialize(removeAttrs()))
    }

    /**
     * 
     */
    private void addConfigs() {
        configs.each { itConfig ->
            int index = -1
            module.config.find {
                if (it.name == itConfig.name) {
                    index = module.config.indexOf(it)
                    module.config.remove(index)
                }
                else {
                    return
                }
            }

            if (index > -1) module.config.add(index, itConfig)
            else            module.config.add(itConfig)
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

        findAndUpdateResource(moduleSm, false)
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

        findAndUpdateResource(moduleQuery, false)
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

        findAndUpdateResource(moduleSchema, false)
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

        findAndUpdateResource(moduleScript, false)
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

        findAndUpdateResource(moduleAct, true)
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

        findAndUpdateResource(moduleWf, true)
    }

    /**
     * Removes attributes that will not be needed in module.xml.
     * 
     * @return
     */
    private String removeAttrs() {
        String moduleContent = Gateway.getMarshaller().marshall(module)

        ATTRS_TO_REPLACE.each {
            moduleContent = moduleContent.replaceAll((String)it, StringUtils.EMPTY)
        }

        return moduleContent
    }

    /**
     * Finds the resource if the it's existing in the module imports.
     * @param moduleImport
     * @param remove
     */
    private void findAndUpdateResource(ModuleImport moduleImport, boolean remove) {
        Logger.msg 0, "ModuleDelegate.findAndUpdateResource() - $moduleImport.name"

        int indexVal =  moduleImports.findIndexOf{ ModuleImport mi -> mi.name == moduleImport.name }

        if (indexVal > -1 && remove) {
             moduleImports.remove(indexVal)
             moduleImports.add(indexVal, moduleImport)
        }
        else if (indexVal == -1) {
            moduleImports.add(moduleImport)
        }
    }
}
