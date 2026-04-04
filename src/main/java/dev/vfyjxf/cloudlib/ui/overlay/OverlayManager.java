package dev.vfyjxf.cloudlib.ui.overlay;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.overlay.OverlayContext;
import dev.vfyjxf.cloudlib.api.ui.overlay.OverlayEntry;
import dev.vfyjxf.cloudlib.api.ui.overlay.OverlayScope;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Matches overlay entries against screens and manages overlay widget lifecycles.
 */
public final class OverlayManager {

    private static final Logger log = LoggerFactory.getLogger(OverlayManager.class);

    private final Collection<OverlayEntry<?>> entries;
    private final Function<Screen, OverlayContext> contextFactory;
    private final List<OverlayRuntime<?>> activeOverlays = new ArrayList<>();

    public OverlayManager(Collection<OverlayEntry<?>> entries) {
        this(entries, OverlayManager::buildDefaultContext);
    }

    OverlayManager(Collection<OverlayEntry<?>> entries, Function<Screen, OverlayContext> contextFactory) {
        this.entries = Objects.requireNonNull(entries, "entries");
        this.contextFactory = Objects.requireNonNull(contextFactory, "contextFactory");
    }

    /**
     * Match entries against the given screen/context, create widgets, and add them to the target group.
     *
     * @return attached runtimes — caller should keep these for later {@link #detachOverlays}
     */
    public List<OverlayRuntime<?>> attachOverlays(@Nullable Screen screen, OverlayContext context, WidgetGroup<Widget> group) {
        var attached = new ArrayList<OverlayRuntime<?>>();
        for (OverlayEntry<?> entry : entries) {
            try {
                var runtime = tryAttach(entry, context, group);
                if (runtime != null) {
                    attached.add(runtime);
                    activeOverlays.add(runtime);
                }
            } catch (Exception e) {
                log.warn("Failed to attach overlay entry: id={}, scope={}, screen={}", entry.id(), entry.scope(), screen == null ? "null" : screen.getClass().getName(), e);
            }
        }
        return List.copyOf(attached);
    }

    private <T extends Widget> @Nullable OverlayRuntime<T> tryAttach(
            OverlayEntry<T> entry, OverlayContext context, WidgetGroup<Widget> group
    ) {
        if (entry.scope() != OverlayScope.screen && entry.scope() != OverlayScope.global) {
            return null;
        }
        T widget = entry.provider().create(context);
        if (widget == null) {
            return null;
        }
        group.addWidget(widget);
        return new OverlayRuntime<>(entry, widget);
    }

    /**
     * Remove previously attached overlays from the group.
     */
    public void detachOverlays(List<OverlayRuntime<?>> runtimes, WidgetGroup<Widget> group) {
        for (OverlayRuntime<?> runtime : runtimes) {
            group.remove(runtime.widget());
        }
        activeOverlays.removeAll(runtimes);
    }

    /**
     * Replace the previously attached overlays with a fresh evaluation against the given context.
     * <p>
     * This is used when the hosting screen is resized or otherwise changes layout-relevant state
     * without changing screen identity.
     */
    public List<OverlayRuntime<?>> refreshOverlays(
            @Nullable Screen screen,
            OverlayContext context,
            WidgetGroup<Widget> group,
            List<OverlayRuntime<?>> currentRuntimes
    ) {
        detachOverlays(currentRuntimes, group);
        return attachOverlays(screen, context, group);
    }

    public List<OverlayRuntime<?>> refreshOverlays(
            @Nullable Screen screen,
            WidgetGroup<Widget> group,
            List<OverlayRuntime<?>> currentRuntimes
    ) {
        return refreshOverlays(screen, createContext(screen), group, currentRuntimes);
    }

    public OverlayContext createContext(@Nullable Screen screen) {
        return contextFactory.apply(screen);
    }

    private static OverlayContext buildDefaultContext(@Nullable Screen screen) {
        Minecraft minecraft = Objects.requireNonNull(Minecraft.getInstance(), "minecraft");
        var window = minecraft.getWindow();
        return new OverlayContext(screen, minecraft, window.getGuiScaledWidth(), window.getGuiScaledHeight(), window.getGuiScale());
    }

    public List<Rect2i> exclusionAreas(@Nullable Screen screen) {
        if (activeOverlays.isEmpty()) {
            return List.of();
        }
        OverlayContext context = contextFactory.apply(screen);
        List<Rect2i> areas = new ArrayList<>();
        for (OverlayRuntime<?> runtime : activeOverlays) {
            areas.addAll(runtime.exclusionAreas(context));
        }
        return List.copyOf(areas);
    }
}
