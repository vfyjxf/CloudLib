package dev.vfyjxf.cloudlib.integration.internal;

import dev.vfyjxf.cloudlib.api.registry.IModPlugin;
import dev.vfyjxf.cloudlib.api.registry.ui.IUIRegistry;
import dev.vfyjxf.cloudlib.api.utils.ModService;

@ModService
public class InternalPlugin implements IModPlugin {
    @Override
    public void registerUI(IUIRegistry registry) {
    }

}
