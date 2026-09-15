package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.unit.exception.NoConversionPathException;
import dev.vfyjxf.cloudlib.api.unit.exception.RuleConflictException;
import dev.vfyjxf.cloudlib.api.unit.units.EnergyUnits;
import dev.vfyjxf.cloudlib.api.unit.units.ItemUnits;
import dev.vfyjxf.cloudlib.api.unit.units.TimeUnits;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnitConverterTest {

    private static final UnitFamily<UnitConverterTest> LOCAL = UnitFamily.measure(Namespace.ofMc("local"));
    private static final Unit<UnitConverterTest> LOCAL_A = Unit.of(LOCAL, Namespace.ofMc("local_a"));
    private static final Unit<UnitConverterTest> LOCAL_B = Unit.of(LOCAL, Namespace.ofMc("local_b"));

    @Test
    void convertUsesRuleAndMultiplies() {
        UnitConverter converter = UnitConverter.builder()
                .convert(TimeUnits.second, TimeUnits.tick).by(20)
                .build();

        Quantity<TimeUnits> result = converter.convert(2, TimeUnits.second, TimeUnits.tick);

        assertEquals(Ratio.of(40), result.value());
        assertEquals(TimeUnits.tick, result.unit());
    }

    @Test
    void rulesApplyInReverseAutomatically() {
        UnitConverter converter = UnitConverter.builder()
                .convert(TimeUnits.second, TimeUnits.tick).by(20)
                .build();

        Quantity<TimeUnits> result = converter.convert(40, TimeUnits.tick, TimeUnits.second);

        assertEquals(Ratio.of(2), result.value());
    }

    @Test
    void bfsResolvesMultiHopPaths() {
        UnitConverter converter = UnitConverter.builder()
                .add(TimeUnits.pack())
                .build();

        assertEquals(Ratio.of(1200), converter.convert(1, TimeUnits.minute, TimeUnits.tick).value());
        assertEquals(Ratio.of(72000), converter.convert(1, TimeUnits.hour, TimeUnits.tick).value());
    }

    @Test
    void sameUnitConvertsToItself() {
        UnitConverter converter = UnitConverter.builder().build();
        assertEquals(Ratio.of(5), converter.convert(5, TimeUnits.tick, TimeUnits.tick).value());
        assertTrue(converter.canConvert(TimeUnits.tick, TimeUnits.tick));
    }

    @Test
    void canConvertReflectsRuleGraph() {
        UnitConverter converter = UnitConverter.builder()
                .add(TimeUnits.pack())
                .build();

        assertTrue(converter.canConvert(TimeUnits.minute, TimeUnits.tick));
        assertFalse(converter.canConvert(TimeUnits.tick, EnergyUnits.fe));
    }

    @Test
    void missingPathThrowsNamingBothFamilies() {
        UnitConverter converter = UnitConverter.builder().build();

        NoConversionPathException e = assertThrows(
                NoConversionPathException.class,
                () -> converter.convert(1, TimeUnits.tick, EnergyUnits.fe)
        );
        assertTrue(e.getMessage().contains("minecraft:time"));
        assertTrue(e.getMessage().contains("minecraft:energy"));
    }

    @Test
    void nonPositiveRatioIsRejected() {
        UnitConverter.Builder builder = UnitConverter.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.convert(TimeUnits.second, TimeUnits.tick).by(0));
        assertThrows(IllegalArgumentException.class, () -> builder.convert(TimeUnits.second, TimeUnits.tick).by(-1));
    }

    @Test
    void ruleEndpointsMustShareFamily() {
        // same-family is compile-time enforced on Builder.rule; the wildcard UnitRule
        // record can still mix families, so the builder re-validates at build time
        UnitRule mixed = new UnitRule(TimeUnits.second, EnergyUnits.fe, Ratio.of(20), UnitRule.Kind.rule, false, null);
        UnitConverter.Builder builder = UnitConverter.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.rules(mixed));
    }

    @Test
    void bridgeEndpointsMustBeCrossFamily() {
        UnitRule mixed = new UnitRule(TimeUnits.second, TimeUnits.tick, Ratio.of(20), UnitRule.Kind.bridge, false, null);
        UnitConverter.Builder builder = UnitConverter.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.rules(mixed));
    }

    @Test
    void testLocalFamilyUsesTestClassAsMarker() {
        UnitConverter converter = UnitConverter.builder()
                .convert(LOCAL_A, LOCAL_B).by(3)
                .build();

        assertEquals(Ratio.of(6), converter.convert(2, LOCAL_A, LOCAL_B).value());
        assertEquals(UnitFamily.Kind.measure, LOCAL.kind());
    }

    @Test
    void duplicateRuleConflicts() {
        UnitConverter.Builder builder = UnitConverter.builder()
                .convert(EnergyUnits.eu, EnergyUnits.fe).by(4);
        assertThrows(
                RuleConflictException.class,
                () -> builder.convert(EnergyUnits.eu, EnergyUnits.fe).by(4)
        );
    }

    @Test
    void nonFixedRuleCanBeOverridden() {
        UnitConverter converter = UnitConverter.builder()
                .add(EnergyUnits.pack())
                .convert(EnergyUnits.eu, EnergyUnits.fe).by(8)
                .build();

        assertEquals(Ratio.of(8), converter.convert(1, EnergyUnits.eu, EnergyUnits.fe).value());
    }

    @Test
    void fixedRuleCannotBeOverridden() {
        UnitConverter.Builder builder = UnitConverter.builder().add(TimeUnits.pack());
        assertThrows(
                RuleConflictException.class,
                () -> builder.convert(TimeUnits.second, TimeUnits.tick).by(10)
        );
        assertThrows(
                RuleConflictException.class,
                () -> builder.convert(TimeUnits.tick, TimeUnits.second).by(1, 10)
        );
    }

    @Test
    void exactRatioSurvivesConversion() {
        UnitConverter converter = UnitConverter.builder()
                .add(ItemUnits.pack())
                .build();

        Quantity<ItemUnits> result = converter.convert(1, ItemUnits.ingot, ItemUnits.block);

        assertEquals(Ratio.of(1, 9), result.value());
    }

    @Test
    void unrelatedFamiliesAreDistinct() {
        UnitFamily<TimeUnits> other = UnitFamily.measure(Namespace.ofMc("time"));
        assertEquals(TimeUnits.family, other);
        assertEquals(TimeUnits.family.hashCode(), other.hashCode());
    }

    @Test
    void approximateRuleMarksResultsInexact() {
        UnitConverter converter = UnitConverter.builder()
                .convert(EnergyUnits.eu, EnergyUnits.fe).byApproximate(4)
                .build();

        Quantity<EnergyUnits> result = converter.convert(2, EnergyUnits.eu, EnergyUnits.fe);

        assertEquals(Ratio.of(8), result.value());
        assertFalse(result.isExact());
        assertFalse(converter.convert(8, EnergyUnits.fe, EnergyUnits.eu).isExact());
    }

    @Test
    void exactChainStaysExact() {
        UnitConverter converter = UnitConverter.builder()
                .add(TimeUnits.pack())
                .build();

        assertTrue(converter.convert(1, TimeUnits.hour, TimeUnits.tick).isExact());
    }

    @Test
    void baseUnitRegistration() {
        UnitConverter converter = UnitConverter.builder()
                .rules(ItemUnits.rules)
                .baseUnit(ItemUnits.base)
                .build();

        assertEquals(ItemUnits.ingot, converter.baseUnit(ItemUnits.family));

        UnitConverter.Builder builder = UnitConverter.builder().baseUnit(ItemUnits.ingot);
        assertThrows(RuleConflictException.class, () -> builder.baseUnit(ItemUnits.nugget));
        assertThrows(RuleConflictException.class, () -> builder.baseUnit(ItemUnits.ingot));
        assertThrows(IllegalArgumentException.class, () -> UnitConverter.builder().baseUnit(TimeUnits.tick));
    }

    @Test
    void addPackRegistersRulesAndBaseUnit() {
        UnitConverter converter = UnitConverter.builder()
                .add(ItemUnits.pack())
                .build();

        // rules are registered
        assertEquals(Ratio.of(9), converter.convert(1, ItemUnits.ingot, ItemUnits.nugget).value());
        // base unit is registered too: normalization works without a separate baseUnit call
        MaterialAmount amount = converter.quantity(9, ItemUnits.nugget).toMaterialAmount(Namespace.ofCommon("iron"));
        assertEquals(Ratio.of(1), amount.value());
        assertEquals(ItemUnits.ingot, amount.baseUnit());
    }

    @Test
    void stepModifiersCombineInEitherOrder() {
        Namespace iron = Namespace.ofCommon("iron");

        UnitConverter a = UnitConverter.builder()
                .convert(EnergyUnits.eu, EnergyUnits.fe).fixed().forMaterial(iron).by(8)
                .build();
        UnitConverter b = UnitConverter.builder()
                .convert(EnergyUnits.eu, EnergyUnits.fe).forMaterial(iron).fixed().by(8)
                .build();

        assertEquals(Ratio.of(8), a.convert(1, EnergyUnits.eu, EnergyUnits.fe, iron).value());
        assertEquals(Ratio.of(8), b.convert(1, EnergyUnits.eu, EnergyUnits.fe, iron).value());

        UnitConverter.Builder builder = UnitConverter.builder()
                .convert(EnergyUnits.eu, EnergyUnits.fe).forMaterial(iron).fixed().by(8);
        assertThrows(
                RuleConflictException.class,
                () -> builder.convert(EnergyUnits.eu, EnergyUnits.fe).forMaterial(iron).by(10)
        );
    }

    @Test
    void fluentMaterialOverrideWinsOverGeneric() {
        Namespace iron = Namespace.ofCommon("iron");
        UnitConverter converter = UnitConverter.builder()
                .add(ItemUnits.pack())
                .convert(ItemUnits.ingot, ItemUnits.block).forMaterial(iron).by(1, 4)
                .build();

        assertEquals(Ratio.of(1, 9), converter.convert(1, ItemUnits.ingot, ItemUnits.block).value());
        assertEquals(Ratio.of(1, 4), converter.convert(1, ItemUnits.ingot, ItemUnits.block, iron).value());
    }
}
