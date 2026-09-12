package dev.vfyjxf.nimbusprojection.internal;

import dev.vfyjxf.cloudlib.api.ui.inworld.InworldAnchor;
import dev.vfyjxf.cloudlib.api.ui.inworld.PanelKey;
import dev.vfyjxf.nimbusprojection.api.NimbusServer;
import dev.vfyjxf.nimbusprojection.api.sync.SharedPanel;
import dev.vfyjxf.nimbusprojection.api.sync.SharedPanelChannel;
import dev.vfyjxf.nimbusprojection.api.sync.SharedPanelSpec;
import dev.vfyjxf.nimbusprojection.network.ClientboundPanelPayload;
import dev.vfyjxf.nimbusprojection.network.SharedPanelRemovePayload;
import dev.vfyjxf.nimbusprojection.network.SharedPanelSpawnPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Server-side shared-panel registry: tracks declarations, applies
 * permission predicates and broadcasts spawn/remove/update to the watchers
 * each spec selects ({@code visibleTo} + same dimension + within
 * {@code maxDistance} of the anchor when it resolves).
 */
public final class NimbusServerImpl implements NimbusServer {

    private final Map<PanelKey, SharedPanelImpl> shared = new LinkedHashMap<>();
    private @Nullable MinecraftServer server;

    public void attach(MinecraftServer server) {
        this.server = server;
    }

    public void detach() {
        this.server = null;
        shared.clear();
    }

    @Override
    public SharedPanel share(SharedPanelSpec spec) {
        SharedPanelImpl panel = new SharedPanelImpl(spec);
        shared.put(spec.key(), panel);
        for (ServerPlayer watcher : watchers(spec)) {
            PacketDistributor.sendToPlayer(watcher, spawnFor(spec, watcher));
        }
        return panel;
    }

    @Override
    public void unshare(PanelKey key) {
        SharedPanelImpl panel = shared.remove(key);
        if (panel == null) return;
        for (ServerPlayer watcher : watchers(panel.spec)) {
            PacketDistributor.sendToPlayer(watcher, new SharedPanelRemovePayload(key));
        }
    }

    @Override
    public Collection<? extends SharedPanel> shared() {
        return Collections.unmodifiableCollection(shared.values());
    }

    /** Join-catch-up: materialize every live shared panel a player can see. */
    public void syncTo(ServerPlayer player) {
        for (SharedPanelImpl panel : shared.values()) {
            if (visible(panel.spec, player)) {
                PacketDistributor.sendToPlayer(player, spawnFor(panel.spec, player));
            }
        }
    }

    private SharedPanelSpawnPayload spawnFor(SharedPanelSpec spec, ServerPlayer watcher) {
        return new SharedPanelSpawnPayload(
                spec.key(),
                spec.dimension(),
                spec.anchor(),
                spec.view(),
                spec.presentation(),
                spec.maxDistance(),
                spec.canInteract().test(watcher),
                spec.payload());
    }

    /** Watchers = same dimension + {@code visibleTo} + inside {@code maxDistance}. */
    private java.util.List<ServerPlayer> watchers(SharedPanelSpec spec) {
        if (server == null) return java.util.List.of();
        ServerLevel level = server.getLevel(spec.dimension());
        if (level == null) return java.util.List.of();
        Vec3 pos = anchorPos(spec, level);
        java.util.List<ServerPlayer> out = new java.util.ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (!spec.visibleTo().test(player)) continue;
            if (pos != null && !player.getEyePosition().closerThan(pos, spec.maxDistance())) continue;
            out.add(player);
        }
        return out;
    }

    private boolean visible(SharedPanelSpec spec, ServerPlayer player) {
        if (player.level().dimension() != spec.dimension()) return false;
        if (!spec.visibleTo().test(player)) return false;
        Vec3 pos = anchorPos(spec, player.serverLevel());
        return pos == null || player.getEyePosition().closerThan(pos, spec.maxDistance());
    }

    /** Server-side anchor position for the distance check — built-ins only;
     *  custom anchors get no distance filter (visibleTo still applies). */
    private @Nullable Vec3 anchorPos(SharedPanelSpec spec, ServerLevel level) {
        InworldAnchor anchor = spec.anchor();
        if (anchor instanceof InworldAnchor.Block b)
            return Vec3.atCenterOf(b.pos()).add(b.offset());
        if (anchor instanceof InworldAnchor.Position p) return p.pos();
        if (anchor instanceof InworldAnchor.EntityTarget e) {
            Entity entity = level.getEntity(e.entityId());
            return entity != null ? entity.position().add(e.offset()) : null;
        }
        return null;
    }

    private final class SharedPanelImpl implements SharedPanel {

        private final SharedPanelSpec spec;

        SharedPanelImpl(SharedPanelSpec spec) {
            this.spec = spec;
        }

        @Override
        public PanelKey key() {
            return spec.key();
        }

        @Override
        public SharedPanelSpec spec() {
            return spec;
        }

        @Override
        public void update(@Nullable CustomPacketPayload payload) {
            if (payload == null) return;
            for (ServerPlayer watcher : watchers(spec)) {
                PacketDistributor.sendToPlayer(watcher, new ClientboundPanelPayload(spec.key(), payload));
            }
        }

        @Override
        public SharedPanelChannel channel() {
            return new SharedPanelChannel() {
                @Override
                public void broadcast(CustomPacketPayload payload) {
                    for (ServerPlayer watcher : watchers(spec)) {
                        PacketDistributor.sendToPlayer(watcher, new ClientboundPanelPayload(spec.key(), payload));
                    }
                }

                @Override
                public void sendTo(ServerPlayer player, CustomPacketPayload payload) {
                    if (visible(spec, player)) {
                        PacketDistributor.sendToPlayer(player, new ClientboundPanelPayload(spec.key(), payload));
                    }
                }
            };
        }

        @Override
        public void unshare() {
            NimbusServerImpl.this.unshare(spec.key());
        }
    }
}
