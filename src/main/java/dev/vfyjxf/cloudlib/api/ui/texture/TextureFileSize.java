package dev.vfyjxf.cloudlib.api.ui.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The pixel size of a texture file, resolved lazily and cached per location.
 * <p>
 * Region textures ({@link ImageTexture#region}, {@link NineSliceTexture#region})
 * address a sub-rectangle of a sheet without knowing the sheet's size up front —
 * theme values are parsed long before the file is loaded, and a theme should not
 * have to repeat pixel dimensions the asset already carries. The size is read from
 * the resource pack the first time such a texture draws (the client is up by then);
 * an unreadable file falls back to {@code 1x1} so a bad path never divides by zero
 * nor kills a frame.
 */
final class TextureFileSize {

    /** The fallback for a file that cannot be read. */
    private static final TextureFileSize unknown = new TextureFileSize(1, 1);

    private static final Map<ResourceLocation, TextureFileSize> cache = new ConcurrentHashMap<>();

    private final int width;
    private final int height;

    private TextureFileSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    static TextureFileSize of(ResourceLocation location) {
        return cache.computeIfAbsent(location, TextureFileSize::read);
    }

    private static TextureFileSize read(ResourceLocation location) {
        try (InputStream in = Minecraft.getInstance().getResourceManager().getResourceOrThrow(location).open();
                NativeImage image = NativeImage.read(in)) {
            return new TextureFileSize(image.getWidth(), image.getHeight());
        } catch (Throwable failure) {
            return unknown;
        }
    }

    int width() {
        return width;
    }

    int height() {
        return height;
    }
}
