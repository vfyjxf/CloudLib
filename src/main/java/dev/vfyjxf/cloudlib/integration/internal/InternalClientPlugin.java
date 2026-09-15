package dev.vfyjxf.cloudlib.integration.internal;

import dev.vfyjxf.cloudlib.api.plugin.CloudLibClientPlugin;
import dev.vfyjxf.cloudlib.api.plugin.PluginMarker;
import dev.vfyjxf.cloudlib.api.util.Namespace;

@PluginMarker
public class InternalClientPlugin implements CloudLibClientPlugin {

    @Override
    public Namespace pluginId() {
        return builtin;
    }
}
