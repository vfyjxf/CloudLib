package dev.vfyjxf.cloudlib.api.text;

import dev.vfyjxf.cloudlib.api.math.Insets;
import dev.vfyjxf.cloudlib.api.ui.style.StyleVar;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.jspecify.annotations.Nullable;

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
 * @param colorVar       the theme color slot this run's ink comes from; {@code null} = the
 *                       literal {@link #style} color. Resolved per frame by the renderer, see
 *                       {@link #textColor(ThemeColorResolver, int)}
 */
public record RichTextStyle(
    Style style,
    @Nullable Boolean shadow,
    @Nullable Integer highlightColor,
    @Nullable VerticalAlign verticalAlign,
    @Nullable Insets padding,
    @Nullable StyleVar<Integer> colorVar
) {

    public static final RichTextStyle empty = new RichTextStyle(Style.EMPTY, null, null, null, null, null);

    public static RichTextStyle of(Style style) {
        return new RichTextStyle(style, null, null, null, null, null);
    }

    /**
     * Merges {@code other} over this style: non-null fields of {@code other} win,
     * vanilla styles follow {@link Style#applyTo} semantics.
     */
    public RichTextStyle merge(RichTextStyle other) {
        if (other == empty) return this;
        if (this == empty) return other;
        return new RichTextStyle(
            this.style.applyTo(other.style),
            other.shadow != null ? other.shadow : this.shadow,
            other.highlightColor != null ? other.highlightColor : this.highlightColor,
            other.verticalAlign != null ? other.verticalAlign : this.verticalAlign,
            other.padding != null ? other.padding : this.padding,
            other.colorVar != null ? other.colorVar : this.colorVar
        );
    }

    public boolean isEmpty() {
        return this == empty
                || (style == Style.EMPTY
                        && shadow == null
                        && highlightColor == null
                        && verticalAlign == null
                        && padding == null
                        && colorVar == null);
    }

    /**
     * The ink color of a run of text under the active theme.
     * <p>
     * Resolution order:
     * <ol>
     *   <li>a bound {@link #colorVar} the resolver resolves — that value wins, alpha
     *       included (a slot bound to {@code transparent} paints nothing);</li>
     *   <li>a bound slot that does <em>not</em> resolve (unset in the active theme, or
     *       not a color) — the literal {@link #style} color, or transparent when the
     *       run carries none, so a document never silently paints in the fallback
     *       color where it asked for a themed one;</li>
     *   <li>no bound slot — the literal color, else {@code fallback}.</li>
     * </ol>
     *
     * @param theme    the active theme resolution, {@code null} outside a themed context
     * @param fallback the renderer's default ink color
     */
    public int textColor(@Nullable ThemeColorResolver theme, int fallback) {
        TextColor literal = style.getColor();
        if (colorVar == null) {
            return literal != null ? literal.getValue() | 0xFF000000 : fallback;
        }
        Integer resolved = theme != null ? theme.resolve(colorVar) : null;
        if (resolved != null) {
            return resolved;
        }
        return literal != null ? literal.getValue() | 0xFF000000 : 0x00000000;
    }

    public RichTextStyle withStyle(Style style) {
        return new RichTextStyle(style, shadow, highlightColor, verticalAlign, padding, colorVar);
    }

    public RichTextStyle withShadow(boolean shadow) {
        return new RichTextStyle(style, shadow, highlightColor, verticalAlign, padding, colorVar);
    }

    public RichTextStyle withHighlightColor(int argb) {
        return new RichTextStyle(style, shadow, argb, verticalAlign, padding, colorVar);
    }

    public RichTextStyle withVerticalAlign(VerticalAlign align) {
        return new RichTextStyle(style, shadow, highlightColor, align, padding, colorVar);
    }

    public RichTextStyle withPadding(Insets padding) {
        return new RichTextStyle(style, shadow, highlightColor, verticalAlign, padding, colorVar);
    }

    /**
     * Points this run's ink at a theme color slot, leaving any literal color in
     * place as the fallback of an unset slot.
     */
    public RichTextStyle withColorVar(@Nullable StyleVar<Integer> colorVar) {
        return new RichTextStyle(style, shadow, highlightColor, verticalAlign, padding, colorVar);
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
