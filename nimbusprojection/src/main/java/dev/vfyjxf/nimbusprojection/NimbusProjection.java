package dev.vfyjxf.nimbusprojection;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
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
        if (dist == Dist.CLIENT) {
            modBus.addListener(this::registerKeys);
        }
    }

    private void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(NimbusKeyMappings.inspect);
        event.register(NimbusKeyMappings.focusNext);
        event.register(NimbusKeyMappings.focusPrevious);
        event.register(NimbusKeyMappings.interact);
    }

}
