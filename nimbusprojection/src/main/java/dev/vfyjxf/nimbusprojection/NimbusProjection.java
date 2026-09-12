package dev.vfyjxf.nimbusprojection;

import dev.vfyjxf.cloudlib.api.plugin.AnnotationPluginLookup;
import dev.vfyjxf.cloudlib.api.plugin.PluginLoader;
import dev.vfyjxf.nimbusprojection.api.Nimbus;
import dev.vfyjxf.nimbusprojection.api.NimbusClient;
import dev.vfyjxf.nimbusprojection.api.plugin.NimbusClientPlugin;
import dev.vfyjxf.nimbusprojection.api.plugin.NimbusPlugin;
import dev.vfyjxf.nimbusprojection.internal.InworldManager;
import dev.vfyjxf.nimbusprojection.internal.NimbusServerImpl;
import dev.vfyjxf.nimbusprojection.internal.section.SectionProviders;
import dev.vfyjxf.nimbusprojection.internal.section.SectionWidgets;
import dev.vfyjxf.nimbusprojection.network.NimbusPayloads;
import dev.vfyjxf.nimbusprojection.network.PresenceTracker;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.eclipse.collections.api.list.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nimbus Projection — an in-world UI mod built on CloudLib.
 * <p>
 * The public contract lives in {@code dev.vfyjxf.nimbusprojection.api}; the
 * runtime implementation is internal. The mod loads on both dists: panel
 * providers and rendering are client-side, while shared panels and presence
 * synchronization have a server side so every watching player sees the same
 * UI and each other's interactions.
 * <p>
 * Feature wiring goes through plugins: {@link NimbusPlugin} on the common
 * side and {@link NimbusClientPlugin} on the client, discovered via
 * {@code @PluginMarker} and dispatched at load-complete — the same
 * mechanism CloudLib itself uses.
 */
@Mod(Constants.modId)
public final class NimbusProjection {

    public static final Logger logger = LoggerFactory.getLogger("NimbusProjection");

    private final ImmutableList<NimbusPlugin> plugins;
    private final ImmutableList<NimbusClientPlugin> clientPlugins;

    public NimbusProjection(ModContainer container, IEventBus modBus, Dist dist) {
        plugins = PluginLoader.loadPlugin(logger, "Nimbus Plugin", AnnotationPluginLookup.of(NimbusPlugin.class))
                .toImmutable();
        clientPlugins = dist == Dist.CLIENT
                ? PluginLoader.loadPlugin(
                                logger, "Nimbus Client Plugin", AnnotationPluginLookup.of(NimbusClientPlugin.class))
                        .toImmutable()
                : null;

        NimbusServerImpl server = new NimbusServerImpl();
        modBus.addListener((RegisterPayloadHandlersEvent e) -> NimbusPayloads.register(e));
        modBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(() -> {
            InworldManager manager = dist == Dist.CLIENT ? InworldManager.init() : null;
            Nimbus.install(manager, server);
        }));
        modBus.addListener((FMLLoadCompleteEvent e) -> {
            dispatchPlugins();
            dispatchClientPlugins();
        });
        // shared-panel registry + presence bookkeeping — joiners catch up on
        // live shared panels and presence reports, leavers get cleared
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> server.attach(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> server.detach());
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
                PresenceTracker.syncTo(player.getServer(), player);
                server.syncTo(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
                PresenceTracker.remove(player.getServer(), player);
            }
        });
        if (dist == Dist.CLIENT) {
            modBus.addListener(this::registerKeys);
        }
    }

    /** Runs the common plugin hooks — section kinds must register before any payload flows. */
    private void dispatchPlugins() {
        for (NimbusPlugin plugin : plugins) {
            try {
                plugin.registerContainerSections(SectionProviders.register);
            } catch (Exception e) {
                logger.warn("Failed to dispatch plugin {}", plugin.pluginId(), e);
            }
        }
    }

    /** Runs the client plugin hooks once the runtime is installed. */
    private void dispatchClientPlugins() {
        NimbusClient client = Nimbus.client();
        if (client == null || clientPlugins == null) return;
        for (NimbusClientPlugin plugin : clientPlugins) {
            try {
                plugin.registerProviders(client);
                plugin.registerPresentations(client);
                plugin.registerSharedViews(client);
                plugin.registerSectionWidgets(SectionWidgets.register);
            } catch (Exception e) {
                logger.warn("Failed to dispatch client plugin {}", plugin.pluginId(), e);
            }
        }
    }

    private void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(NimbusKeyMappings.inspect);
        event.register(NimbusKeyMappings.focusNext);
        event.register(NimbusKeyMappings.focusPrevious);
        event.register(NimbusKeyMappings.interact);
        event.register(NimbusKeyMappings.inventory);
    }
}
