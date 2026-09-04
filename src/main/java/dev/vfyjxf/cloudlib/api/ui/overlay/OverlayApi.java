package dev.vfyjxf.cloudlib.api.ui.overlay;

import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.ui.overlay.OverlayApiImpl;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import org.eclipse.collections.api.RichIterable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.NonExtendable
public interface OverlayApi {

    static OverlayApi instance() {
        OverlayApi api = OverlayApiImpl.instance();
        if (api == null) {
            throw new IllegalStateException("OverlayApi is not initialized");
        }
        return api;
    }

    @Nullable OverlayEntry<?> find(Namespace id);

    default @Nullable OverlayEntry<?> find(String id) {
        return find(Namespace.parse(id));
    }

    RichIterable<OverlayEntry<?>> entries();

    RichIterable<Rect2i> exclusionAreas(@Nullable Screen screen);

    @Nullable Scene activeOverlayScene();
}
