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
 * Units of energy. The EU to FE rule is a non-fixed default and can be overridden.
 * This class itself is the family marker type.
 */
@NotNullByDefault
public final class EnergyUnits {

    public static final UnitFamily<EnergyUnits> family = UnitFamily.measure(Namespace.ofMc("energy"));

    public static final Unit<EnergyUnits> eu = Unit.of(family, Namespace.ofMc("eu"));
    public static final Unit<EnergyUnits> fe = Unit.of(family, Namespace.ofMc("fe"));

    public static final ImmutableList<UnitRule> rules = Lists.immutable.of(
            UnitRule.rule(eu, fe, Ratio.of(4))
    );

    /**
     * Unmodifiable {@link List} view of {@link #rules}, for consumers working with
     * plain Java collections.
     */
    public static final List<UnitRule> rulesAsList = rules.castToList();

    /**
     * The predefined bundle for {@link UnitConverter.Builder#add}: rules only,
     * measure families have no base unit.
     */
    public static UnitPack pack() {
        return UnitPack.of(rules);
    }

    private EnergyUnits() {
    }
}
