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

import org.cristalise.kernel.process.resource.BuiltInResources
import org.cristalise.kernel.utils.FileStringUtility
import org.mvel2.templates.CompiledTemplate
import org.mvel2.templates.TemplateCompiler
import org.mvel2.templates.TemplateRuntime

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

/**
 * Class generating the following DSL files. 
 *
 * <pre>
 * - ${root}/module/Module.groovy (optional)
 * - ${root}/module/${item}.groovy
 * - ${root}/module/State.groovy (optional)
 * - ${root}/module/script/${item}_Aggregate.groovy
 * - ${root}/${resourceRoot}/CA/${item}_Workflow_0,xml
 * - ${root}/${resourceRoot}/CA/State_Manage_0.xml (optional)
 * </pre>
 * 
 * Inputs could be like these using the groovy map literal:
 * 
 * <pre>
 * [rootDir: 'src/test', moduleName: 'Test Module', moduleNs: 'testns', moduleVersion: 0, item: 'TestItem', useConstructor: false]
 * </pre>
 */
@CompileStatic
class CRUDGenerator {

    /**
     * Trigger the generation of the DSL files. It is based on MVEL2 templating.
     * 
     * @param inputs the inputs to the MVEL2 templates
     * @param generateModule whether the Module.groovy file should be generated or not
     * @param itemSpecificFactoryWf whether generate an Item specific Factory workflow or not
     */
    public void generate(Map<String, Object> inputs, boolean generateModule) {
        assert inputs

        new FileTreeBuilder(new File((String)inputs.resourceRoot)).dir('boot') {
            for (def res : BuiltInResources.values()) {
                dir(res.getTypeCode())
            }
        }

        def moduleDir   = new File("${inputs.rootDir}/module")
        def scriptDir   = new File("${inputs.rootDir}/module/script")
        def workflowDir = new File(inputs.resourceRoot ? "${inputs.resourceRoot}/boot/CA" : "${inputs.rootDir}/resources/boot/CA")

        checkAndSetInputs(inputs)

        generateDSL(new File(moduleDir,   "${inputs.item}.groovy"),           'item_groovy.tmpl',           inputs)
        generateDSL(new File(scriptDir,   "${inputs.item}_Aggregate.groovy"), 'item_aggregate_groovy.tmpl', inputs)
        generateDSL(new File(scriptDir,   "${inputs.item}_QueryList.groovy"), 'item_queryList_groovy.tmpl', inputs)
        generateDSL(new File(workflowDir, "${inputs.item}_Workflow_0.xml"),   'item_workflow_xml.tmpl',     inputs)

        if (generateModule) generateModuleFiles(inputs, moduleDir)
    }

    private generateModuleFiles(Map<String, Object> inputs, File moduleDir) {
        if (!inputs['moduleFiles']) {
            inputs['moduleFiles'] = []

            moduleDir.eachFileMatch(~/.*.groovy/) { file ->
                if (file.name != 'Module.groovy') ((List)inputs['moduleFiles']).add(file.name)
            }
        }

        generateDSL(new File(moduleDir, 'Module.groovy'), 'module_groovy.tmpl', inputs)
    }

    private void checkAndSetInputs(Map inputs) {
        assert inputs['item'], "Specify input called 'item'"

        if(inputs['generatedName']) {
            if (!inputs['idPrefix'])    inputs['idPrefix'] = 'ID'
            if (!inputs['leftPadSize']) inputs['leftPadSize'] = '6'
        }
    }

    /**
     * Generates and saves a single DSL file
     * 
     * @param file the File instance to be saved
     * @param templName the MVEL" template to be used, available on the classpath
     * @param vars the inputs needed for the generation
     */
    private void generateDSL(File file, String templName, Map vars) {
        String templ = FileStringUtility.url2String(this.class.getResource(templName))
        CompiledTemplate expr = TemplateCompiler.compileTemplate(templ);
        file.write((String) TemplateRuntime.execute(expr, vars))
    }
}
