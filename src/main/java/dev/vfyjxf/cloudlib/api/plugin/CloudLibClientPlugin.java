package dev.vfyjxf.cloudlib.api.plugin;

import dev.vfyjxf.cloudlib.api.registry.ui.IUIRegistry;

public interface CloudLibClientPlugin extends ModPlugin {

    default void registerUI(IUIRegistry registry) {
    }

}
