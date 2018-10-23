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

import groovy.transform.CompileStatic
import groovy.xml.XmlUtil
import org.codehaus.groovy.control.CompilerConfiguration
import org.cristalise.dsl.entity.AgentBuilder
import org.cristalise.dsl.entity.ItemBuilder
import org.cristalise.dsl.lifecycle.definition.CompActDefBuilder
import org.cristalise.dsl.lifecycle.definition.ElemActDefBuilder
import org.cristalise.dsl.persistency.database.Database
import org.cristalise.dsl.persistency.database.DatabaseBuilder
import org.cristalise.dsl.persistency.outcome.SchemaBuilder
import org.cristalise.dsl.property.PropertyDescriptionBuilder
import org.cristalise.dsl.querying.QueryBuilder
import org.cristalise.dsl.scripting.ScriptBuilder
import org.cristalise.kernel.entity.Agent
import org.cristalise.kernel.entity.imports.ImportAgent
import org.cristalise.kernel.entity.imports.ImportItem
import org.cristalise.kernel.lifecycle.ActivityDef
import org.cristalise.kernel.lifecycle.CompositeActivityDef
import org.cristalise.kernel.persistency.outcome.Schema
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.process.module.*
import org.cristalise.kernel.property.PropertyDescriptionList
import org.cristalise.kernel.querying.Query
import org.cristalise.kernel.scripting.Script
import org.cristalise.kernel.utils.DescriptionObject
import org.cristalise.kernel.utils.FileStringUtility
import org.cristalise.kernel.utils.LocalObjectLoader

/**
 *
 */
@CompileStatic
class ModuleDelegate {

    Module module = new Module()

    /**
     * Collects the generated resources.
     * Used by the {updateAndGenerateResource} method.
     */
    List<? extends DescriptionObject> resources
    List<? extends ImportAgent> agents
    List<? extends ImportItem> items

    int version
    Writer imports

    Binding bindings = new Binding()

    static final String moduleXmlRoot = "src/main/resources"
    static final String exportRoot = "src/main/resources/boot"
    static final String propertyRoot = "${exportRoot}/property"
    static final String exportDBRoot = "src/main/script/"
    static String moduleXml = 'module.xml'

    public ModuleDelegate(String ns, String n, int v) {
        module.ns = ns
        module.name = n
        version = v

        imports = new PrintWriter(System.out)
        resources = new ArrayList<>()
        agents = new ArrayList<>()
        items = new ArrayList<>()

        module = (Module) Gateway.getMarshaller().unmarshall(new File(moduleXmlRoot + "/${moduleXml}").text)
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
        return LocalObjectLoader.getSchema(name, version)
    }

    public Schema Schema(String name, Integer version, Closure cl) {
        def schema = SchemaBuilder.build(name, version, cl)
        schema.export(imports, new File(exportRoot), true)
        resources.add(schema)
        return schema
    }

    public Database Database(String name, Integer version, Closure cl) {
        def database = DatabaseBuilder.build(name, version, cl)
        database.export(new File(exportDBRoot))
        return database
    }

    public Query Query(String name, Integer version) {
        return LocalObjectLoader.getQuery(name, version)
    }

    public Query Query(String name, Integer version, Closure cl) {
        def query = QueryBuilder.build(this.module.name, name, version, cl)
        query.export(imports, new File(exportRoot), true)
        resources.add(query)
        return query
    }

    public Script Script(String name, Integer version) {
        return LocalObjectLoader.getScript(name, version)
    }

    public Script Script(String name, Integer version, Closure cl) {
        def script = ScriptBuilder.build(name, version, cl)
        script.export(imports, new File(exportRoot), true)
        resources.add(script)
        return script
    }

    public ActivityDef Activity(String name, Integer version) {
        return LocalObjectLoader.getActDef(name, version)
    }

    public ActivityDef Activity(String name, Integer version, Closure cl) {
        def eaDef = ElemActDefBuilder.build(name, version, cl)
        eaDef.export(imports, new File(exportRoot), true)
        resources.add(eaDef)
        return eaDef
    }

    public CompositeActivityDef Workflow(String name, Integer version) {
        return LocalObjectLoader.getCompActDef(name, version)
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
        def type = propDescList.list.find{
            it.isClassIdentifier && it.name == 'Type'
        }
        FileStringUtility.string2File(new File(new File(propertyRoot), "${type.defaultValue}.xml"), XmlUtil.serialize(Gateway.getMarshaller().marshall(propDescList)))
    }

    /**
     * Generate agent and add to module.xml.
     * @param name
     * @param password
     * @param cl
     */
    public void Agent(Map args, Closure cl) {
        def agent = AgentBuilder.build((String) args.name, (String) args.password, cl)
        agents.add(agent)
    }

    /**
     * Collects items define in the groovy scripts and add to module.xml
     * or update the definition it is already existing.
     * @param args
     * @param cl
     */
    public void Item(Map args, Closure cl) {
        def item = ItemBuilder.build((String) args.name, (String) args.folder, cl)
        items.add(item)
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
            boolean isExist = false
            int indexVal = module.imports.list.size() != 0 ? module.imports.list.size() + 1 : 0

            if (dObj instanceof CompositeActivityDef) {
                CompositeActivityDef obj = CompositeActivityDef.cast(dObj)
                module.imports.list.find {
                    if (it instanceof ModuleWorkflow) {
                        indexVal = module.imports.list.indexOf(it)
                        ModuleWorkflow importVal = ModuleWorkflow.cast(it)
                        if (importVal.name == obj.name) {
                            module.imports.list.remove(indexVal)
                            isExist = true
                        } else {
                            return
                        }
                    }
                }

                ModuleWorkflow workflow = new ModuleWorkflow()
                workflow.setVersion(obj.version)
                workflow.setName(obj.name)

                if (obj.refChildActDef){
                    obj.refChildActDef.each {
                        ActivityDef act = ActivityDef.cast(it)
                        workflow.activities.add(new ModuleDescRef(act.name, act.itemID, act.version))
                    }
                }

                if (isExist) {
                    module.imports.list.add(indexVal, workflow)
                } else {
                    module.imports.list.add(workflow)
                }

            } else if (dObj instanceof ActivityDef) {
                // Validate if the activity is existing and has been updated.  If not existing it will be added, if updated it will update the details.

                ActivityDef obj = ActivityDef.cast(dObj)
                module.imports.list.find {
                    if (it instanceof ModuleActivity) {
                        indexVal = module.imports.list.indexOf(it)
                        ModuleActivity importVal = ModuleActivity.cast(it)
                        if (importVal.name == obj.name) {
                            module.imports.list.remove(indexVal)
                            isExist = true
                        } else {
                            return
                        }
                    }
                }

                ModuleActivity activity = new ModuleActivity()
                activity.setVersion(obj.version)
                activity.setName(obj.name)
                if (obj.script) {
                    activity.setScript(new ModuleDescRef(obj.script.name, null, obj.script.version))
                }
                if (obj.schema) {
                    activity.setSchema(new ModuleDescRef(obj.schema.name, null, obj.schema.version))
                }
                if (obj.query) {
                    activity.setQuery(new ModuleDescRef(obj.query.name, null, obj.query.version))
                }

                if (isExist) {
                    module.imports.list.add(indexVal, activity)
                } else {
                    module.imports.list.add(activity)
                }


            } else if (dObj instanceof Script) {
                // Validate if the Script is existing or not.  If not it will be added to the module's resources.

                Script obj = Script.cast(dObj)
                module.imports.list.find {
                    if (it instanceof ModuleScript) {
                        ModuleScript script = ModuleScript.cast(it)
                        if (script.name == obj.name) {
                            isExist = true
                        }
                    }
                }

                if (!isExist) {
                    ModuleScript script = new ModuleScript()
                    script.setVersion(obj.version)
                    script.setName(obj.name)
                    module.imports.list.add(script)
                }

            } else if (dObj instanceof Schema) {
                // Validate if the Script is existing or not.  If not it will be added to the module's resources.

                Schema obj = Schema.cast(dObj)
                module.imports.list.find {
                    if (it instanceof ModuleSchema) {
                        ModuleSchema schema = ModuleSchema.cast(it)
                        if (schema.name == obj.name) {
                            isExist = true
                        }
                    }
                }

                if (!isExist) {
                    ModuleSchema schema = new ModuleSchema()
                    schema.setVersion(obj.version)
                    schema.setName(obj.name)
                    module.imports.list.add(schema)
                }

            } else if (dObj instanceof Query) {
                // Validate if the Script is existing or not.  If not it will be added to the module's resources.

                Query obj = Query.cast(dObj)
                module.imports.list.find {
                    if (it instanceof ModuleQuery) {
                        ModuleQuery query = ModuleQuery.cast(it)
                        if (query.name == obj.name) {
                            isExist = true
                        }
                    }
                }

                if (!isExist) {
                    ModuleQuery query = new ModuleQuery()
                    query.setVersion(obj.version)
                    query.setName(obj.name)
                    module.imports.list.add(query)
                }
            }
        }

        FileStringUtility.string2File(new File(new File(moduleXmlRoot), moduleXml), XmlUtil.serialize(Gateway.getMarshaller().marshall(module)))

    }

}
