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
import java.net.InetAddress

typealias KernelConnectionListener = (Tuple2<InetAddress, Int>?) -> Unit

interface KernelConnectionManager {

    /**
     * Whether there is currently a connection.
     */
    val isConnected: Boolean

    /**
     * The coroutine stub on top of the current connection. This throws if there is no connection.
     */
    val stub: ScriptHostGrpcKt.ScriptHostCoroutineStub

    /**
     * Connect to the kernel server running at the [address] and [port].
     *
     * @param address The address the kernel server is running at.
     * @param port The port the kernel server is bound to.
     */
    fun connect(address: InetAddress, port: Int)

    /**
     * Disconnect from the currently connected kernel server. Does nothing if there is no connection.
     */
    fun disconnect()

    /**
     * Add a listener that will be notified when there is a new connection. If there is a disconnection, `null` will
     * be passed instead.
     *
     * @param listener The listener.
     */
    fun addConnectionListener(listener: KernelConnectionListener)
}
