package dev.vfyjxf.nimbusprojection.api.section;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * What a section set belongs to — a block position or an entity, exactly
 * one of the two. The same address shape rides ops payloads, drag
 * identities and the section snapshot cache, so an entity inventory and
 * a chest are interchangeable targets for transfers.
 */
public record SectionTarget(@Nullable BlockPos pos, @Nullable UUID entity) {

    public static SectionTarget of(BlockPos pos) {
        return new SectionTarget(pos, null);
    }

    public static SectionTarget of(UUID entity) {
        return new SectionTarget(null, entity);
    }

    public static SectionTarget of(Entity entity) {
        return of(entity.getUUID());
    }

    /** Whichever shape is present — null when both are absent. */
    public static @Nullable SectionTarget of(@Nullable BlockPos pos, @Nullable UUID entity) {
        if (pos != null) return of(pos);
        return entity != null ? of(entity) : null;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, SectionTarget> streamCodec = StreamCodec.of(
            (buf, target) -> {
                buf.writeBoolean(target.pos != null);
                if (target.pos != null) buf.writeBlockPos(target.pos);
                else buf.writeUUID(target.entity);
            },
            buf -> buf.readBoolean()
                    ? new SectionTarget(buf.readBlockPos(), null)
                    : new SectionTarget(null, buf.readUUID()));

    /** The entity this target points at — server-side lookup only. */
    public @Nullable Entity resolveEntity(Level level) {
        if (entity == null || !(level instanceof ServerLevel server)) return null;
        return server.getEntity(entity);
    }

    /** Reach-check anchor: the block's center or the entity's live position. */
    public @Nullable Vec3 center(Level level) {
        if (pos != null) return pos.getCenter();
        Entity e = resolveEntity(level);
        return e != null ? e.position().add(0, e.getBbHeight() / 2, 0) : null;
    }

    /** The unsided item handler behind this target — block or entity capability. */
    public @Nullable IItemHandler itemHandler(Level level) {
        if (pos != null) return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        Entity e = resolveEntity(level);
        return e != null ? e.getCapability(Capabilities.ItemHandler.ENTITY) : null;
    }
}
