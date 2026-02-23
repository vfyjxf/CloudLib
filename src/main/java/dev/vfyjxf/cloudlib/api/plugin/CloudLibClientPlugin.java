package dev.vfyjxf.cloudlib.api.plugin;

import dev.vfyjxf.cloudlib.api.register.ui.UIOverlayRegister;

public interface CloudLibClientPlugin extends ModPlugin {

    default void registerOverlay(UIOverlayRegister register) {
    }

}
