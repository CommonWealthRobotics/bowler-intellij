package com.commonwealthrobotics.bowlerintellij.module

import arrow.core.Either
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import mu.KotlinLogging
import arrow.fx.IO

class BowlerScriptRunProfileState(private val environment: ExecutionEnvironment) : RunProfileState {

    override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
        return try {
            // TODO: Why does this throw a ClassCastException?
            FooImpl as (Unit) -> Unit
            DefaultExecutionResult()
//            kernelFacade.ensureStarted()
//            val configuration = environment.runnerAndConfigurationSettings?.configuration as BowlerScriptRunConfiguration
//            kernelFacade.runScript(configuration)
        } catch (ex: Throwable) {
            logger.error(ex) { "Failed to execute Bowler run profile." }
            DefaultExecutionResult()
        }
    }

    companion object {

        private val logger = KotlinLogging.logger { }
//        private val kernelFacade by lazy { LocalBowlerKernelFacade() }
    }
}
