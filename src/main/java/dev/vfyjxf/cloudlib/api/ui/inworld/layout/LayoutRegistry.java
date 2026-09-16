package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Registration point for {@link Layout} implementations: layouts are looked
 * up by the <em>type</em> of the layout intent they resolve, and the factory
 * binds the intent instance to the layout it returns.
 */
public final class LayoutRegistry {

    private final Map<Class<?>, Function<?, ? extends Layout<? extends LayoutContext>>> factories =
            new LinkedHashMap<>();

    /** A fresh, empty registry. */
    public static LayoutRegistry create() {
        return new LayoutRegistry();
    }

    private LayoutRegistry() {}

    /**
     * Registers {@code factory} as the layout source for intents of exactly
     * {@code intentType}; a later registration for the same type overrides the
     * earlier one.
     */
    public <I> LayoutRegistry register(
            Class<I> intentType, Function<? super I, ? extends Layout<? extends LayoutContext>> factory) {
        factories.put(intentType, factory);
        return this;
    }

    /**
     * The layout bound to {@code intent}, or null when its exact type is not
     * registered.
     */
    @SuppressWarnings("unchecked")
    public @Nullable Layout<? extends LayoutContext> layoutFor(Object intent) {
        Function<Object, ? extends Layout<? extends LayoutContext>> factory =
                (Function<Object, ? extends Layout<? extends LayoutContext>>) factories.get(intent.getClass());
        return factory == null ? null : factory.apply(intent);
    }
}
