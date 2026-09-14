package dev.vfyjxf.cloudlib.api.text;

import dev.vfyjxf.cloudlib.api.math.Insets;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable style of a rich text node.
 * <p>
 * Text attributes (color, bold, italic, underlined, strikethrough, obfuscated, font,
 * vanilla click/hover events) are carried by the vanilla {@link Style} so that styled
 * runs render through the vanilla glyph pipeline with vanilla inheritance semantics.
 * The remaining fields are CloudLib extensions understood by the rich text layouter
 * and renderer.
 *
 * @param style          the vanilla style, never {@code null} ({@link Style#EMPTY} by default)
 * @param shadow         whether text runs draw with a drop shadow; {@code null} = inherit
 * @param highlightColor ARGB background highlight behind the fragments; {@code null} = none/inherit
 * @param verticalAlign  vertical alignment of inline objects within a line; {@code null} = inherit
 * @param padding        extra padding around an inline object's reserved box; {@code null} = none/inherit
 */
public record RichTextStyle(
        Style style,
        @Nullable Boolean shadow,
        @Nullable Integer highlightColor,
        @Nullable VerticalAlign verticalAlign,
        @Nullable Insets padding
) {

    public static final RichTextStyle EMPTY = new RichTextStyle(Style.EMPTY, null, null, null, null);

    public static RichTextStyle of(Style style) {
        return new RichTextStyle(style, null, null, null, null);
    }

    /**
     * Merges {@code other} over this style: non-null fields of {@code other} win,
     * vanilla styles follow {@link Style#applyTo} semantics.
     */
    public RichTextStyle merge(RichTextStyle other) {
        if (other == EMPTY) return this;
        if (this == EMPTY) return other;
        return new RichTextStyle(
                this.style.applyTo(other.style),
                other.shadow != null ? other.shadow : this.shadow,
                other.highlightColor != null ? other.highlightColor : this.highlightColor,
                other.verticalAlign != null ? other.verticalAlign : this.verticalAlign,
                other.padding != null ? other.padding : this.padding
        );
    }

    public boolean isEmpty() {
        return this == EMPTY
                || (style == Style.EMPTY && shadow == null && highlightColor == null
                && verticalAlign == null && padding == null);
    }

    public RichTextStyle withStyle(Style style) {
        return new RichTextStyle(style, shadow, highlightColor, verticalAlign, padding);
    }

    public RichTextStyle withShadow(boolean shadow) {
        return new RichTextStyle(style, shadow, highlightColor, verticalAlign, padding);
    }

    public RichTextStyle withHighlightColor(int argb) {
        return new RichTextStyle(style, shadow, argb, verticalAlign, padding);
    }

    public RichTextStyle withVerticalAlign(VerticalAlign align) {
        return new RichTextStyle(style, shadow, highlightColor, align, padding);
    }

    public RichTextStyle withPadding(Insets padding) {
        return new RichTextStyle(style, shadow, highlightColor, verticalAlign, padding);
    }

    public boolean shadowOr(boolean fallback) {
        return shadow != null ? shadow : fallback;
    }

    public VerticalAlign verticalAlignOr(VerticalAlign fallback) {
        return verticalAlign != null ? verticalAlign : fallback;
    }

    public Insets paddingOr(Insets fallback) {
        return padding != null ? padding : fallback;
    }
}
