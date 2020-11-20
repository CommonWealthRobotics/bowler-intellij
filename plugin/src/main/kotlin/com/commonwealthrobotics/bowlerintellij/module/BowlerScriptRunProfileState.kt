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

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import mu.KotlinLogging

class BowlerScriptRunProfileState(private val environment: ExecutionEnvironment) : RunProfileState {

    /**
     * Primary documentation as part of [RunProfileState.execute].
     *
     * Note to developers: this must return an [ExecutionResult] immediately. Blocking will block the UI thread.
     *
     * @see [RunProfileState.execute]
     */
    override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
        return try {
            kernelFacade.ensureStarted()
            val configuration =
                environment.runnerAndConfigurationSettings?.configuration as BowlerScriptRunConfiguration
            kernelFacade.runScript(configuration)
        } catch (ex: Throwable) {
            logger.error(ex) { "Failed to initialize Bowler run profile." }
            DefaultExecutionResult()
        }
    }

    companion object {

        private val logger = KotlinLogging.logger { }
        private val kernelFacade by lazy { LocalBowlerKernelFacade(InProcessKernelDaemonConnectionManager()) }
    }
}
