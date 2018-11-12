package org.cristalise.dsl.module

import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.utils.Logger

import groovy.transform.CompileStatic

@CompileStatic
abstract class ModuleScriptBase extends Script {

    private static final String defaultConnect = 'local.clc'
    private static final String defaultConfig  = 'client.conf'

    String configDir = null
    String connect = null
    String config = null

    Integer logLevel = 0

    private void init() {
        Logger.addLogStream(System.out, logLevel)

        if (configDir && (connect || config)) throw new InvalidDataException('Specify only configDir or connect/config')

        if (configDir) {
            config  = config  ?: "$configDir/$defaultConfig"
            connect = connect ?: "$configDir/$defaultConnect"

            Logger.msg(5, 'DslScriptBase - config:%s, connect:%s', config, connect)
        }

        if (!connect || !config) throw new InvalidDataException("Missing connect '"+connect+"' or config '"+config+"' files")

        Gateway.init(AbstractMain.readPropertyFiles(config, connect, null))
        Gateway.connect()
    }

    public void Module(Map args, Closure cl) {
        init()

        ModuleDelegate md = new ModuleDelegate((String)args.ns, (String)args.name, (Integer)args.version, this.binding)

        if(cl) md.processClosure(cl)

        Gateway.close()
    }
}
