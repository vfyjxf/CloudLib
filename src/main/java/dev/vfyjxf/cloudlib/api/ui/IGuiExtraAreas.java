package dev.vfyjxf.cloudlib.api.ui;

import net.minecraft.client.gui.screens.Screen;

import java.util.Collection;

public interface IGuiExtraAreas<T extends Screen> {

    Collection<IArea> getAreas(T screen);

}
