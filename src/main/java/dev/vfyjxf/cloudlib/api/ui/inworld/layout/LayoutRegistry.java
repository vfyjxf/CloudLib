package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Registration point for {@link Layout} implementations: layouts are looked
 * up by the <em>type</em> of the layout intent they resolve, and the factory
 * binds the intent instance to the layout it returns.
 * <p>
 * Registries compose by chaining — a registry created with a
 * {@linkplain #create(LayoutRegistry) parent} falls through to it when an
 * intent type isn't registered locally, so a semantic layer can overlay its
 * own layouts on a shared base registry.
 */
public final class LayoutRegistry {

    private final @Nullable LayoutRegistry parent;
    private final Map<Class<?>, Function<?, ? extends Layout<? extends LayoutContext>>> factories =
            new LinkedHashMap<>();

    private LayoutRegistry(@Nullable LayoutRegistry parent) {
        this.parent = parent;
    }

    /** A fresh, empty registry. */
    public static LayoutRegistry create() {
        return new LayoutRegistry(null);
    }

    /** A fresh registry whose misses fall through to {@code parent}. */
    public static LayoutRegistry create(LayoutRegistry parent) {
        return new LayoutRegistry(parent);
    }

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
     * The layout bound to {@code intent}, or null when its exact type is
     * registered neither here nor in any parent.
     */
    @SuppressWarnings("unchecked")
    public @Nullable Layout<? extends LayoutContext> layoutFor(Object intent) {
        Function<Object, ? extends Layout<? extends LayoutContext>> factory =
                (Function<Object, ? extends Layout<? extends LayoutContext>>) factories.get(intent.getClass());
        if (factory != null) return factory.apply(intent);
        return parent == null ? null : parent.layoutFor(intent);
    }
}
