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
    String moduleXmlDir = null
    String moduleDir = 'src/main/module'

    @Deprecated
    def setModuletDir(URI uri) {
        setModuleDir(uri)
    }

    @Deprecated
    String getModuletDir() {
        return moduleDir
    }

    def setModuleDir(URI uri) {
        moduleDir = Paths.get(uri).parent.toString()
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

        args.resourceRoot = resourceRoot
        args.exportRoot = exportRoot
        args.moduleDir = moduleDir
        args.moduleXmlDir = moduleXmlDir
        args.bindings = this.binding

        ModuleDelegate md = new ModuleDelegate(args)

        if(cl) md.processClosure(cl)

        Gateway.close()
    }
}
