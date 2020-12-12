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

import arrow.core.Tuple2
import com.commonwealthrobotics.proto.script_host.ScriptHostGrpcKt
import com.commonwealthrobotics.proto.script_host.SessionClientMessage
import com.commonwealthrobotics.proto.script_host.SessionServerMessage
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import mu.KotlinLogging
import java.net.InetAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class DefaultKernelConnectionManager(
    private val scope: CoroutineScope
) : KernelConnectionManager {

    private val connectionListeners = mutableListOf<KernelConnectionListener>()

    private var toSend = Channel<SessionClientMessage.Builder?>()
    private var toRecv = Channel<SessionServerMessage>()
    private var sessionEnded = CountDownLatch(1)
    private var internalStub: ScriptHostGrpcKt.ScriptHostCoroutineStub? = null
    private var internalSession: Flow<SessionServerMessage>? = null
    private var channel: ManagedChannel? = null

    override val isConnected: Boolean
        get() = internalStub != null

    override suspend fun <T> request(
        msg: SessionClientMessage.Builder,
        loop: suspend (Channel<SessionClientMessage.Builder?>, Channel<SessionServerMessage>) -> T
    ): T {
        if (isConnected) {
            toSend.send(msg)
            return loop(toSend, toRecv)
        } else {
            error("Cannot process a request when not connected.")
        }
    }

    override fun connect(address: InetAddress, port: Int) {
        synchronized(this) {
            if (!isConnected) {
                // TODO: Use TLS instead of plaintext
                channel = ManagedChannelBuilder.forAddress(address.hostAddress, port)
                    .usePlaintext()
                    .build()

                internalStub = ScriptHostGrpcKt.ScriptHostCoroutineStub(channel!!)
                toSend = Channel()
                toRecv = Channel()
                sessionEnded = CountDownLatch(1)

                internalSession = internalStub!!.session(
                    flow {
                        do {
                            val cont = select<Boolean> {
                                toSend.onReceive {
                                    it?.let {
                                        val response = it.build()
                                        logger.debug { "Sending response: $response" }
                                        emit(response)
                                        true
                                    } ?: false
                                }
                            }
                        } while (cont)
                        sessionEnded.countDown()
                        logger.debug { "Session ended." }
                    }
                )

                scope.launch {
                    internalSession!!.collect {
                        logger.debug { "Got reply: $it" }
                        toRecv.send(it)
                    }
                }

                connectionListeners.forEach { it(Tuple2(address, port)) }
            }
        }
    }

    override fun disconnect() {
        synchronized(this) {
            if (isConnected) {
                // Send null to make the client-side flow finish so that the session ends
                runBlocking { toSend.send(null) }
                if (!sessionEnded.await(10, TimeUnit.SECONDS)) {
                    // Cancel the scope if we timed out waiting for the session to finish
                    scope.cancel("Timed out waiting for the session to finish.")
                }

                channel!!.shutdown()
                try {
                    channel!!.awaitTermination(10, TimeUnit.SECONDS)
                } catch (ex: InterruptedException) {
                    logger.warn(ex) {
                        "Failed to gracefully shutdown the managed connection to the kernel. Forcing a shutdown."
                    }

                    channel!!.shutdownNow()

                    try {
                        channel!!.awaitTermination(10, TimeUnit.SECONDS)
                    } catch (ex: InterruptedException) {
                        logger.error(ex) {
                            "Failed to forcefully shutdown the managed connection to the kernel."
                        }

                        // Exit early without resetting the internal state
                        return
                    }
                }

                // If we get to this point, the channel should be terminated
                check(channel!!.isTerminated)

                channel = null
                internalStub = null
                connectionListeners.forEach { it(null) }
            }
        }
    }

    override fun addConnectionListener(listener: KernelConnectionListener) {
        connectionListeners.add(listener)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
