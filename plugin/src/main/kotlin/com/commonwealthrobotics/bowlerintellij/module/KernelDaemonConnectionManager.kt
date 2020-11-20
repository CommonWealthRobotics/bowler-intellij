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

/**
 * Manages the connection between this plugin and the kernel daemon.
 *
 * TODO: Implement a version with a real kernel daemon that persists outside of the plugin's process.
 */
interface KernelDaemonConnectionManager {

    /**
     * Ensures that the daemon is started.
     */
    fun ensureStarted()

    /**
     * Ensures that the daemon is stopped.
     */
    fun ensureStopped()

    /**
     * @return The port the daemon is running on.
     */
    fun getPort(): Int
}
