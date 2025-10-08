package dev.vfyjxf.cloudlib.api.plugin;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.Set;

public interface ModPlugin {

    //region plugin info

    ResourceLocation pluginId();

    default Set<PluginDependency> dependencies() {
        return Collections.emptySet();
    }

    //endregion

}
