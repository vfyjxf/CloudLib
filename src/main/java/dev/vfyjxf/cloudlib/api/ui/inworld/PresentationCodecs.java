package dev.vfyjxf.cloudlib.api.ui.inworld;

import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wire-codec registry for {@link Presentation} — the same opt-in model as
 * {@link AnchorCodecs}: a presentation type crosses the network only when a
 * codec was registered under its {@link Presentation#type()} id.
 * <p>
 * Built-ins registered: {@link Presentation.Face}, {@link Presentation.Follow},
 * {@link Presentation.Dock}, {@link Presentation.Expand} and
 * {@link Presentation.InspectOnly}. {@link Presentation.Floating} is
 * deliberately NOT registered — its middleware list is arbitrary client-side
 * behaviour and cannot be serialized; a receiving client's view chooses its
 * own placement anyway. Custom presentations may opt in with a codec of
 * their own (middleware-free shapes only — the wire carries data, not code).
 */
public final class PresentationCodecs {

    private static final Map<ResourceLocation, StreamCodec<? super RegistryFriendlyByteBuf, ? extends Presentation>> BY_ID =
            new ConcurrentHashMap<>();

    private PresentationCodecs() {
    }

    public static <P extends Presentation> void register(
            ResourceLocation type,
            StreamCodec<? super RegistryFriendlyByteBuf, P> codec
    ) {
        BY_ID.put(type, codec);
    }

    /** The codec registered for this descriptor's type, or null = not shareable. */
    @SuppressWarnings("unchecked")
    public static @Nullable StreamCodec<? super RegistryFriendlyByteBuf, Presentation> of(Presentation presentation) {
        return (StreamCodec<? super RegistryFriendlyByteBuf, Presentation>) BY_ID.get(presentation.type());
    }

    /** Whether this presentation may cross the network (a codec is registered). */
    public static boolean shareable(Presentation presentation) {
        return BY_ID.containsKey(presentation.type());
    }

    /** Encodes a presentation as {@code typeId + payload}; fails fast when unshareable. */
    public static void write(RegistryFriendlyByteBuf buf, Presentation presentation) {
        StreamCodec<? super RegistryFriendlyByteBuf, Presentation> codec = of(presentation);
        if (codec == null) {
            throw new IllegalArgumentException(
                    "Presentation " + presentation.type() + " has no registered codec — not shareable");
        }
        ResourceLocation.STREAM_CODEC.encode(buf, presentation.type());
        codec.encode(buf, presentation);
    }

    public static Presentation read(RegistryFriendlyByteBuf buf) {
        ResourceLocation type = ResourceLocation.STREAM_CODEC.decode(buf);
        StreamCodec<? super RegistryFriendlyByteBuf, ? extends Presentation> codec = BY_ID.get(type);
        if (codec == null) {
            throw new IllegalArgumentException("Unknown presentation type on the wire: " + type);
        }
        return codec.decode(buf);
    }

    static {
        register(Presentation.Builtin.FACE, StreamCodec.of(
                (buf, p) -> {
                    Presentation.Face f = (Presentation.Face) p;
                    buf.writeEnum(f.face());
                    buf.writeDouble(f.u());
                    buf.writeDouble(f.v());
                    buf.writeDouble(f.pixelsPerBlock());
                },
                buf -> new Presentation.Face(
                        buf.readEnum(Direction.class), buf.readDouble(), buf.readDouble(), buf.readDouble())));
        register(Presentation.Builtin.FOLLOW, StreamCodec.of(
                (buf, p) -> {
                    Presentation.Follow f = (Presentation.Follow) p;
                    buf.writeDouble(f.offsetX());
                    buf.writeDouble(f.offsetY());
                },
                buf -> new Presentation.Follow(buf.readDouble(), buf.readDouble())));
        register(Presentation.Builtin.DOCK, StreamCodec.of(
                (buf, p) -> buf.writeEnum(((Presentation.Dock) p).corner()),
                buf -> new Presentation.Dock(buf.readEnum(Presentation.DockCorner.class))));
        register(Presentation.Builtin.EXPAND, StreamCodec.of(
                (buf, p) -> buf.writeDouble(((Presentation.Expand) p).pixelsPerBlock()),
                buf -> new Presentation.Expand(buf.readDouble())));
        register(Presentation.Builtin.INSPECT_ONLY, StreamCodec.of(
                (buf, p) -> buf.writeBoolean(((Presentation.InspectOnly) p).affordance()),
                buf -> new Presentation.InspectOnly(buf.readBoolean())));
    }
}
