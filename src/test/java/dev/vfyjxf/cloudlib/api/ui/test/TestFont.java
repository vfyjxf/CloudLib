package dev.vfyjxf.cloudlib.api.ui.test;

import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;

import java.lang.reflect.Field;

/**
 * Creates lightweight {@link Font} instances for unit testing without requiring
 * Minecraft's rendering infrastructure (FontSet, TextureManager, GlyphProvider, etc.).
 * <p>
 * This implementation constructs a real {@link Font} object but replaces its internal
 * {@link StringSplitter} with a fixed-width (or custom) {@link StringSplitter.WidthProvider}.
 * This allows all measurement methods ({@code width()}, {@code getSplitter()}, {@code lineHeight})
 * to work correctly in tests.
 *
 * <h3>Version compatibility</h3>
 * <b>Target: NeoForge 21.1.x (Minecraft 1.21.1)</b>
 * <p>
 * This class relies on the internal structure of {@code net.minecraft.client.gui.Font}:
 * <ul>
 *   <li>Font constructor: {@code Font(Function<ResourceLocation, FontSet>, boolean)}</li>
 *   <li>Font has a private field {@code splitter} of type {@link StringSplitter}</li>
 *   <li>{@code Font.width(String)} delegates to {@code splitter.stringWidth()}</li>
 *   <li>{@code Font.lineHeight} is a final field initialized to {@code 9}</li>
 * </ul>
 * If the internal structure of Font changes in future Minecraft versions (field renamed,
 * constructor signature changed, etc.), this class will need to be updated accordingly.
 *
 * <h3>Supported methods</h3>
 * <ul>
 *   <li>{@code width(String)} — {@code ceil(charWidth × length)}</li>
 *   <li>{@code width(FormattedText)} — via StringSplitter</li>
 *   <li>{@code width(FormattedCharSequence)} — via StringSplitter</li>
 *   <li>{@code lineHeight} — always 9 (Minecraft default)</li>
 *   <li>{@code getSplitter()} — returns the custom StringSplitter</li>
 *   <li>{@code plainSubstrByWidth()}, {@code substrByWidth()},
 *       {@code wordWrapHeight()}, {@code split()} — all functional via StringSplitter</li>
 * </ul>
 *
 * <h3>Unsupported methods</h3>
 * All <b>rendering</b> methods ({@code drawInBatch}, {@code drawInBatch8xOutline}, etc.)
 * will throw {@link NullPointerException} because the underlying FontSet is not available.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // Fixed-width font (6px per character, Minecraft default average)
 * Font font = TestFont.create();
 *
 * // Custom fixed width
 * Font font = TestFont.create(8.0f);
 *
 * // Custom per-character width provider
 * Font font = TestFont.create((codePoint, style) -> {
 *     if (codePoint == ' ') return 4.0f;
 *     return 6.0f;
 * });
 *
 * // Use with TestSceneHost
 * var host = new TestSceneHost(800, 600, TestFont.create());
 * }</pre>
 */
public final class TestFont {

    /**
     * Default character width (6px), approximating Minecraft's default font average.
     */
    public static final float DEFAULT_CHAR_WIDTH = 6.0f;

    private TestFont() {
    }

    /**
     * Creates a Font where every character has a fixed width of {@value #DEFAULT_CHAR_WIDTH} pixels.
     */
    public static Font create() {
        return create(DEFAULT_CHAR_WIDTH);
    }

    /**
     * Creates a Font where every character has the specified fixed width.
     *
     * @param charWidth the width in pixels for each character
     */
    public static Font create(float charWidth) {
        return create((codePoint, style) -> charWidth);
    }

    /**
     * Creates a Font with a custom width provider function.
     *
     * @param widthProvider provides width for each (codePoint, style) pair
     */
    public static Font create(StringSplitter.WidthProvider widthProvider) {
        // Construct Font with a dummy FontSet provider.
        // The WidthProvider lambda inside Font's constructor captures `this`
        // and calls getFontSet(), but it won't be invoked because we replace
        // the splitter immediately after construction.
        Font font = new Font(loc -> null, false);

        StringSplitter customSplitter = new StringSplitter(widthProvider);
        setSplitter(font, customSplitter);

        return font;
    }

    private static void setSplitter(Font font, StringSplitter splitter) {
        try {
            Field field = Font.class.getDeclaredField("splitter");
            field.setAccessible(true);
            field.set(font, splitter);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(
                    "Font.splitter field not found. " +
                    "TestFont targets NeoForge 21.1.x (MC 1.21.1). " +
                    "The Font class structure may have changed.", e);
        } catch (IllegalAccessException e) {
            // Final field restriction on some JDK versions — fallback to Unsafe
            setSplitterUnsafe(font, splitter);
        }
    }

    @SuppressWarnings("removal")
    private static void setSplitterUnsafe(Font font, StringSplitter splitter) {
        try {
            Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);

            long offset = unsafe.objectFieldOffset(Font.class.getDeclaredField("splitter"));
            unsafe.putObject(font, offset, splitter);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to create test Font: cannot write Font.splitter field. " +
                    "TestFont targets NeoForge 21.1.x (MC 1.21.1). " +
                    "Ensure --add-opens java.base/sun.misc=ALL-UNNAMED if using Java modules.", e);
        }
    }
}
