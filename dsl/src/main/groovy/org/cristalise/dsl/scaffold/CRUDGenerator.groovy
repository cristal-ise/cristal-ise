package org.cristalise.dsl.scaffold

import org.cristalise.kernel.utils.FileStringUtility
import org.mvel2.templates.CompiledTemplate
import org.mvel2.templates.TemplateCompiler
import org.mvel2.templates.TemplateRuntime

import groovy.transform.CompileStatic

/**
 * Class generating the following DSL files. 
 * 
 * <pre>
 * - ${root}/module/Module.groovy (optional)
 * - ${root}/module/${item}.groovy
 * - ${root}/module/State.groovy (optional)
 * - ${root}/module/script/${item}_Aggregate.xml
 * - ${root}/module/resources/CA/${item}_Workflow_0,xml
 * - ${root}/module/resources/CA/State_Manage_0.xml (optional)
 * </pre>
 * 
 * Inputs could be like these using the groovy map literal:
 * 
 * <pre>
 * [rootDir: 'src/test', moduleName: 'Test Module', moduleNs: 'testns', moduleVersion: 0, item: 'TestItem']
 * </pre>
 */
@CompileStatic
class CRUDGenerator {

    /**
     * Trigger the generation of the DSL files. It is based on MVEL2 templating.
     * 
     * @param vars the inputs to the MVEL2 templates
     * @param generateModule if the Module.groovy file should be generated or not
     * @param generateStateWf if the State.groovy and State_Manage_0.xml files should be generated or not
     */
    public void generate(Map vars, boolean generateModule, boolean generateStateWf) {
        def moduleDir   = new File("${vars.rootDir}/module")
        def scriptDir   = new File("${vars.rootDir}/module/script")
        def workflowDir = new File("${vars.rootDir}/resources/boot/CA")

        if (generateModule) generateDSL(new File(moduleDir, 'Module.groovy'), 'module_groovy.tmpl', vars)

        generateDSL(new File(moduleDir,   "${vars.item}.groovy"),           'item_groovy.tmpl', vars)
        generateDSL(new File(scriptDir,   "${vars.item}_Aggregate.groovy"), 'item_aggregate_groovy.tmpl', vars)
        generateDSL(new File(workflowDir, "${vars.item}_Workflow_0.xml"),   'item_workflow_xml.tmpl', vars)

        if (generateStateWf) {
            generateDSL(new File(moduleDir,   'State.groovy'),       'state_groovy.tmpl', vars)
            generateDSL(new File(workflowDir, 'State_Manage_0.xml'), 'state_manage_xml.tmpl', vars)
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
