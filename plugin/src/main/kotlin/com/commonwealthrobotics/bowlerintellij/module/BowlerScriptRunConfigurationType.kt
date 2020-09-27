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

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import icons.JetgroovyIcons
import javax.swing.Icon

class BowlerScriptRunConfigurationType : ConfigurationType {

    override fun getDisplayName(): String {
        return "Bowler"
    }

    override fun getConfigurationTypeDescription(): String {
        return "Bowler script"
    }

    override fun getIcon(): Icon {
        return JetgroovyIcons.Groovy.Groovy_16x16
    }

    override fun getId(): String {
        return "BowlerScriptRunConfiguration"
    }

    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return arrayOf(BowlerScriptRunConfigurationFactory())
    }
}
