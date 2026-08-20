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
package org.cristalise.dsl.persistency.outcome

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.cristalise.dsl.csv.TabularGroovyParser
import org.cristalise.dsl.persistency.outcome.generator.SchemaContext
import org.cristalise.dsl.persistency.outcome.generator.XsdSchemaGenerator
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.scripting.Script

import static org.cristalise.kernel.lifecycle.instance.Activity.PREDEF_STEPS_ELEMENT

/**
 * DSL delegate for parsing Schema definitions into an intermediate Struct AST.
 */
@Slf4j @CompileStatic
class SchemaDelegate {

    String  module = ''
    String  name
    Integer version

    String xsdString
    Struct rootStruct
    SchemaContext context

    Map<String, Script> expressionScripts = [:]
    Map<String, List<String>> expressionScriptsInputFields = [:]

    private updateScriptReferences(Struct s) {
        if (!s || (!s.fields && !s.attributes)) return

        s.fields.each { name, f ->
            if (f.expression) this.generateExpressionScript(s, f)
        }

        expressionScriptsInputFields.each { scriptName, inputFields ->
            inputFields.each { fieldName ->
                def inputField = s.fields[fieldName]
                if (inputField.dynamicForms == null) inputField.dynamicForms = new DynamicForms()
                inputField.dynamicForms.updateScriptRef = expressionScripts[scriptName]
            }
        }
    }

    private void addPredefinedStepsField(Struct s) {
        // empty schema is not extended further
        if (!s || !s.fields) return

        // Schema can only contain one xsd:any
        if (s.anyField) return

        def predefinedStepsF = new Field(
            name: PREDEF_STEPS_ELEMENT, 
            multiplicity: '0..1', 
            type: 'anyType',
            dynamicForms: new DynamicForms(required: false, hidden: true)
        )

        s.addField(predefinedStepsF)
    }

    @CompileDynamic
    public void processClosure(Closure cl) {
        assert cl, "Schema only works with a valid Closure"

        def objBuilder = new ObjectGraphBuilder()
        objBuilder.setChildPropertySetter(new DSLPropertySetter())
        objBuilder.classLoader = this.class.classLoader
        objBuilder.classNameResolver = 'org.cristalise.dsl.persistency.outcome'

        cl.delegate = objBuilder

        Struct s = cl() as Struct
        addPredefinedStepsField(s)
        updateScriptReferences(s)
        this.rootStruct = s

        initContext()
        xsdString = buildXSD(s)
    }

    public void processTabularData(TabularGroovyParser parser) {
        def tsb = new TabularSchemaBuilder()

        Struct s = tsb.build(parser)

        addPredefinedStepsField(s)
        updateScriptReferences(s)
        this.rootStruct = s

        initContext()
        xsdString = buildXSD(s)
    }

    private void initContext() {
        this.context = new SchemaContext(module ?: '', name ?: '', version != null ? version : -1)
        this.context.expressionScripts = this.expressionScripts
        this.context.expressionScriptsInputFields = this.expressionScriptsInputFields
    }

    private void generateExpressionScript(Struct s, Field f) {
        log.debug('generateExpressionScript(struct:{}, field:{}) - script:{}', s.name, f.name, f.expression.name)

        def script = new Script('groovy', f.expression.generateUpdateScript(s, f, name, version))
        // this constructor adds a default output which is not needed
        script.getOutputParams().clear()

        script.name = f.expression.name ?: f.expression.generateName(name, f.name)
        script.version = f.expression.version != null ? f.expression.version : version
        script.addInputParam(name, 'org.json.JSONObject')
        script.addInputParam('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
        script.addInputParam('agent', 'org.cristalise.kernel.entity.proxy.AgentProxy')
        script.addOutput(name+'Xml', 'java.lang.String')

        expressionScripts[script.name] = script
        expressionScriptsInputFields[script.name] = f.expression.inputFields
    }

    public String buildXSD(Struct s) {
        if (!s) throw new InvalidDataException("Schema cannot be built from empty declaration")
        if (this.context == null) {
            initContext()
        }
        return new XsdSchemaGenerator().generate(s, this.context)
    }
}
