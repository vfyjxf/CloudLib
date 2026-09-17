package dev.vfyjxf.cloudlib;

import dev.vfyjxf.cloudlib.api.plugin.AnnotationPluginLookup;
import dev.vfyjxf.cloudlib.api.plugin.CloudLibClientPlugin;
import dev.vfyjxf.cloudlib.api.plugin.PluginLoader;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.InworldExclusions;
import dev.vfyjxf.cloudlib.data.lang.CloudLibLangProvider;
import dev.vfyjxf.cloudlib.internal.ui.style.StyleConfig;
import dev.vfyjxf.cloudlib.internal.ui.style.StyleLoader;
import dev.vfyjxf.cloudlib.internal.ui.style.StyleWatcher;
import dev.vfyjxf.cloudlib.ui.CloudLibCommands;
import dev.vfyjxf.cloudlib.ui.KeyMappings;
import dev.vfyjxf.cloudlib.ui.hud.VanillaHudExclusions;
import dev.vfyjxf.cloudlib.ui.hud.VanillaHudSampler;
import dev.vfyjxf.cloudlib.ui.overlay.OverlayApiImpl;
import dev.vfyjxf.cloudlib.ui.overlay.OverlayEventHandler;
import dev.vfyjxf.cloudlib.ui.overlay.OverlayRegisterImpl;
import net.minecraft.data.DataProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
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
    private final VanillaHudSampler hudSampler = new VanillaHudSampler();

    public CloudLibClient(ModContainer container, IEventBus modBus, Dist dist) {
        super(container, modBus, dist);
        StyleConfig.register(container);
        clientPlugins = PluginLoader.loadPlugin(
                        logger, "CloudLib Client Plugin", AnnotationPluginLookup.of(CloudLibClientPlugin.class))
                .toImmutable();
        modBus.addListener(this::gatherData);
        modBus.addListener(this::registerClientTooltipComponentFactories);
        modBus.addListener(this::registerClientReloadListeners);
        modBus.addListener(KeyMappings::register);
        modBus.addListener(hudSampler::registerGuiLayer);
        NeoForge.EVENT_BUS.addListener(CloudLibCommands::register);
        NeoForge.EVENT_BUS.addListener(hudSampler::onBossEventProgress);
        NeoForge.EVENT_BUS.addListener(hudSampler::onChatOverlay);
    }

    @Override
    protected void loadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(StyleWatcher::start);
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

        InworldExclusions.register(VanillaHudExclusions.vanilla(hudSampler));
    }

    private void registerClientTooltipComponentFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        //        event.register(RichTooltipComponent.class, Function.identity());
    }

    private void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(StyleLoader.instance);
    }

    private void gatherData(GatherDataEvent event) {
        event.getGenerator()
                .addProvider(event.includeClient(), (DataProvider.Factory<DataProvider>) CloudLibLangProvider::new);
    }
}
