package dev.vfyjxf.cloudlib.integration.internal;

import dev.vfyjxf.cloudlib.api.registry.ModuleEntryPoint;
import dev.vfyjxf.cloudlib.api.registry.ui.IUIRegistry;
import dev.vfyjxf.cloudlib.api.utils.ModService;

@ModService
public class InternalPlugin implements ModuleEntryPoint {
    @Override
    public void registerUI(IUIRegistry registry) {
    }

}
