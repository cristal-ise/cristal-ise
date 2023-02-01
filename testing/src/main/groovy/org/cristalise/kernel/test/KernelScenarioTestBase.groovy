package org.cristalise.kernel.test;

import static org.cristalise.dev.scaffold.CRUDItemCreator.UpdateMode.ERASE
import java.time.LocalDateTime
import org.cristalise.dev.dsl.DevItemDSL
import org.cristalise.dev.scaffold.DevItemCreator
import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.mvel2.templates.CompiledTemplate
import org.mvel2.templates.TemplateCompiler
import org.mvel2.templates.TemplateRuntime
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j


/**
 * 
 */
@CompileStatic @Slf4j
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
            log.debug('evalMVELTemplate() - Compiling template for {}', templ);
            expr = TemplateCompiler.compileTemplate( new File(templ) );
            mvelExpressions.put(templ, expr);
        }
        else {
            log.debug('evalMVELTemplate() - CompiledTemplate was found for {}', templ);
        }

        return (String) TemplateRuntime.execute(expr, vars);
    }

    public static String getNowString() {
        return LocalDateTime.now().format("yyyy-MM-dd_HH-mm-ss_SSS")
    }

    /**
     * 
     * @param config
     * @param connect
     */
    public void init(String config, String connect) {
        Gateway.init(AbstractMain.readPropertyFiles(config, connect, null));
        agent = Gateway.connect("user", "test")
        creator = new DevItemCreator('integtest', ERASE, agent)
    }

    @BeforeEach
    public void before() {
        timeStamp = getNowString()
    }

    @BeforeAll
    public void beforeClass() {
        init('src/main/bin/client.conf', 'src/main/bin/integTest.clc')
    }

    @AfterAll
    public void afterClass() {
        Gateway.close()
    }
}
