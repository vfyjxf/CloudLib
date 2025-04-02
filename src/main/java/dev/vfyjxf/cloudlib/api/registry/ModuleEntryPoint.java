package dev.vfyjxf.cloudlib.api.registry;

import dev.vfyjxf.cloudlib.api.registry.ui.IUIRegistry;

public interface ModuleEntryPoint {

    default void registerUI(IUIRegistry registry) {
    }

}
