package org.cristalise.kernel.test;

import static java.util.concurrent.TimeUnit.*
import static org.cristalise.dev.scaffold.CRUDItemCreator.UpdateMode.ERASE

import java.time.LocalDateTime

import org.awaitility.Awaitility
import org.cristalise.dev.dsl.DevItemDSL
import org.cristalise.dev.dsl.DevXMLUtility
import org.cristalise.dev.scaffold.DevItemCreator
import org.cristalise.kernel.entity.Job
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lookup.AgentPath
import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.mvel2.templates.CompiledTemplate
import org.mvel2.templates.TemplateCompiler
import org.mvel2.templates.TemplateRuntime

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * 
 */
@CompileStatic @Slf4j
@TestInstance(Lifecycle.PER_CLASS)
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
        Awaitility.setDefaultTimeout(10, SECONDS)
        Awaitility.setDefaultPollInterval(500, MILLISECONDS)
        Awaitility.setDefaultPollDelay(200, MILLISECONDS)

        init('src/main/bin/client.conf', 'src/main/bin/integTest.clc')
    }

    @AfterAll
    public void afterClass() {
        Gateway.close()
    }
    
    /**
     *
     * @param factoryPath
     * @param factoryActName
     * @param name
     * @param folder
     * @return
     */
    public void createNewItemByFactory(String factoryPath, String factoryActName, String name, String folder) {
        ItemProxy factory = agent.getItem(factoryPath)
        assert factory && factory.getName() == factoryPath.substring(factoryPath.lastIndexOf('/')+1)

        createNewItemByFactory(factory, factoryActName, name, folder)
    }

    /**
     *
     * @param factory
     * @param factoryActName
     * @param name
     * @param folder
     * @return
     */
    public void createNewItemByFactory(ItemProxy factory, String factoryActName, String name, String folder) {
        executeDoneJob(factory, factoryActName, DevXMLUtility.recordToXML('NewDevObjectDef', [ObjectName: name, SubFolder: folder]))
    }


    /**
     * 
     * @param item
     * @param agent
     * @param expectedJobs
     */
    public void checkJobs(ItemProxy item, AgentPath agent, List<Map<String, Object>> expectedJobs) {
        List<Job> jobs = item.getJobs(agent)

        assert jobs.size() == expectedJobs.size()

        expectedJobs.each { Map expectedJob ->
            assert expectedJob && expectedJob.stepName &&  expectedJob.transitionName

            assert jobs.find { Job j ->
                j.stepName == expectedJob.stepName && j.transition.name == expectedJob.transitionName
            }, "Cannot find Job: '${expectedJob.stepName}' , '${expectedJob.agentRole}' , '${expectedJob.transitionName}'"
        }
    }

    /**
     * 
     * @param item
     * @param expectedJobs
     */
    public void checkJobs(ItemProxy item, List<Map<String, Object>> expectedJobs) {
        checkJobs(item, agent.getPath(), expectedJobs)
    }
}
