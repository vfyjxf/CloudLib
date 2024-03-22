package dev.vfyjxf.cloudlib.api.ui

import dev.vfyjxf.cloudlib.api.ui.state.IState
import kotlin.reflect.KProperty


operator fun <T> IState<T>.getValue(self: Any?, property: KProperty<*>): T = this.get()

operator fun <T> IState<T>.setValue(self: Any?, property: KProperty<*>, value: T): IState<T> = this.set(value)