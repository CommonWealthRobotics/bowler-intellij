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

import org.koin.core.KoinComponent
import org.koin.dsl.koinApplication
import org.koin.dsl.module

object GlobalPluginState {

    val koinComponent = object : KoinComponent {
        private val koinApp = koinApplication {
            modules(
                module {
                    single<KernelConnectionManager> { DefaultKernelConnectionManager() }
                    single<BowlerKernelFacade> { DefaultBowlerKernelFacade(get()) }
                }
            )
        }

        override fun getKoin() = koinApp.koin
    }
}
