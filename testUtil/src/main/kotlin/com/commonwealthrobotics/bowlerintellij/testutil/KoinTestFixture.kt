package com.commonwealthrobotics.bowlerintellij.testutil

import org.junit.jupiter.api.AfterEach
import org.koin.core.Koin
import org.koin.core.KoinComponent
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.koinApplication

open class KoinTestFixture {

    private var additionalAfterEach: () -> Unit = {}
    lateinit var testLocalKoin: KoinComponent
        private set

    /**
     * Sets the [testLocalKoin] instance to a new instance using the given modules.
     *
     * @param module The module(s) to start the instance with.
     */
    fun initKoin(vararg module: Module) {
        testLocalKoin = object : KoinComponent {
            val koinApp = koinApplication {
                modules(module.toList())
            }

            override fun getKoin(): Koin = koinApp.koin
        }
    }

    @AfterEach
    fun afterEach() {
        additionalAfterEach()
        stopKoin()
    }

    /**
     * Sets an [AfterEach] thunk that runs before Koin is stopped in the default implementation ([afterEach]).
     */
    fun additionalAfterEach(configure: () -> Unit) {
        additionalAfterEach = configure
    }
}
