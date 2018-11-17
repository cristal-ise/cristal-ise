package org.cristalise.dsl.scripting

import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.entity.agent.Job
import org.cristalise.kernel.entity.proxy.AgentProxy
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.utils.Logger

import groovy.transform.CompileStatic

@CompileStatic
abstract class ScriptDevelopment extends DelegatingScript {

    private static final String defaultConnect = 'local.clc'
    private static final String defaultConfig  = 'client.conf'

    String configDir = null
    String connect   = null
    String config    = null

    Integer logLevel = 5

    String user = null
    String pwd  = null

    String itemPath       = null
    String activityName   = null
    String transitionName = "Done"

    private void init() {
        Logger.addLogStream(System.out, logLevel)

        if (configDir && (connect || config)) throw new InvalidDataException('Specify only configDir or connect/config')

        if (configDir) {
            config  = config  ?: "$configDir/$defaultConfig"
            connect = connect ?: "$configDir/$defaultConnect"

            Logger.msg(5, 'ScriptDevelopment - config:%s, connect:%s', config, connect)
        }

        if (!connect || !config) throw new InvalidDataException("Missing connect '"+connect+"' or config '"+config+"' files")

        Gateway.init(AbstractMain.readPropertyFiles(config, connect, null))

        //These are the default binding variables created by the Script class
        AgentProxy agent = Gateway.connect(user, pwd)
        ItemProxy  item  = agent.getItem(itemPath)
        Job        job   = item.getJobByTransitionName(activityName, transitionName, agent)

        assert job

        binding.setVariable('agent', agent)
        binding.setVariable('item',  item)
        binding.setVariable('job',   job)
    }

    /**
     * Method called by the groovy script framework automatically
     */
    def run() {
        try {
            init()

            // Run actually script code.
            final result = runCode()

            Logger.msg(5, "ScriptDevelopment - script returned: %s", result)
        }
        finally {
            Gateway.close()
        }
    }

    /**
     * Abstract method as placeholder for the actual script code to run.
     *  
     * @return
     */
    abstract def runCode()
}
