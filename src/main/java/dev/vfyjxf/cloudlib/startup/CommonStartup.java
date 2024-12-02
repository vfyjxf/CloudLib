package dev.vfyjxf.cloudlib.startup;

import dev.vfyjxf.cloudlib.api.registry.IModPlugin;
import dev.vfyjxf.cloudlib.api.registry.PluginLoading;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

public class CommonStartup {

    protected static final Logger LOGGER = LogManager.getLogger("CloudLib Startup");
    protected final Collection<IModPlugin> plugins;
    protected final IEventBus modBus;

    public CommonStartup(IEventBus modBus) {
        this.modBus = modBus;
        plugins = PluginLoading.load(IModPlugin.class);
    }


    public void init() {
    }

    public void commonSetup(FMLCommonSetupEvent event) {
    }

    public void loadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
//            for (IModPlugin plugin : plugins) {
//                plugin.registerSerialization(Singletons.get(ISerializeRegistry.class));
//            }
        });
    }

}
