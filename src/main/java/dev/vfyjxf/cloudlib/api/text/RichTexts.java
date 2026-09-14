package dev.vfyjxf.cloudlib.api.text;

import dev.vfyjxf.cloudlib.api.text.layout.LaidOutText;
import dev.vfyjxf.cloudlib.api.text.layout.LayoutConstraints;
import dev.vfyjxf.cloudlib.api.text.layout.RichTextLayouter;
import dev.vfyjxf.cloudlib.api.text.layout.RichTextMeasure;
import dev.vfyjxf.cloudlib.api.text.render.RichTextRenderer;
import dev.vfyjxf.cloudlib.text.RichTextManager;
import org.jetbrains.annotations.ApiStatus;

/**
 * Client-side entry point of the rich text system.
 * <p>
 * Use {@link #measure(RichText)} to obtain a taffy {@code MeasureFunc} for a
 * document (the usual way to give a rich text an intrinsic size inside the
 * widget tree), {@link #layout(RichNode, LayoutConstraints)} for a one-off
 * layout outside the widget pipeline (e.g. tooltip sizing), and
 * {@link #renderer()} to draw a laid-out document onto a canvas.
 * <p>
 * Widgets such as {@code RichTextWidget} already route through these methods;
 * call them directly only when integrating rich text with custom rendering or
 * layout code.
 */
@ApiStatus.NonExtendable
public final class RichTexts {

    /**
     * Creates a taffy measure function for the given document.
     */
    public static RichTextMeasure measure(RichText text) {
        return RichTextManager.getInstance().measure(text);
    }

    /**
     * Convenience one-off layout for code outside the widget pipeline.
     */
    public static LaidOutText layout(RichNode root, LayoutConstraints constraints) {
        return RichTextManager.getInstance().layout(root, constraints);
    }

    /**
     * The shared layouter (vanilla font measuring + the active language).
     */
    public static RichTextLayouter layouter() {
        return RichTextManager.getInstance().layouter();
    }

    /**
     * The default renderer for laid-out documents.
     */
    public static RichTextRenderer renderer() {
        return RichTextManager.getInstance().renderer();
    }

    private RichTexts() {
        throw new AssertionError("This class should not be instantiated!");
    }
}
