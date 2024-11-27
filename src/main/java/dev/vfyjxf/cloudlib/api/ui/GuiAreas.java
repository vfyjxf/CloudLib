package dev.vfyjxf.cloudlib.api.ui;

import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

//TODO:Rewrite
public interface GuiAreas<T extends Screen> {

    /**
     * If null, returns {@link Area#EMPTY}
     */
    Area mainArea(@Nullable T screen);

    Collection<Area> extraAreas(T screen);

}
