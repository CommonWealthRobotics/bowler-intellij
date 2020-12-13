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

import com.intellij.AbstractBundle
import mu.KotlinLogging
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.lang.ref.Reference
import java.lang.ref.SoftReference
import java.util.Locale
import java.util.ResourceBundle

/**
 * The BowlerIntelliJBundle (BIB) for specifying localizable strings.
 */
object BIB {

    private val logger = KotlinLogging.logger { }
    var bundle: Reference<ResourceBundle>? = null

    @NonNls private const val BUNDLE_ID = "bowler-intellij"

    init {
        val locale = Locale.getDefault()
        logger.info { "Initializing bowler-intellij bundle for locale ${locale.toLanguageTag()}" }
    }

    fun message(@PropertyKey(resourceBundle = BUNDLE_ID) key: String, vararg params: Any): String {
        logger.debug { "Getting message: $key ${params.joinToString()}" }
        return AbstractBundle.message(getBundle(), key, params)
    }

    private fun getBundle(): ResourceBundle {
        var propBundle = bundle?.get()
        if (propBundle != null) {
            return propBundle
        }

        propBundle = ResourceBundle.getBundle(BUNDLE_ID)
        bundle = SoftReference(propBundle)
        return propBundle
    }
}
