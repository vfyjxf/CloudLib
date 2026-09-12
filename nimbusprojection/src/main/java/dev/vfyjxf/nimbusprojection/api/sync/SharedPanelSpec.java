package dev.vfyjxf.nimbusprojection.api.sync;

import dev.vfyjxf.cloudlib.api.ui.inworld.InworldAnchor;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPlacement;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Server-side declaration of a shared panel: one record broadcast to every
 * watching client so all players see the same UI at the same anchor.
 * <p>
 * The spec carries no widget code — clients materialize the panel through
 * the {@link SharedPanelView} registered under {@link #view}
 * ({@link dev.vfyjxf.nimbusprojection.api.NimbusClient#registerView}),
 * feeding it {@link #payload} as the server-supplied data.
 * <p>
 * Constraints:
 * <ul>
 *   <li>{@link #key} must be a {@link String} — keys cross the network.</li>
 *   <li>{@link #anchor} must be a block- or entity-bound anchor; dynamic
 *       supplier anchors have no network representation.</li>
 *   <li>{@link #payload} may be null — re-pushable later via
 *       {@link SharedPanel#update}.</li>
 * </ul>
 */
public record SharedPanelSpec(
        String key,
        InworldAnchor anchor,
        ResourceLocation view,
        @Nullable CustomPacketPayload payload,
        InworldPlacement placement,
        double maxDistance,
        @Nullable ServerPanelMessageHandler channel
) {

    public SharedPanelSpec(String key, InworldAnchor anchor, ResourceLocation view) {
        this(key, anchor, view, null, InworldPlacement.floating(), 32, null);
    }

    /** Attaches the client → server receive handler. */
    public SharedPanelSpec channel(ServerPanelMessageHandler handler) {
        return new SharedPanelSpec(key, anchor, view, payload, placement, maxDistance, handler);
    }

    public SharedPanelSpec placement(InworldPlacement placement) {
        return new SharedPanelSpec(key, anchor, view, payload, placement, maxDistance, channel);
    }

    public SharedPanelSpec payload(@Nullable CustomPacketPayload payload) {
        return new SharedPanelSpec(key, anchor, view, payload, placement, maxDistance, channel);
    }

    public SharedPanelSpec maxDistance(double maxDistance) {
        return new SharedPanelSpec(key, anchor, view, payload, placement, maxDistance, channel);
    }

}
