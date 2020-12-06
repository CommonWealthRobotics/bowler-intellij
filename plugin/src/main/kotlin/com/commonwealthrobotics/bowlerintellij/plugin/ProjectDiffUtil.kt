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

import com.commonwealthrobotics.proto.script_host.RunRequest
import com.google.protobuf.ByteString
import com.intellij.openapi.vfs.VirtualFile
import mu.KotlinLogging
import java.io.File

object ProjectDiffUtil {

    internal fun setFileSpec(runRequestBuilder: RunRequest.Builder, projectDir: VirtualFile, scriptFile: VirtualFile) {
        // TODO: Support projects that are not git-init'd

        val projectDirFile = projectDir.toNioPath().toFile()
        val relativeScriptPath = scriptFile.path.substring(projectDir.path.length + 1)
        logger.debug { "relativeScriptPath=$relativeScriptPath" }

        runRequestBuilder.fileBuilder.projectBuilder.repoRemote = getRemoteURL(projectDirFile)
        runRequestBuilder.fileBuilder.projectBuilder.revision = getRemoteHead(projectDirFile)
        runRequestBuilder.fileBuilder.projectBuilder.patchBuilder.patch = ByteString.copyFrom(getDiff(projectDirFile))
        runRequestBuilder.fileBuilder.path = relativeScriptPath
    }

    private fun getRemoteURL(projectDir: File): String {
        // TODO: Use JGit instead
        val url = run(projectDir, "git", "remote", "get-url", "origin")
            .inputStream
            .readAllBytes()
            .decodeToString()
            .trim()

        // Transform an SSH url into an HTTPS url because the kernel doesn't support SSH yet
        return if (url.startsWith("git@")) {
            val newUrl = "https://github.com/${url.split("git@github.com:", ignoreCase = true, limit = 2)[1]}"
            logger.warn { "Transforming SSH URL into HTTPS URL: $url -> $newUrl" }
            newUrl
        } else url
    }

    internal fun getBranchName(projectDir: File): String {
        // TODO: Use JGit instead
        return run(projectDir, "git", "rev-parse", "--abbrev-ref", "HEAD")
            .inputStream
            .readAllBytes()
            .decodeToString()
            .trim()
    }

    internal fun getRemoteHead(projectDir: File): String {
        val branchName = getBranchName(projectDir)
        // TODO: Use JGit instead
        return run(projectDir, "git", "rev-parse", "origin/$branchName")
            .inputStream
            .readAllBytes()
            .decodeToString()
            .trim()
    }

    internal fun getDiff(projectDir: File): ByteArray {
        // TODO: Get the diff including only staged changes
        // TODO: Get the diff relative to the remote's HEAD
        // TODO: Use JGit instead
        val ref = getBranchName(projectDir)
        return try {
            run(projectDir, "git", "diff", "origin/$ref", "--cached").inputStream.readAllBytes()
        } catch (ex: IllegalStateException) {
            // TODO: Can we work around this somehow?
            throw IllegalStateException(
                "Failed to get project diff against the remote. Do you have unpushed commits and an empty remote?",
                ex
            )
        }
    }

    private val logger = KotlinLogging.logger { }
}
