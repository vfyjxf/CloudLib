package dev.vfyjxf.cloudlib.test.inworld;

import com.mojang.serialization.Codec;
import dev.vfyjxf.cloudlib.api.data.handle.Handle;
import dev.vfyjxf.cloudlib.api.network.UnaryFlowHandler;
import dev.vfyjxf.cloudlib.api.network.expose.UnaryReversed;
import dev.vfyjxf.cloudlib.blockentity.BasicSyncedBlockEntity;
import dev.vfyjxf.cloudlib.blockentity.Schema;
import dev.vfyjxf.cloudlib.test.TestRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Demo block entity for the in-world tracker: scans nearby living entities on
 * the server, syncs the count/nearest-name to clients, and accepts ping/alert
 * actions back over a reversed channel.
 */
public class TrackerBlockEntity extends BasicSyncedBlockEntity {

    public static final int ACTION_PING = 0;
    public static final int ACTION_TOGGLE_ALERT = 1;

    /** Entity scan radius around the block. */
    public static final int RANGE = 14;
    /** Upper bound of the client-settable alert threshold. */
    public static final int MAX_THRESHOLD = 32;

    private static final class Network {
        static final Schema<Integer> entities = Schema.of("entities", 0, Codec.INT, UnaryFlowHandler.codecOf(ByteBufCodecs.INT));
        static final Schema<String> nearest = Schema.of("nearest", "--", Codec.STRING, UnaryFlowHandler.codecOf(ByteBufCodecs.STRING_UTF8));
        static final Schema<Boolean> alert = Schema.of("alert", false, Codec.BOOL, UnaryFlowHandler.codecOf(ByteBufCodecs.BOOL));
        static final Schema<Integer> pings = Schema.of("pings", 0, Codec.INT, UnaryFlowHandler.codecOf(ByteBufCodecs.INT));
        /** Crowd threshold the client can tune via the threshold slider. */
        static final Schema<Integer> threshold = Schema.of("threshold", 6, Codec.INT, UnaryFlowHandler.codecOf(ByteBufCodecs.INT));
        /** Trace-puzzle solve count — proof the trace committed all the way to the server. */
        static final Schema<Integer> solves = Schema.of("solves", 0, Codec.INT, UnaryFlowHandler.codecOf(ByteBufCodecs.INT));
    }

    /** Glyph ids the sigil pad can send — mapped server-side onto real actions. */
    public static final int GLYPH_PULSE = 0;
    public static final int GLYPH_SURGE = 1;
    public static final int GLYPH_MARK = 2;
    public static final int GLYPH_CHANNEL = 3;

    private final Handle<Integer> entities = useSynced(Network.entities);
    private final Handle<String> nearest = useSynced(Network.nearest);
    private final Handle<Boolean> alert = useSynced(Network.alert);
    private final Handle<Integer> pings = useSynced(Network.pings);
    private final Handle<Integer> threshold = useSynced(Network.threshold);
    private final Handle<Integer> solves = useSynced(Network.solves);
    private final UnaryReversed<Integer> action = unaryReversed("action", UnaryFlowHandler.codecOf(ByteBufCodecs.VAR_INT));
    private final UnaryReversed<Integer> setThreshold = unaryReversed("setThreshold", UnaryFlowHandler.codecOf(ByteBufCodecs.VAR_INT));
    private final UnaryReversed<Integer> solved = unaryReversed("solved", UnaryFlowHandler.codecOf(ByteBufCodecs.VAR_INT));
    private final UnaryReversed<Integer> glyph = unaryReversed("glyph", UnaryFlowHandler.codecOf(ByteBufCodecs.VAR_INT));

    private long tick;

    public TrackerBlockEntity(BlockPos pos, BlockState state) {
        super(TestRegistry.trackerBlockEntity.get(), pos, state);
        action.whenReceiveFromClient(this::onAction);
        setThreshold.whenReceiveFromClient(v -> threshold.set(Math.clamp(v, 0, MAX_THRESHOLD)));
        solved.whenReceiveFromClient(v -> solves.set(solves.get() + 1));
        glyph.whenReceiveFromClient(this::onGlyph);
    }

    public Handle<Integer> entities() {
        return entities;
    }

    public Handle<String> nearest() {
        return nearest;
    }

    public Handle<Boolean> alert() {
        return alert;
    }

    public Handle<Integer> pings() {
        return pings;
    }

    public Handle<Integer> threshold() {
        return threshold;
    }

    public Handle<Integer> solves() {
        return solves;
    }

    /** Client-side: queues an action for the server and flushes the channel. */
    public void sendAction(int actionId) {
        try {
            action.sendToServer(actionId);
        } catch (IllegalStateException ignored) {
            return; //a value is already queued this tick
        }
        pushReversed();
    }

    /** Client-side: queues a new crowd threshold (clamped server-side). */
    public void sendThreshold(int value) {
        try {
            setThreshold.sendToServer(value);
        } catch (IllegalStateException ignored) {
            return;
        }
        pushReversed();
    }

    /** Client-side: the trace puzzle was solved. */
    public void sendSolved() {
        try {
            solved.sendToServer(1);
        } catch (IllegalStateException ignored) {
            return;
        }
        pushReversed();
    }

    /** Client-side: a sigil was drawn and recognized on the pad. */
    public void sendGlyph(int glyphId) {
        try {
            glyph.sendToServer(glyphId);
        } catch (IllegalStateException ignored) {
            return;
        }
        pushReversed();
    }

    /** Sigil → action mapping, applied on the server. */
    private void onGlyph(int id) {
        switch (id) {
            case GLYPH_PULSE -> {
                pings.set(pings.get() + 1);
                nearest.set("pulse " + pings.get());
            }
            case GLYPH_SURGE -> alert.set(!alert.get());
            case GLYPH_MARK -> solves.set(solves.get() + 1);
            case GLYPH_CHANNEL -> threshold.set((threshold.get() + 8) % (MAX_THRESHOLD + 1));
            default -> {
                //unknown glyph — drop silently
            }
        }
    }

    private void onAction(int actionId) {
        switch (actionId) {
            case ACTION_PING -> {
                pings.set(pings.get() + 1);
                nearest.set("pong " + pings.get());
            }
            case ACTION_TOGGLE_ALERT -> alert.set(!alert.get());
            default -> {
                //unknown action — drop silently
            }
        }
    }

    public static BlockEntityTicker<TrackerBlockEntity> ticker() {
        return (level, pos, state, be) -> {
            if (level.isClientSide) return;
            be.tick++;
            if (be.tick % 20 != 0) return;
            var box = AABB.ofSize(Vec3.atCenterOf(pos), RANGE * 2.0, 12, RANGE * 2.0);
            var found = level.getEntitiesOfClass(LivingEntity.class, box);
            be.entities.set(found.size());
            be.nearest.set(found.stream()
                    .min((a, b) -> Double.compare(
                            a.distanceToSqr(Vec3.atCenterOf(pos)),
                            b.distanceToSqr(Vec3.atCenterOf(pos))))
                    .map(e -> e.getName().getString())
                    .orElse("--"));
        };
    }
}
