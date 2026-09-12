package dev.vfyjxf.nimbusprojection.api.sync;

import dev.vfyjxf.cloudlib.api.ui.inworld.AnchorCodecs;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldAnchor;
import dev.vfyjxf.cloudlib.api.ui.inworld.PanelKey;
import dev.vfyjxf.cloudlib.api.ui.inworld.Presentation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

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
 *   <li>{@link #anchor} must have a registered {@link AnchorCodecs} codec —
 *       lazily-resolved anchors cannot cross the network.</li>
 *   <li>{@link #payload} may be null — re-pushable later via
 *       {@link SharedPanel#update}.</li>
 * </ul>
 * <p>
 * Visibility is two-tier: {@link #visibleTo} decides who may see the panel
 * at all (invisible players never learn it exists); {@link #canInteract}
 * decides who may operate it — a visible non-interactor sees a read-only
 * panel and its channel input is dropped server-side.
 */
public record SharedPanelSpec(
        PanelKey key,
        ResourceKey<Level> dimension,
        InworldAnchor anchor,
        ResourceLocation view,
        @Nullable CustomPacketPayload payload,
        Presentation presentation,
        double maxDistance,
        @Nullable ServerPanelMessageHandler channel,
        Predicate<ServerPlayer> visibleTo,
        Predicate<ServerPlayer> canInteract) {

    private static final Predicate<ServerPlayer> any = p -> true;

    public SharedPanelSpec(PanelKey key, ResourceKey<Level> dimension, InworldAnchor anchor, ResourceLocation view) {
        this(key, dimension, anchor, view, null, Presentation.floating(), 32, null, any, any);
    }

    /** Attaches the client → server receive handler. */
    public SharedPanelSpec channel(ServerPanelMessageHandler handler) {
        return copy(payload, presentation, maxDistance, handler, visibleTo, canInteract);
    }

    public SharedPanelSpec presentation(Presentation presentation) {
        return copy(payload, presentation, maxDistance, channel, visibleTo, canInteract);
    }

    public SharedPanelSpec payload(@Nullable CustomPacketPayload payload) {
        return copy(payload, presentation, maxDistance, channel, visibleTo, canInteract);
    }

    public SharedPanelSpec maxDistance(double maxDistance) {
        return copy(payload, presentation, maxDistance, channel, visibleTo, canInteract);
    }

    /** Who may see this panel at all — invisible players never learn it exists. */
    public SharedPanelSpec visibleTo(Predicate<ServerPlayer> predicate) {
        return copy(payload, presentation, maxDistance, channel, predicate, canInteract);
    }

    /** Who may operate this panel — defaults to {@link #visibleTo}. */
    public SharedPanelSpec canInteract(Predicate<ServerPlayer> predicate) {
        return copy(payload, presentation, maxDistance, channel, visibleTo, predicate);
    }

    private SharedPanelSpec copy(
            @Nullable CustomPacketPayload payload,
            Presentation presentation,
            double maxDistance,
            @Nullable ServerPanelMessageHandler channel,
            Predicate<ServerPlayer> visibleTo,
            Predicate<ServerPlayer> canInteract) {
        return new SharedPanelSpec(
                key, dimension, anchor, view, payload, presentation, maxDistance, channel, visibleTo, canInteract);
    }
}
