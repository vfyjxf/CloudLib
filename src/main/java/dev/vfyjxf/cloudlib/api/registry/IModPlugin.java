package dev.vfyjxf.cloudlib.api.registry;

import dev.vfyjxf.cloudlib.api.registry.ui.IUIRegistry;

//TODO:Rename or refactor register entrypoint
public interface IModPlugin {

    default void registerUI(IUIRegistry registry) {
    }

}
