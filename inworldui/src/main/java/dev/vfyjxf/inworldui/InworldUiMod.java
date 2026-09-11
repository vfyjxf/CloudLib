package dev.vfyjxf.inworldui;

import dev.vfyjxf.cloudlib.api.ui.inworld.InworldUi;
import dev.vfyjxf.inworldui.demo.DemoRegistry;
import dev.vfyjxf.inworldui.demo.SyncedPanelProvider;
import dev.vfyjxf.inworldui.demo.TrackerPanelProvider;
import dev.vfyjxf.inworldui.demo.WaypointPanelProvider;
import dev.vfyjxf.inworldui.internal.InworldManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(value = Constants.modId, dist = Dist.CLIENT)
public final class InworldUiMod {

    public static final Logger logger = LoggerFactory.getLogger("InworldUi");

    public InworldUiMod(ModContainer container, IEventBus modBus, Dist dist) {
        modBus.addListener(this::clientSetup);
        modBus.addListener(this::loadComplete);
        modBus.addListener(this::registerKeys);
        DemoRegistry.register(modBus);
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
        api.registerProvider(new WaypointPanelProvider(), 20);
    }

    private void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(InworldKeyMappings.inspect);
        event.register(InworldKeyMappings.focusNext);
        event.register(InworldKeyMappings.focusPrevious);
        event.register(InworldKeyMappings.interact);
    }
}
