package dev.vfyjxf.cloudlib;

import dev.vfyjxf.cloudlib.api.registry.ModuleEntryPoint;
import dev.vfyjxf.cloudlib.api.registry.ui.IUIRegistry;
import dev.vfyjxf.cloudlib.api.ui.text.RichTooltipComponent;
import dev.vfyjxf.cloudlib.data.lang.LangKeyProvider;
import dev.vfyjxf.cloudlib.ui.GuiEventHandler;
import dev.vfyjxf.cloudlib.ui.UIManager;
import dev.vfyjxf.cloudlib.ui.UIRegistry;
import dev.vfyjxf.cloudlib.utils.Singletons;
import net.minecraft.data.DataProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.function.Function;

@Mod(value = Constants.MOD_ID, dist = Dist.CLIENT)
public class CloudLibClient extends CloudLib {

    public CloudLibClient(IEventBus modBus, Dist dist) {
        super(modBus, dist);
        modBus.addListener(this::gatherData);
        modBus.addListener(this::registerClientTooltipComponentFactories);
        Singletons.attachInstance(GuiEventHandler.class, new GuiEventHandler());
        NeoForge.EVENT_BUS.register(GuiEventHandler.getInstance());
        Singletons.attachInstance(IUIRegistry.class, new UIRegistry());
        NeoForge.EVENT_BUS.register(UIManager.instance());
    }

    @Override
    protected void loadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            IUIRegistry registry = Singletons.get(IUIRegistry.class);
            for (ModuleEntryPoint plugin : plugins) {
                plugin.registerUI(registry);
            }
        });
    }

    private void registerClientTooltipComponentFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(RichTooltipComponent.class, Function.identity());
    }

    private void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(
                event.includeClient(),
                (DataProvider.Factory<DataProvider>) (output) -> new LangKeyProvider(Constants.MOD_ID, output)
        );
    }


}
