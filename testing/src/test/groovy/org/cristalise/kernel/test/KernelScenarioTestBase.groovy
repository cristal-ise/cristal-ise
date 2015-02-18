package org.cristalise.kernel.test;

import static org.junit.Assert.*
import groovy.transform.CompileStatic

import org.cristalise.kernel.entity.proxy.AgentProxy
import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway
import org.junit.After



/**
 * 
 * @author kovax
 *
 */
@CompileStatic
class KernelScenarioTestBase {

    AgentProxy agent = null
    String timeStamp = null
    
    /**
     * 
     * @param config
     * @param connect
     */
    void init(String config, String connect) {
        String[] args = ['-logLevel', '8', '-config', config, '-connect', connect]
        
        Properties props = AbstractMain.readC2KArgs(args)
        Gateway.init(props)
        
        timeStamp = new Date().format("yyyy-MM-dd_HH:mm:ss_SSS")
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

        Gateway.startServer( Gateway.connect() );
    }

    @After
    void after() {
        Gateway.close()
    }
}
