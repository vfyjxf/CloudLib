package dev.vfyjxf.cloudlib.startup;

import dev.vfyjxf.cloudlib.Constants;
import dev.vfyjxf.cloudlib.api.registry.ui.IUIRegistry;
import dev.vfyjxf.cloudlib.api.ui.overlay.IUIOverlay;
import dev.vfyjxf.cloudlib.data.lang.LangKeyProvider;
import dev.vfyjxf.cloudlib.ui.ClientModularUI;
import dev.vfyjxf.cloudlib.ui.GuiEventHandler;
import dev.vfyjxf.cloudlib.ui.UIRegistry;
import dev.vfyjxf.cloudlib.ui.overlay.UIOverlay;
import dev.vfyjxf.cloudlib.utils.Singletons;
import net.minecraft.data.DataProvider;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class ClientStartup extends CommonStartup {

    @Override
    public void init() {
        super.init();
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::gatherData);
        Singletons.attachInstance(GuiEventHandler.class, new GuiEventHandler());
        MinecraftForge.EVENT_BUS.register(GuiEventHandler.getInstance());

    }

    private static void attachInstances() {
        Singletons.attachInstance(IUIRegistry.class, new UIRegistry());
        Singletons.attachInstance(IUIOverlay.class, new UIOverlay(new ClientModularUI()));
    }

    @Override
    public void loadComplete() {
    }


    @SubscribeEvent
    public void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(
                event.includeClient(),
                (DataProvider.Factory<DataProvider>) (output) -> new LangKeyProvider(Constants.MOD_ID, output)
        );
    }

}
