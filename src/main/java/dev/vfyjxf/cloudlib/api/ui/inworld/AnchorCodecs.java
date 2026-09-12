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

    private static final Map<AnchorType<?>, AnchorCodec<?>> codecs = new ConcurrentHashMap<>();

    private AnchorCodecs() {}

    public static <A extends InworldAnchor> void register(AnchorCodec<A> codec) {
        codecs.put(codec.type(), codec);
    }

    /** The codec registered for this anchor's kind, or null. */
    public static @Nullable AnchorCodec<?> of(InworldAnchor anchor) {
        return codecs.get(anchor.type());
    }

    /** The codec registered under a kind token, or null. */
    public static @Nullable AnchorCodec<?> of(AnchorType<?> type) {
        return codecs.get(type);
    }

    /** The codec registered under a kind id, or null. */
    public static @Nullable AnchorCodec<?> byId(ResourceLocation id) {
        return codecs.get(AnchorType.of(id));
    }

    /** Whether this anchor can cross the network (has a registered codec). */
    public static boolean shareable(InworldAnchor anchor) {
        return of(anchor) != null;
    }

    // region built-in codecs

    private static <A extends InworldAnchor> AnchorCodec<A> builtin(
            AnchorType<A> type, StreamCodec<RegistryFriendlyByteBuf, A> codec) {
        return new AnchorCodec<>() {
            @Override
            public AnchorType<A> type() {
                return type;
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, A> codec() {
                return codec;
            }
        };
    }

    private static final StreamCodec<RegistryFriendlyByteBuf, Vec3> vec3 = StreamCodec.of(
            (buf, v) -> {
                buf.writeDouble(v.x);
                buf.writeDouble(v.y);
                buf.writeDouble(v.z);
            },
            buf -> new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));

    static {
        register(builtin(
                AnchorType.block,
                StreamCodec.of(
                        (buf, a) -> {
                            BlockPos.STREAM_CODEC.encode(buf, a.pos());
                            vec3.encode(buf, a.offset());
                        },
                        buf -> new InworldAnchor.Block(BlockPos.STREAM_CODEC.decode(buf), vec3.decode(buf)))));
        register(builtin(AnchorType.position, vec3.map(InworldAnchor.Position::new, InworldAnchor.Position::pos)));
        register(builtin(
                AnchorType.entity,
                StreamCodec.of(
                        (buf, a) -> {
                            buf.writeVarInt(a.entityId());
                            vec3.encode(buf, a.offset());
                        },
                        buf -> new InworldAnchor.EntityTarget(buf.readVarInt(), vec3.decode(buf)))));
    }

    // endregion
}
