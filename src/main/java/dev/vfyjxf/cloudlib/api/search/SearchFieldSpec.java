package dev.vfyjxf.cloudlib.api.search;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Declarative constraints of a {@link SearchField}: everything the shared
 * search machinery needs to know about a field without any global registry —
 * scenarios declare their fields against this spec and bind them into a
 * {@link SearchFieldSet}.
 *
 * @param weight        relevance weight used by scorers
 * @param numeric       whether the field carries numeric values (ranges, comparisons)
 * @param contentChannel whether the field is a content channel that supports
 *                      {@code * amount} modifiers and rejects bare comparison operators
 * @param jeiPrefix     optional single-character query prefix (JEI style, e.g. {@code @})
 * @param unit          unit interpretation for numeric parsing ({@link Unit#none} for plain numbers)
 * @param aliases       lowercase query aliases resolving to this field
 * @param virtual       whether this field is the union of the set's non-virtual
 *                      content channels (an aggregate like {@code contains});
 *                      virtual fields hold no per-document entries of their own
 */
public record SearchFieldSpec(
    int weight,
    boolean numeric,
    boolean contentChannel,
    @Nullable Character jeiPrefix,
    Unit unit,
    List<String> aliases,
    boolean virtual
) {
    public SearchFieldSpec {
        aliases = List.copyOf(aliases == null ? List.of() : aliases);
        if (unit == null) {
            unit = Unit.none;
        }
    }

    public SearchFieldSpec(int weight, boolean numeric, boolean contentChannel, @Nullable Character jeiPrefix, String... aliases) {
        this(weight, numeric, contentChannel, jeiPrefix, Unit.none, List.of(aliases), false);
    }

    public SearchFieldSpec(int weight, boolean numeric, boolean contentChannel, @Nullable Character jeiPrefix, Unit unit, List<String> aliases) {
        this(weight, numeric, contentChannel, jeiPrefix, unit, aliases, false);
    }

    /** Unit conventions applied when parsing numeric query values. */
    public enum Unit {
        none,
        time,
        energy,
        chance
    }
}
