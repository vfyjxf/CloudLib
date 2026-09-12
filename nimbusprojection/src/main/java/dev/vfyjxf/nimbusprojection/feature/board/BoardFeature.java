package dev.vfyjxf.nimbusprojection.feature.board;

import dev.vfyjxf.cloudlib.api.ui.inworld.InworldAnchor;
import dev.vfyjxf.cloudlib.api.ui.inworld.PanelKey;
import dev.vfyjxf.cloudlib.api.ui.inworld.Presentation;
import dev.vfyjxf.nimbusprojection.api.Nimbus;
import dev.vfyjxf.nimbusprojection.api.NimbusServer;
import dev.vfyjxf.nimbusprojection.api.sync.SharedPanel;
import dev.vfyjxf.nimbusprojection.api.sync.SharedPanelSpec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The shared-panel view-protocol demo: {@code /nimbus board} on a looked-at
 * block shares a counter panel every watching player sees — the reference
 * implementation of the spec checklist
 * (share → registerView → channel ops → server-authoritative update).
 * <p>
 * Server state lives here only: the payload a client receives is produced
 * by this class and nothing the client sends is trusted — a
 * {@link BoardOpPayload} is a request the channel re-validates.
 */
public final class BoardFeature {

    /** The view id clients register a {@code SharedPanelView} under. */
    public static final ResourceLocation view = ResourceLocation.fromNamespaceAndPath("nimbusprojection", "board");

    private static final double reach = 24.0;
    private static final double maxDistance = 32.0;

    private static final Map<PanelKey, BoardState> boards = new ConcurrentHashMap<>();

    private record BoardState(SharedPanel panel, int count, String lastBy) {}

    private BoardFeature() {}

    /**
     * Toggles a shared board over the block the player is looking at:
     * first use shares it, a second use on the same block unshares it.
     *
     * @return the shared panel's key, or null when nothing was looked at
     */
    public static @Nullable PanelKey toggle(ServerPlayer player) {
        HitResult hit = player.pick(reach, 0, false);
        if (!(hit instanceof BlockHitResult blockHit)) return null;
        BlockPos pos = blockHit.getBlockPos();
        PanelKey key = key(pos);
        BoardState existing = boards.remove(key);
        if (existing != null) {
            existing.panel().unshare();
            return key;
        }

        NimbusServer server = Nimbus.server();
        if (server == null) return null;
        SharedPanel panel = server.share(new SharedPanelSpec(
                        key,
                        player.level().dimension(),
                        InworldAnchor.of(pos, new Vec3(0.5, 1.25, 0.5)),
                        view,
                        new BoardPayload(0, ""),
                        Presentation.floating(),
                        maxDistance,
                        BoardFeature::receive,
                        p -> true,
                        p -> true)
                .visibleTo(p -> p.level().dimension().equals(player.level().dimension())));
        boards.put(key, new BoardState(panel, 0, ""));
        return key;
    }

    /** Clears all live boards — server stopping detaches every watcher. */
    public static void reset() {
        boards.clear();
    }

    /**
     * The channel's server side: ops are validated here and the result is
     * broadcast as a fresh {@link BoardPayload} — the client's own click
     * never edits its own label.
     */
    private static void receive(SharedPanel panel, ServerPlayer sender, CustomPacketPayload payload) {
        if (!(payload instanceof BoardOpPayload op)) return;
        BoardState state = boards.get(panel.key());
        if (state == null) return;
        // reach re-check: the anchor may be far from where the clicker now stands
        BlockPos anchor = panel.spec().anchor().blockPos();
        if (anchor != null && sender.blockPosition().distSqr(anchor) > maxDistance * maxDistance) return;
        int count = Math.clamp(state.count() + op.delta(), -999, 999);
        String by = sender.getGameProfile().getName();
        boards.put(panel.key(), new BoardState(panel, count, by));
        panel.update(new BoardPayload(count, by));
    }

    private static PanelKey key(BlockPos pos) {
        return PanelKey.of("nimbusprojection", "board/" + pos.getX() + "," + pos.getY() + "," + pos.getZ());
    }
}
