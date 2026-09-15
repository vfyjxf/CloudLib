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
 * Units of time. All rules are fixed: time conversion can't be overridden.
 * This class itself is the family marker type.
 */
@NotNullByDefault
public final class TimeUnits {

    public static final UnitFamily<TimeUnits> family = UnitFamily.measure(Namespace.ofMc("time"));

    public static final Unit<TimeUnits> tick = Unit.of(family, Namespace.ofMc("tick"));
    public static final Unit<TimeUnits> second = Unit.of(family, Namespace.ofMc("second"));
    public static final Unit<TimeUnits> minute = Unit.of(family, Namespace.ofMc("minute"));
    public static final Unit<TimeUnits> hour = Unit.of(family, Namespace.ofMc("hour"));

    public static final ImmutableList<UnitRule> rules = Lists.immutable.of(
            UnitRule.fixedRule(second, tick, Ratio.of(20)),
            UnitRule.fixedRule(minute, second, Ratio.of(60)),
            UnitRule.fixedRule(hour, minute, Ratio.of(60))
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

    private TimeUnits() {
    }
}
