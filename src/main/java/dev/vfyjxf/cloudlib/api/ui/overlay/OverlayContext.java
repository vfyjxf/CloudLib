package dev.vfyjxf.cloudlib.api.ui.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Read-only information used to evaluate and build overlays.
 */
public record OverlayContext(@Nullable Screen screen, Minecraft minecraft, int width, int height, double scale) {
    public OverlayContext {
        Objects.requireNonNull(minecraft, "minecraft");
    }

    public boolean is(Class<? extends Screen> type) {
        return type.isInstance(screen);
    }

}
