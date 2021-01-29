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
package org.cristalise.dsl.persistency.outcome;

import org.cristalise.kernel.utils.FileStringUtility
import org.mvel2.templates.CompiledTemplate
import org.mvel2.templates.TemplateCompiler
import org.mvel2.templates.TemplateRuntime

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic @Slf4j
public class Expression {
    static final String updateScriptTemplate = '/org/cristalise/dsl/resources/templates/item_updateExpression_groovy.tmpl'

    private static CompiledTemplate compiledUpdateScript = null
    
    static {
        String templ = FileStringUtility.url2String(Expression.getResource(updateScriptTemplate))
        compiledUpdateScript = TemplateCompiler.compileTemplate(templ);
    }

    //Expression could generate save scripts as well
    //Boolean generateUpdateScript = true

    String name //optional
    Integer version  //optional
    List<String> imports = []
    List<String> inputFields = []
    String loggerPrefix //optional : e.g. org.cristalise.template
    String loggerName //optional (e.g. org.cristalise.template.Script.Patient.ComputeAgeUpdateExpression)
    String expression
    Boolean compileStatic = true

    
    
    public String generateName(String schemaName, String fieldName) {
        return "${schemaName}${fieldName}UpdateExpression";
    }

    public String generateLoggerName(String schemaName, String fieldName) {
        def scriptName = generateName(schemaName, fieldName).replace('_', '.')
        return loggerPrefix ? "${loggerPrefix}.Script.${scriptName}" : "Script.${scriptName}";
    }

    public String generateUpdateScript(Struct s, Field f, String schemaName, Integer schemaVersion) {
        Map vars = [:]
        Map<String, String> inputFieldsType = [:]

        inputFields.each { inputFieldsType[it] = s.fields[it].getJavaType().getSimpleName() }

        vars.field           = f.name
        vars.schemaName      = schemaName
        vars.schemaVersion   = schemaVersion
        vars.imports         = imports
        vars.inputFields     = inputFields
        vars.inputFieldsType = inputFieldsType
        vars.requiredFields  = inputFields.findAll { s.fields[it].isRequired() }
        vars.loggerName      = loggerName ?: generateLoggerName(s.name, f.name)
        vars.expression      = expression
        vars.compileStatic   = compileStatic

        def script = (String) TemplateRuntime.execute(compiledUpdateScript, vars)

        log.debug('generateUpdateScript(field:{}) - script:{}', f.name, script)

        return script
    }
}
