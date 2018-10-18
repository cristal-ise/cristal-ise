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

import org.codehaus.groovy.control.CompilerConfiguration
import org.cristalise.dsl.lifecycle.definition.CompActDefBuilder
import org.cristalise.dsl.lifecycle.definition.ElemActDefBuilder
import org.cristalise.dsl.persistency.database.Database
import org.cristalise.dsl.persistency.database.DatabaseBuilder
import org.cristalise.dsl.persistency.outcome.SchemaBuilder
import org.cristalise.dsl.querying.QueryBuilder
import org.cristalise.dsl.scripting.ScriptBuilder
import org.cristalise.kernel.lifecycle.ActivityDef
import org.cristalise.kernel.lifecycle.CompositeActivityDef
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine
import org.cristalise.kernel.persistency.outcome.Schema
import org.cristalise.kernel.process.module.Module
import org.cristalise.kernel.querying.Query
import org.cristalise.kernel.scripting.Script
import org.cristalise.kernel.utils.LocalObjectLoader

import groovy.transform.CompileStatic
/**
 *
 */
@CompileStatic
class ModuleDelegate {
    
    Module module = new Module()
    int version

    Writer imports
    
    Binding bindings = new Binding()

    static final String exportRoot = "src/main/resources/boot"
    static final String exportDBRoot = "src/main/script/"


    public ModuleDelegate(String ns, String n, int v) {
        module.ns = ns
        module.name = n
        version = v

        imports = new PrintWriter(System.out)
    }

    public include(String scriptFile) {
        CompilerConfiguration cc = new CompilerConfiguration();
        cc.setScriptBaseClass(DelegatingScript.class.getName());
        
        GroovyShell shell = new GroovyShell(this.class.classLoader, bindings, cc);
        DelegatingScript script = (DelegatingScript)shell.parse(new File(scriptFile))

        script.setDelegate(this);
        script.run();
    }



    public Schema Schema(String name, Integer version) {
        return LocalObjectLoader.getSchema(name, version);
    }

    public Schema Schema(String name, Integer version, Closure cl) {
        def schema = SchemaBuilder.build(name, version, cl)
        schema.export(imports, new File(exportRoot), true)
        return schema
    }

    public Database Database(String name, Integer version, Closure cl) {
        def database = DatabaseBuilder.build(name, version, cl)
        database.export(new File(exportDBRoot))
        return database
    }

    public Query Query(String name, Integer version) {
        return LocalObjectLoader.getQuery(name, version);
    }

    public Query Query(String name, Integer version, Closure cl) {
        def query = QueryBuilder.build(this.module.name, name, version, cl)
        query.export(imports, new File(exportRoot), true)
        return query
    }

    public Script Script(String name, Integer version) {
        return LocalObjectLoader.getScript(name, version);
    }

    public Script Script(String name, Integer version, Closure cl) {
        def script = ScriptBuilder.build(name, version, cl)
        script.export(imports, new File(exportRoot), true)
        return script
    }

    public StateMachine StateMachine(String name, Integer version) {
        return LocalObjectLoader.getStateMachine(name, version);
    }

    public ActivityDef Activity(String name, Integer version) {
        return LocalObjectLoader.getActDef(name, version);
    }

    public ActivityDef Activity(String name, Integer version, Closure cl) {
        def eaDef = ElemActDefBuilder.build(name, version, cl)
        eaDef.export(imports, new File(exportRoot), true)
        return eaDef
    }

    public CompositeActivityDef Workflow(String name, Integer version) {
        return LocalObjectLoader.getCompActDef(name, version);
    }

    public CompositeActivityDef Workflow(String name, Integer version, Closure cl) {
        def caDef = CompActDefBuilder.build(name, version, cl)
        caDef.export(imports, new File(exportRoot), true)
        return caDef
    }

    public void processClosure(Closure cl) {
        assert cl
        assert module.name

        cl.delegate = this
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()

        imports.close()
    }
}
