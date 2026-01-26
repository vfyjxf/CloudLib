package dev.vfyjxf.cloudlib.api.registry.ui;

import dev.vfyjxf.cloudlib.api.ui.overlay.OverlayProvider;
import dev.vfyjxf.cloudlib.util.Singletons;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

//TODO:Refactor and cleanup
public interface IUIRegistry {

    void registerOverlayProvider(OverlayProvider provider);

    @Unmodifiable
    List<OverlayProvider> getOverlayProviders();

}
