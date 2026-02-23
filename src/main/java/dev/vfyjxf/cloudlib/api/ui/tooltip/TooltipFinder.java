package dev.vfyjxf.cloudlib.api.ui.tooltip;

import dev.vfyjxf.cloudlib.api.util.Namespace;

import java.util.function.Predicate;

/**
 * Describes how to locate existing entries in a {@link Tooltip}.
 * <p>
 * Finders are used by {@link TooltipLocator.Relative} to determine the anchor position
 * for insertion. Two built-in strategies are provided:
 * <ul>
 *   <li>{@link ByMarker} — match entries by their {@link Namespace} marker</li>
 *   <li>{@link ByEntry} — match entries by a {@link Predicate} on the {@link TooltipEntry} itself,
 *       supporting Java 21 pattern matching</li>
 * </ul>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * // Find all entries tagged with Tooltip.body
 * TooltipFinder.marker(Tooltip.body);
 *
 * // Find entries containing "Damage" text via pattern matching
 * TooltipFinder.entry(e -> e instanceof TooltipEntry.TextEntry(var t)
 *     && t.getString().contains("Damage"));
 * }</pre>
 *
 * @see TooltipLocator
 */
public sealed interface TooltipFinder {

    /**
     * Matches entries by their {@link Namespace} marker.
     */
    record ByMarker(Namespace marker) implements TooltipFinder {
    }

    /**
     * Matches entries by a predicate applied to the {@link TooltipEntry} content.
     */
    record ByEntry(Predicate<TooltipEntry> matcher) implements TooltipFinder {
    }

    //region factory

    /**
     * Creates a finder that matches entries tagged with the given marker.
     */
    static TooltipFinder marker(Namespace marker) {
        return new ByMarker(marker);
    }

    /**
     * Creates a finder that matches entries satisfying the given predicate.
     * <p>
     * Example with pattern matching:
     * <pre>{@code
     * TooltipFinder.entry(e -> switch (e) {
     *     case TooltipEntry.TextEntry(var t) -> t.getString().startsWith("[Title]");
     *     default -> false;
     * });
     * }</pre>
     */
    static TooltipFinder entry(Predicate<TooltipEntry> matcher) {
        return new ByEntry(matcher);
    }

    //endregion

}
