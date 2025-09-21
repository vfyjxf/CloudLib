package dev.vfyjxf.cloudlib.util;

import dev.vfyjxf.cloudlib.Constants;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Rename {@link ResourceLocation}'s factory methods.
 */
@ApiStatus.Internal
public final class Locations {

    public static <T> ResourceKey<Registry<T>> createKey(ResourceLocation location) {
        return ResourceKey.createRegistryKey(location);
    }

    public static <T> ResourceKey<T> createKey(ResourceKey<? extends Registry<T>> registryKey, ResourceLocation location) {
        return ResourceKey.create(registryKey, location);
    }

    public static ResourceLocation parse(String location) {
        return ResourceLocation.parse(location);
    }

    public static @Nullable ResourceLocation maybe(String location) {
        return ResourceLocation.tryParse(location);
    }

    public static ResourceLocation create(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    public static ResourceLocation ofMc(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    public static ResourceLocation ofMod(String path) {
        return ResourceLocation.fromNamespaceAndPath(Constants.NAMESPACE, path);
    }

    private Locations() {}
}
