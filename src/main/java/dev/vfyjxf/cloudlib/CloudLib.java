package dev.vfyjxf.cloudlib;

import dev.vfyjxf.cloudlib.startup.ClientStartup;
import dev.vfyjxf.cloudlib.startup.CommonStartup;
import dev.vfyjxf.cloudlib.startup.ServerStartup;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Constants.MOD_ID)
public class CloudLib {
    public static final Logger logger = LogManager.getLogger("CloudLib");
    private static CommonStartup startup;

    public CloudLib(IEventBus modBus, Dist dist) {
        startup = dist == Dist.CLIENT ? new ClientStartup(modBus) : new ServerStartup(modBus);
        startup.init();
        modBus.register(this);
    }

    @SubscribeEvent
    public void commonSetup(FMLCommonSetupEvent event) {
        startup.commonSetup(event);
    }


    @SubscribeEvent
    public void loadComplete(FMLLoadCompleteEvent event) {
        startup.loadComplete(event);
    }


}
