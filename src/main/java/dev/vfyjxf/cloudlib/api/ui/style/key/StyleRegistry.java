package dev.vfyjxf.cloudlib.api.ui.style.key;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The style-key index: css name → {@link StyleKey}, plus the shorthand table.
 * <p>
 * Builtin keys self-register when {@link Styles} class-initializes; the plugin
 * bootstrap ({@code registerStyleKeys}) forces that init at the right time and
 * lets mods add their own keys.
 */
public final class StyleRegistry {

    private static final StyleRegistry instance = new StyleRegistry();

    /** The global registry. */
    public static StyleRegistry get() {
        return instance;
    }

    private final Map<String, StyleKey<?>> byId = new ConcurrentHashMap<>();
    private final Map<String, Shorthand> shorthands = new ConcurrentHashMap<>();
    private final Map<String, String> aliases = new ConcurrentHashMap<>();

    private StyleRegistry() {}

    /**
     * Registers a key under its css name. Registration writes the index only —
     * the key itself is a self-contained constant.
     *
     * @return the key, for field-initializer use
     */
    public <T> StyleKey<T> register(StyleKey<T> key) {
        if (byId.putIfAbsent(key.id(), key) != null) {
            throw new IllegalArgumentException("duplicate style key: " + key.id());
        }
        return key;
    }

    /** Registers a css shorthand expander (e.g. {@code padding} → 4 longhands). */
    public void registerShorthand(String name, Shorthand shorthand) {
        if (shorthands.putIfAbsent(name, shorthand) != null) {
            throw new IllegalArgumentException("duplicate shorthand: " + name);
        }
    }

    /** Registers a css-name alias pointing at an existing key ({@code text-color} → {@code color}). */
    public void registerAlias(String alias, String canonicalId) {
        aliases.putIfAbsent(alias, canonicalId);
    }

    /** The key for a css property name (aliases resolved), or {@code null} when unknown. */
    public @Nullable StyleKey<?> byId(String cssName) {
        String id = aliases.getOrDefault(cssName, cssName);
        return byId.get(id);
    }

    /** The shorthand expander for a css name, or {@code null}. */
    public @Nullable Shorthand shorthand(String name) {
        return shorthands.get(name);
    }

    /** All registered keys. */
    public Collection<StyleKey<?>> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    /** All registered longhand ids + shorthand names. */
    public List<String> names() {
        var out = new java.util.ArrayList<String>(byId.keySet());
        out.addAll(shorthands.keySet());
        Collections.sort(out);
        return out;
    }
}
