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
package org.cristalise.dsl.test.module

import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.cristalise.dsl.module.GitStatus
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.dircache.DirCache
import org.eclipse.jgit.dircache.DirCacheBuilder
import org.eclipse.jgit.dircache.DirCacheEntry
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository
import org.eclipse.jgit.lib.*
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static org.cristalise.dsl.module.GitStatus.*

@Slf4j
class GitStatusSpecs extends Specification {

    private ObjectId createBlob(ObjectInserter inserter, String content) {
        return inserter.insert(Constants.OBJ_BLOB, content.getBytes(StandardCharsets.UTF_8))
    }

    private ObjectId createTree(ObjectInserter inserter, Map<String, ObjectId> entries) {
        DirCache dc = DirCache.newInCore()
        DirCacheBuilder builder = dc.builder()
        entries.each { path, blobId ->
            DirCacheEntry entry = new DirCacheEntry(path)
            entry.setFileMode(FileMode.REGULAR_FILE)
            entry.setObjectId(blobId)
            builder.add(entry)
        }
        builder.finish()
        return dc.writeTree(inserter)
    }

    private ObjectId createCommit(InMemoryRepository repo, ObjectInserter inserter, ObjectId treeId, ObjectId parentCommitId = null) {
        PersonIdent author = new PersonIdent("Test User", "test@cristalise.org")
        CommitBuilder cb = new CommitBuilder()
        cb.setTreeId(treeId)
        cb.setAuthor(author)
        cb.setCommitter(author)
        cb.setMessage("Test commit")
        if (parentCommitId != null) {
            cb.setParentId(parentCommitId)
        }
        ObjectId commitId = inserter.insert(cb)
        inserter.flush()

        RefUpdate ru = repo.updateRef(Constants.HEAD)
        ru.setNewObjectId(commitId)
        ru.update()

        return commitId
    }

    def "InMemoryRepository - getStatusMap with HEAD commit against parent commit"() {
        given: "an InMemoryRepository with two consecutive commits"
        DfsRepositoryDescription desc = new DfsRepositoryDescription("test-repo")
        InMemoryRepository repo = new InMemoryRepository(desc)
        ObjectInserter inserter = repo.newObjectInserter()

        ObjectId blobA = createBlob(inserter, "Initial A")
        ObjectId tree1 = createTree(inserter, ["src/FileA.txt": blobA])
        ObjectId commit1 = createCommit(repo, inserter, tree1)

        ObjectId blobAMod = createBlob(inserter, "Modified A")
        ObjectId blobB = createBlob(inserter, "Added B")
        ObjectId tree2 = createTree(inserter, ["src/FileA.txt": blobAMod, "src/FileB.txt": blobB])
        ObjectId commit2 = createCommit(repo, inserter, tree2, commit1)

        when: "getting status map using default HEAD"
        Map<GitStatus, List<Path>> statusMap = getStatusMap(repo, null)
        log.info("Status map for HEAD vs parent: {} commit: {}", statusMap, commit2)

        then: "status map detects changes in HEAD compared to parent"
        statusMap[MODIFIED] == [Paths.get("src/FileA.txt")]
        statusMap[ADDED] == [Paths.get("src/FileB.txt")]

        cleanup:
        repo?.close()
    }

    def "InMemoryRepository - getStatusMap on initial commit detects all files as ADDED"() {
        given: "an InMemoryRepository with a single initial commit"
        DfsRepositoryDescription desc = new DfsRepositoryDescription("test-repo")
        InMemoryRepository repo = new InMemoryRepository(desc)
        ObjectInserter inserter = repo.newObjectInserter()

        ObjectId blob1 = createBlob(inserter, "File 1")
        ObjectId blob2 = createBlob(inserter, "File 2")
        ObjectId tree = createTree(inserter, [
                "boot/schema/Schema1.xml": blob1,
                "boot/schema/Schema2.xml": blob2
        ])
        createCommit(repo, inserter, tree)

        when: "getting status map for initial commit"
        Map<GitStatus, List<Path>> statusMap = getStatusMap(repo, null)
        log.info("Status map for initial commit: {}", statusMap)

        then: "all files are detected as ADDED"
        statusMap[ADDED].size() == 2
        statusMap[ADDED].contains(Paths.get("boot/schema/Schema1.xml"))
        statusMap[ADDED].contains(Paths.get("boot/schema/Schema2.xml"))
        !statusMap.containsKey(MODIFIED)
        !statusMap.containsKey(REMOVED)

        cleanup:
        repo?.close()
    }

    def "InMemoryRepository - getStatusMap with workDir filter restricts paths"() {
        given: "an InMemoryRepository with changes in different directories"
        DfsRepositoryDescription desc = new DfsRepositoryDescription("test-repo")
        InMemoryRepository repo = new InMemoryRepository(desc)
        ObjectInserter inserter = repo.newObjectInserter()

        ObjectId blob1 = createBlob(inserter, "Schema")
        ObjectId blob2 = createBlob(inserter, "Script")
        ObjectId tree = createTree(inserter, [
                "boot/schema/Schema1.xml": blob1,
                "boot/script/Script1.groovy": blob2
        ])
        createCommit(repo, inserter, tree)

        when: "filtering status map by workDir 'boot/schema'"
        Map<GitStatus, List<Path>> statusMap = getStatusMap(repo, "boot/schema")

        then: "only files within 'boot/schema' are included"
        statusMap[ADDED] == [Paths.get("boot/schema/Schema1.xml")]

        cleanup:
        repo?.close()
    }

    def "InMemoryRepository - getStatusMap on empty repo returns empty map"() {
        given: "an empty InMemoryRepository with no commits"
        DfsRepositoryDescription desc = new DfsRepositoryDescription("test-repo")
        InMemoryRepository repo = new InMemoryRepository(desc)

        when:
        Map<GitStatus, List<Path>> statusMap = getStatusMap(repo, null)

        then:
        statusMap.isEmpty()

        cleanup:
        repo?.close()
    }

    def "Disk-based repository - getStatusMap correctly identifies changes"() {
        given: "a temporary git repository on disk"
        Path tempDir = Files.createTempDirectory("git-disk-test")
        Git git = Git.init().setDirectory(tempDir.toFile()).call()
        Repository diskRepo = git.repository

        File file1 = new File(tempDir.toFile(), "file1.txt")
        file1.text = "Initial file 1"
        git.add().addFilepattern("file1.txt").call()
        git.commit().setMessage("Initial commit").call()

        // Modify file1, add untracked file2
        file1.text = "Modified file 1"
        File file2 = new File(tempDir.toFile(), "file2.txt")
        file2.text = "Untracked file 2"

        when: "checking status map for work tree by path and by Repository"
        Map<GitStatus, List<Path>> statusMapByPath = getStatusMap(tempDir.toString())
        Map<GitStatus, List<Path>> statusMapByRepo = getStatusMap(diskRepo, tempDir.toString())

        then:
        statusMapByPath[MODIFIED] == [Paths.get(tempDir.toString(), "file1.txt").normalize()]
        statusMapByPath[UNTRACKED] == [Paths.get(tempDir.toString(), "file2.txt").normalize()]
        statusMapByRepo == statusMapByPath

        cleanup:
        diskRepo?.close()
        git?.close()
        FileUtils.deleteDirectory(tempDir.toFile())
    }

    def "Error - getStatusMap when repo is null and workDir does not exist throws IllegalArgumentException"() {
        when:
        getStatusMap(null, "non/existing/directory/path/here")

        then:
        thrown(IllegalArgumentException)
    }

    def "Error - getStatusMap on null repository with null workDir throws NullPointerException"() {
        when:
        getStatusMap(null, null)

        then:
        thrown(NullPointerException)
    }
}
