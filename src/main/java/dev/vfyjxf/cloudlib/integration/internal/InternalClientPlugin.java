package dev.vfyjxf.cloudlib.integration.internal;

import com.google.auto.service.AutoService;
import dev.vfyjxf.cloudlib.api.plugin.CloudLibClientPlugin;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.CloudNamespaces;

@AutoService(CloudLibClientPlugin.class)
public class InternalClientPlugin implements CloudLibClientPlugin {
    public static final Namespace id = CloudNamespaces.ofMod("client/builtin");

    @Override
    public Namespace pluginId() {
        return id;
    }
}
