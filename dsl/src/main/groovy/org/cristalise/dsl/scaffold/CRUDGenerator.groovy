package org.cristalise.dsl.scaffold

import org.cristalise.kernel.utils.FileStringUtility
import org.mvel2.templates.CompiledTemplate
import org.mvel2.templates.TemplateCompiler
import org.mvel2.templates.TemplateRuntime

import groovy.transform.CompileStatic

@CompileStatic
class CRUDGenerator {

    public void generate(Map vars, boolean generateModule, boolean generateStateWf) {
        def moduleDir   = new File((String)vars.rootDir + '/module')
        def scriptDir   = new File((String)vars.rootDir + '/module/script')
        def workflowDir = new File((String)vars.rootDir + '/resources/boot/CA')

        if (generateModule) generateDSL(new File(moduleDir, "Module.groovy"), 'module_groovy.tmpl', vars)

        generateDSL(new File(moduleDir,   "${vars.item}.groovy"),           'item_groovy.tmpl', vars)
        generateDSL(new File(scriptDir,   "${vars.item}_Aggregate.groovy"), 'item_aggregate_groovy.tmpl', vars)
        generateDSL(new File(workflowDir, "${vars.item}_Workflow_0.xml"),   'item_workflow_xml.tmpl', vars)

        if (generateStateWf) {
            generateDSL(new File(moduleDir,   "State.groovy"),       'state_groovy.tmpl', vars)
            generateDSL(new File(workflowDir, "State_Manage_0.xml"), 'state_manage_xml.tmpl', vars)
        }
    }

    private void generateDSL(File file, String templName, Map vars) {
        String templ = FileStringUtility.url2String(this.class.getResource(templName))
        CompiledTemplate expr = TemplateCompiler.compileTemplate(templ);
        file.write((String) TemplateRuntime.execute(expr, vars))
    }
}
