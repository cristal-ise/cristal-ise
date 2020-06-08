package org.cristalise.dsl.module

import java.nio.file.Path
import java.nio.file.Paths

import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway

import groovy.transform.CompileStatic
import groovy.transform.SourceURI
import groovy.util.logging.Slf4j

@CompileStatic @Slf4j
abstract class ModuleScriptBase extends DelegatingScript {

    private static final String defaultConnect = 'local.clc'
    private static final String defaultConfig  = 'client.conf'

    String configDir = null
    String connect = null
    String config = null

    String resourceRoot = null
    String exportRoot = null

    String moduletDir = 'src/main/module'
    
    Integer logLevel = 0
    
    def setModuletDir(URI uri) {
        moduletDir = Paths.get(uri).parent.toString()
    }

    public void init() {
        if (configDir && (connect || config)) throw new InvalidDataException('Specify only configDir or connect/config')

        if (configDir) {
            config  = config  ?: "$configDir/$defaultConfig"
            connect = connect ?: "$configDir/$defaultConnect"

            log.info('config:{}, connect:{}', config, connect)
        }

        if (!connect || !config) throw new InvalidDataException("Missing connect '"+connect+"' or config '"+config+"' files")

        Gateway.init(AbstractMain.readPropertyFiles(config, connect, null))
        Gateway.connect()
    }

    public void Module(Map args, Closure cl) {        
        init()

        ModuleDelegate md = new ModuleDelegate(
            (String)args.ns, 
            (String)args.name, 
            (Integer)args.version,
            resourceRoot,
            exportRoot,
            moduletDir,
            this.binding
        )

        if(cl) md.processClosure(cl)

        Gateway.close()
    }
}
