package dev.vfyjxf.cloudlib.api.ui;

import net.minecraft.client.gui.screens.Screen;

import java.util.Collection;

public interface GuiExtraAreas<T extends Screen> {

    Collection<Area> getAreas(T screen);

}
