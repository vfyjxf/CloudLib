package dev.vfyjxf.cloudlib.api.unit.units;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
import dev.vfyjxf.cloudlib.api.unit.Ratio;
import dev.vfyjxf.cloudlib.api.unit.Unit;
import dev.vfyjxf.cloudlib.api.unit.UnitConverter;
import dev.vfyjxf.cloudlib.api.unit.UnitFamily;
import dev.vfyjxf.cloudlib.api.unit.UnitPack;
import dev.vfyjxf.cloudlib.api.unit.UnitRule;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;

import java.util.List;

/**
 * Units of item matter. Default rules are material-agnostic and can be
 * overridden per material via {@code convert(from, to).forMaterial(material).by(...)}.
 * This class itself is the family marker type.
 */
@NotNullByDefault
public final class ItemUnits {

    public static final UnitFamily<ItemUnits> family = UnitFamily.matter(Namespace.ofMc("item"));

    public static final Unit<ItemUnits> ingot = Unit.of(family, Namespace.ofMc("ingot"));
    public static final Unit<ItemUnits> block = Unit.of(family, Namespace.ofMc("block"));
    public static final Unit<ItemUnits> nugget = Unit.of(family, Namespace.ofMc("nugget"));
    public static final Unit<ItemUnits> dust = Unit.of(family, Namespace.ofMc("dust"));
    public static final Unit<ItemUnits> smallDust = Unit.of(family, Namespace.ofMc("small_dust"));
    public static final Unit<ItemUnits> tinyDust = Unit.of(family, Namespace.ofMc("tiny_dust"));
    /**
     * Definition only, without default rules on purpose: metal blocks are 9 ingots
     * while gem blocks are 9 gems, so a generic {@code block}/{@code ingot} ↔ gem rule
     * would wrongly equate ingot and gem transitively. Wire gem conversions per
     * material or via a convention pack such as {@link TicUnits}.
     */
    public static final Unit<ItemUnits> gem = Unit.of(family, Namespace.ofMc("gem"));
    public static final Unit<ItemUnits> plate = Unit.of(family, Namespace.ofMc("plate"));
    public static final Unit<ItemUnits> gear = Unit.of(family, Namespace.ofMc("gear"));
    public static final Unit<ItemUnits> rod = Unit.of(family, Namespace.ofMc("rod"));

    /**
     * The conventional base unit of this family, for use with {@code baseUnit(...)}.
     */
    public static final Unit<ItemUnits> base = ingot;

    public static final ImmutableList<UnitRule> rules = Lists.immutable.of(
            UnitRule.matter(ingot, nugget, Ratio.of(9)),
            UnitRule.matter(block, ingot, Ratio.of(9)),
            UnitRule.matter(dust, smallDust, Ratio.of(4)),
            UnitRule.matter(dust, tinyDust, Ratio.of(9))
    );

    /**
     * Unmodifiable {@link List} view of {@link #rules}, for consumers working with
     * plain Java collections.
     */
    public static final List<UnitRule> rulesAsList = rules.castToList();

    /**
     * The predefined bundle for {@link UnitConverter.Builder#add}: rules plus the
     * base unit ({@link #base}).
     */
    public static UnitPack pack() {
        return UnitPack.of(rules, base);
    }

    private ItemUnits() {
    }
}
