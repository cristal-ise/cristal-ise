package org.cristalise.kernel.test;

import static org.junit.Assert.*
import groovy.transform.CompileStatic

import org.cristalise.dev.test.utils.DevItemUtility
import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.utils.Logger
import org.junit.After
import org.junit.Before
import org.mvel2.templates.CompiledTemplate
import org.mvel2.templates.TemplateCompiler
import org.mvel2.templates.TemplateRuntime



/**
 * 
 * @author kovax
 *
 */
@CompileStatic
class KernelScenarioTestBase extends DevItemUtility {

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

    /**
     * 
     * @param config
     * @param connect
     */
    void init(String config, String connect) {
        String[] args = ['-logLevel', '8', '-config', config, '-connect', connect]
        
        Properties props = AbstractMain.readC2KArgs(args)
        Gateway.init(props)
        
        timeStamp = new Date().format("yyyy-MM-dd_HH-mm-ss_SSS")
    }

    @Before
    public void beforClient() {
        beforeClient('src/integration-test/bin/client.conf', 'src/integration-test/bin/integTest.clc')
    }

    /**
     * 
     * @param config
     * @param connect
     */
    void beforeClient(String config, String connect) {
        init(config, connect)

        agent = Gateway.connect("dev", "test")
    }
    
    /**
     * 
     * @param config
     * @param connect
     */
    void beforeServer(String config, String connect) {
        init(config, connect)
        Gateway.connect();
        Gateway.startServer();
    }

    @After
    void after() {
        Gateway.close()
    }
}
