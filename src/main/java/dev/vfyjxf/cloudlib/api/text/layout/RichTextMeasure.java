package dev.vfyjxf.cloudlib.api.text.layout;

import dev.vfyjxf.cloudlib.api.text.RichNode;
import dev.vfyjxf.cloudlib.api.text.RichText;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.util.MeasureFunc;

/**
 * Taffy text-measurement adapter: a {@link MeasureFunc} that reports the intrinsic
 * size of a rich text document to the taffy layout engine.
 * <p>
 * Semantics follow CSS:
 * <ul>
 *   <li>known (style-driven) width wraps at exactly that width;</li>
 *   <li>{@code AvailableSpace.DEFINITE(w)} wraps at {@code w} and reports the widest
 *       resulting line as width;</li>
 *   <li>{@code MAX_CONTENT} reports the longest natural (unwrapped) line;</li>
 *   <li>{@code MIN_CONTENT} reports the longest unbreakable unit (word or object);</li>
 *   <li>a known height always wins over the laid-out height.</li>
 * </ul>
 * The laid-out result is cached per width; {@link #layoutAt(int)} doubles as the
 * render-time accessor so a widget measures and renders from the same layout.
 * <p>
 * Not thread-safe: layout happens on the client thread.
 */
public final class RichTextMeasure implements MeasureFunc {

    private final RichNode root;
    private final RichTextLayouter layouter;

    private TextAlignment alignment = TextAlignment.LEFT;
    private int lineSpacing = 0;
    private boolean wrap = true;

    private LaidOutText cachedLayout;
    private int cachedWidth = Integer.MIN_VALUE;

    public RichTextMeasure(RichText text, RichTextLayouter layouter) {
        this(text.root(), layouter);
    }

    public RichTextMeasure(RichNode root, RichTextLayouter layouter) {
        this.root = root;
        this.layouter = layouter;
    }

    public RichNode root() {
        return root;
    }

    public RichTextLayouter layouter() {
        return layouter;
    }

    public TextAlignment alignment() {
        return alignment;
    }

    public RichTextMeasure withAlignment(TextAlignment alignment) {
        this.alignment = alignment;
        invalidate();
        return this;
    }

    public int lineSpacing() {
        return lineSpacing;
    }

    public RichTextMeasure withLineSpacing(int lineSpacing) {
        this.lineSpacing = lineSpacing;
        invalidate();
        return this;
    }

    public boolean wrap() {
        return wrap;
    }

    public RichTextMeasure withWrap(boolean wrap) {
        this.wrap = wrap;
        invalidate();
        return this;
    }

    /**
     * Drops the cached layout (e.g. after the text or the locale changed).
     */
    public void invalidate() {
        cachedLayout = null;
        cachedWidth = Integer.MIN_VALUE;
    }

    /**
     * Lays out the text at the given wrap width, reusing the cached result when the
     * width is unchanged. This is both the measure-time and the render-time entry
     * point.
     */
    public LaidOutText layoutAt(int maxWidth) {
        if (cachedLayout != null && cachedWidth == maxWidth) {
            return cachedLayout;
        }
        LayoutConstraints constraints = new LayoutConstraints(
                wrap ? maxWidth : LayoutConstraints.UNCONSTRAINED,
                alignment,
                lineSpacing
        );
        LaidOutText laidOut = layouter.layout(root, constraints);
        cachedLayout = laidOut;
        cachedWidth = maxWidth;
        return laidOut;
    }

    @Override
    public FloatSize measure(FloatSize knownDimensions, TaffySize<AvailableSpace> availableSpace) {
        float knownWidth = knownDimensions.width;
        float knownHeight = knownDimensions.height;

        LaidOutText laidOut;
        float width;
        if (!Float.isNaN(knownWidth)) {
            laidOut = layoutAt((int) Math.ceil(knownWidth));
            width = knownWidth;
        } else if (!wrap) {
            laidOut = layoutAt(LayoutConstraints.UNCONSTRAINED);
            width = contentWidth(laidOut);
        } else {
            AvailableSpace available = availableSpace.width;
            if (available.isDefinite()) {
                laidOut = layoutAt((int) Math.ceil(available.getValue()));
                width = contentWidth(laidOut);
            } else if (available.isMinContent()) {
                float minWidth = layouter.minContentWidth(root);
                laidOut = layoutAt((int) Math.ceil(minWidth));
                width = contentWidth(laidOut);
            } else {
                laidOut = layoutAt(LayoutConstraints.UNCONSTRAINED);
                width = contentWidth(laidOut);
            }
        }

        float height = !Float.isNaN(knownHeight) ? knownHeight : laidOut.height();
        return new FloatSize(width, height);
    }

    private static float contentWidth(LaidOutText laidOut) {
        float width = 0;
        for (TextLine line : laidOut.lines()) {
            width = Math.max(width, line.width());
        }
        return width;
    }
}
