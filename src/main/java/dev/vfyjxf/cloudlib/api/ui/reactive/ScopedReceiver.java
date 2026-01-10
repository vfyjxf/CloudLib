package dev.vfyjxf.cloudlib.api.ui.reactive;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Scoped receiver for collecting child blueprints in DSL-style builders.
 * <p>
 * This enables a React/Compose-like DSL where child widgets are declared
 * within a lambda scope rather than passed as explicit arguments.
 * <p>
 * Example usage:
 * <pre>{@code
 * var column = Column(() -> {
 *     Text("Hello");
 *     Text("World");
 *     Row(() -> {
 *         Button("Click me");
 *     });
 * });
 * }</pre>
 * <p>
 * The mechanism works by:
 * <ol>
 *   <li>Setting a thread-local receiver before executing the builder lambda</li>
 *   <li>Child blueprint factory methods add themselves to the current receiver</li>
 *   <li>After the lambda completes, collecting all added children</li>
 * </ol>
 */
@ApiStatus.Experimental
public final class ScopedReceiver {

    /**
     * Thread-local storage for the current scope's receiver.
     */
    private static final ThreadLocal<ScopedReceiver> CURRENT = new ThreadLocal<>();

    /**
     * The collected child blueprints.
     */
    private final List<Blueprint> children = new ArrayList<>();

    /**
     * Attributes for this scope (optional).
     */
    @Nullable
    private final Map<String, Object> attributes;

    private ScopedReceiver(@Nullable Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    /**
     * Adds a blueprint to the current scope.
     * <p>
     * This is called by blueprint factory methods to register themselves
     * with the current parent scope.
     *
     * @param blueprint the blueprint to add
     */
    public static void addToScope(Blueprint blueprint) {
        ScopedReceiver receiver = CURRENT.get();
        if (receiver != null) {
            receiver.children.add(blueprint);
        }
    }

    /**
     * Checks if there's an active scope.
     *
     * @return true if inside a build scope
     */
    public static boolean hasScope() {
        return CURRENT.get() != null;
    }

    /**
     * Gets the current receiver if one exists.
     *
     * @return the current receiver, or null
     */
    @Nullable
    public static ScopedReceiver current() {
        return CURRENT.get();
    }

    /**
     * Executes a builder block and collects all child blueprints.
     *
     * @param builder the builder lambda that declares children
     * @return the list of collected child blueprints
     */
    public static List<Blueprint> buildScope(Runnable builder) {
        return buildScope(null, builder);
    }

    /**
     * Executes a builder block with attributes and collects all child blueprints.
     *
     * @param attributes optional attributes for this scope
     * @param builder    the builder lambda that declares children
     * @return the list of collected child blueprints
     */
    public static List<Blueprint> buildScope(@Nullable Map<String, Object> attributes, Runnable builder) {
        ScopedReceiver parent = CURRENT.get();
        ScopedReceiver newReceiver = new ScopedReceiver(attributes);
        CURRENT.set(newReceiver);

        try {
            builder.run();
        } finally {
            CURRENT.set(parent);
        }

        return newReceiver.children;
    }

    /**
     * Builds a scope and wraps the result with a factory function.
     *
     * @param builder the builder lambda
     * @param factory the factory to create the result
     * @param <T>     the result type
     * @return the created result
     */
    public static <T extends Blueprint> T buildAndWrap(Runnable builder, ScopeFactory<T> factory) {
        List<Blueprint> children = buildScope(builder);
        T result = factory.create(children);
        addToScope(result);
        return result;
    }

    /**
     * Builds a scope with attributes and wraps the result.
     *
     * @param attributes the attributes
     * @param builder    the builder lambda
     * @param factory    the factory to create the result
     * @param <T>        the result type
     * @return the created result
     */
    public static <T extends Blueprint> T buildAndWrap(
            @Nullable Map<String, Object> attributes,
            Runnable builder,
            ScopeFactoryWithAttributes<T> factory
    ) {
        List<Blueprint> children = buildScope(attributes, builder);
        T result = factory.create(children, attributes);
        addToScope(result);
        return result;
    }

    /**
     * Gets the collected children.
     *
     * @return the children list
     */
    public List<Blueprint> getChildren() {
        return children;
    }

    /**
     * Gets the attributes for this scope.
     *
     * @return the attributes, or null
     */
    @Nullable
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * Factory interface for creating blueprints from children.
     *
     * @param <T> the blueprint type
     */
    @FunctionalInterface
    public interface ScopeFactory<T extends Blueprint> {
        T create(List<Blueprint> children);
    }

    /**
     * Factory interface for creating blueprints from children with attributes.
     *
     * @param <T> the blueprint type
     */
    @FunctionalInterface
    public interface ScopeFactoryWithAttributes<T extends Blueprint> {
        T create(List<Blueprint> children, @Nullable Map<String, Object> attributes);
    }
}
