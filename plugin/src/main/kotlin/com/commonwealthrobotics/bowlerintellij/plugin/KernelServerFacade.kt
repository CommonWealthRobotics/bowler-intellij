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

import mu.KotlinLogging
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class KernelServerFacade(
    private val desiredName: String,
//        private val cliPath: Path = Paths.get(System.getProperty("user.home"), ".bowler", "bowler-kernel")
    private val cliPath: Path = Paths.get(
        "/home/salmon/Documents/bowler-kernel/cli/build/install/cli/bin/bowler-kernel"
    )
) {

    private var proc: Process? = null
    private var procOS: BufferedWriter? = null
    var isStarted = false

    /**
     * @return false if there was an error.
     */
    fun ensureStarted() {
        synchronized(this) {
            if (proc == null) {
                val processBuilder = ProcessBuilder(cliPath.toAbsolutePath().toString())

                logger.debug { "Starting discovery server and kernel server." }
                proc = processBuilder.start()
                procOS = BufferedWriter(OutputStreamWriter(proc!!.outputStream)).apply {
                    write(
                        """
                        |server start
                        |discovery start-server --name $desiredName
                        |
                    """.trimMargin()
                    )
                    flush()
                }
                isStarted = true
            }
        }
    }

    /**
     * @return false is there was an error.
     */
    fun ensureStopped() {
        synchronized(this) {
            proc?.also {
                logger.debug { "Stopping discovery server and kernel server." }
                procOS!!.apply {
                    write(
                        """
                        |discovery stop-server
                        |server stop
                        |
                    """.trimMargin()
                    )
                    flush()
                    close()
                }

                if (it.waitFor(10, TimeUnit.SECONDS)) {
                    val exitCode = it.exitValue()
                    if (exitCode != 0) {
                        logger.warn {
                            """
                            |Kernel server exited with non-zero exit code.
                            |exit code: $exitCode
                            |stdout: ${it.inputStream.readAllBytes().decodeToString()}
                            |stderr: ${it.errorStream.readAllBytes().decodeToString()}
                            """.trimMargin()
                        }
                    }
                } else {
                    logger.error {
                        "Timed out waiting for the kernel server to stop."
                    }
                    it.destroyForcibly()
                }
            }

            proc = null
            isStarted = false
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
