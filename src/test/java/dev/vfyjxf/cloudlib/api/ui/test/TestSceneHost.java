package dev.vfyjxf.cloudlib.api.ui.test;

import dev.vfyjxf.cloudlib.api.ui.base.SceneHost;
import net.minecraft.client.gui.Font;
import org.jetbrains.annotations.Nullable;

/**
 * A lightweight {@link SceneHost} for pure JUnit tests.
 * <p>
 * Does not require a running Minecraft client. When no Font is provided,
 * a fixed-width {@link TestFont} is created automatically on first access
 * (each character defaults to {@value TestFont#DEFAULT_CHAR_WIDTH}px).
 *
 * @see TestFont
 */
public final class TestSceneHost implements SceneHost {

    private int width;
    private int height;
    private @Nullable Font font;

    public TestSceneHost(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public TestSceneHost(int width, int height, @Nullable Font font) {
        this.width = width;
        this.height = height;
        this.font = font;
    }

    /**
     * Returns the font for this test scene.
     * If no font was explicitly set, a fixed-width {@link TestFont} is created
     * lazily (each character = {@value TestFont#DEFAULT_CHAR_WIDTH}px, lineHeight = 9).
     */
    @Override
    public Font font() {
        if (font == null) {
            font = TestFont.create();
        }
        return font;
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
    }
}
