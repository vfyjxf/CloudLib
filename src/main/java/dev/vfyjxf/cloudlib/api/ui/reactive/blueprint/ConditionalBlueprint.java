package dev.vfyjxf.cloudlib.api.ui.reactive.blueprint;

import dev.vfyjxf.cloudlib.api.ui.reactive.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * A blueprint that conditionally renders one of two branches.
 * <p>
 * Example:
 * <pre>{@code
 * Signal<Boolean> isLoggedIn = Signal.of(false);
 * 
 * If(isLoggedIn::get, 
 *     () -> Text("Welcome back!"),
 *     () -> Text("Please log in")
 * );
 * }</pre>
 */
@ApiStatus.Experimental
public class ConditionalBlueprint implements CompositeBlueprint {

    private final Supplier<Boolean> condition;
    private final Runnable thenBuilder;
    @Nullable
    private final Runnable elseBuilder;
    @Nullable
    private final Key key;

    private ConditionalBlueprint(
            Supplier<Boolean> condition,
            Runnable thenBuilder,
            @Nullable Runnable elseBuilder,
            @Nullable Key key
    ) {
        this.condition = condition;
        this.thenBuilder = thenBuilder;
        this.elseBuilder = elseBuilder;
        this.key = key;
    }

    @Override
    public List<Blueprint> getChildren() {
        // Evaluate condition at build time
        if (condition.get()) {
            return ScopedReceiver.buildScope(thenBuilder);
        } else if (elseBuilder != null) {
            return ScopedReceiver.buildScope(elseBuilder);
        }
        return Collections.emptyList();
    }

    @Override
    @Nullable
    public Key key() {
        return key;
    }

    @Override
    public UIElement<?> createElement() {
        return new ConditionalElement(this);
    }

    /**
     * Gets the condition supplier.
     */
    public Supplier<Boolean> getCondition() {
        return condition;
    }

    /**
     * Gets the 'then' branch builder.
     */
    public Runnable getThenBuilder() {
        return thenBuilder;
    }

    /**
     * Gets the 'else' branch builder.
     */
    @Nullable
    public Runnable getElseBuilder() {
        return elseBuilder;
    }

    // ========== Static DSL Methods ==========

    /**
     * Creates a conditional blueprint with only a 'then' branch.
     *
     * @param condition   the condition to evaluate
     * @param thenBuilder the builder for when condition is true
     * @return the created blueprint
     */
    public static ConditionalBlueprint If(Supplier<Boolean> condition, Runnable thenBuilder) {
        ConditionalBlueprint blueprint = new ConditionalBlueprint(condition, thenBuilder, null, null);
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * Creates a conditional blueprint with 'then' and 'else' branches.
     *
     * @param condition   the condition to evaluate
     * @param thenBuilder the builder for when condition is true
     * @param elseBuilder the builder for when condition is false
     * @return the created blueprint
     */
    public static ConditionalBlueprint If(
            Supplier<Boolean> condition,
            Runnable thenBuilder,
            Runnable elseBuilder
    ) {
        ConditionalBlueprint blueprint = new ConditionalBlueprint(condition, thenBuilder, elseBuilder, null);
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * Creates a conditional blueprint with a key.
     *
     * @param key         the key
     * @param condition   the condition to evaluate
     * @param thenBuilder the builder for when condition is true
     * @param elseBuilder the builder for when condition is false
     * @return the created blueprint
     */
    public static ConditionalBlueprint If(
            Key key,
            Supplier<Boolean> condition,
            Runnable thenBuilder,
            @Nullable Runnable elseBuilder
    ) {
        ConditionalBlueprint blueprint = new ConditionalBlueprint(condition, thenBuilder, elseBuilder, key);
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * Element for conditional blueprints.
     */
    private static class ConditionalElement extends CompositeElement<ConditionalBlueprint> {

        public ConditionalElement(ConditionalBlueprint blueprint) {
            super(blueprint);
        }

        @Override
        protected void build() {
            // Re-evaluate condition and rebuild children
            List<Blueprint> newChildren = blueprint.getChildren();
            reconcileChildren(newChildren);
        }
    }
}
