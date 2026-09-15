package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.unit.exception.NoConversionPathException;
import dev.vfyjxf.cloudlib.api.unit.exception.RuleConflictException;
import dev.vfyjxf.cloudlib.api.unit.units.EnergyUnits;
import dev.vfyjxf.cloudlib.api.unit.units.FluidUnits;
import dev.vfyjxf.cloudlib.api.unit.units.ItemUnits;
import dev.vfyjxf.cloudlib.api.unit.units.TimeUnits;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrossFamilyTest {

    private static final Namespace IRON = Namespace.ofCommon("iron");

    @Test
    void bridgeConvertsAcrossFamilies() {
        UnitConverter converter = UnitConverter.builder()
                .convert(ItemUnits.ingot, FluidUnits.millibucket).by(144)
                .build();

        assertEquals(
                Ratio.of(144),
                converter.convert(1, ItemUnits.ingot, FluidUnits.millibucket).value()
        );
    }

    @Test
    void bridgeAppliesInReverseAutomatically() {
        UnitConverter converter = UnitConverter.builder()
                .convert(ItemUnits.ingot, FluidUnits.millibucket).by(144)
                .build();

        assertEquals(
                Ratio.of(1),
                converter.convert(144, FluidUnits.millibucket, ItemUnits.ingot).value()
        );
    }

    @Test
    void materialSpecificBridgeBeatsGeneric() {
        UnitConverter converter = UnitConverter.builder()
                .convert(ItemUnits.ingot, FluidUnits.millibucket).by(144)
                .convert(ItemUnits.ingot, FluidUnits.millibucket).forMaterial(IRON).by(100)
                .build();

        assertEquals(
                Ratio.of(144),
                converter.convert(1, ItemUnits.ingot, FluidUnits.millibucket).value()
        );
        assertEquals(
                Ratio.of(100),
                converter.convert(1, ItemUnits.ingot, FluidUnits.millibucket, IRON).value()
        );
    }

    @Test
    void bridgeChainsWithFamilyRules() {
        UnitConverter converter = UnitConverter.builder()
                .add(ItemUnits.pack())
                .convert(ItemUnits.ingot, FluidUnits.millibucket).by(144)
                .build();

        assertEquals(
                Ratio.of(1296),
                converter.convert(1, ItemUnits.block, FluidUnits.millibucket).value()
        );
    }

    @Test
    void crossFamilyWithoutBridgeThrows() {
        UnitConverter converter = UnitConverter.builder()
                .add(ItemUnits.pack())
                .add(FluidUnits.pack())
                .build();

        NoConversionPathException e = assertThrows(
                NoConversionPathException.class,
                () -> converter.convert(1, ItemUnits.ingot, FluidUnits.millibucket)
        );
        assertTrue(e.getMessage().contains("minecraft:item"));
        assertTrue(e.getMessage().contains("minecraft:fluid"));
    }

    @Test
    void crossFamilyConvertAutoClassifiesAsBridge() {
        // same-family endpoints become a family rule, cross-family become a bridge;
        // both directions resolve through the same registration
        UnitConverter converter = UnitConverter.builder()
                .convert(ItemUnits.ingot, FluidUnits.millibucket).by(144)
                .build();

        assertEquals(
                Ratio.of(144),
                converter.convert(1, ItemUnits.ingot, FluidUnits.millibucket).value()
        );
        assertEquals(
                Ratio.of(1),
                converter.convert(144, FluidUnits.millibucket, ItemUnits.ingot).value()
        );
    }

    @Test
    void fixedBridgeCannotBeOverridden() {
        UnitConverter.Builder builder = UnitConverter.builder()
                .convert(ItemUnits.ingot, FluidUnits.millibucket).fixed().by(144);
        assertThrows(
                RuleConflictException.class,
                () -> builder.convert(ItemUnits.ingot, FluidUnits.millibucket).by(100)
        );
        assertThrows(
                RuleConflictException.class,
                () -> builder.convert(FluidUnits.millibucket, ItemUnits.ingot).by(1, 100)
        );
    }

    @Test
    void crossFamilyAddGoesThroughBridge() {
        UnitConverter converter = UnitConverter.builder()
                .convert(ItemUnits.ingot, FluidUnits.millibucket).by(144)
                .build();

        // add() is compile-time same-family; cross-family goes through toCross explicitly
        Quantity<ItemUnits> sum = converter.quantity(1, ItemUnits.ingot)
                .add(converter.quantity(144, FluidUnits.millibucket).toCross(ItemUnits.ingot));

        assertEquals(Ratio.of(2), sum.value());
        assertEquals(ItemUnits.ingot, sum.unit());
    }

    @Test
    void toCrossConvertsAcrossFamilies() {
        UnitConverter converter = UnitConverter.builder()
                .add(ItemUnits.pack())
                .convert(ItemUnits.ingot, FluidUnits.millibucket).by(144)
                .build();

        Quantity<FluidUnits> millibuckets = converter.quantity(1, ItemUnits.block)
                .toCross(FluidUnits.millibucket);
        assertEquals(Ratio.of(1296), millibuckets.value());

        Quantity<FluidUnits> ironMb = converter.quantity(1, ItemUnits.ingot)
                .toCross(FluidUnits.millibucket, Namespace.ofCommon("iron"));
        assertEquals(Ratio.of(144), ironMb.value());
    }

    @Test
    void toStaysWithinFamily() {
        UnitConverter converter = UnitConverter.builder()
                .add(ItemUnits.pack())
                .build();

        Quantity<ItemUnits> blocks = converter.quantity(9, ItemUnits.ingot).to(ItemUnits.block);
        assertEquals(Ratio.of(1), blocks.value());
    }

    @Test
    void approximateBridgePropagatesInexactness() {
        UnitConverter converter = UnitConverter.builder()
                .add(ItemUnits.pack())
                .convert(ItemUnits.ingot, FluidUnits.millibucket).byApproximate(144)
                .build();

        assertFalse(converter.convert(1, ItemUnits.ingot, FluidUnits.millibucket).isExact());
        // exact family rule chained with an approximate bridge is inexact end to end
        assertFalse(converter.convert(1, ItemUnits.block, FluidUnits.millibucket).isExact());
        // the reverse direction uses the inverse of the approximate ratio, still inexact
        assertFalse(converter.convert(144, FluidUnits.millibucket, ItemUnits.ingot).isExact());
        // exact-only hops are unaffected
        assertTrue(converter.convert(1, ItemUnits.block, ItemUnits.ingot).isExact());
    }
}
