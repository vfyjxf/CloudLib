package dev.vfyjxf.cloudlib.api.ui.style;

import dev.vfyjxf.cloudlib.api.event.EventDefinition;
import dev.vfyjxf.cloudlib.api.event.Events;

/**
 * Style-layer events.
 */
public interface StyleEvents {

    /**
     * Fired whenever the effective theme may have changed — a resource reload,
     * a manual {@link Themes#reload()}, or an activation change via
     * {@link Themes#setActive}. Scenes subscribe on mount and re-resolve their
     * widget trees; other listeners can react to the new {@link Themes#active()}.
     */
    EventDefinition<OnThemeReload> themeReload = Events.define(OnThemeReload.class, listeners -> () -> {
        for (var listener : listeners) {
            listener.onThemeReload();
        }
    });

    @FunctionalInterface
    interface OnThemeReload {

        void onThemeReload();
    }
}
