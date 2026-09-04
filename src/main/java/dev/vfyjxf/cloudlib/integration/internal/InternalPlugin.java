package dev.vfyjxf.cloudlib.integration.internal;

import dev.vfyjxf.cloudlib.api.plugin.CloudLibPlugin;
import dev.vfyjxf.cloudlib.api.plugin.PluginMarker;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.CloudNamespaces;

@PluginMarker
public class InternalPlugin implements CloudLibPlugin {

    public static final Namespace id = CloudNamespaces.ofMod("builtin");

    @Override
    public Namespace pluginId() {
        return id;
    }
}
