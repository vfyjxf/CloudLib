package dev.vfyjxf.cloudlib.api.ui

import dev.vfyjxf.cloudlib.api.ui.traits.CollectorTrait
import dev.vfyjxf.cloudlib.api.ui.traits.ITrait
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidgetGroup


fun <T : IWidget> T.build(builder: T.() -> Unit) = apply {
    this.builder()
}

infix fun <T : IWidget> IWidget.childOf(group: IWidgetGroup<T>) = apply {
    this.asChild(group)
}

infix fun <T : IWidget> T.trait(config: ITrait.() -> Unit) = apply {
    val collector  = CollectorTrait()
    collector.config()
    this.trait = collector.toImmutable()
}

inline fun <T : IWidget> T.onEvent(listeners: T.() -> Unit) = apply {
    this.listeners()
}

inline fun <C : IWidget, T : IWidgetGroup<C>> T.children(childrenAdditions: T.() -> Unit) = apply {
    this.childrenAdditions()
}


