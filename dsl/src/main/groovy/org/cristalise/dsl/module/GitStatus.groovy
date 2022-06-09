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

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.transform.CompileStatic

/**
 * 
 */
@CompileStatic
public enum GitStatus {

    ADDED, MODIFIED, REMOVED, UNTRACKED;

    public static Map<GitStatus, List<Path>> getStatusMapForWorkTree(String workDir) {
        RepositoryBuilder repositoryBuilder = new RepositoryBuilder()

        Repository repo = repositoryBuilder
            .findGitDir(new File(workDir))
            .setMustExist(true)
            .build()

        Git git = Git.wrap(repo)
        Status currentStatus = git.status().call()

        def gitParentDirPath = repo.directory.parentFile.toPath()

        Map<GitStatus, List<Path>> statusMap = new LinkedHashMap<>()
        Path workDirPath = Paths.get(workDir).normalize();

        if (!currentStatus.isClean()) {
            updateStatusMap(statusMap, ADDED,     currentStatus.getAdded(),     gitParentDirPath, workDirPath)
            updateStatusMap(statusMap, MODIFIED,  currentStatus.getModified(),  gitParentDirPath, workDirPath)
            updateStatusMap(statusMap, REMOVED,   currentStatus.getRemoved(),   gitParentDirPath, workDirPath)
            updateStatusMap(statusMap, UNTRACKED, currentStatus.getUntracked(), gitParentDirPath, workDirPath)
        }

        return statusMap
    }

    private static void updateStatusMap(Map<GitStatus, List<Path>> statusMap, GitStatus status, Set<String> files, Path gitDir, Path workDir) {
        // static final log variable created by @slf4j does not work with enums
        final Logger log = LoggerFactory.getLogger(GitStatus.class)

        for (String file : files) {
            Path filePath = Paths.get(gitDir.toString(), file).normalize();

            if (filePath.startsWith(workDir)) {
                log.info('updateStatusMap() - adding {}:{}', status, filePath)
    
                if (!statusMap[status]) statusMap[status] = []
                statusMap[status].add(filePath)
            }
            else {
                log.debug('updateStatusMap() - SKIPPING {}:{} workDir:{}', status, filePath, workDir)
            }
        }
    }
}
