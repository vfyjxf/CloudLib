package dev.vfyjxf.cloudlib;

import dev.vfyjxf.cloudlib.api.plugin.AnnotationPluginLookup;
import dev.vfyjxf.cloudlib.api.plugin.CloudLibClientPlugin;
import dev.vfyjxf.cloudlib.api.plugin.PluginLoader;
import dev.vfyjxf.cloudlib.data.lang.LangKeyProvider;
import dev.vfyjxf.cloudlib.ui.overlay.OverlayApiImpl;
import dev.vfyjxf.cloudlib.ui.overlay.OverlayEventHandler;
import dev.vfyjxf.cloudlib.ui.overlay.OverlayRegisterImpl;
import net.minecraft.data.DataProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.eclipse.collections.api.list.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(value = Constants.modId, dist = Dist.CLIENT)
public final class CloudLibClient extends CloudLib {

    public static final Logger logger = LoggerFactory.getLogger("CloudLib Client");

    private final ImmutableList<CloudLibClientPlugin> clientPlugins;

    public CloudLibClient(ModContainer container, IEventBus modBus, Dist dist) {
        super(container, modBus, dist);
        clientPlugins = PluginLoader.loadPlugin(logger, "CloudLib Client Plugin", AnnotationPluginLookup.of(CloudLibClientPlugin.class)).toImmutable();
        modBus.addListener(this::gatherData);
        modBus.addListener(this::registerClientTooltipComponentFactories);
    }

    @Override
    protected void loadComplete(FMLLoadCompleteEvent event) {
        var register = new OverlayRegisterImpl();

        for (CloudLibClientPlugin plugin : clientPlugins) {
            try {
                plugin.registerOverlay(register);
            } catch (Exception e) {
                logger.warn("Failed to register overlays for plugin {}", plugin.pluginId(), e);
            }
        }

        var api = new OverlayApiImpl(register);
        OverlayApiImpl.attach(api);

        var eventHandler = new OverlayEventHandler(api.manager());
        api.setEventHandler(eventHandler);
        NeoForge.EVENT_BUS.register(eventHandler);
        eventHandler.refreshCurrentScreen();
    }

    private void registerClientTooltipComponentFactories(RegisterClientTooltipComponentFactoriesEvent event) {
//        event.register(RichTooltipComponent.class, Function.identity());
    }

    private void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(
                event.includeClient(),
                (DataProvider.Factory<DataProvider>) (output) -> new LangKeyProvider(Constants.modId, output)
        );
    }


}
