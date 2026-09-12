package dev.vfyjxf.cloudlib.api.ui.inworld;

import dev.vfyjxf.cloudlib.util.Checks;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * Typed identity of an in-world panel: {@code namespace:path}.
 * <p>
 * Keys are the reconcile unit of the panel layer — a provider re-offers the
 * same key each pass to keep a panel alive; a missing key closes it. Because
 * every key is a namespaced string it serializes trivially, so local panels
 * and network-shared panels share one identity model. Providers should use
 * their own mod id as the namespace; the path may be hierarchical
 * ("tracker/ent/12") — the parent path derives the default zoning group.
 */
public record PanelKey(String namespace, String path) {

    public static final StreamCodec<ByteBuf, PanelKey> STREAM_CODEC =
            ResourceLocation.STREAM_CODEC.map(
                    rl -> new PanelKey(rl.getNamespace(), rl.getPath()),
                    key -> ResourceLocation.fromNamespaceAndPath(key.namespace, key.path)
            );

    public PanelKey {
        Checks.checkArgument(
                ResourceLocation.isValidNamespace(namespace),
                "invalid panel key namespace '%s'", namespace);
        Checks.checkArgument(
                ResourceLocation.isValidPath(path),
                "invalid panel key path '%s'", path);
    }

    public static PanelKey of(String namespace, String path) {
        return new PanelKey(namespace, path);
    }

    /** Parses {@code "namespace:path"}. */
    public static PanelKey parse(String combined) {
        ResourceLocation rl = ResourceLocation.parse(combined);
        return new PanelKey(rl.getNamespace(), rl.getPath());
    }

    /** The path's parent ("tracker/ent/12" → "tracker/ent"), for zoning. */
    public String parentPath() {
        int slash = path.lastIndexOf('/');
        return slash > 0 ? path.substring(0, slash) : path;
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }

}
