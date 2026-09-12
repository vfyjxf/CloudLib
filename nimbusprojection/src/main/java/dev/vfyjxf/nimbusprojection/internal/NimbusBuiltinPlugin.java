package dev.vfyjxf.nimbusprojection.internal;

import dev.vfyjxf.cloudlib.api.plugin.PluginMarker;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.nimbusprojection.api.NimbusClient;
import dev.vfyjxf.nimbusprojection.api.plugin.NimbusClientPlugin;
import dev.vfyjxf.nimbusprojection.api.provider.ProviderOptions;
import dev.vfyjxf.nimbusprojection.feature.container.ContainerPanelProvider;

/**
 * Nimbus's own registrations as a first-class plugin — the built-in
 * features dogfood the same extension point third parties use.
 */
@PluginMarker
public final class NimbusBuiltinPlugin implements NimbusClientPlugin {

    @Override
    public Namespace pluginId() {
        return NimbusClientPlugin.builtin;
    }

    @Override
    public void registerProviders(NimbusClient client) {
        client.registerProvider(new ContainerPanelProvider(), 3, ProviderOptions.shared());
    }
}
