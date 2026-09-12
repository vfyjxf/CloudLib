package dev.vfyjxf.cloudlib.api.ui.inworld;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of {@link AnchorCodec}s — the normalization layer that makes
 * anchor kinds uniformly transmissible.
 * <p>
 * Built-in codecs cover block positions, fixed positions and entity
 * targets; lazily-resolved {@code Tracked} anchors are client-only and
 * have no codec. A custom {@code InworldAnchor} becomes shareable by
 * reporting its {@link AnchorType} and registering a codec for it — no
 * runtime involvement needed.
 */
public final class AnchorCodecs {

    private static final Map<AnchorType<?>, AnchorCodec<?>> CODECS = new ConcurrentHashMap<>();

    private AnchorCodecs() {
    }

    public static <A extends InworldAnchor> void register(AnchorCodec<A> codec) {
        CODECS.put(codec.type(), codec);
    }

    /** The codec registered for this anchor's kind, or null. */
    public static @Nullable AnchorCodec<?> of(InworldAnchor anchor) {
        return CODECS.get(anchor.type());
    }

    /** The codec registered under a kind token, or null. */
    public static @Nullable AnchorCodec<?> of(AnchorType<?> type) {
        return CODECS.get(type);
    }

    /** The codec registered under a kind id, or null. */
    public static @Nullable AnchorCodec<?> byId(ResourceLocation id) {
        return CODECS.get(AnchorType.of(id));
    }

    /** Whether this anchor can cross the network (has a registered codec). */
    public static boolean shareable(InworldAnchor anchor) {
        return of(anchor) != null;
    }

    //region built-in codecs

    private static <A extends InworldAnchor> AnchorCodec<A> builtin(
            AnchorType<A> type, StreamCodec<RegistryFriendlyByteBuf, A> codec
    ) {
        return new AnchorCodec<>() {
            @Override public AnchorType<A> type() {
                return type;
            }

            @Override public StreamCodec<? super RegistryFriendlyByteBuf, A> codec() {
                return codec;
            }
        };
    }

    private static final StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC3 = StreamCodec.of(
            (buf, v) -> {
                buf.writeDouble(v.x);
                buf.writeDouble(v.y);
                buf.writeDouble(v.z);
            },
            buf -> new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble())
    );

    static {
        register(builtin(AnchorType.BLOCK, StreamCodec.of(
                (buf, a) -> {
                    BlockPos.STREAM_CODEC.encode(buf, a.pos());
                    VEC3.encode(buf, a.offset());
                },
                buf -> new InworldAnchor.Block(BlockPos.STREAM_CODEC.decode(buf), VEC3.decode(buf))
        )));
        register(builtin(AnchorType.POSITION,
                VEC3.map(InworldAnchor.Position::new, InworldAnchor.Position::pos)));
        register(builtin(AnchorType.ENTITY, StreamCodec.of(
                (buf, a) -> {
                    buf.writeVarInt(a.entityId());
                    VEC3.encode(buf, a.offset());
                },
                buf -> new InworldAnchor.EntityTarget(buf.readVarInt(), VEC3.decode(buf))
        )));
    }

    //endregion
}
