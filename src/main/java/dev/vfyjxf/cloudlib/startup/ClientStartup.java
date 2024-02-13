package dev.vfyjxf.cloudlib.startup;

import dev.vfyjxf.cloudlib.Constants;
import dev.vfyjxf.cloudlib.api.registry.IModPlugin;
import dev.vfyjxf.cloudlib.api.registry.ui.IUIRegistry;
import dev.vfyjxf.cloudlib.api.ui.overlay.IUIOverlay;
import dev.vfyjxf.cloudlib.data.lang.LangKeyProvider;
import dev.vfyjxf.cloudlib.ui.ClientModularUI;
import dev.vfyjxf.cloudlib.ui.GuiEventHandler;
import dev.vfyjxf.cloudlib.ui.UIRegistry;
import dev.vfyjxf.cloudlib.ui.overlay.UIOverlay;
import dev.vfyjxf.cloudlib.utils.Singletons;
import net.minecraft.data.DataProvider;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public class ClientStartup extends CommonStartup {

    public ClientStartup(IEventBus modBus) {
        super(modBus);
    }

    @Override
    public void init() {
        super.init();
        this.modBus.addListener(this::gatherData);
        Singletons.attachInstance(GuiEventHandler.class, new GuiEventHandler());
        NeoForge.EVENT_BUS.register(GuiEventHandler.getInstance());
        Singletons.attachInstance(IUIRegistry.class, new UIRegistry());
    }

    @Override
    public void loadComplete(FMLLoadCompleteEvent event) {
        super.loadComplete(event);
        event.enqueueWork(() -> {
            Singletons.attachInstance(IUIOverlay.class, new UIOverlay(new ClientModularUI()));
            for (IModPlugin plugin : plugins) {
                plugin.registerUI(Singletons.get(IUIRegistry.class));
            }
        });
    }


    public void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(
                event.includeClient(),
                (DataProvider.Factory<DataProvider>) (output) -> new LangKeyProvider(Constants.MOD_ID, output)
        );
    }

}
