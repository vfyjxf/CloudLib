package dev.vfyjxf.cloudlib.api.registry;

import dev.vfyjxf.cloudlib.api.registry.ui.IUIRegistry;

public interface IModPlugin {

    void registerUI(IUIRegistry registry);

    void registerSerialization(ISerializeRegistry registry);

}
