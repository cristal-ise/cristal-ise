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

@CompileStatic
public class Expression {
    static final String updateScriptTemplate = '/org/cristalise/dsl/resources/templates/item_updateExpression_groovy.tmpl'

    //Expression could generate save scripts as well
    //Boolean generateUpdateScript = true

    String name
    Integer version
    List<String> imports = []
    List<String> inputFields = []
    String loggerName //e.g.: org.cristalise.template.Script.Patient.ComputeAgeUpdateExpression
    String expression

    public String generateUpdateScript(String field, String schemaName, Integer schemaVersion) {
        Map vars = [:]
        vars.field = field
        vars.schemaName = schemaName
        vars.schemaVersion = schemaVersion
        vars.imports = imports
        vars.inputFields = inputFields
        vars.loggerName = loggerName
        vars.expression = expression

        String templ = FileStringUtility.url2String(this.getClass().getResource(updateScriptTemplate))
        CompiledTemplate expr = TemplateCompiler.compileTemplate(templ);
        return (String) TemplateRuntime.execute(expr, vars)
    }
}