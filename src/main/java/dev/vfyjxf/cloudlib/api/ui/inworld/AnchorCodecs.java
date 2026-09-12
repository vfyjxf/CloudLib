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
 * registering its own codec — no runtime involvement needed.
 */
public final class AnchorCodecs {

    private static final Map<Class<? extends InworldAnchor>, AnchorCodec<?>> BY_TYPE = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, AnchorCodec<?>> BY_ID = new ConcurrentHashMap<>();

    private AnchorCodecs() {
    }

    public static <A extends InworldAnchor> void register(AnchorCodec<A> codec) {
        BY_TYPE.put(codec.anchorType(), codec);
        BY_ID.put(codec.id(), codec);
    }

    /** The codec registered for this anchor's implementation, or null. */
    public static @Nullable AnchorCodec<?> of(InworldAnchor anchor) {
        return BY_TYPE.get(anchor.getClass());
    }

    public static @Nullable AnchorCodec<?> byId(ResourceLocation id) {
        return BY_ID.get(id);
    }

    /** Whether this anchor can cross the network (has a registered codec). */
    public static boolean shareable(InworldAnchor anchor) {
        return of(anchor) != null;
    }

    //region built-in codecs

    private static <A extends InworldAnchor> AnchorCodec<A> builtin(
            String path, Class<A> type, StreamCodec<RegistryFriendlyByteBuf, A> codec
    ) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("cloudlib", path);
        return new AnchorCodec<>() {
            @Override public ResourceLocation id() {
                return id;
            }

            @Override public Class<A> anchorType() {
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
        register(builtin("block", InworldAnchor.Block.class, StreamCodec.of(
                (buf, a) -> {
                    BlockPos.STREAM_CODEC.encode(buf, a.pos());
                    VEC3.encode(buf, a.offset());
                },
                buf -> new InworldAnchor.Block(BlockPos.STREAM_CODEC.decode(buf), VEC3.decode(buf))
        )));
        register(builtin("position", InworldAnchor.Position.class,
                VEC3.map(InworldAnchor.Position::new, InworldAnchor.Position::pos)));
        register(builtin("entity", InworldAnchor.EntityTarget.class, StreamCodec.of(
                (buf, a) -> {
                    buf.writeVarInt(a.entityId());
                    VEC3.encode(buf, a.offset());
                },
                buf -> new InworldAnchor.EntityTarget(buf.readVarInt(), VEC3.decode(buf))
        )));
    }

    //endregion
}
