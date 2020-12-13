/*
 * This file is part of bowler-intellij.
 *
 * bowler-intellij is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * bowler-intellij is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with bowler-intellij.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.commonwealthrobotics.bowlerintellij.plugin

import arrow.core.Tuple3
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeZero
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.IllegalStateException

internal class ProjectDiffTest {

    @Test
    fun `get the diff of a project without staged changes`(@TempDir tempDir: File) {
        val (local, _, file1) = initReposWithOnePushedCommit(tempDir)

        // Write something new into the file to generate an unstaged diff
        file1.writeText("2")

        // The diff should be empty because there are no staged changes
        ProjectDiffUtil.getDiff(local).size.shouldBeZero()
    }

    @Test
    fun `get the diff of a project with staged changes`(@TempDir tempDir: File) {
        val (local, _, file1) = initReposWithOnePushedCommit(tempDir)

        // Write something new into the file to generate an unstaged diff
        file1.writeText("2")
        // Stage that change
        runAndPrintOutput(local, "git", "add", file1.path)

        // The diff should include the change from 1 to 2 inside file1
        val diff = ProjectDiffUtil.getDiff(local).decodeToString()
        diff.shouldContain("-1")
        diff.shouldContain("+2")
        diff.shouldContain(file1.name)
    }

    @Test
    fun `get the diff of a project with a branch that is ahead of an empty remote`(@TempDir tempDir: File) {
        val (local, _, _) = initReposWithOneUnpushedCommit(tempDir)

        // Getting the diff will fail because the remote is empty
        // TODO: Can we work around this somehow?
        shouldThrow<IllegalStateException> { ProjectDiffUtil.getDiff(local) }
    }

    @Test
    fun `get the diff of a project with a branch that is ahead of the remote`(@TempDir tempDir: File) {
        val (local, _, file1) = initReposWithOnePushedCommit(tempDir)

        // Make a new commit to be ahead of the remote again
        file1.writeText("2")
        runAndPrintOutput(local, "git", "add", file1.path)
        runAndPrintOutput(local, "git", "commit", "-m", "a")

        // The diff should include this commit because we haven't pushed it yet
        val diff = ProjectDiffUtil.getDiff(local).decodeToString()
        diff.shouldContain("-1")
        diff.shouldContain("+2")
        diff.shouldContain(file1.name)

        // Now push the commit. The diff should be empty.
        runAndPrintOutput(local, "git", "push")
        ProjectDiffUtil.getDiff(local).size.shouldBeZero()
    }

    private fun initReposWithOneUnpushedCommit(tempDir: File): Tuple3<File, File, File> {
        val local = tempDir.resolve("local").apply { mkdir().shouldBeTrue() }
        val remote = tempDir.resolve("remote").apply { mkdir().shouldBeTrue() }

        // Create a bare repo on the remote
        runAndPrintOutput(remote, "git", "init", "--bare", ".")

        // Clone the remote
        runAndPrintOutput(local.parentFile, "git", "clone", remote.path, local.path)

        // Make a commit to be ahead of the remote
        val file1 = createTempFile(suffix = ".groovy", directory = local)
        file1.writeText("1")
        runAndPrintOutput(local, "git", "add", file1.path)
        runAndPrintOutput(local, "git", "commit", "-m", "a")

        return Tuple3(local, remote, file1)
    }

    private fun initReposWithOnePushedCommit(tempDir: File): Tuple3<File, File, File> {
        val (local, remote, file1) = initReposWithOneUnpushedCommit(tempDir)

        // Push that commit so that the remote is in the working tree
        runAndPrintOutput(local, "git", "push")

        return Tuple3(local, remote, file1)
    }
}
