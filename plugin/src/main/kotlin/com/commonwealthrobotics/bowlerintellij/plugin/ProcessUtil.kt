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

import mu.KLogger
import java.io.File

fun run(dir: File, vararg cmd: String): Process = ProcessBuilder(*cmd).directory(dir).start().also {
    val exitCode = it.waitFor()
    check(exitCode == 0) {
        """
            |Process exited with a non-zero exit code.
            |exit code: $exitCode
            |dir: $dir
            |cmd: ${cmd.joinToString()}
            """.trimMargin()
    }
}

fun runAndPrintOutput(
    logger: KLogger,
    dir: File,
    vararg cmd: String
): Process = ProcessBuilder(*cmd).directory(dir).start().also {
    val exitCode = it.waitFor()
    logger.debug { it.inputStream.readAllBytes().decodeToString() }
    logger.debug { it.errorStream.readAllBytes().decodeToString() }
    check(exitCode == 0) {
        """
            |Process exited with a non-zero exit code.
            |exit code: $exitCode
            |dir: $dir
            |cmd: ${cmd.joinToString()}
            """.trimMargin()
    }
}

fun runAndPrintOutput(dir: File, vararg cmd: String): Process = ProcessBuilder(*cmd).directory(dir).start().also {
    val exitCode = it.waitFor()
    // Print these so that they show up in the console output. console appenders don't work during tests.
    println(it.inputStream.readAllBytes().decodeToString())
    println(it.errorStream.readAllBytes().decodeToString())
    check(exitCode == 0) {
        """
            |Process exited with a non-zero exit code.
            |exit code: $exitCode
            |dir: $dir
            |cmd: ${cmd.joinToString()}
            """.trimMargin()
    }
}
