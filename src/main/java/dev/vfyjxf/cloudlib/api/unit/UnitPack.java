package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
import dev.vfyjxf.cloudlib.util.Checks;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A predefined bundle of unit rules plus the family's base unit when applicable,
 * registerable in one call via {@link UnitConverter.Builder#add(UnitPack)}.
 */
@NotNullByDefault
public record UnitPack(ImmutableList<UnitRule> rules, @Nullable Unit<?> baseUnit) {

    public static UnitPack of(ImmutableList<UnitRule> rules) {
        return new UnitPack(rules, null);
    }

    public static UnitPack of(ImmutableList<UnitRule> rules, Unit<?> baseUnit) {
        return new UnitPack(rules, baseUnit);
    }

    public static UnitPack of(List<UnitRule> rules) {
        return new UnitPack(Lists.immutable.ofAll(rules), null);
    }

    public static UnitPack of(List<UnitRule> rules, Unit<?> baseUnit) {
        return new UnitPack(Lists.immutable.ofAll(rules), baseUnit);
    }

    public UnitPack {
        Checks.checkNotNull(rules, "rules");
    }

    /**
     * Unmodifiable {@link List} view of {@link #rules()}, for consumers working with
     * plain Java collections.
     */
    public List<UnitRule> rulesAsList() {
        return rules.castToList();
    }
}
