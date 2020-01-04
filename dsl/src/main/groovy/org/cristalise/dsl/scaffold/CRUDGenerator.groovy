package org.cristalise.dsl.scaffold

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
 * - ${root}/module/Factory.groovy
 * - ${root}/module/${item}.groovy
 * - ${root}/module/State.groovy (optional)
 * - ${root}/module/script/Factory_InstantiateItem.groovy
 * - ${root}/module/script/Entity_ChangeName.groovy
 * - ${root}/module/script/${item}_Aggregate.groovy
 * - ${root}/module/resources/CA/Factory_Workflow_0,xml
 * - ${root}/module/resources/CA/${item}_Workflow_0,xml
 * - ${root}/module/resources/CA/State_Manage_0.xml (optional)
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
     * @param generateStateWf whether the State.groovy and State_Manage_0.xml files should be generated or not
     * @param itemSpecificFactoryWf whether generate an Item specific Factory workflow or not
     */
    public void generate(Map<String, Object> inputs, boolean generateModule, boolean generateStateWf) {
        assert inputs

        def moduleDir   = new File("${inputs.rootDir}/module")
        def scriptDir   = new File("${inputs.rootDir}/module/script")
        def workflowDir = new File("${inputs.rootDir}/resources/boot/CA")

        checkAndSetInputs(inputs)

        generateDSL(new File(moduleDir,   'Factory.groovy'),                  'factory_groovy.tmpl',                 inputs)
        generateDSL(new File(scriptDir,   'Factory_InstantiateItem.groovy'),  'factory_instantiateItem_groovy.tmpl', inputs)
        generateDSL(new File(scriptDir,   'Entity_ChangeName.groovy'),        'entity_changeName_groovy.tmpl',       inputs)
        generateDSL(new File(workflowDir, 'Factory_Workflow_0.xml'),          'factory_workflow_xml.tmpl',           inputs)
        generateDSL(new File(moduleDir,   "${inputs.item}.groovy"),           'item_groovy.tmpl',                    inputs)
        generateDSL(new File(scriptDir,   "${inputs.item}_Aggregate.groovy"), 'item_aggregate_groovy.tmpl',          inputs)
        generateDSL(new File(scriptDir,   "${inputs.item}_QueryList.groovy"), 'item_queryList_groovy.tmpl',          inputs)
        generateDSL(new File(workflowDir, "${inputs.item}_Workflow_0.xml"),   'item_workflow_xml.tmpl',              inputs)

        if (generateStateWf) {
            generateDSL(new File(moduleDir,   'State.groovy'),       'state_groovy.tmpl',     inputs)
            generateDSL(new File(workflowDir, 'State_Manage_0.xml'), 'state_manage_xml.tmpl', inputs)
        }

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
