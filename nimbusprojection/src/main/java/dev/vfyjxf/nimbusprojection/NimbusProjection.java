package dev.vfyjxf.nimbusprojection;

import dev.vfyjxf.nimbusprojection.api.Nimbus;
import dev.vfyjxf.nimbusprojection.internal.InworldManager;
import dev.vfyjxf.nimbusprojection.internal.NimbusServerImpl;
import dev.vfyjxf.nimbusprojection.network.NimbusPayloads;
import dev.vfyjxf.nimbusprojection.network.PresenceTracker;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
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
 */
@Mod(Constants.modId)
public final class NimbusProjection {

    public static final Logger logger = LoggerFactory.getLogger("NimbusProjection");

    public NimbusProjection(ModContainer container, IEventBus modBus, Dist dist) {
        NimbusServerImpl server = new NimbusServerImpl();
        modBus.addListener((RegisterPayloadHandlersEvent e) -> NimbusPayloads.register(e));
        modBus.addListener((FMLCommonSetupEvent e) ->
                e.enqueueWork(() -> {
                    InworldManager manager = dist == Dist.CLIENT ? InworldManager.init() : null;
                    Nimbus.install(manager, server);
                    if (manager != null) {
                        manager.registerProvider(
                                new dev.vfyjxf.nimbusprojection.feature.container.ContainerPanelProvider(),
                                3,
                                dev.vfyjxf.nimbusprojection.api.provider.ProviderOptions.shared());
                    }
                }));
        //shared-panel registry + presence bookkeeping — joiners catch up on
        //live shared panels and presence reports, leavers get cleared
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

    private void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(NimbusKeyMappings.inspect);
        event.register(NimbusKeyMappings.focusNext);
        event.register(NimbusKeyMappings.focusPrevious);
        event.register(NimbusKeyMappings.interact);
        event.register(NimbusKeyMappings.inventory);
    }

}
