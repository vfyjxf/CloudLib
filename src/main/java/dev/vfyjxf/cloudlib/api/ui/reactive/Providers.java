package dev.vfyjxf.cloudlib.api.ui.reactive;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Dependency injection system for the reactive UI.
 * <p>
 * Providers are <b>separate from the UI tree</b>. Unlike Flutter's InheritedWidget,
 * providers form their own hierarchy and can be accessed from anywhere without
 * threading through widget constructors.
 * <p>
 * Example:
 * <pre>{@code
 * // Define keys
 * static final Key<Theme> THEME = Providers.key("theme");
 * static final Key<UserService> USER_SERVICE = Providers.key("userService");
 * 
 * // Create a provider scope
 * var scope = Providers.builder()
 *     .provide(THEME, Theme.dark())
 *     .provide(USER_SERVICE, new UserServiceImpl())
 *     .build();
 * 
 * // Access from any component
 * Component.stateful(ctx -> {
 *     Theme theme = ctx.provide(THEME);
 *     UserService users = ctx.provide(USER_SERVICE);
 *     // ...
 * });
 * }</pre>
 */
public final class Providers {

    private static final ThreadLocal<Scope> CURRENT_SCOPE = new ThreadLocal<>();

    private Providers() {}

    /**
     * Creates a typed key for provider lookup.
     *
     * @param name the key name
     * @param <T>  the value type
     * @return a new key
     */
    public static <T> Key<T> key(String name) {
        return new Key<>(name);
    }

    /**
     * Creates a new scope builder.
     *
     * @return a builder
     */
    public static Builder builder() {
        return new Builder(null);
    }

    /**
     * Gets a value from the current scope.
     *
     * @param key the key
     * @param <T> the value type
     * @return the value
     * @throws NoSuchElementException if not found
     */
    public static <T> T get(Key<T> key) {
        Scope scope = CURRENT_SCOPE.get();
        if (scope == null) {
            throw new NoSuchElementException("No active provider scope for key: " + key.name);
        }
        return scope.get(key);
    }

    /**
     * Tries to get a value from the current scope.
     *
     * @param key the key
     * @param <T> the value type
     * @return optional containing the value if found
     */
    public static <T> Optional<T> tryGet(Key<T> key) {
        Scope scope = CURRENT_SCOPE.get();
        if (scope == null) {
            return Optional.empty();
        }
        return scope.tryGet(key);
    }

    /**
     * Gets the current scope.
     *
     * @return current scope or null
     */
    @Nullable
    public static Scope currentScope() {
        return CURRENT_SCOPE.get();
    }

    /**
     * Typed key for provider lookup.
     *
     * @param <T> the value type
     */
    public static final class Key<T> {
        private final String name;

        private Key(String name) {
            this.name = name;
        }

        public String name() {
            return name;
        }

        @Override
        public String toString() {
            return "Key[" + name + "]";
        }
    }

    /**
     * Provider scope that holds values.
     */
    public static final class Scope {
        @Nullable
        private final Scope parent;
        private final Map<Key<?>, Object> instances = new HashMap<>();
        private final Map<Key<?>, Supplier<?>> factories = new HashMap<>();

        private Scope(@Nullable Scope parent) {
            this.parent = parent;
        }

        /**
         * Gets a value by key.
         *
         * @param key the key
         * @param <T> the value type
         * @return the value
         * @throws NoSuchElementException if not found
         */
        @SuppressWarnings("unchecked")
        public <T> T get(Key<T> key) {
            Object instance = instances.get(key);
            if (instance != null) {
                return (T) instance;
            }
            Supplier<?> factory = factories.get(key);
            if (factory != null) {
                T value = (T) factory.get();
                instances.put(key, value);
                return value;
            }
            if (parent != null) {
                return parent.get(key);
            }
            throw new NoSuchElementException("No provider for key: " + key.name);
        }

        /**
         * Tries to get a value by key.
         *
         * @param key the key
         * @param <T> the value type
         * @return optional containing the value if found
         */
        public <T> Optional<T> tryGet(Key<T> key) {
            try {
                return Optional.of(get(key));
            } catch (NoSuchElementException e) {
                return Optional.empty();
            }
        }

        /**
         * Creates a child builder.
         *
         * @return a builder with this as parent
         */
        public Builder child() {
            return new Builder(this);
        }

        /**
         * Enters this scope (makes it current).
         */
        public void enter() {
            CURRENT_SCOPE.set(this);
        }

        /**
         * Exits this scope (restores parent as current).
         */
        public void exit() {
            CURRENT_SCOPE.set(parent);
        }

        /**
         * Runs a supplier within this scope.
         *
         * @param action the supplier
         * @param <T>    the return type
         * @return the result
         */
        public <T> T runWith(Supplier<T> action) {
            enter();
            try {
                return action.get();
            } finally {
                exit();
            }
        }

        /**
         * Runs an action within this scope.
         *
         * @param action the action
         */
        public void runWith(Runnable action) {
            runWith(() -> {
                action.run();
                return null;
            });
        }
    }

    /**
     * Builder for provider scopes.
     */
    public static final class Builder {
        @Nullable
        private final Scope parent;
        private final Map<Key<?>, Object> instances = new HashMap<>();
        private final Map<Key<?>, Supplier<?>> factories = new HashMap<>();

        private Builder(@Nullable Scope parent) {
            this.parent = parent;
        }

        /**
         * Provides a value for a key.
         *
         * @param key   the key
         * @param value the value
         * @param <T>   the value type
         * @return this builder
         */
        public <T> Builder provide(Key<T> key, T value) {
            instances.put(key, value);
            return this;
        }

        /**
         * Provides a lazy factory for a key.
         *
         * @param key     the key
         * @param factory the factory
         * @param <T>     the value type
         * @return this builder
         */
        public <T> Builder provideLazy(Key<T> key, Supplier<T> factory) {
            factories.put(key, factory);
            return this;
        }

        /**
         * Builds the scope.
         *
         * @return the new scope
         */
        public Scope build() {
            Scope scope = new Scope(parent);
            scope.instances.putAll(instances);
            scope.factories.putAll(factories);
            return scope;
        }
    }
}
