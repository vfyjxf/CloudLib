package dev.vfyjxf.cloudlib.ui;

import dev.vfyjxf.cloudlib.api.registry.ui.IUIRegistry;
import dev.vfyjxf.cloudlib.api.ui.overlay.OverlayProvider;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public class UIRegistry implements IUIRegistry {

    private final MutableList<OverlayProvider> overlayProviders = Lists.mutable.empty();

    @Override
    public void registerOverlayProvider(OverlayProvider provider) {
        if (overlayProviders.contains(provider)) {
            throw new IllegalArgumentException("Overlay handler " + provider.getClass().getName() + " already exists!");
        }
        overlayProviders.add(provider);
    }

    @Override
    public @Unmodifiable List<OverlayProvider> getOverlayProviders() {
        return List.of();
    }

}
