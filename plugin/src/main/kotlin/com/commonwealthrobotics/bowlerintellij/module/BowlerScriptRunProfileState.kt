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
            val configuration = environment.runnerAndConfigurationSettings?.configuration as BowlerScriptRunConfiguration
            kernelFacade.runScript(configuration)
        } catch (ex: Throwable) {
            logger.error(ex) { "Failed to initialize Bowler run profile." }
            DefaultExecutionResult()
        }
    }

    companion object {

        private val logger = KotlinLogging.logger { }
        private val kernelFacade by lazy { LocalBowlerKernelFacade() }
    }
}
