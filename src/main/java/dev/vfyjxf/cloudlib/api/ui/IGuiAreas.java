package dev.vfyjxf.cloudlib.api.ui;

import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface IGuiAreas<T extends Screen> {

    /**
     *If null, returns {@link IArea#EMPTY}
     */
    IArea mainArea(@Nullable T screen);

    Collection<IArea> extraAreas(T screen);

}
