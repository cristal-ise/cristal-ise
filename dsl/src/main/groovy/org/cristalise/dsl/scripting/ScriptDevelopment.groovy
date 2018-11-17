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
abstract class ScriptDevelopment extends Script {

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
        }

        if (!connect  || !config) throw new InvalidDataException("Missing connect '"+connect+"' or config '"+config+"' files")
        if (!itemPath)            throw new InvalidDataException("Missing itemPath '"+itemPath)

        Logger.msg(5, '++++++ ScriptDevelopment - config:%s, connect:%s, itemPath:%s, activityName:%s', config, connect, itemPath, activityName)

        Gateway.init(AbstractMain.readPropertyFiles(config, connect, null))

        //These are the default binding variables created by the Script class
        AgentProxy agent = Gateway.connect(user, pwd)
        ItemProxy  item  = agent.getItem(itemPath)

        binding.setProperty('agent', agent)
        binding.setProperty('item',  item)

        //e.g. aggregate script do not require a job
        if (activityName) {
            Job job = item.getJobByTransitionName(activityName, transitionName, agent)
            assert job
            binding.setProperty('job',   job)
        }
    }

    /**
     * Method called by the groovy script framework automatically
     */
    def WriteScriptHere(String path, String actName = null, Closure cl) {
        try {
            itemPath = path
            activityName = actName

            init()

            // Run actually script code.
            cl.delegate = this
            final result = cl()

            Logger.msg(5, "ScriptDevelopment - script returned: %s", result)
        }
        catch(Exception e) {
            Logger.error(e)
        }
        finally {
            Gateway.close()
        }
    }
}
