package org.cristalise.kernel.test;

import static org.junit.Assert.*

import org.cristalise.dev.dsl.DevItemDSL
import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.utils.Logger
import org.junit.After
import org.junit.Before
import org.mvel2.templates.CompiledTemplate
import org.mvel2.templates.TemplateCompiler
import org.mvel2.templates.TemplateRuntime

import groovy.transform.CompileStatic


/**
 * 
 */
@CompileStatic
class KernelScenarioTestBase extends DevItemDSL {

    String timeStamp = null
    String folder = "integTest"

    private static Map<String, CompiledTemplate> mvelExpressions = new HashMap<String, CompiledTemplate>();

    /**
     * Utility method to evaluate MVEL templates which are based on simple maps
     *
     * @param vars input parameters
     * @return the XML string
     * @throws IOException
     */
    public static String evalMVELTemplate(String templ, Map<String, Object> vars) {
        CompiledTemplate expr = mvelExpressions.get(templ);

        if(expr == null) {
            Logger.debug(1,"KernelScenarioTestBase.evalMVELTemplate() - Compiling template for "+templ);
            expr = TemplateCompiler.compileTemplate( new File(templ) );
            mvelExpressions.put(templ, expr);
        }
        else {
            Logger.debug(5,"KernelScenarioTestBase.evalMVELTemplate() - CompiledTemplate was found for "+templ);
        }

        return (String) TemplateRuntime.execute(expr, vars);
    }

    public static String getNowString() {
        return new Date().format("yyyy-MM-dd_HH-mm-ss_SSS")
    }

    /**
     * 
     * @param config
     * @param connect
     */
    public void init(String config, String connect) {
        String[] args = ['-logLevel', '5', '-config', config, '-connect', connect]
        
        Properties props = AbstractMain.readC2KArgs(args)
        Gateway.init(props)
        
        timeStamp = getNowString()
        agent = Gateway.connect("dev", "test")
    }

    @Before
    public void before() {
        init('src/main/bin/client.conf', 'src/main/bin/integTest.clc')
    }

    @After
    public void after() {
        Gateway.close()
    }
}
