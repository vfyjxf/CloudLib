package dev.vfyjxf.cloudlib.api.ui.event;

import dev.vfyjxf.cloudlib.api.event.EventDefinition;
import dev.vfyjxf.cloudlib.api.event.EventFactory;
import dev.vfyjxf.cloudlib.api.ui.overlay.UIOverlay;

public interface OverlayEvent {

    EventDefinition<OnOverlayBuild> onOverlayBuild = EventFactory.define(OnOverlayBuild.class, (listeners) -> (overlay) -> {
        for (OnOverlayBuild listener : listeners) {
            listener.onBuild(overlay);
        }
    });

    @FunctionalInterface
    interface OnOverlayBuild extends OverlayEvent {

        void onBuild(UIOverlay overlay);

    }

}
