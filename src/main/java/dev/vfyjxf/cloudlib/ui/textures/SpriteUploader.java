package dev.vfyjxf.cloudlib.ui.textures;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.TextureAtlasHolder;
import net.minecraft.resources.ResourceLocation;

public class SpriteUploader extends TextureAtlasHolder {

    public final ResourceLocation atlasLocation;
    public final String modid;

    public SpriteUploader(TextureManager textureManager, String modid, ResourceLocation atlasLocation) {
        super(textureManager, atlasLocation, new ResourceLocation(modid, "gui"));
        this.atlasLocation = atlasLocation;
        this.modid = modid;
    }


    /**
     * Overridden to make it public
     */
    @Override
    public TextureAtlasSprite getSprite(ResourceLocation location) {
        return super.getSprite(location);
    }
}
