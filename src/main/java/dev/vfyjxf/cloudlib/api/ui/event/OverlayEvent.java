package dev.vfyjxf.cloudlib.api.ui.event;

import dev.vfyjxf.cloudlib.api.event.EventDefinition;
import dev.vfyjxf.cloudlib.api.event.Events;
import dev.vfyjxf.cloudlib.api.ui.overlay.SceneOverlay;

public interface OverlayEvent {

    EventDefinition<OnOverlayBuild> onOverlayBuild = Events.define(OnOverlayBuild.class, (listeners) -> (overlay) -> {
        for (OnOverlayBuild listener : listeners) {
            listener.onBuild(overlay);
        }
    });

    @FunctionalInterface
    interface OnOverlayBuild extends OverlayEvent {

        void onBuild(SceneOverlay overlay);

    }

}
