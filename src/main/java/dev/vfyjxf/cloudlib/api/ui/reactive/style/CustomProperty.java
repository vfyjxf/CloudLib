package dev.vfyjxf.cloudlib.api.ui.reactive.style;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * A custom style property that allows arbitrary styling via a lambda.
 * <p>
 * Use this for quick one-off styles or when you don't want to create
 * a full StyleProperty implementation.
 * <p>
 * Example:
 * <pre>{@code
 * var style = Style.of(
 *     padding(10),
 *     custom("shadow", ctx -> {
 *         ctx.setCustom("shadowOffsetX", 2);
 *         ctx.setCustom("shadowOffsetY", 2);
 *         ctx.setCustom("shadowBlur", 4);
 *         ctx.setCustom("shadowColor", 0x80000000);
 *     })
 * );
 * }</pre>
 *
 * @see Styles#custom(String, Consumer)
 */
@ApiStatus.Experimental
public final class CustomProperty implements StyleProperty {

    private final String propertyName;
    private final Consumer<StyleContext> applier;

    public CustomProperty(String name, Consumer<StyleContext> applier) {
        this.propertyName = Objects.requireNonNull(name, "name");
        this.applier = Objects.requireNonNull(applier, "applier");
    }

    @Override
    public void apply(StyleContext context) {
        applier.accept(context);
    }

    @Override
    public String name() {
        return propertyName;
    }

    @Override
    public String valueToString() {
        return "<custom>";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomProperty that)) return false;
        return propertyName.equals(that.propertyName) && applier.equals(that.applier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(propertyName, applier);
    }
}
