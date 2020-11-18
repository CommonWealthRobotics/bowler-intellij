package com.commonwealthrobotics.bowlerintellij.module

import com.commonwealthrobotics.bowlerkernel.server.KernelServer
import com.commonwealthrobotics.proto.script_host.ConfirmationValue
import com.commonwealthrobotics.proto.script_host.RequestError
import com.commonwealthrobotics.proto.script_host.ScriptHostGrpcKt
import com.commonwealthrobotics.proto.script_host.SessionClientMessage
import com.google.protobuf.ByteString
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.openapi.project.guessProjectDir
import io.grpc.ManagedChannelBuilder
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import mu.KotlinLogging

class LocalBowlerKernelFacade {

    private var started = false
    private lateinit var server: KernelServer
    private lateinit var stub: ScriptHostGrpcKt.ScriptHostCoroutineStub
    private val runScriptRequests = mutableMapOf<String, Long>()

    private val responses = Channel<SessionClientMessage.Builder>()

    private fun start() {
        try {
            // TODO: Use the CLI instead. Make the path to the CLI user-configurable.
            server = KernelServer()
            server.start()
            val channel = ManagedChannelBuilder.forAddress("localhost", server.port).usePlaintext().build()
            stub = ScriptHostGrpcKt.ScriptHostCoroutineStub(channel)
        } catch (ex: Throwable) {
            logger.debug(ex) { "Failed to start server." }
            throw ex
        }
    }

    fun ensureStarted() {
        synchronized(this) {
            if (!started) {
                started = true
                start()
            }
        }
    }

    /**
     * Execute the [BowlerScriptRunConfiguration] in a suspending environment. Returns an [ExecutionResult] immediately.
     *
     * @param config The config to run.
     * @return An execution result with the running script.
     */
    fun runScript(config: BowlerScriptRunConfiguration): ExecutionResult {
        logger.debug { "Running configuration: $config" }
        val scriptFile = config.getScriptFile()
        logger.debug { "Project directory: ${config.project.guessProjectDir()}" }
        val relativeScriptPath = scriptFile.path.substring(config.project.guessProjectDir()!!.path.length + 1)
        logger.debug { "relativeScriptPath=$relativeScriptPath" }
        val scriptContents = ByteString.readFrom(scriptFile.inputStream)

        val requestId = nextRequestId()
        runScriptRequests[relativeScriptPath] = requestId

        val session = stub.session(flow {
            val msg = SessionClientMessage.newBuilder().apply {
                runRequestBuilder.requestId = requestId

                // TODO: Get the remoteRemote and revision from the project's `origin` remote and local HEAD
                runRequestBuilder.fileBuilder.projectBuilder.repoRemote = ""
                runRequestBuilder.fileBuilder.projectBuilder.revision = ""
                // TODO: Set the patch via a real Git diff
                runRequestBuilder.fileBuilder.projectBuilder.patchBuilder.patch = scriptContents
                runRequestBuilder.fileBuilder.path = relativeScriptPath
            }.build()
            logger.debug { "Sending request: $msg" }
            emit(msg)

            do {
                val cont = select<Boolean> {
                    responses.onReceive {
                        val response = it.build()
                        logger.debug { "Sending response: $response" }
                        emit(response)
                        true
                    }
                }
            } while (cont)
        })

        var error: RequestError? = null
        // TODO: Return some sort of execution result immediately and run the session in some threaded, suspending scope
        runBlocking {
            session.collect {
                when {
                    it.hasCredentialsRequest() -> responses.send(SessionClientMessage.newBuilder().apply {
                        credentialsResponseBuilder.requestId = requestId
                    })

                    it.hasConfirmationRequest() -> responses.send(SessionClientMessage.newBuilder().apply {
                        confirmationResponseBuilder.requestId = requestId
                        confirmationResponseBuilder.response = ConfirmationValue.ALLOWED
                    })

                    it.hasError() -> error = it.error
                }
            }
        }

        // TODO: Move this error handling into `session.collect` because we need to return an execution result immediately
        return if (error == null) {
            logger.debug { "Run request finished successfully." }
            DefaultExecutionResult()
        } else {
            logger.debug { "Run request finished with an error: $error" }
            DefaultExecutionResult()
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
        private val requestId = AtomicLong(0)
        private fun nextRequestId() = requestId.getAndIncrement()
    }
}
