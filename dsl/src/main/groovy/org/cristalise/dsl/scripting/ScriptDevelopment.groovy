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
package org.cristalise.dsl.scripting

import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.entity.Job
import org.cristalise.kernel.entity.proxy.AgentProxy
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic @Slf4j
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
        if (configDir && (connect || config)) throw new InvalidDataException('Specify only configDir or connect/config')

        if (configDir) {
            config  = config  ?: "$configDir/$defaultConfig"
            connect = connect ?: "$configDir/$defaultConnect"
        }

        if (!connect  || !config) throw new InvalidDataException("Missing connect '"+connect+"' or config '"+config+"' files")
        if (!itemPath)            throw new InvalidDataException("Missing itemPath '"+itemPath)

        log.info 'ScriptDevelopment - config:{}, connect:{}, itemPath:{}, activityName:{}', config, connect, itemPath, activityName

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

            log.debug "script returned: {}", result
        }
        catch(Exception e) {
            log.error("", e)
        }
        finally {
            Gateway.close()
        }
    }
}
