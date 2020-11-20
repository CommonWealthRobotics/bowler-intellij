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
package com.commonwealthrobotics.bowlerintellij.module

import com.commonwealthrobotics.bowlerkernel.server.KernelServer

/**
 * Manages a kernel daemon that runs in the current process.
 */
class InProcessKernelDaemonConnectionManager : KernelDaemonConnectionManager {

    private var server: KernelServer? = null

    override fun ensureStarted() {
        synchronized(this) {
            if (server != null) {
                // TODO: Expose start's params via this class' constructor
                server = KernelServer().apply { start() }
            }
        }
    }

    override fun ensureStopped() {
        synchronized(this) {
            if (server != null) {
                server!!.stop()
                server = null
            }
        }
    }

    override fun getPort(): Int {
        return synchronized(this) {
            server?.port ?: error("Cannot get the port because the server is not started.")
        }
    }
}
