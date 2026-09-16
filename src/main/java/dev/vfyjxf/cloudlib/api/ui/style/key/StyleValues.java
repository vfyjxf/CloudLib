package dev.vfyjxf.cloudlib.api.ui.style.key;

import java.util.List;

/**
 * A group of {@link StyleValue}s — what shorthand factories like
 * {@code UIStyles.padding(4)} produce. Flattens into the four longhands when
 * collected.
 */
public record StyleValues(List<StyleValue<?>> values) implements StyleEntry {

    public static StyleValues of(StyleValue<?>... values) {
        return new StyleValues(List.of(values));
    }

    public static StyleValues of(List<StyleValue<?>> values) {
        return new StyleValues(List.copyOf(values));
    }

    @Override
    public void collectInto(StyleCollector out) {
        values.forEach(out::accept);
    }
}
