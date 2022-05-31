/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
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
    String moduleXmlDir = null
    String moduleDir = './src/main/module'

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
        //This solution is used because getString('...') trims the value which will trim new line characters as well
        String lineSepType = Gateway.getProperties().getString('ModuleScript.lineSeparator', 'linux');
        System.setProperty('line.separator', lineSepType == 'linux' ? '\n' : '\r\n' )

        if (configDir && (connect || config)) throw new InvalidDataException('Specify only configDir or connect/config')

        if (configDir) {
            config  = config  ?: "$configDir/$defaultConfig"
            connect = connect ?: "$configDir/$defaultConnect"
        }

        if (!connect || !config) {
            log.info('init() - generation only mode')

            // Runs the dsl scripts in the generation only mode, no connection to database
            Gateway.init(new Properties())
        }
        else {
            log.info('init() - config:{}, connect:{}', config, connect)

            Gateway.init(AbstractMain.readPropertyFiles(config, connect, null))
            Gateway.connect()
        }
    }

    public void Module(Map args, @DelegatesTo(ModuleDelegate) Closure cl) {
        init()

        args.resourceRoot = resourceRoot
        args.moduleDir = moduleDir
        args.moduleXmlDir = moduleXmlDir
        args.bindings = this.binding

        ModuleDelegate md = new ModuleDelegate(args)

        if(cl) md.processClosure(cl)

        Gateway.close()
    }
}
