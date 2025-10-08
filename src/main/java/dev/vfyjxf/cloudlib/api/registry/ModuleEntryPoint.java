package dev.vfyjxf.cloudlib.api.registry;

import dev.vfyjxf.cloudlib.api.registry.ui.IUIRegistry;

//TODO:Redesign
public interface ModuleEntryPoint {

    default void registerUI(IUIRegistry registry) {
    }

}
