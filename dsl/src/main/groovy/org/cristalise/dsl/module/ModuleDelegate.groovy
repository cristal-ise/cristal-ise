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

    Writer imports

    Binding bindings = new Binding()

    static final String MODULE_RESOURCE_ROOT = "src/main/resources"
    static final String MODULE_EXPORT_ROOT   = "$MODULE_RESOURCE_ROOT/boot"
    static final String PROPERTY_ROOT        = "${MODULE_EXPORT_ROOT}/property"
    static final String EXPORT_DB_ROOT       = "src/main/script/"

    File moduleXMLFile = new File("$MODULE_RESOURCE_ROOT/module.xml")

    public ModuleDelegate(String ns, String n, int v) {
        resources = new ArrayList<>()

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
     * Generate agent and add to module.xml.
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
     * Process define roles and include/update on module.xml.
     * @param cl
     */
    public void Roles(Closure cl) {
        def importRoles = RoleBuilder.build(cl)
        roles.addAll(importRoles)
    }

    /**
     * Process define config and include/update on module.xml.
     * @param attr
     */
    public void Config(Map attr){
        configs.add(new ModuleConfig((String) attr.name, (String) attr.value, (String) attr.target ? (String) attr.target : null))
    }

    /**
     * rocess define info and include/update on module.xml.
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
     * It compares the 'newResources' with the module's resources collection, if not found then it will be added.
     * The resource will be updated if it is existing but elements are the not same with the one in the module's resources.
     */
    private void updateAndGenerateResource() {
        resources.each { dObj ->

            if (dObj instanceof CompositeActivityDef) {
                CompositeActivityDef obj = CompositeActivityDef.cast(dObj)

                ModuleWorkflow workflow = new ModuleWorkflow()
                workflow.setVersion(obj.version)
                workflow.setName(obj.name)

                if (obj.refChildActDef) {
                    obj.refChildActDef.each {
                        ActivityDef act = ActivityDef.cast(it)
                        workflow.activities.add(new ModuleDescRef(act.name, act.itemID, act.version))
                    }
                }
                int indexVal = findResource(workflow, true)

                if (indexVal > -1) module.imports.list.add(indexVal, workflow)
                else               module.imports.list.add(workflow)
            }
            else if (dObj instanceof ActivityDef) {
                // Validate if the activity is existing and has been updated.  If not existing it will be added, if updated it will update the details.

                ActivityDef obj = ActivityDef.cast(dObj)
                ModuleActivity activity = new ModuleActivity()
                activity.setVersion(obj.version)
                activity.setName(obj.name)

                if (obj.script) activity.setScript(new ModuleDescRef(obj.script.name, null, obj.script.version))
                if (obj.schema) activity.setSchema(new ModuleDescRef(obj.schema.name, null, obj.schema.version))
                if (obj.query)  activity.setQuery( new ModuleDescRef(obj.query.name,  null, obj.query.version))

                int indexVal = findResource(activity, true)

                if (indexVal > -1) module.imports.list.add(indexVal, activity)
                else               module.imports.list.add(activity)
            }
            else if (dObj instanceof Script) {
                // Validate if the Script is existing or not.  If not it will be added to the module's resources.

                Script obj = Script.cast(dObj)
                ModuleScript script = new ModuleScript()
                script.setVersion(obj.version)
                script.setName(obj.name)

                int indexVal = findResource(script, false)

                if (indexVal == -1) module.imports.list.add(script)
            }
            else if (dObj instanceof Schema) {
                // Validate if the Script is existing or not.  If not it will be added to the module's resources.

                Schema obj = Schema.cast(dObj)
                ModuleSchema schema = new ModuleSchema()
                schema.setVersion(obj.version)
                schema.setName(obj.name)

                int indexVal = findResource(schema, false)

                if (indexVal == -1) module.imports.list.add(schema)
            }
            else if (dObj instanceof Query) {
                // Validate if the Script is existing or not.  If not it will be added to the module's resources.

                Query obj = Query.cast(dObj)
                ModuleQuery query = new ModuleQuery()
                query.setVersion(obj.version)
                query.setName(obj.name)

                int indexVal = findResource(query, false)
                if (indexVal == -1) module.imports.list.add(query)
            }
            else if (dObj instanceof StateMachine) {
                // Validate if the StateMachine is existing or not.  If not it will be added to the module's resources.

                StateMachine obj = StateMachine.cast(dObj)
                ModuleStateMachine sm = new ModuleStateMachine()
                sm.setVersion(obj.version)
                sm.setName(obj.name)

                int indexVal = findResource(sm, false)
                if (indexVal == -1) module.imports.list.add(sm)
            }
        }

        agents.each { itAgent ->
            ImportAgent agent = ImportAgent.cast(itAgent)
            int index = findResource(agent, true)
            
            if (index > -1) module.imports.list.add(index, agent)
            else            module.imports.list.add(agent)
        }

        items.each { itItem ->
            ImportItem item = ImportItem.cast(itItem)
            int index = findResource(item, true)

            if (index > -1) module.imports.list.add(index, item)
            else            module.imports.list.add(item)
        }

        roles.each { itRole ->
            ImportRole role = ImportRole.cast(itRole)
            int index = findResource(role, true)

            if (index > -1) module.imports.list.add(index, role)
            else            module.imports.list.add(role)
        }

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

        if (moduleXMLFile.exists()) FileStringUtility.string2File(moduleXMLFile, XmlUtil.serialize(Gateway.getMarshaller().marshall(module)))
    }

    /**
     * Finds the resource if the it's existing in the module imports.
     * @param moduleImport
     * @param remove
     * @return
     */
    private int findResource(ModuleImport moduleImport, boolean remove){
        int indexVal = -1
        module.imports.list.find {
            /* 
             * WHY THIS? if needed replace it with something more clever, e.g. add the accepted classes to a static array
             * 
            boolean isValidInstance = false
            if (it instanceof ImportAgent) {
                isValidInstance = true
            } else if (it instanceof ImportItem) {
                isValidInstance = true
            } else if (it instanceof ModuleActivity) {
                isValidInstance = true
            } else if (it instanceof ModuleSchema) {
                isValidInstance = true
            } else if (it instanceof ModuleScript) {
                isValidInstance = true
            } else if (it instanceof ModuleQuery) {
                isValidInstance = true
            } else if (it instanceof ModuleWorkflow) {
                isValidInstance = true
            } else if (it instanceof ImportRole) {
                isValidInstance = true
            } else if (it instanceof ModuleConfig) {
                isValidInstance = true
            } else if (it instanceof ModuleStateMachine) {
                isValidInstance = true
            } else {
                return
            }
            */

            if (/*isValidInstance && */it.name == moduleImport.name) {
                indexVal = module.imports.list.indexOf(it)
                if (remove) {
                    module.imports.list.remove(indexVal)
                }
            }
        }
        return indexVal
    }

}
