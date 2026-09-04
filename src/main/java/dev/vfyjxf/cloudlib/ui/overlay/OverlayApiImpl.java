package dev.vfyjxf.cloudlib.ui.overlay;

import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.overlay.OverlayApi;
import dev.vfyjxf.cloudlib.api.ui.overlay.OverlayEntry;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.collection.mutable.CollectionAdapter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@ApiStatus.Internal
public final class OverlayApiImpl implements OverlayApi {

    private static @Nullable OverlayApiImpl instance;

    public static @Nullable OverlayApiImpl instance() {
        return instance;
    }

    public static void attach(OverlayApiImpl api) {
        Objects.requireNonNull(api, "api");
        instance = api;
    }

    private final Map<Namespace, OverlayEntry<?>> entries;
    private final OverlayManager manager;
    private @Nullable OverlayEventHandler eventHandler;

    public OverlayApiImpl(OverlayRegisterImpl builder) {
        this.entries = new HashMap<>(builder.entries);
        this.manager = new OverlayManager(this.entries.values());
    }

    OverlayApiImpl(OverlayRegisterImpl builder, OverlayManager manager) {
        this.entries = new HashMap<>(builder.entries);
        this.manager = Objects.requireNonNull(manager, "manager");
    }

    public OverlayManager manager() {
        return manager;
    }

    public void setEventHandler(OverlayEventHandler eventHandler) {
        this.eventHandler = Objects.requireNonNull(eventHandler, "eventHandler");
    }

    @Override
    public @Nullable OverlayEntry<?> find(Namespace id) {
        return entries.get(id);
    }

    @Override
    public RichIterable<OverlayEntry<?>> entries() {
        return CollectionAdapter.adapt(entries.values());
    }

    @Override
    public RichIterable<Rect2i> exclusionAreas(@Nullable Screen screen) {
        return CollectionAdapter.adapt(manager.exclusionAreas(screen));
    }

    @Override
    public @Nullable Scene activeOverlayScene() {
        return eventHandler != null ? eventHandler.activeOverlayScene() : null;
    }
}
