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

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.layout.CellBuilder
import com.intellij.ui.layout.panel
import javax.swing.JComponent

class BowlerRunConfigurationEditor(private val project: Project) : SettingsEditor<BowlerScriptRunConfiguration>() {

    private lateinit var textFieldWithBrowseButton: CellBuilder<TextFieldWithBrowseButton>
    private val panel = panel {
        row {
            label("Bowler script")
            textFieldWithBrowseButton = textFieldWithBrowseButton(value = "", project = project) {
                it.path
            }
        }
    }

    override fun resetEditorFrom(s: BowlerScriptRunConfiguration) {
        textFieldWithBrowseButton.component.text = s.scriptFilePath
    }

    override fun applyEditorTo(s: BowlerScriptRunConfiguration) {
        s.scriptFilePath = textFieldWithBrowseButton.component.text
    }

    override fun createEditor(): JComponent {
        return panel
    }
}
