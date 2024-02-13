package dev.vfyjxf.cloudlib.startup;

import dev.vfyjxf.cloudlib.api.registry.IModPlugin;
import dev.vfyjxf.cloudlib.api.registry.ISerializeRegistry;
import dev.vfyjxf.cloudlib.api.registry.PluginLoading;
import dev.vfyjxf.cloudlib.api.ui.UIConfig;
import dev.vfyjxf.cloudlib.data.SerializeRegistry;
import dev.vfyjxf.cloudlib.utils.Singletons;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

public class CommonStartup {

    protected static final Logger LOGGER = LogManager.getLogger("CloudLib Startup");
    protected final Collection<IModPlugin> plugins;
    protected final IEventBus modBus;
    public CommonStartup(IEventBus modBus) {
        this.modBus = modBus;
        Singletons.attachInstance(UIConfig.class, new UIConfig());
        Singletons.attachInstance(ISerializeRegistry.class, new SerializeRegistry());
        plugins = PluginLoading.load(IModPlugin.class);
    }


    public void init() {
    }

    public void commonSetup(FMLCommonSetupEvent event) {
    }

    public void loadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            for (IModPlugin plugin : plugins) {
                plugin.registerSerialization(Singletons.get(ISerializeRegistry.class));
            }
        });
    }

}
