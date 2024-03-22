package dev.vfyjxf.cloudlib.ui

import dev.vfyjxf.cloudlib.api.ui.state.IState
import dev.vfyjxf.cloudlib.api.ui.stateOf
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidgetGroup
import dev.vfyjxf.cloudlib.ui.widgets.WidgetGroup
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class TestScreen(title: Component) : Screen(title) {

    val group: IWidgetGroup<IWidget> = WidgetGroup()
    val mutableName :IState<String> = stateOf("AA")
    override fun init() {
    }
}

