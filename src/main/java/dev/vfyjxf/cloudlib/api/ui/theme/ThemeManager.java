package dev.vfyjxf.cloudlib.api.ui.theme;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The theme registry: loaded themes by id plus the active theme stack.
 * <p>
 * The stack is ordered lowest-priority first — {@link #active()} returns the
 * effective theme (the last entry). Themes register through the resource reload
 * listener; code can additionally {@link #register} programmatic themes.
 * <p>
 * Call {@link #refreshTree(Widget)} after switching the active theme to
 * re-resolve a live widget tree.
 */
public final class ThemeManager {

    private ThemeManager() {}

    private static final Map<ResourceLocation, Theme> themes = new ConcurrentHashMap<>();
    private static final List<ResourceLocation> stack = new CopyOnWriteArrayList<>();
    private static final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    /** Registers (or replaces) a parsed theme. */
    public static void register(Theme theme) {
        themes.put(theme.id(), theme);
    }

    public static void unregister(ResourceLocation id) {
        themes.remove(id);
        stack.remove(id);
    }

    public static @Nullable Theme get(ResourceLocation id) {
        return themes.get(id);
    }

    public static List<ResourceLocation> themeIds() {
        return List.copyOf(themes.keySet());
    }

    /** Pushes a theme onto the stack — last entry wins (later = higher priority). */
    public static void activate(ResourceLocation id) {
        if (themes.containsKey(id) && !stack.contains(id)) {
            stack.add(id);
            notifyChanged();
        }
    }

    /**
     * Replaces the active stack wholesale — lowest priority first. Used by the
     * theme loader after a resource reload to install the effective selection.
     */
    public static void setStack(List<ResourceLocation> ids) {
        stack.clear();
        for (ResourceLocation id : ids) {
            if (themes.containsKey(id)) {
                stack.add(id);
            }
        }
    }

    public static void deactivate(ResourceLocation id) {
        if (stack.remove(id)) {
            notifyChanged();
        }
    }

    /** The effective theme — the top of the stack, or null when un-themed. */
    public static @Nullable Theme active() {
        return stack.isEmpty() ? null : themes.get(stack.get(stack.size() - 1));
    }

    /** All active themes bottom→top for layered resolution. */
    public static List<Theme> activeStack() {
        return stack.stream()
                .map(themes::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /** Listener fired whenever the active stack changes (reload, activate, deactivate). */
    public static void onChange(Runnable listener) {
        changeListeners.add(listener);
    }

    /**
     * Re-resolves the theme for a whole widget subtree after a stack change.
     * Shares one cascade context — each node resolves once per pass.
     * Honors a scene-level theme override when the root is mounted.
     */
    public static void refreshTree(Widget root) {
        Theme theme = root.lifecycle().mounted() ? root.scene().theme() : active();
        if (theme != null) {
            ThemeEngine.applyTree(theme, root, null);
        }
    }

    public static void clear() {
        themes.clear();
        stack.clear();
    }

    /** Fires change listeners — called by the theme loader after registration. */
    public static void notifyChanged() {
        for (Runnable listener : changeListeners) {
            listener.run();
        }
    }
}
