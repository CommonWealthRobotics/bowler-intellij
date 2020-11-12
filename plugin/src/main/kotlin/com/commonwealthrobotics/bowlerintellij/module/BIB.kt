package com.commonwealthrobotics.bowlerintellij.module

import com.intellij.AbstractBundle
import com.intellij.CommonBundle
import java.lang.ref.Reference
import java.lang.ref.SoftReference
import java.util.Locale
import java.util.ResourceBundle
import mu.KotlinLogging
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

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
