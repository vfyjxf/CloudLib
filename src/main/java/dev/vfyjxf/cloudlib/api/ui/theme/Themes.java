package dev.vfyjxf.cloudlib.api.ui.theme;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The theme registry: loaded themes by id plus the active selection.
 * <p>
 * The active selection is an ordered id list — lowest priority first.
 * {@link #active()} composes the layers into a single effective {@link Theme}
 * (later layers win equal-specificity ties), so resolving code always sees
 * one theme. Themes register through the resource reload listener; code can
 * additionally {@link #register} programmatic themes.
 * <p>
 * Call {@link #refreshTree(Widget)} after switching the active selection to
 * re-resolve a live widget tree.
 */
public final class Themes {

    private Themes() {}

    private static final Map<ResourceLocation, Theme> themes = new ConcurrentHashMap<>();
    private static final List<ResourceLocation> activeIds = new CopyOnWriteArrayList<>();
    private static final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    /** The composed view of {@link #activeIds} — rebuilt lazily, null = stale. */
    private static volatile @Nullable Theme merged;

    /** Registers (or replaces) a parsed theme. */
    public static void register(Theme theme) {
        themes.put(theme.id(), theme);
        merged = null;
    }

    public static void unregister(ResourceLocation id) {
        themes.remove(id);
        if (activeIds.remove(id)) {
            merged = null;
            notifyChanged();
        }
    }

    public static @Nullable Theme get(ResourceLocation id) {
        return themes.get(id);
    }

    public static List<ResourceLocation> themeIds() {
        return List.copyOf(themes.keySet());
    }

    /**
     * Replaces the active selection wholesale — lowest priority first. Used by
     * the theme loader after a resource reload to install the effective
     * selection; unknown ids are skipped.
     */
    public static void setActive(List<ResourceLocation> ids) {
        activeIds.clear();
        for (ResourceLocation id : ids) {
            if (themes.containsKey(id)) {
                activeIds.add(id);
            }
        }
        merged = null;
        notifyChanged();
    }

    /** Pins a single active theme. */
    public static void setActive(ResourceLocation id) {
        setActive(List.of(id));
    }

    /** The active selection, lowest priority first. */
    public static List<ResourceLocation> activeIds() {
        return List.copyOf(activeIds);
    }

    /**
     * The effective theme — every active layer {@linkplain Theme#compose
     * composed} into one, or null when un-themed.
     */
    public static @Nullable Theme active() {
        Theme m = merged;
        if (m != null) {
            return m;
        }
        List<Theme> layers = activeIds.stream()
                .map(themes::get)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (layers.isEmpty()) {
            return null;
        }
        m = Theme.compose(layers);
        merged = m;
        return m;
    }

    /** Listener fired whenever the active theme may have changed (reload, selection). */
    public static void onChange(Runnable listener) {
        changeListeners.add(listener);
    }

    /**
     * Re-resolves the theme for a whole widget subtree after a selection change.
     * Honors a scene-level theme override when the root is mounted.
     */
    public static void refreshTree(Widget root) {
        Theme theme = root.lifecycle().mounted() ? root.scene().theme() : active();
        if (theme != null) {
            theme.applyTree(root);
        }
    }

    public static void clear() {
        themes.clear();
        activeIds.clear();
        merged = null;
    }

    /** Fires change listeners — called by the theme loader after registration. */
    public static void notifyChanged() {
        for (Runnable listener : changeListeners) {
            listener.run();
        }
    }
}
