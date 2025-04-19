package dev.vfyjxf.cloudlib.ui;

import dev.vfyjxf.cloudlib.ui.textures.SpriteUploader;
import org.jetbrains.annotations.ApiStatus;

/**
 * Based on {@link  mezz.jei.common.gui.textures.Textures}
 */
public final class Textures {

    private static SpriteUploader uploader;

    @ApiStatus.Internal
    public static void setUploader(SpriteUploader uploader) {
        Textures.uploader = uploader;
    }


    private Textures() {
    }

}
