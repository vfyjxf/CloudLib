package dev.vfyjxf.cloudlib.api.ui.base;

import java.util.ArrayList;
import java.util.List;

/**
 * DSL scope for building UI hierarchies declaratively.
 * <p>
 * Uses ThreadLocal to track the current scope, allowing nested
 * builder blocks similar to SwiftUI's ViewBuilder.
 * <p>
 * Example:
 * <pre>{@code
 * VStack(() -> {
 *     Text("Hello");
 *     HStack(() -> {
 *         Text("World");
 *         Button("Click", () -> System.out.println("clicked"));
 *     });
 * });
 * }</pre>
 */
public final class ScopedReceiver {

    private static final ThreadLocal<ScopedReceiver> CURRENT = new ThreadLocal<>();

    private final List<Blueprint<?>> blueprints = new ArrayList<>();

    private ScopedReceiver() {}

    /**
     * Adds a blueprint to the current scope.
     *
     * @param blueprint the blueprint to add
     * @param <T>       the blueprint type
     * @return the same blueprint for chaining
     */
    public static <T extends Blueprint<?>> T add(T blueprint) {
        ScopedReceiver scope = CURRENT.get();
        if (scope != null) {
            scope.blueprints.add(blueprint);
        }
        return blueprint;
    }

    /**
     * Builds children within a new scope.
     *
     * @param block the builder block
     * @return list of blueprints created in the block
     */
    public static List<Blueprint<?>> buildChildren(Runnable block) {
        ScopedReceiver parent = CURRENT.get();
        ScopedReceiver newScope = new ScopedReceiver();
        CURRENT.set(newScope);

        try {
            block.run();
            return List.copyOf(newScope.blueprints);
        } finally {
            CURRENT.set(parent);
        }
    }

    /**
     * Checks if currently inside a DSL scope.
     *
     * @return true if in scope
     */
    public static boolean inScope() {
        return CURRENT.get() != null;
    }
}
