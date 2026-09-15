package dev.vfyjxf.cloudlib.api.unit.units;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
import dev.vfyjxf.cloudlib.api.unit.Ratio;
import dev.vfyjxf.cloudlib.api.unit.Unit;
import dev.vfyjxf.cloudlib.api.unit.UnitConverter;
import dev.vfyjxf.cloudlib.api.unit.UnitPack;
import dev.vfyjxf.cloudlib.api.unit.UnitRule;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;

import java.util.List;

/**
 * GregTech conventions (GTCEu / GT Modern): fluid amounts in liters, exactly
 * 1 L = 1 mB. Nugget 16 L, ingot 144 L, block 1296 L, dust 144 L, small dust 36 L,
 * tiny dust 16 L, plate 144 L, rod 72 L, gear 576 L; item forms: plate = 1 ingot,
 * rod = 1/2 ingot, gear = 4 ingots.
 *
 * <p>All rules are fixed: they are a convention, not a default, and must not be
 * silently overridden. This pack conflicts with {@link TicUnits} by design — the two
 * are alternative conventions (144 vs 90 mB per ingot), so never add both packs to
 * one converter; doing so throws {@code RuleConflictException}.
 */
@NotNullByDefault
public final class GtUnits {

    /**
     * GregTech's fluid display unit; exactly 1 liter = 1 millibucket.
     */
    public static final Unit<FluidUnits> liter = Unit.of(FluidUnits.family, Namespace.ofMc("liter"));

    public static final ImmutableList<UnitRule> rules = Lists.immutable.of(
            UnitRule.fixedRule(liter, FluidUnits.millibucket, Ratio.of(1)),
            UnitRule.fixedBridge(ItemUnits.nugget, FluidUnits.millibucket, Ratio.of(16)),
            UnitRule.fixedBridge(ItemUnits.ingot, FluidUnits.millibucket, Ratio.of(144)),
            UnitRule.fixedBridge(ItemUnits.block, FluidUnits.millibucket, Ratio.of(1296)),
            UnitRule.fixedBridge(ItemUnits.dust, FluidUnits.millibucket, Ratio.of(144)),
            UnitRule.fixedBridge(ItemUnits.smallDust, FluidUnits.millibucket, Ratio.of(36)),
            UnitRule.fixedBridge(ItemUnits.tinyDust, FluidUnits.millibucket, Ratio.of(16)),
            UnitRule.fixedBridge(ItemUnits.plate, FluidUnits.millibucket, Ratio.of(144)),
            UnitRule.fixedBridge(ItemUnits.rod, FluidUnits.millibucket, Ratio.of(72)),
            UnitRule.fixedBridge(ItemUnits.gear, FluidUnits.millibucket, Ratio.of(576)),
            UnitRule.matter(ItemUnits.plate, ItemUnits.ingot, Ratio.of(1)),
            UnitRule.matter(ItemUnits.rod, ItemUnits.ingot, Ratio.of(1, 2)),
            UnitRule.matter(ItemUnits.gear, ItemUnits.ingot, Ratio.of(4))
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

    private GtUnits() {
    }
}
