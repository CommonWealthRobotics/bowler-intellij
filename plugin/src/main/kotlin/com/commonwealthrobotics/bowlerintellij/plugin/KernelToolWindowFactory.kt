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

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import org.koin.core.KoinComponent

/**
 * Relevant docs: [Tool Windows](https://jetbrains.org/intellij/sdk/docs/user_interface_components/tool_windows.html)
 */
class KernelToolWindowFactory(
    private val koinComponent: KoinComponent = GlobalPluginState.koinComponent
) : ToolWindowFactory {

    /**
     * Called when the user clicks the tool window button. Initializes the UI of the tool window.
     */
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val kernelDiscoveryForm = KernelDiscoveryForm(toolWindow, koinComponent.getKoin().get())
        toolWindow.contentManager.addContent(
            ContentFactory.SERVICE.getInstance().createContent(kernelDiscoveryForm.content, "", false)
        )
    }

    /**
     * Only displays the tool window button if necessary.
     */
    override fun isApplicable(project: Project): Boolean {
        // TODO: Detect if the project is a Bowler project and return true if so.
        return super.isApplicable(project)
    }
}
