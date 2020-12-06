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
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import mu.KotlinLogging
import java.net.InetAddress
import java.util.concurrent.TimeUnit

class DefaultKernelConnectionManager : KernelConnectionManager {

    private val connectionListeners = mutableListOf<KernelConnectionListener>()

    private var internalStub: ScriptHostGrpcKt.ScriptHostCoroutineStub? = null
    private var channel: ManagedChannel? = null

    override val isConnected: Boolean
        get() = internalStub != null

    override val stub: ScriptHostGrpcKt.ScriptHostCoroutineStub
        get() = if (isConnected) internalStub!! else error("Cannot get the stub when there is no connection.")

    override fun connect(address: InetAddress, port: Int) {
        synchronized(this) {
            if (!isConnected) {
                // TODO: Use TLS instead of plaintext
                channel = ManagedChannelBuilder.forAddress(address.hostAddress, port)
                    .usePlaintext()
                    .build()
                internalStub = ScriptHostGrpcKt.ScriptHostCoroutineStub(channel!!)
                connectionListeners.forEach { it(Tuple2(address, port)) }
            }
        }
    }

    override fun disconnect() {
        synchronized(this) {
            if (isConnected) {
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
