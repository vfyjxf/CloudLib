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
 * Units of fluid matter. This class itself is the family marker type.
 */
@NotNullByDefault
public final class FluidUnits {

    public static final UnitFamily<FluidUnits> family = UnitFamily.matter(Namespace.ofMc("fluid"));

    public static final Unit<FluidUnits> millibucket = Unit.of(family, Namespace.ofMc("millibucket"));
    public static final Unit<FluidUnits> bucket = Unit.of(family, Namespace.ofMc("bucket"));
    public static final Unit<FluidUnits> droplet = Unit.of(family, Namespace.ofMc("droplet"));

    /**
     * The conventional base unit of this family, for use with {@code baseUnit(...)}.
     */
    public static final Unit<FluidUnits> base = millibucket;

    public static final ImmutableList<UnitRule> rules = Lists.immutable.of(
            UnitRule.matter(bucket, millibucket, Ratio.of(1000))
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

    private FluidUnits() {
    }
}
