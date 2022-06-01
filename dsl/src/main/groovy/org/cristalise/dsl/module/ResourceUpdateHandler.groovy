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

import java.nio.file.Path

import org.cristalise.kernel.entity.Job
import org.cristalise.kernel.entity.proxy.AgentProxy
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.instance.predefined.Erase
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.process.resource.BuiltInResources

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic @Slf4j
class ResourceUpdateHandler {
    AgentProxy updateAgent = null
    String moduleNamespace

    public ResourceUpdateHandler(AgentProxy agent, String moduleNs) {
        updateAgent = agent
        moduleNamespace = moduleNs
    }

    /**
     * Retrieves git status of XML files in the resources/boot directory 
     * and updates the Items using Activities defined in the kernel.
     * 
     * @param resourceBootDir the actual resources/boot directory
     */
    public void updateChanges(String resourceBootDir) {
        def resourcesStatus = GitStatus.getStatusMapForWorkTree(resourceBootDir)

        for (GitStatus status: resourcesStatus.keySet()) {
            List<Path> files = resourcesStatus.getAt(status)

            switch(status) {
                case ADDED:
                case UNTRACKED:
                    createItems(files)
                    break

                case MODIFIED:
                    updateItems(files)
                    break

                case REMOVED:
                    removeItems(files)
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

    private void createItems(List<Path> files) {
        log.warn('createItems() - NO factories defined in the kernel - files:{}', files)
    }

    private void removeItems(List<Path> files) {
        for (Path filePath : files) {
            ItemProxy resItem = getResourceItem(filePath)
            updateAgent.execute(resItem, Erase.class)
        }
    }

    private void updateItems(List<Path> files) {
        for (Path filePath : files) {
            ItemProxy resItem = getResourceItem(filePath)

            def resourceType = BuiltInResources.getValue(filePath.getParent().getFileName().toString())

            Job editJob = resItem.getJobByName(resourceType.getEditActivityName(), updateAgent)
            editJob.setOutcome(filePath.getText('UTF-8'))
            updateAgent.execute(editJob)

            Job moveJob = resItem.getJobByName(resourceType.getMoveVersionActivityName(), updateAgent)
            updateAgent.execute(moveJob)
        }
    }
}
