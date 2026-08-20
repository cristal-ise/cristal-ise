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
import org.eclipse.jgit.diff.*
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectInserter
import org.eclipse.jgit.lib.ObjectReader
import spock.lang.Specification

import java.nio.charset.StandardCharsets

/**
 * Specification demonstrating how to compare two strings in memory using JGit tools.
 */
@Slf4j
class GitStringCompareSpecs extends Specification {

    def "compare identical strings in memory produces empty edit list"() {
        given: "two identical strings"
        String text1 = "line 1\nline 2\nline 3\n"
        String text2 = "line 1\nline 2\nline 3\n"

        RawText rawText1 = new RawText(text1.getBytes(StandardCharsets.UTF_8))
        RawText rawText2 = new RawText(text2.getBytes(StandardCharsets.UTF_8))

        DiffAlgorithm algorithm = DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.HISTOGRAM)

        when: "comparing the two strings"
        EditList edits = algorithm.diff(RawTextComparator.DEFAULT, rawText1, rawText2)

        then: "there are no differences"
        edits.isEmpty()
    }

    def "compare different strings and inspect edit types and line ranges"() {
        given: "two strings with modifications, additions, and deletions"
        String original = """line 1
line 2
line 3
line 4
"""
        String modified = """line 1
line 2 modified
line 3
line 4
line 5 added
"""

        RawText rawOriginal = new RawText(original.getBytes(StandardCharsets.UTF_8))
        RawText rawModified = new RawText(modified.getBytes(StandardCharsets.UTF_8))

        DiffAlgorithm algorithm = new HistogramDiff()

        when: "diffing the strings"
        EditList edits = algorithm.diff(RawTextComparator.DEFAULT, rawOriginal, rawModified)

        then: "edits contain the expected changes"
        edits.size() == 2

        // First edit is a replacement at line 2 (0-based index 1)
        Edit edit1 = edits[0]
        edit1.type == Edit.Type.REPLACE
        edit1.beginA == 1
        edit1.endA == 2
        edit1.beginB == 1
        edit1.endB == 2

        // Second edit is an insertion at the end
        Edit edit2 = edits[1]
        edit2.type == Edit.Type.INSERT
        edit2.beginA == 4
        edit2.endA == 4
        edit2.beginB == 4
        edit2.endB == 5
    }

    def "generate formatted unified diff between two strings in memory"() {
        given: "two strings"
        String textA = "apple\nbanana\ncherry\n"
        String textB = "apple\nblueberry\ncherry\ndate\n"

        RawText rawA = new RawText(textA.getBytes(StandardCharsets.UTF_8))
        RawText rawB = new RawText(textB.getBytes(StandardCharsets.UTF_8))

        DiffAlgorithm algorithm = DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.HISTOGRAM)
        EditList edits = algorithm.diff(RawTextComparator.DEFAULT, rawA, rawB)

        ByteArrayOutputStream out = new ByteArrayOutputStream()
        DiffFormatter formatter = new DiffFormatter(out)

        when: "formatting the diff into the output stream"
        formatter.format(edits, rawA, rawB)
        String diffResult = out.toString(StandardCharsets.UTF_8.name())
        log.info("Unified diff output:\n{}", diffResult)

        then: "the diff output contains unified diff hunks"
        diffResult != null
        diffResult.contains("-banana")
        diffResult.contains("+blueberry")
        diffResult.contains("+date")
        diffResult.contains("@@ -1,3 +1,4 @@")
    }

    def "compare strings ignoring whitespace differences"() {
        given: "two strings differing only by whitespace"
        String text1 = "def   foo()  {\n    return true\n}\n"
        String text2 = "def foo() {\n  return true\n}\n"

        RawText rawText1 = new RawText(text1.getBytes(StandardCharsets.UTF_8))
        RawText rawText2 = new RawText(text2.getBytes(StandardCharsets.UTF_8))

        DiffAlgorithm algorithm = new HistogramDiff()

        when: "comparing with DEFAULT comparator"
        EditList defaultEdits = algorithm.diff(RawTextComparator.DEFAULT, rawText1, rawText2)

        and: "comparing with WS_IGNORE_ALL comparator"
        EditList wsIgnoredEdits = algorithm.diff(RawTextComparator.WS_IGNORE_ALL, rawText1, rawText2)

        then: "default comparator detects changes but whitespace-ignoring comparator does not"
        !defaultEdits.isEmpty()
        wsIgnoredEdits.isEmpty()
    }

    def "compare strings using MyersDiff algorithm"() {
        given: "two strings"
        String textA = "alpha\nbeta\ngamma\n"
        String textB = "alpha\nbeta modified\ngamma\n"

        RawText rawA = new RawText(textA.getBytes(StandardCharsets.UTF_8))
        RawText rawB = new RawText(textB.getBytes(StandardCharsets.UTF_8))

        when: "diffing with Myers algorithm"
        EditList edits = MyersDiff.INSTANCE.diff(RawTextComparator.DEFAULT, rawA, rawB)

        then: "edit is detected as REPLACE"
        edits.size() == 1
        edits[0].type == Edit.Type.REPLACE
    }

    def "compare strings with line deletions and inspect deleted lines"() {
        given: "an original string and a modified string with lines removed"
        String original = "line 1\nline 2 to remove\nline 3\n"
        String modified = "line 1\nline 3\n"

        RawText rawOriginal = new RawText(original.getBytes(StandardCharsets.UTF_8))
        RawText rawModified = new RawText(modified.getBytes(StandardCharsets.UTF_8))

        when: "diffing the strings"
        EditList edits = new HistogramDiff().diff(RawTextComparator.DEFAULT, rawOriginal, rawModified)

        then: "edit type is DELETE and deleted content can be retrieved from original"
        edits.size() == 1
        Edit edit = edits[0]
        edit.type == Edit.Type.DELETE
        edit.beginA == 1
        edit.endA == 2
        rawOriginal.getString(edit.beginA) == "line 2 to remove"
    }

    def "compare XML strings and configure diff context lines"() {
        given: "two XML documents differing in one property"
        String originalXml = """<item>
    <name>TestItem</name>
    <version>1</version>
    <description>Old description</description>
    <author>Consortium</author>
</item>
"""
        String modifiedXml = """<item>
    <name>TestItem</name>
    <version>1</version>
    <description>New description</description>
    <author>Consortium</author>
</item>
"""

        RawText rawA = new RawText(originalXml.getBytes(StandardCharsets.UTF_8))
        RawText rawB = new RawText(modifiedXml.getBytes(StandardCharsets.UTF_8))

        EditList edits = DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.HISTOGRAM)
                .diff(RawTextComparator.DEFAULT, rawA, rawB)

        ByteArrayOutputStream out = new ByteArrayOutputStream()
        DiffFormatter formatter = new DiffFormatter(out)
        formatter.setContext(1)

        when: "formatting diff with context = 1"
        formatter.format(edits, rawA, rawB)
        String diff = out.toString(StandardCharsets.UTF_8.name())
        log.info("XML diff output with custom context:\n{}", diff)

        then: "diff includes only 1 line of context around the change"
        diff.contains("-    <description>Old description</description>")
        diff.contains("+    <description>New description</description>")
        diff.contains("     <version>1</version>")
        diff.contains("     <author>Consortium</author>")
        !diff.contains("<name>TestItem</name>")
    }

    def "compare strings as blobs in an in-memory repository"() {
        given: "an in-memory repository and two strings"
        DfsRepositoryDescription desc = new DfsRepositoryDescription("test-repo")
        InMemoryRepository repo = new InMemoryRepository(desc)
        ObjectInserter inserter = repo.newObjectInserter()

        String text1 = "Hello World\nLine 2\n"
        String text2 = "Hello World\nLine 2 modified\nLine 3\n"

        ObjectId id1 = inserter.insert(Constants.OBJ_BLOB, text1.getBytes(StandardCharsets.UTF_8))
        ObjectId id2 = inserter.insert(Constants.OBJ_BLOB, text2.getBytes(StandardCharsets.UTF_8))
        inserter.flush()
        log.info("Inserted blobs with ids {} and {}", id1, id2)

        ObjectReader reader = repo.newObjectReader()
        RawText raw1 = new RawText(reader.open(id1, Constants.OBJ_BLOB).getBytes())
        RawText raw2 = new RawText(reader.open(id2, Constants.OBJ_BLOB).getBytes())

        when: "comparing the loaded blobs"
        EditList edits = new HistogramDiff().diff(RawTextComparator.DEFAULT, raw1, raw2)

        ByteArrayOutputStream out = new ByteArrayOutputStream()
        DiffFormatter formatter = new DiffFormatter(out)
        formatter.format(edits, raw1, raw2)
        String diff = out.toString(StandardCharsets.UTF_8.name())
        log.info("In-memory repository diff output:\n{}", diff)

        then: "diff correctly identifies the modifications"
        edits.size() == 1
        edits[0].type == Edit.Type.REPLACE
        diff.contains("-Line 2")
        diff.contains("+Line 2 modified")
        diff.contains("+Line 3")

        cleanup:
        repo?.close()
    }
}
