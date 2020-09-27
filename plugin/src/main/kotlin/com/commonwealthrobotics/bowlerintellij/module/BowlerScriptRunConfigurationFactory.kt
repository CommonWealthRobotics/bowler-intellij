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

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import icons.JetgroovyIcons
import org.jetbrains.plugins.groovy.GroovyFileType

class BowlerScriptRunConfigurationFactory : SimpleConfigurationType(
    "Bowler script",
    "Bowler script configuration factory",
    "Runs a Bowler script",
    NotNullLazyValue.createConstantValue(JetgroovyIcons.Groovy.Groovy_16x16)
) {

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return BowlerScriptRunConfiguration(project, this, "Bowler Script")
    }

    override fun isApplicable(project: Project): Boolean {
        return FileTypeIndex.containsFileOfType(GroovyFileType.GROOVY_FILE_TYPE, GlobalSearchScope.allScope(project))
    }
}
