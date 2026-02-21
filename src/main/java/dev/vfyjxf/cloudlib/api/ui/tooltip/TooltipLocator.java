package dev.vfyjxf.cloudlib.api.ui.tooltip;

import dev.vfyjxf.cloudlib.api.util.Namespace;

import java.util.function.Predicate;

/**
 * Specifies where to insert an entry into a {@link Tooltip}.
 * <p>
 * Locators fall into two categories:
 * <ul>
 *   <li><b>Positional</b> — {@link #head()}, {@link #tail()}, {@link #at(int)} for absolute positions</li>
 *   <li><b>Relative</b> — {@link #before(TooltipFinder)} / {@link #after(TooltipFinder)} for positions
 *       relative to existing entries found by a {@link TooltipFinder}</li>
 * </ul>
 *
 * <h3>Standard locators:</h3>
 * <pre>{@code
 * // Insert at the very beginning
 * tooltip.insert(entry, TooltipLocator.head());
 *
 * // Insert at index 3
 * tooltip.insert(entry, TooltipLocator.at(3));
 *
 * // Insert before the first "body" entry
 * tooltip.insert(entry, TooltipLocator.before(Tooltip.body));
 *
 * // Insert after the last "footer" entry
 * tooltip.insert(entry, TooltipLocator.after(Tooltip.footer));
 * }</pre>
 *
 * <h3>Offset and match priority:</h3>
 * <pre>{@code
 * // Insert 2 positions after the first "body" entry
 * tooltip.insert(entry, TooltipLocator.after(Tooltip.body).offset(2).matchFirst());
 *
 * // Insert before the last entry matching a predicate
 * tooltip.insert(entry, TooltipLocator.before(
 *     TooltipFinder.entry(e -> e instanceof TooltipEntry.TextEntry(var t)
 *         && t.getString().contains("Damage"))
 * ).matchLast());
 * }</pre>
 *
 * @see TooltipFinder
 * @see Tooltip#insert(TooltipEntry, TooltipLocator)
 */
public sealed interface TooltipLocator {

    //region enums

    /**
     * Whether to insert before or after the found position.
     */
    enum Anchor {
        before, after
    }

    /**
     * Which match to use when the finder locates multiple entries.
     * <p>
     * This acts as a fallback priority: {@link #first} selects the earliest match,
     * {@link #last} selects the latest match in the entry list.
     */
    enum MatchPriority {
        first, last
    }

    //endregion

    //region records

    /**
     * Insert at the very beginning of the list.
     */
    enum Head implements TooltipLocator {
        instance
    }

    /**
     * Append to the end of the list.
     */
    enum Tail implements TooltipLocator {
        instance
    }

    /**
     * Insert at a specific index. Clamped to {@code [0, size]}.
     */
    record At(int index) implements TooltipLocator {}

    /**
     * Insert relative to entries found by a {@link TooltipFinder}.
     * <p>
     * If no entries match, falls back to appending at the end of the list.
     *
     * @param anchor        whether to insert before or after the found entry
     * @param finder        the strategy for finding existing entries
     * @param offset        additional positional offset from the anchor point
     * @param matchPriority which match to use when multiple entries are found
     */
    record Relative(
        Anchor anchor,
        TooltipFinder finder,
        int offset,
        MatchPriority matchPriority
    ) implements TooltipLocator {

        /**
         * Returns a new locator with the given offset applied.
         * Positive values shift towards the end; negative values shift towards the beginning.
         */
        public Relative offset(int offset) {
            return new Relative(anchor, finder, offset, matchPriority);
        }

        /**
         * Returns a new locator that selects the <b>first</b> match when multiple entries match.
         */
        public Relative matchFirst() {
            return new Relative(anchor, finder, offset, MatchPriority.first);
        }

        /**
         * Returns a new locator that selects the <b>last</b> match when multiple entries match.
         */
        public Relative matchLast() {
            return new Relative(anchor, finder, offset, MatchPriority.last);
        }
    }

    //endregion

    //region factory — positional

    /**
     * Locator that inserts at the very beginning.
     */
    static TooltipLocator head() {
        return Head.instance;
    }

    /**
     * Locator that appends to the end.
     */
    static TooltipLocator tail() {
        return Tail.instance;
    }

    /**
     * Locator that inserts at the given index (clamped to valid range).
     */
    static TooltipLocator at(int index) {
        return new At(index);
    }

    //endregion

    //region factory — relative (finder)

    /**
     * Locator that inserts <b>before</b> entries found by the given finder.
     * Defaults to {@link MatchPriority#first}.
     */
    static Relative before(TooltipFinder finder) {
        return new Relative(Anchor.before, finder, 0, MatchPriority.first);
    }

    /**
     * Locator that inserts <b>after</b> entries found by the given finder.
     * Defaults to {@link MatchPriority#last}.
     */
    static Relative after(TooltipFinder finder) {
        return new Relative(Anchor.after, finder, 0, MatchPriority.last);
    }

    //endregion

    //region factory — convenience (marker)

    /**
     * Locator that inserts <b>before</b> the first entry with the given marker.
     */
    static Relative before(Namespace marker) {
        return before(TooltipFinder.marker(marker));
    }

    /**
     * Locator that inserts <b>after</b> the last entry with the given marker.
     */
    static Relative after(Namespace marker) {
        return after(TooltipFinder.marker(marker));
    }

    //endregion

    //region factory — convenience (predicate)

    /**
     * Locator that inserts <b>before</b> the first entry matching the predicate.
     */
    static Relative before(Predicate<TooltipEntry> matcher) {
        return before(TooltipFinder.entry(matcher));
    }

    /**
     * Locator that inserts <b>after</b> the last entry matching the predicate.
     */
    static Relative after(Predicate<TooltipEntry> matcher) {
        return after(TooltipFinder.entry(matcher));
    }

    //endregion

}
