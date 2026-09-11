package dev.vfyjxf.cloudlib.integration.internal;

import dev.vfyjxf.cloudlib.api.plugin.CloudLibClientPlugin;
import dev.vfyjxf.cloudlib.api.plugin.PluginMarker;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldUiApi;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.test.inworld.SyncedPanelProvider;

@PluginMarker
public class InternalClientPlugin implements CloudLibClientPlugin {

    @Override
    public Namespace pluginId() {
        return builtin;
    }

    @Override
    public void registerInworld(InworldUiApi inworld) {
        inworld.registerProvider(new SyncedPanelProvider(), 10);
    }
}
