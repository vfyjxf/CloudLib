package dev.vfyjxf.cloudlib.text;

import dev.vfyjxf.cloudlib.api.text.RichNode;
import dev.vfyjxf.cloudlib.api.text.RichText;
import dev.vfyjxf.cloudlib.api.text.layout.GlyphMeasurer;
import dev.vfyjxf.cloudlib.api.text.layout.LaidOutText;
import dev.vfyjxf.cloudlib.api.text.layout.LayoutConstraints;
import dev.vfyjxf.cloudlib.api.text.layout.RichTextLayouter;
import dev.vfyjxf.cloudlib.api.text.layout.RichTextMeasure;
import dev.vfyjxf.cloudlib.api.text.layout.TranslationResolver;
import dev.vfyjxf.cloudlib.api.text.render.RichTextRenderer;
import net.minecraft.client.Minecraft;

/**
 * Client-side entry point of the rich text system: holds the shared
 * {@link RichTextLayouter} (vanilla font measuring + the active language) and the
 * default {@link RichTextRenderer}.
 * <p>
 * Use {@link #measure(RichText)} to obtain a taffy {@code MeasureFunc} for a
 * document, and {@link #layouter()} when a one-off layout is needed outside the
 * widget pipeline (e.g. tooltip sizing).
 */
public final class RichTextManager {

    private static RichTextManager instance;

    public static RichTextManager getInstance() {
        if (instance == null) {
            instance = new RichTextManager();
        }
        return instance;
    }

    /**
     * Drops cached state derived from the active language / font. Called when the
     * client reloads resources (language or resource pack change).
     */
    public static void invalidate() {
        instance = null;
    }

    private final GlyphMeasurer measurer;
    private final TranslationResolver translations;
    private final RichTextLayouter layouter;
    private final RichTextRenderer renderer;

    private RichTextManager() {
        this.measurer = new VanillaGlyphMeasurer(Minecraft.getInstance().font);
        this.translations = VanillaTranslationResolver.INSTANCE;
        this.layouter = new RichTextLayouter(measurer, translations);
        this.renderer = new DefaultRichTextRenderer();
    }

    public GlyphMeasurer measurer() {
        return measurer;
    }

    public TranslationResolver translations() {
        return translations;
    }

    public RichTextLayouter layouter() {
        return layouter;
    }

    public RichTextRenderer renderer() {
        return renderer;
    }

    /**
     * Creates a taffy measure function for the given document.
     */
    public RichTextMeasure measure(RichText text) {
        return new RichTextMeasure(text, layouter);
    }

    /**
     * Convenience one-off layout for code outside the widget pipeline.
     */
    public LaidOutText layout(RichNode root, LayoutConstraints constraints) {
        return layouter.layout(root, constraints);
    }
}
