package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TextAlign;

import java.util.Objects;

/**
 * Layout property that maps to taffy {@link TaffyStyle#textAlign}.
 * <p>
 * Controls text alignment within text content.
 * <p>
 * The text align values supported by taffy include:
 * <ul>
 *   <li>{@link TextAlign#AUTO} - automatic based on direction</li>
 *   <li>{@link TextAlign#LEFT} - align to left</li>
 *   <li>{@link TextAlign#RIGHT} - align to right</li>
 *   <li>{@link TextAlign#CENTER} - center align</li>
 *   <li>{@link TextAlign#START} - align to logical start</li>
 *   <li>{@link TextAlign#END} - align to logical end</li>
 *   <li>{@link TextAlign#JUSTIFY} - justify text</li>
 *   <li>{@link TextAlign#JUSTIFY_ALL} - justify all lines</li>
 *   <li>{@link TextAlign#MATCH_PARENT} - match parent alignment</li>
 * </ul>
 *
 * @see TaffyStyle#textAlign
 * @see TextAlign
 */
public record TextAlignProperty(TextAlign textAlign) implements LayoutProperty {

    public static final StyleType<TextAlign> type = StyleType.of("text-align", () -> TextAlign.AUTO);

    public TextAlignProperty(TextAlign textAlign) {
        this.textAlign = Objects.requireNonNull(textAlign, "textAlign");
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.textAlign = textAlign;
    }

    @Override
    public String toString() {
        return textAlign.toString();
    }
}
