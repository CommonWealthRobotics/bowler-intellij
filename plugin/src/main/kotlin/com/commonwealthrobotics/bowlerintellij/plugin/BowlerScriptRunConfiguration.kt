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

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.xmlb.XmlSerializer
import org.jdom.Element
import org.jetbrains.plugins.groovy.GroovyFileType
import org.koin.core.KoinComponent
import org.koin.core.inject

class BowlerScriptRunConfiguration(
    project: Project,
    factory: ConfigurationFactory?,
    name: String?,
    private val koinComponent: KoinComponent
) : RunConfigurationBase<Any>(project, factory, name) {

    private val kernelConnectionManager by koinComponent.inject<KernelConnectionManager>()

    var scriptFilePath: String = ""

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        return if (kernelConnectionManager.isConnected) {
            BowlerScriptRunProfileState(environment, koinComponent)
        } else {
            null
        }
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return BowlerRunConfigurationEditor(project)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        XmlSerializer.deserializeInto(this, element)
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        XmlSerializer.serializeInto(this, element)
    }

    fun getScriptFile(): VirtualFile {
        if (StringUtil.isEmptyOrSpaces(scriptFilePath)) {
            throw RuntimeConfigurationError(BIB.message("path.to.script.not.set"))
        }

        val file = LocalFileSystem.getInstance().findFileByPath(scriptFilePath)
        if (file == null || file.isDirectory) {
            throw RuntimeConfigurationError(
                BIB.message("script.file.not.found", FileUtil.toSystemDependentName(scriptFilePath))
            )
        }

        if (file.fileType != GroovyFileType.GROOVY_FILE_TYPE) {
            throw RuntimeConfigurationError(
                BIB.message("script.file.must.be.groovy.file", FileUtil.toSystemDependentName(scriptFilePath))
            )
        }

        val projectPath = project.guessProjectDir()?.path
        if (projectPath == null) {
            throw RuntimeConfigurationError("Default project?")
        } else if (!file.path.startsWith(projectPath)) {
            throw RuntimeConfigurationError(
                BIB.message("script.file.must.be.in.project", FileUtil.toSystemDependentName(scriptFilePath))
            )
        }

        return file
    }

    override fun toString(): String {
        return "BowlerScriptRunConfiguration(scriptFilePath='$scriptFilePath')"
    }
}
