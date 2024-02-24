package dev.vfyjxf.cloudlib.integration.internal;

import dev.vfyjxf.cloudlib.api.registry.IModPlugin;
import dev.vfyjxf.cloudlib.api.registry.RegisterPlugin;
import dev.vfyjxf.cloudlib.api.registry.ui.IUIRegistry;

@RegisterPlugin
public class InternalPlugin implements IModPlugin {
    @Override
    public void registerUI(IUIRegistry registry) {
    }

}
