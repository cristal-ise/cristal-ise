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

    public static Map<GitStatus, List<Path>> getStatusMapForWorkingTree(String workDir) {
        RepositoryBuilder repositoryBuilder = new RepositoryBuilder()

        Repository repo = repositoryBuilder
            //.setWorkTree(new File(workDir))
            .findGitDir(new File(workDir))
            .setMustExist(true)
            .build()

        Git git = Git.wrap(repo)
        Status currentStatus = git.status().call()

        Map<GitStatus, List<Path>> statusMap = [:]
        Path workDirPath = Paths.get(workDir).normalize();

        if (!currentStatus.isClean()) {
            currentStatus.getAdded().each {
                Path filePath = Paths.get(it).normalize();
                updateStatusMap(statusMap, ADDED, filePath, workDirPath)
            }

            currentStatus.getModified().each {
                Path filePath = Paths.get(it).normalize();
                updateStatusMap(statusMap, MODIFIED, filePath, workDirPath)
            }

            currentStatus.getRemoved().each {
                Path filePath = Paths.get(it).normalize();
                updateStatusMap(statusMap, REMOVED, filePath, workDirPath)
            }

            currentStatus.getUntracked().each {
                Path filePath = Paths.get(it).normalize();
                updateStatusMap(statusMap, UNTRACKED, filePath, workDirPath)
            }
        }

        return statusMap
    }

    private static void updateStatusMap(Map<GitStatus, List<Path>> statusMap, GitStatus status, Path file, Path workDir) {
        // static final log variable created by @slf4j does not work with enums
        final Logger log = LoggerFactory.getLogger(GitStatus.class)


        if (file.startsWith(workDir)) {
            log.info('updateStatusMaps() - {}:{}', status, file)

            if (!statusMap[status]) statusMap[status] = []
            statusMap[status].add(file)
        }
        else {
            log.debug('updateStatusMaps() - SKIPPING {}:{} workDir:{}', status, file, workDir)
        }
    }
}
