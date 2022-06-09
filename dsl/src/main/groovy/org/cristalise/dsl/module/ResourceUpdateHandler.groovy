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

import static org.cristalise.dsl.module.GitStatus.*
import static org.cristalise.kernel.process.resource.BuiltInResources.*

import java.nio.file.Path

import org.cristalise.kernel.entity.Job
import org.cristalise.kernel.entity.proxy.AgentProxy
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.instance.predefined.Erase
import org.cristalise.kernel.persistency.outcome.Outcome
import org.cristalise.kernel.process.resource.BuiltInResources
import org.cristalise.kernel.scripting.Script

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic @Slf4j
class ResourceUpdateHandler {
    AgentProxy updateAgent = null
    String moduleNamespace
    String resourceBootDir // the actual resources/boot directory
    List<Path> changedScriptFiles = []

    public ResourceUpdateHandler(AgentProxy agent, String moduleNs, String bootDir, List<Path> scriptFiles) {
        updateAgent = agent
        moduleNamespace = moduleNs
        resourceBootDir = bootDir
        if (scriptFiles) changedScriptFiles = scriptFiles
    }

    /**
     * 
     */
    public void updateChanges() {
        updateResourceItems()
        updateScriptItems()
    }

    /**
     * Retrieves git status of XML files in the resources/boot directory 
     * and updates the Items using Activities defined in the kernel.
     */
    private void updateResourceItems() {
        def resourcesStatus = GitStatus.getStatusMapForWorkTree(resourceBootDir)

        for (GitStatus status: resourcesStatus.keySet()) {
            List<Path> files = resourcesStatus.get(status)

            switch(status) {
                case ADDED:
                case UNTRACKED:
                    createResourceItems(files)
                    break

                case MODIFIED:
                    updateResourceItems(files)
                    break

                case REMOVED:
                    removeResourceItems(files)
                    break

                default:
                    log.error('uploadChangedItems() - unknow GitStatus:{}', status)
                    break
            }
        }
    }

    private String getResourceName(Path filePath) {
        def resourceFileName = filePath.getFileName().toString()
        def resourceFileNameNoExt = resourceFileName.substring(0, resourceFileName.lastIndexOf("."))

        return resourceFileNameNoExt.substring(0, resourceFileNameNoExt.lastIndexOf("_"))
    }

    private ItemProxy getResourceItem(Path filePath) {
        def resourceName = getResourceName(filePath)
        def resourceType = BuiltInResources.getValue(filePath.getParent().getFileName().toString())

        return updateAgent.getItem(resourceType.getTypeRoot() + '/' + moduleNamespace + '/' + resourceName)
    }

    private void createResourceItems(List<Path> files) {
        log.warn('createResourceItems() - SKIPPED - NO factories defined in the kernel - files:{}', files)
    }

    private void removeResourceItems(List<Path> files) {
        for (Path filePath : files) {
            ItemProxy resItem = getResourceItem(filePath)
            updateAgent.execute(resItem, Erase.class)
        }
    }

    private updateResourceItem(ItemProxy resItem, BuiltInResources resourceType, String OutcomeString) {
        Job editJob = resItem.getJobByName(resourceType.getEditActivityName(), updateAgent)
        editJob.setOutcome(OutcomeString)
        updateAgent.execute(editJob)

        Job moveJob = resItem.getJobByName(resourceType.getMoveVersionActivityName(), updateAgent)
        updateAgent.execute(moveJob)
    }

    private void updateResourceItems(List<Path> files) {
        for (Path filePath : files) {
            ItemProxy resItem = getResourceItem(filePath)

            def resourceTypeName = filePath.getParent().getFileName().toString()
            def resourceType = BuiltInResources.getValue(resourceTypeName)

            updateResourceItem(resItem, resourceType, filePath.getText('UTF-8'))
        }
    }

    private ItemProxy getScriptItem(Path scriptPath) {
        def scriptRoot = SCRIPT_RESOURCE.getTypeRoot()
        def scriptFileName = scriptPath.getFileName().toString()
        def itemTypeName = scriptPath.parent.parent.fileName.toString()
        def scriptName = scriptFileName.substring(0, scriptFileName.lastIndexOf("."))

        return updateAgent.getItem(scriptRoot + '/' + moduleNamespace + '/' + itemTypeName + '_' + scriptName)
    }

    private updateScriptItems() {
        changedScriptFiles.each { scriptPath ->
            String newScriptContent = scriptPath.toFile().getText('UTF-8')
            ItemProxy scriptItem = getScriptItem(scriptPath)
            Outcome scriptOutcome = scriptItem.getOutcome(scriptItem.getViewpoint("Script", "last"))
            def scriptNode = scriptOutcome.getNodeByXPath('//script')
            scriptOutcome.setNodeValue(scriptNode, newScriptContent, true)
            //scriptOutcome.setField("script", newScriptContent)

            //parses xml and compiles script to check syntax
            new Script(scriptItem.name, -1, null, scriptOutcome.getData()) 

            updateResourceItem(scriptItem, SCRIPT_RESOURCE, scriptOutcome.getData(true))
        }
    }
}
