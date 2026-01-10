package dev.vfyjxf.cloudlib.api.ui.reactive.style;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * Built-in text style visual property for bold, italic, underline, strikethrough.
 * <p>
 * Text styles are applied to the {@link VisualContext} for rendering.
 *
 * @see Styles#bold()
 * @see Styles#italic()
 */
@ApiStatus.Experimental
public final class TextStyleProperty implements VisualProperty {

    public static final String NAME = "textStyle";

    private final boolean bold;
    private final boolean italic;
    private final boolean underline;
    private final boolean strikethrough;

    public TextStyleProperty(boolean bold, boolean italic, boolean underline, boolean strikethrough) {
        this.bold = bold;
        this.italic = italic;
        this.underline = underline;
        this.strikethrough = strikethrough;
    }

    public static TextStyleProperty bold() {
        return new TextStyleProperty(true, false, false, false);
    }

    public static TextStyleProperty italic() {
        return new TextStyleProperty(false, true, false, false);
    }

    public static TextStyleProperty underline() {
        return new TextStyleProperty(false, false, true, false);
    }

    public static TextStyleProperty strikethrough() {
        return new TextStyleProperty(false, false, false, true);
    }

    @Override
    public void applyToWidget(VisualContext context) {
        if (bold) context.setBold(true);
        if (italic) context.setItalic(true);
        if (underline) context.setUnderline(true);
        if (strikethrough) context.setStrikethrough(true);
    }

    @Override
    public String name() {
        return NAME;
    }

    public boolean isBold() { return bold; }
    public boolean isItalic() { return italic; }
    public boolean isUnderline() { return underline; }
    public boolean isStrikethrough() { return strikethrough; }

    @Override
    public String valueToString() {
        StringBuilder sb = new StringBuilder();
        if (bold) sb.append("bold ");
        if (italic) sb.append("italic ");
        if (underline) sb.append("underline ");
        if (strikethrough) sb.append("strikethrough ");
        return sb.toString().trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TextStyleProperty that)) return false;
        return bold == that.bold && italic == that.italic &&
               underline == that.underline && strikethrough == that.strikethrough;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bold, italic, underline, strikethrough);
    }
}
