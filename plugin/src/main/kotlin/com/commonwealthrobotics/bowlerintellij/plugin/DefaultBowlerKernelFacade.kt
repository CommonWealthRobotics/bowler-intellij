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

import arrow.core.Either
import com.commonwealthrobotics.proto.script_host.ConfirmationValue
import com.commonwealthrobotics.proto.script_host.RequestError
import com.commonwealthrobotics.proto.script_host.ScriptOutput
import com.commonwealthrobotics.proto.script_host.SessionClientMessage
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.process.BaseOSProcessHandler
import com.intellij.openapi.project.guessProjectDir
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.selects.select
import mu.KotlinLogging
import org.junit.runner.Request
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/**
 * Simplifies interactions with the kernel server's API.
 *
 * @param kernelConnectionManager The [KernelConnectionManager] used to interact with the kernel server's API.
 */
class DefaultBowlerKernelFacade(private val kernelConnectionManager: KernelConnectionManager) : BowlerKernelFacade {

    private val responses = Channel<SessionClientMessage.Builder?>()
    private val scope = CoroutineScope(Executors.newFixedThreadPool(2).asCoroutineDispatcher())

    override fun runScript(config: BowlerScriptRunConfiguration): ExecutionResult {
        if (!kernelConnectionManager.isConnected) {
            return DefaultExecutionResult()
        }

        logger.debug { "Running configuration: $config" }
        val scriptFile = config.getScriptFile()
        logger.debug { "Project directory: ${config.project.guessProjectDir()}" }
        val projectDir = config.projectDir
        val requestId = nextRequestId()

        val msg = SessionClientMessage.newBuilder().apply {
            runRequestBuilder.requestId = requestId
            ProjectDiffUtil.setFileSpec(runRequestBuilder, projectDir, scriptFile)
        }

        val process = BowlerProcess.suspended(scope) {
            @Suppress("UNREACHABLE_CODE")
            val result = kernelConnectionManager.request<Either<RequestError, ScriptOutput>>(msg) { sendCh, recvCh ->
                while (true) {
                    logger.debug { "Waiting to receive" }
                    val response = recvCh.receive()
                    when {
                        response.hasCredentialsRequest() -> sendCh.send(
                            SessionClientMessage.newBuilder().apply {
                                credentialsResponseBuilder.requestId = response.credentialsRequest.requestId
                            }
                        )

                        response.hasConfirmationRequest() -> sendCh.send(
                            SessionClientMessage.newBuilder().apply {
                                confirmationResponseBuilder.requestId = response.confirmationRequest.requestId
                                confirmationResponseBuilder.response = ConfirmationValue.ALLOWED
                            }
                        )

                        response.hasError() -> {
                            logger.debug { "Script error: ${response.error}" }
                            return@request Either.Left(response.error)
                        }

                        response.hasScriptOutput() -> {
                            logger.debug { "Script output: ${response.scriptOutput.output}" }
                            return@request Either.Right(response.scriptOutput)
                        }
                    }
                }

                error("Not possible to get here.")
            }

            logger.debug { "Result of RunRequest ${msg.runRequest.requestId}: $result" }
            result.fold({ 1 }, { 0 })
        }

        return DefaultExecutionResult(BaseOSProcessHandler(process, "bowler-process-fake-cmdline", Charsets.UTF_8))
    }

    companion object {
        private val logger = KotlinLogging.logger { }
        private val requestId = AtomicLong(1)
        private fun nextRequestId() = requestId.getAndIncrement()
    }
}
