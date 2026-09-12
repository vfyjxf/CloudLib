package dev.vfyjxf.cloudlib.api.ui.theme;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * The metadata a node exposes to the theme engine for selector matching.
 * <p>
 * {@code Widget} implements this; tests may provide lightweight fixtures. All
 * methods have defaults so a bare widget is still matchable by tag.
 * <p>
 * Naming mirrors the DOM:
 * <ul>
 *   <li>{@link #themeTag()} — the element type ({@code button}, {@code item-slot}…)</li>
 *   <li>{@link #themeClasses()} — {@code .class} members</li>
 *   <li>{@link #themeId()} — {@code #id}</li>
 *   <li>{@link #themeAttr(String)} — {@code [name=value]} attributes</li>
 *   <li>{@link #themeStates()} — pseudo-classes ({@code hovered}, {@code focused}…)</li>
 * </ul>
 */
public interface Themeable {

    /** The element tag matched by type selectors — e.g. {@code "button"}. */
    default String themeTag() {
        String name = getClass().getSimpleName();
        // ButtonWidget → button, ItemGridWidget → item-grid
        if (name.endsWith("Widget")) {
            name = name.substring(0, name.length() - "Widget".length());
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append('-');
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    /** Class memberships matched by {@code .name} selectors. */
    default List<String> themeClasses() {
        return List.of();
    }

    /** The unique id matched by {@code #id} selectors, or null. */
    default @Nullable String themeId() {
        return null;
    }

    /** Attribute lookup matched by {@code [name]} / {@code [name=value]} selectors. */
    default @Nullable String themeAttr(String name) {
        return null;
    }

    /**
     * Active state names matched by pseudo-class selectors.
     * <p>
     * Built-in states: {@code hovered} {@code active} {@code inactive}
     * {@code interactive} {@code non-interactive} {@code focused} {@code disabled}
     * {@code enabled}. Widgets may add their own ({@code checked}, {@code empty},
     * {@code engaged}…).
     */
    default Set<String> themeStates() {
        return Set.of();
    }

    /** The parent node for ancestor/child combinators, or null at the root. */
    default @Nullable Themeable themeParent() {
        return null;
    }

    /** Siblings in document order (self included) for {@code + ~ :nth-*} matching. */
    default List<? extends Themeable> themeSiblings() {
        return List.of(this);
    }

    /** Direct children in document order for descendant/child/{@code :has} matching. */
    default List<? extends Themeable> themeChildren() {
        return List.of();
    }

    /** A part name for {@code ::part(name)} matching inside compound widgets. */
    default @Nullable String themePart() {
        return null;
    }
}
