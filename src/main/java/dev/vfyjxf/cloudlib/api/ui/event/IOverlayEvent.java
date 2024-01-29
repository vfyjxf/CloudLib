package dev.vfyjxf.cloudlib.api.ui.event;

import dev.vfyjxf.cloudlib.api.event.EventFactory;
import dev.vfyjxf.cloudlib.api.event.IEventDefinition;
import dev.vfyjxf.cloudlib.api.ui.overlay.IUIOverlay;

public interface IOverlayEvent {

    IEventDefinition<OnOverlayBuild> onOverlayBuild = EventFactory.define(OnOverlayBuild.class, (listeners) -> (overlay) -> {
        for (OnOverlayBuild listener : listeners) {
            listener.onBuild(overlay);
        }
    });

    interface OnOverlayBuild extends IOverlayEvent {

        void onBuild(IUIOverlay overlay);

    }

}
