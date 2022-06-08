package org.cristalise.dsl.module

import static org.cristalise.dsl.module.GitStatus.*

import java.nio.file.Path
import java.nio.file.Paths

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic @Slf4j
class IncludeHandler {
    
    private List<Path> changedModuleFiles = []

    public void captureModuleFileChanges(String moduleDir) {
        def moduleFilesStatus = GitStatus.getStatusMapForWorkTree(moduleDir)

        for (GitStatus status: moduleFilesStatus.keySet()) {
            List<Path> files = moduleFilesStatus.get(status)

            switch(status) {
                case ADDED:
                case UNTRACKED:
                case MODIFIED:
                    changedModuleFiles.addAll(files)
                    break

                case REMOVED:
                    log.trace('captureModuleFileChanges() - removed files:()', files)
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
