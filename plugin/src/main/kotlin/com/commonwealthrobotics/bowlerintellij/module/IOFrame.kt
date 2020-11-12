package com.commonwealthrobotics.bowlerintellij.module

// TODO: Remove after solving the runIde problem
interface Foo<in A, out B> : (A) -> B {
    override fun invoke(p1: A): B
}

object FooImpl : Foo<Unit, Unit> {
    override fun invoke(p1: Unit) = Unit
}
