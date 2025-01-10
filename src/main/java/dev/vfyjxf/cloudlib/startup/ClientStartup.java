package dev.vfyjxf.cloudlib.startup;

import dev.vfyjxf.cloudlib.Constants;
import dev.vfyjxf.cloudlib.api.registry.IModPlugin;
import dev.vfyjxf.cloudlib.api.registry.ui.IUIRegistry;
import dev.vfyjxf.cloudlib.api.ui.text.RichTooltipComponent;
import dev.vfyjxf.cloudlib.data.lang.LangKeyProvider;
import dev.vfyjxf.cloudlib.ui.GuiEventHandler;
import dev.vfyjxf.cloudlib.ui.UIManager;
import dev.vfyjxf.cloudlib.ui.UIRegistry;
import dev.vfyjxf.cloudlib.utils.Singletons;
import net.minecraft.data.DataProvider;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.function.Function;

public class ClientStartup extends CommonStartup {

    public ClientStartup(IEventBus modBus) {
        super(modBus);
    }

    @Override
    public void init() {
        super.init();
        this.modBus.addListener(this::gatherData);
        this.modBus.addListener(this::registerClientTooltipComponentFactories);
        Singletons.attachInstance(GuiEventHandler.class, new GuiEventHandler());
        NeoForge.EVENT_BUS.register(GuiEventHandler.getInstance());
        Singletons.attachInstance(IUIRegistry.class, new UIRegistry());
        NeoForge.EVENT_BUS.register(UIManager.instance());
    }

    @Override
    public void loadComplete(FMLLoadCompleteEvent event) {
        super.loadComplete(event);
        event.enqueueWork(() -> {
            IUIRegistry registry = Singletons.get(IUIRegistry.class);
            for (IModPlugin plugin : plugins) {
                plugin.registerUI(registry);
            }
        });
    }


    public void registerClientTooltipComponentFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(RichTooltipComponent.class, Function.identity());
    }

    public void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(
                event.includeClient(),
                (DataProvider.Factory<DataProvider>) (output) -> new LangKeyProvider(Constants.MOD_ID, output)
        );
    }

}
