package dev.vfyjxf.cloudlib.api.plugin;

import dev.vfyjxf.cloudlib.api.util.Namespace;

import java.util.Collections;
import java.util.Set;

public interface ModPlugin {

    //region plugin info

    Namespace pluginId();

    default Set<PluginDependency> dependencies() {
        return Collections.emptySet();
    }

    //endregion

}
