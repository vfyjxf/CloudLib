package dev.vfyjxf.cloudlib;

import dev.vfyjxf.cloudlib.api.registry.ModuleEntryPoint;
import dev.vfyjxf.cloudlib.api.utils.ServiceLoading;
import dev.vfyjxf.cloudlib.network.CloudlibNetworkPayloads;
import dev.vfyjxf.cloudlib.test.TestRegistry;
import dev.vfyjxf.cloudlib.test.sync.TestBlockEntity;
import dev.vfyjxf.cloudlib.utils.Locations;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.collection.ImmutableCollection;

public abstract class CloudLib {
    public static final Logger logger = LogManager.getLogger("CloudLib");
    protected final ImmutableCollection<ModuleEntryPoint> plugins;

    public CloudLib(IEventBus modBus, Dist dist) {
        //region internal init
        plugins = ServiceLoading.load(ModuleEntryPoint.class).toImmutable();
        //endregion

        //region debug & test init
        TestRegistry.register(modBus);
        //region

        //region fml lifecycle listener
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::loadComplete);
        //region

        //region register
        modBus.addListener(CloudlibNetworkPayloads::register);
        //endregion
    }

    protected void constructMod(FMLConstructModEvent event) {
        event.enqueueWork(() -> {
            var a = TestBlockEntity.Menu.INFO;
        });
    }

    protected void commonSetup(FMLCommonSetupEvent event) {
    }

    protected void loadComplete(FMLLoadCompleteEvent event) {
    }

    public static ResourceLocation of(String path) {
        return Locations.of(path);
    }

}
