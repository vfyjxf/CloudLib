package dev.vfyjxf.cloudlib.api.text;

import dev.vfyjxf.cloudlib.api.text.layout.GlyphMeasurer;
import dev.vfyjxf.cloudlib.api.text.layout.RichTextLayouter;
import dev.vfyjxf.cloudlib.api.text.layout.TranslationResolver;
import net.minecraft.network.chat.Style;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Test fixtures: a monospace fake measurer (6px per codepoint, 9px line height) and a
 * map-backed translation resolver, so the layout engine can be tested without a real
 * Minecraft font or language manager.
 */
final class TestTextEnvs {

    static final int CHAR_WIDTH = 6;
    static final int LINE_HEIGHT = 9;

    private TestTextEnvs() {
    }

    static GlyphMeasurer fakeMeasurer() {
        return new GlyphMeasurer() {
            @Override
            public int width(String text, Style style) {
                return text.codePointCount(0, text.length()) * CHAR_WIDTH;
            }

            @Override
            public int lineHeight() {
                return LINE_HEIGHT;
            }
        };
    }

    static TranslationResolver mapResolver(Map<String, String> patterns) {
        return key -> Optional.ofNullable(patterns.get(key));
    }

    static TranslationResolver emptyResolver() {
        return key -> Optional.empty();
    }

    static RichTextLayouter layouter() {
        return new RichTextLayouter(fakeMeasurer(), emptyResolver());
    }

    static RichTextLayouter layouter(Map<String, String> patterns) {
        return new RichTextLayouter(fakeMeasurer(), mapResolver(patterns));
    }

    static Map<String, String> patterns(String... keyValues) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    static int width(String text) {
        return text.codePointCount(0, text.length()) * CHAR_WIDTH;
    }

    /**
     * JUnit 5.7 has no {@code assertInstanceOf}; this is a drop-in replacement.
     */
    static <T> T assertInstanceOf(Class<T> type, Object value) {
        if (!type.isInstance(value)) {
            throw new AssertionError("expected instance of " + type.getSimpleName()
                    + " but was " + (value == null ? "null" : value.getClass().getSimpleName()));
        }
        return type.cast(value);
    }
}
