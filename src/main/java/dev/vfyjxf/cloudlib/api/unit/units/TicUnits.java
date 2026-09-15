package dev.vfyjxf.cloudlib.api.unit.units;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
import dev.vfyjxf.cloudlib.api.unit.Ratio;
import dev.vfyjxf.cloudlib.api.unit.UnitConverter;
import dev.vfyjxf.cloudlib.api.unit.UnitPack;
import dev.vfyjxf.cloudlib.api.unit.UnitRule;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;

import java.util.List;

/**
 * Tinkers' Construct 3 smeltery conventions (1.18+): 1 nugget = 10 mB,
 * 1 ingot = 90 mB, 1 block = 810 mB, 1 gem = 100 mB.
 *
 * <p>All bridges are fixed: they are a convention, not a default, and must not be
 * silently overridden. This pack conflicts with {@link GtUnits} by design — the two
 * are alternative conventions (90 vs 144 mB per ingot), so never add both packs to
 * one converter; doing so throws {@code RuleConflictException}.
 */
@NotNullByDefault
public final class TicUnits {

    public static final ImmutableList<UnitRule> rules = Lists.immutable.of(
            UnitRule.fixedBridge(ItemUnits.nugget, FluidUnits.millibucket, Ratio.of(10)),
            UnitRule.fixedBridge(ItemUnits.ingot, FluidUnits.millibucket, Ratio.of(90)),
            UnitRule.fixedBridge(ItemUnits.block, FluidUnits.millibucket, Ratio.of(810)),
            UnitRule.fixedBridge(ItemUnits.gem, FluidUnits.millibucket, Ratio.of(100))
    );

    /**
     * Unmodifiable {@link List} view of {@link #rules}, for consumers working with
     * plain Java collections.
     */
    public static final List<UnitRule> rulesAsList = rules.castToList();

    /**
     * The predefined bundle for {@link UnitConverter.Builder#add}: rules only,
     * convention packs carry no base unit.
     */
    public static UnitPack pack() {
        return UnitPack.of(rules);
    }

    private TicUnits() {
    }
}
