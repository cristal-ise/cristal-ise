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

import groovy.transform.CompileStatic
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.EmptyTreeIterator
import org.eclipse.jgit.util.io.DisabledOutputStream
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Path
import java.nio.file.Paths

/**
 * 
 */
@CompileStatic
enum GitStatus {

    ADDED, MODIFIED, REMOVED, UNTRACKED;

    // static final log variable created by @SLF4J does not work with enums
    final static Logger log = LoggerFactory.getLogger(GitStatus.class)

    static Map<GitStatus, List<Path>> getStatusMap(Repository repo = null, String workDir) {
        if (repo == null) repo = getDiskRepository(workDir)

        if (repo.isBare()) {
            return getStatusMapForHeadCommit(repo, workDir)
        }
        else {
            log.info("getStatusMap() - FILE Repository ")
            return getStatusMapForWorkDir(repo, workDir)
        }
    }

    private static LinkedHashMap<GitStatus, List<Path>> getStatusMapForWorkDir(Repository repo, String workDir) {
        Git git = Git.wrap(repo)
        Status currentStatus = git.status().call()

        Path gitParentDirPath = repo.directory.parentFile.toPath()
        Path workDirPath = workDir ? Paths.get(workDir).normalize() : null

        Map<GitStatus, List<Path>> statusMap = new LinkedHashMap<>()

        if (!currentStatus.isClean()) {
            updateStatusMap(statusMap, ADDED, currentStatus.getAdded(), gitParentDirPath, workDirPath)
            updateStatusMap(statusMap, MODIFIED, currentStatus.getModified(), gitParentDirPath, workDirPath)
            updateStatusMap(statusMap, REMOVED, currentStatus.getRemoved(), gitParentDirPath, workDirPath)
            updateStatusMap(statusMap, UNTRACKED, currentStatus.getUntracked(), gitParentDirPath, workDirPath)
        }

        return statusMap
    }

    private static Repository getDiskRepository(String workDir) {
        File workDirFile = new File(workDir)

        RepositoryBuilder repositoryBuilder = new RepositoryBuilder()

        return repositoryBuilder
            .findGitDir(workDirFile)
            .setMustExist(true)
            .build()
    }

    private static Map<GitStatus, List<Path>> getStatusMapForHeadCommit(Repository repo, String workDir) {
        log.info("getStatusMapForHeadCommit() - {} workDir:{}", repo, workDir)

        ObjectId head = repo.resolve(Constants.HEAD)

        if (head == null) return new LinkedHashMap<>()

        RevWalk rw = new RevWalk(repo)
        try {
            RevCommit headCommit = rw.parseCommit(head)
            ObjectId parentTreeId = headCommit.parentCount > 0 ? rw.parseCommit(headCommit.getParent(0)).tree.id : null
            ObjectId headTreeId = headCommit.tree.id

            return getStatusMapForTrees(repo, parentTreeId, headTreeId, workDir)
        }
        finally {
            rw.close()
        }
    }

    private static Map<GitStatus, List<Path>> getStatusMapForTrees(Repository repo, ObjectId oldTreeOrCommitId, ObjectId newTreeOrCommitId, String workDir) {
        ObjectReader reader = repo.newObjectReader()
        try {
            AbstractTreeIterator oldTreeIter = getTreeIterator(reader, repo, oldTreeOrCommitId)
            AbstractTreeIterator newTreeIter = getTreeIterator(reader, repo, newTreeOrCommitId)
            return getStatusMapForTreeIterators(repo, oldTreeIter, newTreeIter, workDir)
        }
        finally {
            reader.close()
        }
    }

    private static Map<GitStatus, List<Path>> getStatusMapForTreeIterators(Repository repo, AbstractTreeIterator oldTreeIter, AbstractTreeIterator newTreeIter, String workDir = null) {
        DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE)
        df.setRepository(repo)

        List<DiffEntry> diffEntries = df.scan(oldTreeIter, newTreeIter)

        Path gitParentDirPath = getGitParentDirPath(repo)
        Path workDirPath = workDir ? Paths.get(workDir).normalize() : null

        Map<GitStatus, List<Path>> statusMap = new LinkedHashMap<>()

        for (DiffEntry entry : diffEntries) {
            switch (entry.changeType) {
                case DiffEntry.ChangeType.ADD:
                case DiffEntry.ChangeType.COPY:
                    updateStatusMap(statusMap, ADDED, Collections.singletonList(entry.newPath), gitParentDirPath, workDirPath)
                    break
                case DiffEntry.ChangeType.MODIFY:
                    updateStatusMap(statusMap, MODIFIED, Collections.singletonList(entry.newPath), gitParentDirPath, workDirPath)
                    break
                case DiffEntry.ChangeType.DELETE:
                    updateStatusMap(statusMap, REMOVED, Collections.singletonList(entry.oldPath), gitParentDirPath, workDirPath)
                    break
                case DiffEntry.ChangeType.RENAME:
                    updateStatusMap(statusMap, REMOVED, Collections.singletonList(entry.oldPath), gitParentDirPath, workDirPath)
                    updateStatusMap(statusMap, ADDED, Collections.singletonList(entry.newPath), gitParentDirPath, workDirPath)
                    break
            }
        }

        return statusMap
    }

    private static Path getGitParentDirPath(Repository repo) {
        if (repo == null) {
            return Paths.get("")
        }
        if (repo.directory != null) {
            return repo.directory.parentFile.toPath()
        }
        try {
            if (!repo.isBare() && repo.workTree != null) {
                return repo.workTree.toPath()
            }
        }
        catch (Exception ignored) {}

        return Paths.get("")
    }

    private static AbstractTreeIterator getTreeIterator(ObjectReader reader, Repository repo, ObjectId treeOrCommitId) {
        if (treeOrCommitId == null) {
            return new EmptyTreeIterator()
        }

        RevWalk rw = new RevWalk(repo)
        try {
            RevTree tree = rw.parseTree(treeOrCommitId)
            CanonicalTreeParser treeIter = new CanonicalTreeParser()
            treeIter.reset(reader, tree.id)
            return treeIter
        }
        finally {
            rw.close()
        }
    }

    private static void updateStatusMap(Map<GitStatus, List<Path>> statusMap, GitStatus status, Collection<String> files, Path gitDir, Path workDir) {
        if (files == null) return

        for (String file : files) {
            Path filePath = (gitDir != null && !gitDir.toString().isEmpty()) ? Paths.get(gitDir.toString(), file).normalize() : Paths.get(file).normalize();

            if (workDir == null || filePath.startsWith(workDir)) {
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
