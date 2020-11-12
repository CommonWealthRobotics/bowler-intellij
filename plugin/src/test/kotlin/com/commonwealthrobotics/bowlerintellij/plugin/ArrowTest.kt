package com.commonwealthrobotics.bowlerintellij.plugin

import arrow.core.Either
import arrow.fx.IO
import com.commonwealthrobotics.bowlerintellij.module.FooImpl
import io.kotest.matchers.booleans.shouldBeTrue
import org.junit.jupiter.api.Test

// TODO: Remove after solving the runIde problem
internal class ArrowTest {

    @Test
    fun test1() {
        IO { }.attempt()
        true.shouldBeTrue()
    }

    @Test
    fun test2() {
        FooImpl as (Unit) -> Unit
        true.shouldBeTrue()
    }
}
