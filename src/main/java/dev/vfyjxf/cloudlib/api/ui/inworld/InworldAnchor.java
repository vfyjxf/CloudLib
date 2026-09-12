package dev.vfyjxf.cloudlib.api.ui.inworld;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * A world-space point an in-world UI surface is anchored to.
 * <p>
 * The anchor is resolved every frame against the client level, so anchors
 * backed by entities or dynamic suppliers track their target automatically.
 * <p>
 * The interface is open — a custom anchor becomes network-shareable by
 * registering an {@link AnchorCodec} on {@link AnchorCodecs}.
 */
public interface InworldAnchor {

    //region factories

    /** Anchor at the center of a block. */
    static InworldAnchor of(BlockPos pos) {
        return new Block(pos, new Vec3(0.5, 0.5, 0.5));
    }

    /** Anchor at {@code pos + offset} (offset in block units). */
    static InworldAnchor of(BlockPos pos, Vec3 offset) {
        return new Block(pos, offset);
    }

    /** Anchor at a fixed world position. */
    static InworldAnchor of(Vec3 pos) {
        return new Position(pos);
    }

    /** Anchor at a dynamically resolved world position. */
    static InworldAnchor of(Supplier<Vec3> pos) {
        return new Tracked(pos);
    }

    /** Anchor at {@code entity position + offset}, resolved by entity id. */
    static InworldAnchor ofEntity(int entityId, Vec3 offset) {
        return new EntityTarget(entityId, offset);
    }

    //endregion

    /**
     * The anchor kind's identity token — see {@link AnchorType}. Custom
     * anchors become network-shareable by reporting their own token and
     * registering an {@link AnchorCodec} for it.
     */
    AnchorType<?> type();

    /**
     * Resolves the anchor to an absolute world position.
     *
     * @return the world position, or {@code null} when the anchor is currently
     * invalid (unloaded chunk, dead entity, ...)
     */
    @Nullable
    Vec3 position(ClientLevel level);

    /**
     * Resolves the anchor interpolated for the current rendered frame.
     * Tick-snapped implementations should override this to lerp between the
     * previous and current position — without it panels anchored to moving
     * entities step-jitter against the smooth rendered world.
     */
    default @Nullable Vec3 position(ClientLevel level, float partialTick) {
        return position(level);
    }

    /** @return whether this anchor currently resolves to a position */
    default boolean alive(ClientLevel level) {
        return position(level) != null;
    }

    /** @return the backing block position when this anchor is block-bound, else null */
    default @Nullable BlockPos blockPos() {
        return null;
    }

    //region impls

    /** A block-bound anchor; {@code offset} is in block units from the block's min corner. */
    record Block(BlockPos pos, Vec3 offset) implements InworldAnchor {

        @Override
        public AnchorType<?> type() {
            return AnchorType.BLOCK;
        }

        @Override
        public @Nullable Vec3 position(ClientLevel level) {
            if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) return null;
            return Vec3.atLowerCornerOf(pos).add(offset);
        }

        @Override
        public BlockPos blockPos() {
            return pos;
        }
    }

    /** A fixed world position. */
    record Position(Vec3 pos) implements InworldAnchor {
        @Override
        public AnchorType<?> type() {
            return AnchorType.POSITION;
        }

        @Override
        public Vec3 position(ClientLevel level) {
            return pos;
        }
    }

    /** A lazily resolved world position (e.g. tracking a moving target). */
    record Tracked(Supplier<@Nullable Vec3> pos) implements InworldAnchor {
        @Override
        public AnchorType<?> type() {
            return AnchorType.TRACKED;
        }

        @Override
        public @Nullable Vec3 position(ClientLevel level) {
            return pos.get();
        }
    }

    /** An entity-bound anchor resolved through {@link ClientLevel#getEntity(int)}. */
    record EntityTarget(int entityId, Vec3 offset) implements InworldAnchor {
        @Override
        public AnchorType<?> type() {
            return AnchorType.ENTITY;
        }

        @Override
        public @Nullable Vec3 position(ClientLevel level) {
            Entity entity = level.getEntity(entityId);
            if (entity == null || !entity.isAlive()) return null;
            return entity.position().add(offset);
        }

        @Override
        public @Nullable Vec3 position(ClientLevel level, float partialTick) {
            Entity entity = level.getEntity(entityId);
            if (entity == null || !entity.isAlive()) return null;
            return entity.getPosition(partialTick).add(offset);
        }
    }

    //endregion
}
