package dev.vfyjxf.inworldui;

import dev.vfyjxf.cloudlib.api.ui.inworld.InworldUi;
import dev.vfyjxf.inworldui.demo.DemoRegistry;
import dev.vfyjxf.inworldui.demo.InventoryPanelProvider;
import dev.vfyjxf.inworldui.demo.SyncedPanelProvider;
import dev.vfyjxf.inworldui.demo.TrackerPanelProvider;
import dev.vfyjxf.inworldui.demo.WaypointPanelProvider;
import dev.vfyjxf.inworldui.internal.InworldManager;
import dev.vfyjxf.inworldui.net.InworldPayloads;
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
public final class InworldUiMod {

    public static final Logger logger = LoggerFactory.getLogger("InworldUi");

    public InworldUiMod(ModContainer container, IEventBus modBus, Dist dist) {
        modBus.addListener((RegisterPayloadHandlersEvent e) -> InworldPayloads.register(e));
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
        api.registerProvider(new SyncedPanelProvider(), 10);
        api.registerProvider(new TrackerPanelProvider(), 10);
        api.registerProvider(new InventoryPanelProvider(), 5);
        api.registerProvider(new WaypointPanelProvider(), 20);
    }

    private void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(InworldKeyMappings.inspect);
        event.register(InworldKeyMappings.focusNext);
        event.register(InworldKeyMappings.focusPrevious);
        event.register(InworldKeyMappings.interact);
    }
}
