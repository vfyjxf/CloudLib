package dev.vfyjxf.nimbusprojection;

import dev.vfyjxf.cloudlib.api.ui.inworld.InworldUi;
import dev.vfyjxf.nimbusprojection.demo.DemoRegistry;
import dev.vfyjxf.nimbusprojection.demo.InventoryPanelProvider;
import dev.vfyjxf.nimbusprojection.demo.SyncedPanelProvider;
import dev.vfyjxf.nimbusprojection.demo.TrackerPanelProvider;
import dev.vfyjxf.nimbusprojection.demo.WaypointPanelProvider;
import dev.vfyjxf.nimbusprojection.internal.InworldManager;
import dev.vfyjxf.nimbusprojection.network.NimbusPayloads;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads on both dists: the demo blocks/items and the world-drag payload need a
 * server side (the drag commit handler inserts into real containers), while
 * the in-world runtime, providers and key mappings are client-only.
 */
@Mod(Constants.modId)
public final class NimbusProjection {

    public static final Logger logger = LoggerFactory.getLogger("NimbusProjection");

    public NimbusProjection(ModContainer container, IEventBus modBus, Dist dist) {
        modBus.addListener((RegisterPayloadHandlersEvent e) -> NimbusPayloads.register(e));
        DemoRegistry.register(modBus);
        if (dist == Dist.CLIENT) {
            modBus.addListener(this::clientSetup);
            modBus.addListener(this::loadComplete);
            modBus.addListener(this::registerKeys);
        }
    }

    /**
     * Installs the runtime during client setup — this runs before
     * {@code FMLLoadCompleteEvent}, so {@link InworldUi#available()} is already
     * true when CloudLib client plugins get their {@code registerInworld} hook.
     */
    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> InworldUi.install(InworldManager.init()));
    }

    private void loadComplete(FMLLoadCompleteEvent event) {
        var api = InworldUi.instance();
        //interactive providers run near every tick — dormant targets must
        //exist by the time the soft-focus cone could select them, and stale
        //anchors read as lag
        api.registerProvider(new SyncedPanelProvider(), 3);
        api.registerProvider(new TrackerPanelProvider(), 3);
        api.registerProvider(new InventoryPanelProvider(), 1);
        api.registerProvider(new WaypointPanelProvider(), 20);
    }

    private void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(NimbusKeyMappings.inspect);
        event.register(NimbusKeyMappings.focusNext);
        event.register(NimbusKeyMappings.focusPrevious);
        event.register(NimbusKeyMappings.interact);
    }
}
