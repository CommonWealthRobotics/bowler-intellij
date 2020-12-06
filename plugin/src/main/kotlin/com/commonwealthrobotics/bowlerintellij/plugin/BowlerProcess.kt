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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicInteger

class BowlerProcess(private val job: Job, private val exitCode: AtomicInteger) : Process() {

    override fun getOutputStream(): OutputStream {
        return OutputStream.nullOutputStream()
    }

    override fun getInputStream(): InputStream {
        return InputStream.nullInputStream()
    }

    override fun getErrorStream(): InputStream {
        return InputStream.nullInputStream()
    }

    override fun waitFor(): Int {
        runBlocking {
            job.join()
        }
        return exitCode.get()
    }

    override fun exitValue(): Int {
        if (job.isCompleted) {
            return exitCode.get()
        } else {
            throw IllegalThreadStateException()
        }
    }

    override fun destroy() {
        job.cancel()
    }

    companion object {

        fun suspended(scope: CoroutineScope, thunk: suspend CoroutineScope.() -> Int): BowlerProcess {
            val exitCode = AtomicInteger(-1)
            val job = scope.launch {
                exitCode.set(thunk())
            }
            return BowlerProcess(job, exitCode)
        }
    }
}
