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
import java.nio.file.Paths

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic @Slf4j
class IncludeHandler {
    
    private List<Path> changedModuleFiles = []
    private List<Path> changedScriptFiles = []
    
    public List<Path> getChangedScriptFiles() {
        return changedScriptFiles
    }

    private void checkChangedFile(Path file) {
        def dirName = file.parent.fileName.toString()

        if ( ['script', 'scripts'].contains(dirName) ) {
            changedScriptFiles.add(file)
        }
        else {
            changedModuleFiles.add(file)
        }
    }

    public void captureModuleFileChanges(String moduleDir) {
        def moduleFilesStatus = GitStatus.getStatusMapForWorkTree(moduleDir)

        for (GitStatus status: moduleFilesStatus.keySet()) {
            List<Path> files = moduleFilesStatus.get(status)

            switch(status) {
                case ADDED:
                case UNTRACKED:
                case MODIFIED:
                    for (Path file : files) checkChangedFile(file)
                    break

                case REMOVED:
                    log.trace('captureModuleFileChanges() - cannot handle removed files:()', files)
                    break

                default:
                    log.error('captureModuleFileChanges() - unknow GitStatus:{}', status)
                    break
            }
        }
    }

    public boolean shallInclude(String file) {
        return changedModuleFiles.contains(Paths.get(file).normalize())
    }
}
