package dev.vfyjxf.cloudlib.api.ui.reactive;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Key system for identifying components across rebuilds, similar to Flutter's Key system.
 * 
 * <p>Keys serve two main purposes:</p>
 * <ul>
 *   <li>Preserve state when widgets move in the tree</li>
 *   <li>Optimize diffing by providing stable identity</li>
 * </ul>
 * 
 * <h2>Key Types</h2>
 * 
 * <h3>{@link ValueKey} - Identity by value</h3>
 * <pre>{@code
 * // Two keys with same value are equal
 * Key.of("user-123")  // equals Key.of("user-123")
 * Key.of(item.id())   // Use item's ID as key
 * }</pre>
 * 
 * <h3>{@link ObjectKey} - Identity by object reference</h3>
 * <pre>{@code
 * // Two keys are equal only if they wrap the same object instance
 * Key.object(myObject)  // Uses == comparison
 * }</pre>
 * 
 * <h3>{@link UniqueKey} - Always unique</h3>
 * <pre>{@code
 * // Every call creates a new unique key
 * Key.unique()  // Never equals any other key
 * }</pre>
 * 
 * <h3>{@link CompositeKey} - Combine multiple values</h3>
 * <pre>{@code
 * // Useful for nested structures
 * Key.of("list", index)           // "list" + index
 * Key.of("section", "row", col)   // Multiple parts
 * }</pre>
 * 
 * <h3>{@link AutoKey} - Auto-incrementing within scope</h3>
 * <pre>{@code
 * // Automatically generates sequential keys
 * var scope = Key.scope("buttons");
 * scope.next()  // "buttons/0"
 * scope.next()  // "buttons/1"
 * }</pre>
 * 
 * <h2>Usage in Components</h2>
 * <pre>{@code
 * // Instead of manual string keys:
 * Embed("widgets.popup", PopupDemo.PopupHolder(...));
 * 
 * // Use typed keys:
 * Embed(Key.of("widgets", "popup"), PopupDemo.PopupHolder(...));
 * 
 * // For lists:
 * forEach(items, (item, i) -> {
 *     Embed(Key.of(item.id()), ItemComponent(item));
 * });
 * }</pre>
 */
public sealed interface Key permits Key.ValueKey, Key.ObjectKey, Key.UniqueKey, Key.CompositeKey, Key.AutoKey {
    
    // ===== Current Scope (ThreadLocal for auto key generation) =====
    
    /**
     * ThreadLocal holding the current key scope for auto-generation.
     */
    ThreadLocal<KeyScope> CURRENT_SCOPE = new ThreadLocal<>();
    
    /**
     * Gets the current scope, or creates a root scope if none exists.
     * 
     * @return the current scope
     */
    static KeyScope currentScope() {
        KeyScope scope = CURRENT_SCOPE.get();
        if (scope == null) {
            scope = new KeyScope("");  // Empty root path
            CURRENT_SCOPE.set(scope);
        }
        return scope;
    }
    
    /**
     * Executes a block within a new child scope.
     * <p>
     * All {@link #auto()} calls within the block will generate keys relative to this scope.
     * 
     * <pre>{@code
     * Key.withScope("mySection", () -> {
     *     Key.auto()  // mySection/0
     *     Key.auto()  // mySection/1
     *     Key.withScope("nested", () -> {
     *         Key.auto()  // mySection/nested/0
     *     });
     * });
     * }</pre>
     * 
     * @param name  the scope name
     * @param block the block to execute
     */
    static void withScope(String name, Runnable block) {
        KeyScope parent = currentScope();
        KeyScope child = parent.child(name);
        CURRENT_SCOPE.set(child);
        try {
            block.run();
        } finally {
            CURRENT_SCOPE.set(parent);
        }
    }
    
    /**
     * Executes a block within a new child scope and returns a result.
     * 
     * @param name     the scope name
     * @param supplier the supplier to execute
     * @param <T>      the result type
     * @return the result
     */
    static <T> T withScope(String name, java.util.function.Supplier<T> supplier) {
        KeyScope parent = currentScope();
        KeyScope child = parent.child(name);
        CURRENT_SCOPE.set(child);
        try {
            return supplier.get();
        } finally {
            CURRENT_SCOPE.set(parent);
        }
    }
    
    /**
     * Creates an auto-generated key based on the current scope.
     * <p>
     * Keys are generated sequentially within each scope. The scope is determined
     * by the current {@link #withScope} context.
     * 
     * <pre>{@code
     * Key.withScope("buttons", () -> {
     *     Embed(Key.auto(), ButtonA());  // buttons/0
     *     Embed(Key.auto(), ButtonB());  // buttons/1
     * });
     * }</pre>
     * 
     * @return an auto-generated key
     */
    static Key auto() {
        return currentScope().next();
    }
    
    /**
     * Creates an auto-generated key with a hint.
     * <p>
     * The hint is combined with the auto-generated index to create a more descriptive key.
     * 
     * @param hint a descriptive hint
     * @return an auto-generated key with hint
     */
    static Key auto(String hint) {
        KeyScope scope = currentScope();
        return new AutoKey(scope.getPath(), scope.nextIndex(), hint);
    }
    
    /**
     * Resets the current scope counters.
     * <p>
     * Call this at the start of each render cycle.
     */
    static void resetScope() {
        KeyScope scope = CURRENT_SCOPE.get();
        if (scope != null) {
            scope.resetAll();
        }
    }
    
    // ===== Factory Methods =====
    
    /**
     * Creates a key from a value.
     * <p>
     * Two keys are equal if their values are equal (using {@code equals()}).
     * 
     * @param value the value
     * @return a value key
     */
    static Key of(Object value) {
        return new ValueKey<>(value);
    }
    
    /**
     * Creates a composite key from multiple values.
     * <p>
     * Useful for nested or hierarchical keys.
     * 
     * @param first  the first part
     * @param second the second part
     * @return a composite key
     */
    static Key of(Object first, Object second) {
        return new CompositeKey(new Object[]{first, second});
    }
    
    /**
     * Creates a composite key from multiple values.
     * 
     * @param first  the first part
     * @param second the second part
     * @param third  the third part
     * @return a composite key
     */
    static Key of(Object first, Object second, Object third) {
        return new CompositeKey(new Object[]{first, second, third});
    }
    
    /**
     * Creates a composite key from multiple values.
     * 
     * @param parts the key parts
     * @return a composite key
     */
    static Key of(Object... parts) {
        if (parts.length == 0) {
            throw new IllegalArgumentException("Key must have at least one part");
        }
        if (parts.length == 1) {
            return new ValueKey<>(parts[0]);
        }
        return new CompositeKey(parts.clone());
    }
    
    /**
     * Creates an object key using reference equality.
     * <p>
     * Two keys are equal only if they wrap the exact same object instance.
     * 
     * @param object the object
     * @return an object key
     */
    static Key object(Object object) {
        return new ObjectKey(object);
    }
    
    /**
     * Creates a unique key.
     * <p>
     * Each call returns a new key that is never equal to any other key.
     * 
     * @return a unique key
     */
    static Key unique() {
        return new UniqueKey();
    }
    
    /**
     * Creates a key scope for auto-generating sequential keys.
     * <p>
     * Useful when you have multiple similar components and want automatic numbering.
     * 
     * @param prefix the prefix for generated keys
     * @return a key scope
     */
    static KeyScope scope(String prefix) {
        return new KeyScope(prefix);
    }
    
    /**
     * Creates a child scope under the current scope.
     * <p>
     * This is equivalent to {@code currentScope().child(name)}.
     * 
     * @param name the child scope name
     * @return a child key scope
     */
    static KeyScope childScope(String name) {
        return currentScope().child(name);
    }
    
    // ===== Key Implementations =====
    
    /**
     * A key based on value equality.
     */
    record ValueKey<T>(T value) implements Key {
        @Override
        public String toString() {
            return "Key(" + value + ")";
        }
    }
    
    /**
     * A key based on object reference equality.
     */
    record ObjectKey(Object object) implements Key {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ObjectKey that)) return false;
            return this.object == that.object; // Reference equality!
        }
        
        @Override
        public int hashCode() {
            return System.identityHashCode(object);
        }
        
        @Override
        public String toString() {
            return "ObjectKey@" + Integer.toHexString(System.identityHashCode(object));
        }
    }
    
    /**
     * A globally unique key.
     */
    final class UniqueKey implements Key {
        private static final AtomicLong counter = new AtomicLong(0);
        private final long id = counter.incrementAndGet();
        
        @Override
        public boolean equals(Object o) {
            return this == o; // Only equal to itself
        }
        
        @Override
        public int hashCode() {
            return Long.hashCode(id);
        }
        
        @Override
        public String toString() {
            return "UniqueKey#" + id;
        }
    }
    
    /**
     * A key composed of multiple parts.
     */
    record CompositeKey(Object[] parts) implements Key {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CompositeKey that)) return false;
            if (parts.length != that.parts.length) return false;
            for (int i = 0; i < parts.length; i++) {
                if (!Objects.equals(parts[i], that.parts[i])) {
                    return false;
                }
            }
            return true;
        }
        
        @Override
        public int hashCode() {
            int result = 1;
            for (Object part : parts) {
                result = 31 * result + Objects.hashCode(part);
            }
            return result;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("Key(");
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) sb.append("/");
                sb.append(parts[i]);
            }
            return sb.append(")").toString();
        }
    }
    
    /**
     * An auto-generated key based on scope path and index.
     * <p>
     * AutoKey is created by {@link Key#auto()} and derives its identity from:
     * <ul>
     *   <li>The scope path (determined by nested {@link Key#withScope} calls)</li>
     *   <li>The index within that scope</li>
     *   <li>An optional hint for debugging</li>
     * </ul>
     */
    record AutoKey(String scopePath, int index, @Nullable String hint) implements Key {
        public AutoKey(String scopePath, int index) {
            this(scopePath, index, null);
        }
        
        @Override
        public String toString() {
            if (hint != null) {
                return "AutoKey(" + scopePath + "/" + index + ":" + hint + ")";
            }
            return "AutoKey(" + scopePath + "/" + index + ")";
        }
    }
    
    // ===== Key Scope for Auto-generation =====
    
    /**
     * A scope for generating sequential keys.
     * <p>
     * KeyScope tracks a hierarchical path and generates sequential keys within that path.
     * Scopes can be nested to create hierarchical key structures.
     * 
     * <h3>Manual Usage:</h3>
     * <pre>{@code
     * var scope = Key.scope("buttons");
     * Button("A", ..., scope.next());  // key = AutoKey(buttons/0)
     * Button("B", ..., scope.next());  // key = AutoKey(buttons/1)
     * 
     * var childScope = scope.child("special");
     * Button("C", ..., childScope.next());  // key = AutoKey(buttons/special/0)
     * }</pre>
     * 
     * <h3>Automatic Usage with withScope:</h3>
     * <pre>{@code
     * Key.withScope("buttons", () -> {
     *     Embed(Key.auto(), ButtonA());  // AutoKey(buttons/0)
     *     Embed(Key.auto(), ButtonB());  // AutoKey(buttons/1)
     *     
     *     Key.withScope("special", () -> {
     *         Embed(Key.auto(), ButtonC());  // AutoKey(buttons/special/0)
     *     });
     * });
     * }</pre>
     * 
     * <p><b>Warning:</b> Scope-generated keys rely on call order. If components
     * are conditionally rendered or reordered, use explicit keys instead.</p>
     */
    final class KeyScope {
        private final String path;
        private final @Nullable KeyScope parent;
        private int counter = 0;
        private final java.util.Map<String, KeyScope> children = new java.util.HashMap<>();
        
        /**
         * Creates a root scope with the given name.
         */
        public KeyScope(String name) {
            this.path = name;
            this.parent = null;
        }
        
        /**
         * Creates a child scope.
         */
        private KeyScope(String path, KeyScope parent) {
            this.path = path;
            this.parent = parent;
        }
        
        /**
         * Gets the full path of this scope.
         * 
         * @return the path
         */
        public String getPath() {
            return path;
        }
        
        /**
         * Gets the parent scope, if any.
         * 
         * @return the parent scope, or null if this is a root scope
         */
        public @Nullable KeyScope getParent() {
            return parent;
        }
        
        /**
         * Gets the next index without incrementing.
         * 
         * @return the current counter value
         */
        public int current() {
            return counter;
        }
        
        /**
         * Gets the next index and increments the counter.
         * 
         * @return the next index
         */
        public int nextIndex() {
            return counter++;
        }
        
        /**
         * Gets the next auto-generated key in sequence.
         * 
         * @return the next key
         */
        public Key next() {
            return new AutoKey(path, counter++);
        }
        
        /**
         * Gets an auto-generated key with a hint.
         * 
         * @param hint a descriptive hint
         * @return the next key with hint
         */
        public Key next(String hint) {
            return new AutoKey(path, counter++, hint);
        }
        
        /**
         * Gets a key with a specific suffix (does not increment counter).
         * 
         * @param suffix the suffix
         * @return a key with the suffix
         */
        public Key with(Object suffix) {
            return Key.of(path, suffix);
        }
        
        /**
         * Gets or creates a child scope.
         * <p>
         * Child scopes are cached, so calling {@code child("foo")} twice
         * returns the same scope instance.
         * 
         * @param childName the child name
         * @return the child scope
         */
        public KeyScope child(String childName) {
            return children.computeIfAbsent(childName, name -> {
                String childPath = path.isEmpty() ? name : path + "/" + name;
                return new KeyScope(childPath, this);
            });
        }
        
        /**
         * Resets this scope's counter.
         */
        public void reset() {
            counter = 0;
        }
        
        /**
         * Resets this scope and all child scopes recursively.
         */
        public void resetAll() {
            counter = 0;
            for (KeyScope child : children.values()) {
                child.resetAll();
            }
        }
        
        @Override
        public String toString() {
            return "KeyScope(" + path + ", counter=" + counter + ")";
        }
    }
}
