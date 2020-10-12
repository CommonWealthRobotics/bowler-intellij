package com.commonwealthrobotics.bowlerintellij.module

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner

class BowlerScriptRunProfileState(private val environment: ExecutionEnvironment) : RunProfileState {

    override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
        val configuration = environment.runnerAndConfigurationSettings?.configuration as BowlerScriptRunConfiguration
        println(configuration.scriptFilePath)
        return DefaultExecutionResult()
    }
}
