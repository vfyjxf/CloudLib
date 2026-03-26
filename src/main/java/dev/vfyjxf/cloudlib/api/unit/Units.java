package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.util.Namespace;

import java.util.List;

public final class Units {

    private Units() {
    }

    //region Family keys

    public static final Namespace familyTime = Namespace.ofMc("time");
    public static final Namespace familyEnergy = Namespace.ofMc("energy");
    public static final Namespace familyMatter = Namespace.ofMc("matter");
    public static final Namespace familyItem = Namespace.ofMc("item");
    public static final Namespace familyFluid = Namespace.ofMc("fluid");

    //endregion

    //region Unit ID keys - Time

    public static final Namespace unitTick = Namespace.ofMc("tick");
    public static final Namespace unitSecond = Namespace.ofMc("second");
    public static final Namespace unitMinute = Namespace.ofMc("minute");
    public static final Namespace unitHour = Namespace.ofMc("hour");

    //endregion

    //region Unit ID keys - Energy

    public static final Namespace unitEu = Namespace.ofMc("eu");
    public static final Namespace unitFe = Namespace.ofMc("fe");

    //endregion

    //region Unit ID keys - Item forms

    public static final Namespace unitIngot = Namespace.ofMc("ingot");
    public static final Namespace unitBlock = Namespace.ofMc("block");
    public static final Namespace unitNugget = Namespace.ofMc("nugget");
    public static final Namespace unitDust = Namespace.ofMc("dust");
    public static final Namespace unitPlate = Namespace.ofMc("plate");
    public static final Namespace unitGear = Namespace.ofMc("gear");
    public static final Namespace unitRod = Namespace.ofMc("rod");

    //endregion

    //region Unit ID keys - Fluid measures

    public static final Namespace unitMillibucket = Namespace.ofMc("millibucket");
    public static final Namespace unitBucket = Namespace.ofMc("bucket");
    public static final Namespace unitDroplet = Namespace.ofMc("droplet");

    //endregion

    //region Pre-built Unit instances

    private static final class TimeDef {}
    private static final class EnergyDef {}

    private static final MeasureFamily<TimeDef> TIME_F = MeasureFamily.create(familyTime);
    private static final MeasureFamily<EnergyDef> ENERGY_F = MeasureFamily.create(familyEnergy);

    public static final Unit<?> TICK = Unit.create(TIME_F, unitTick);
    public static final Unit<?> SECOND = Unit.create(TIME_F, unitSecond);
    public static final Unit<?> MINUTE = Unit.create(TIME_F, unitMinute);
    public static final Unit<?> HOUR = Unit.create(TIME_F, unitHour);

    public static final Unit<?> FE = Unit.create(ENERGY_F, unitFe);
    public static final Unit<?> EU = Unit.create(ENERGY_F, unitEu);

    //endregion

    //region Default rule sets

    private static final Unit<TimeDef> TICK_T = Unit.create(TIME_F, unitTick);
    private static final Unit<TimeDef> SECOND_T = Unit.create(TIME_F, unitSecond);
    private static final Unit<TimeDef> MINUTE_T = Unit.create(TIME_F, unitMinute);
    private static final Unit<TimeDef> HOUR_T = Unit.create(TIME_F, unitHour);

    private static final Unit<EnergyDef> FE_T = Unit.create(ENERGY_F, unitFe);
    private static final Unit<EnergyDef> EU_T = Unit.create(ENERGY_F, unitEu);

    @SuppressWarnings("unchecked")
    public static List<FamilyRule<?>> timeRules() {
        return List.of(
                (FamilyRule<?>) FamilyRule.createFixedFraction(SECOND_T, TICK_T, 20, 1),
                (FamilyRule<?>) FamilyRule.createFixedFraction(MINUTE_T, SECOND_T, 60, 1),
                (FamilyRule<?>) FamilyRule.createFixedFraction(HOUR_T, MINUTE_T, 60, 1)
        );
    }

    @SuppressWarnings("unchecked")
    public static List<FamilyRule<?>> energyRules() {
        return List.of(
                (FamilyRule<?>) FamilyRule.create(EU_T, FE_T, 4.0)
        );
    }

    public static List<FamilyRule<?>> defaultRules() {
        var rules = new java.util.ArrayList<FamilyRule<?>>();
        rules.addAll(timeRules());
        rules.addAll(energyRules());
        return List.copyOf(rules);
    }

    //endregion
}
