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

import com.commonwealthrobotics.proto.script_host.ConfirmationValue
import com.commonwealthrobotics.proto.script_host.RequestError
import com.commonwealthrobotics.proto.script_host.SessionClientMessage
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.process.BaseOSProcessHandler
import com.intellij.openapi.project.guessProjectDir
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.selects.select
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicLong

/**
 * Simplifies interactions with the kernel server's API.
 *
 * @param kernelConnectionManager The [KernelConnectionManager] used to interact with the kernel server's API.
 */
class DefaultBowlerKernelFacade(private val kernelConnectionManager: KernelConnectionManager) : BowlerKernelFacade {

    private val responses = Channel<SessionClientMessage.Builder?>()
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun runScript(config: BowlerScriptRunConfiguration): ExecutionResult {
        if (!kernelConnectionManager.isConnected) {
            return DefaultExecutionResult()
        }

        logger.debug { "Running configuration: $config" }
        val scriptFile = config.getScriptFile()
        logger.debug { "Project directory: ${config.project.guessProjectDir()}" }
        val projectDir = config.project.guessProjectDir()!!
        val requestId = nextRequestId()

        val session = kernelConnectionManager.stub.session(
            flow {
                val msg = SessionClientMessage.newBuilder().apply {
                    runRequestBuilder.requestId = requestId
                    ProjectDiffUtil.setFileSpec(runRequestBuilder, projectDir, scriptFile)
                }.build()
                logger.debug { "Sending request: $msg" }
                emit(msg)

                do {
                    val cont = select<Boolean> {
                        responses.onReceive {
                            it?.let {
                                val response = it.build()
                                logger.debug { "Sending response: $response" }
                                emit(response)
                                true
                            } ?: false
                        }
                    }
                } while (cont)
            }
        )

        val process = BowlerProcess.suspended(scope) {
            var error: RequestError? = null

            session.collect {
                when {
                    it.hasCredentialsRequest() -> responses.send(
                        SessionClientMessage.newBuilder().apply {
                            credentialsResponseBuilder.requestId = it.credentialsRequest.requestId
                        }
                    )

                    it.hasConfirmationRequest() -> responses.send(
                        SessionClientMessage.newBuilder().apply {
                            confirmationResponseBuilder.requestId = it.confirmationRequest.requestId
                            confirmationResponseBuilder.response = ConfirmationValue.ALLOWED
                        }
                    )

                    it.hasError() -> {
                        error = it.error
                        logger.debug { "Script error: $error" }
                        responses.send(null)
                    }

                    it.hasScriptOutput() -> {
                        logger.debug { "Script output: ${it.scriptOutput.output}" }
                        responses.send(null)
                    }
                }
            }

            if (error == null) {
                logger.debug { "Script exited successfully." }
                0
            } else {
                logger.debug { "Script exited with an error." }
                1
            }
        }

        return DefaultExecutionResult(BaseOSProcessHandler(process, "bowler-process-fake-cmdline", Charsets.UTF_8))
    }

    companion object {
        private val logger = KotlinLogging.logger { }
        private val requestId = AtomicLong(0)
        private fun nextRequestId() = requestId.getAndIncrement()
    }
}
