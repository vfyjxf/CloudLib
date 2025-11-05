package dev.vfyjxf.cloudlib;

import dev.vfyjxf.cloudlib.api.plugin.CloudLibPlugin;
import dev.vfyjxf.cloudlib.api.plugin.PluginLoader;
import dev.vfyjxf.cloudlib.debug.DebugConfig;
import dev.vfyjxf.cloudlib.network.CloudlibPayloads;
import dev.vfyjxf.cloudlib.test.TestRegistry;
import dev.vfyjxf.cloudlib.util.Locations;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import org.eclipse.collections.api.list.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CloudLib {
    public static final Logger logger = LoggerFactory.getLogger("CloudLib");
    protected final ImmutableList<CloudLibPlugin> plugins;

    //TODO:Move thread unsafe operations to constructModEvent
    public CloudLib(ModContainer container, IEventBus modBus, Dist dist) {
        //region internal init
        plugins = PluginLoader.loadPlugin(logger, "CloudLib Plugin", CloudLibPlugin.class).toImmutable();
        //endregion

        //region debug & test init
        if (!FMLEnvironment.production) {
            TestRegistry.register(modBus);
            DebugConfig.register(container);
        }
        //region

        //region fml lifecycle listener
        modBus.addListener(this::constructMod);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::loadComplete);
        //region

        //region register
        modBus.addListener(CloudlibPayloads::register);
        //endregion
    }

    protected void constructMod(FMLConstructModEvent event) {}

    protected void commonSetup(FMLCommonSetupEvent event) {}

    protected void loadComplete(FMLLoadCompleteEvent event) {}

    public static ResourceLocation of(String path) {
        return Locations.ofMod(path);
    }

}
