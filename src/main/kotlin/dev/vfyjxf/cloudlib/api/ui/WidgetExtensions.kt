package dev.vfyjxf.cloudlib.api.ui

import dev.vfyjxf.cloudlib.api.ui.traits.ITrait
import dev.vfyjxf.cloudlib.api.ui.traits.IUITraits
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidgetGroup


fun <T : IWidget> T.build(builder: T.() -> Unit) = apply {
    this.builder()
}

infix fun <T : IWidget> IWidget.childOf(group: IWidgetGroup<T>) = apply {
    this.asChild(group)
}

inline infix fun <T : IWidget> T.trait(traitConfig: IUITraits.() -> Unit) = apply {
    this.traits().traitConfig()
}

infix fun <T : IWidget> T.trait(trait: ITrait) = apply {
    this.traits().then(trait)
}

inline fun <T : IWidget> T.onEvent(listeners : T.() -> Unit) = apply {
    this.listeners()
}


