package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.unit.exception.NoConversionPathException;
import dev.vfyjxf.cloudlib.api.unit.units.FluidUnits;
import dev.vfyjxf.cloudlib.api.unit.units.ItemUnits;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialAmountTest {

    private static final Namespace iron = Namespace.ofCommon("iron");
    private static final Namespace copper = Namespace.ofCommon("copper");

    private static final UnitFamily<MaterialAmountTest> gasFamily = UnitFamily.matter(Namespace.ofMc("gas"));
    private static final Unit<MaterialAmountTest> gasUnit = Unit.of(gasFamily, Namespace.ofMc("gas_unit"));

    private UnitConverter converter() {
        return UnitConverter.builder().add(ItemUnits.pack()).add(FluidUnits.pack()).baseUnit(gasUnit)
                .convert(ItemUnits.ingot, FluidUnits.millibucket).forMaterial(iron).by(144)
                .convert(ItemUnits.ingot, gasUnit).forMaterial(iron).by(2).build();
    }

    @Test
    void sameFamilyNormalizesToBaseUnit() {
        MaterialAmount amount = converter().materialAmount(9, ItemUnits.nugget, iron);

        assertEquals(ItemUnits.ingot, amount.baseUnit());
        assertEquals(Ratio.of(1), amount.value());
        assertEquals(iron, amount.material());
    }

    @Test
    void crossFamilyEquivalenceIsThreeWay() {
        UnitConverter converter = converter();
        MaterialAmount ingot = converter.quantity(1, ItemUnits.ingot).toMaterialAmount(iron);
        MaterialAmount molten = converter.quantity(144, FluidUnits.millibucket).toMaterialAmount(iron);
        MaterialAmount gas = converter.quantity(2, gasUnit).toMaterialAmount(iron);

        assertTrue(ingot.equivalentTo(molten));
        assertTrue(molten.equivalentTo(gas));
        assertTrue(ingot.equivalentTo(gas));

        assertFalse(ingot.equivalentTo(converter.materialAmount(1, gasUnit, iron)));
    }

    @Test
    void differentMaterialsAreNeverEquivalent() {
        UnitConverter converter = converter();
        MaterialAmount ironAmount = converter.materialAmount(1, ItemUnits.ingot, iron);
        MaterialAmount copperAmount = converter.materialAmount(1, ItemUnits.ingot, copper);

        assertFalse(ironAmount.equivalentTo(copperAmount));
    }

    @Test
    void missingBridgeThrows() {
        UnitConverter converter = UnitConverter.builder().add(ItemUnits.pack()).baseUnit(gasUnit).build();
        MaterialAmount ingot = converter.materialAmount(1, ItemUnits.ingot, iron);
        MaterialAmount gas = converter.materialAmount(2, gasUnit, iron);

        assertThrows(NoConversionPathException.class, () -> ingot.equivalentTo(gas));
    }

    @Test
    void missingBaseUnitThrowsNamingFamily() {
        UnitConverter converter = UnitConverter.builder().rules(FluidUnits.rules).build();

        IllegalStateException e = assertThrows(
            IllegalStateException.class,
            () -> converter.quantity(1, FluidUnits.millibucket).toMaterialAmount(iron)
        );
        assertTrue(Objects.requireNonNull(e.getMessage()).contains("minecraft:fluid"));
    }

    @Test
    void toTargetActsAsConversionHub() {
        UnitConverter converter = converter();
        MaterialAmount amount = converter.quantity(2, ItemUnits.ingot).toMaterialAmount(iron);

        assertEquals(Ratio.of(18), amount.to(ItemUnits.nugget).value());
        assertEquals(Ratio.of(288), amount.to(FluidUnits.millibucket).value());
        assertEquals(Ratio.of(4), amount.to(gasUnit).value());
    }

    @Test
    void materialSpecificOverrideAppliesDuringNormalization() {
        UnitConverter converter = UnitConverter.builder().add(ItemUnits.pack())
                .convert(ItemUnits.ingot, ItemUnits.block).forMaterial(iron).by(1, 4).build();

        assertEquals(Ratio.of(4), converter.materialAmount(1, ItemUnits.block, iron).value());
        assertEquals(Ratio.of(9), converter.materialAmount(1, ItemUnits.block, copper).value());
    }

    @Test
    void inexactnessPropagatesIntoMaterialAmount() {
        UnitConverter converter = UnitConverter.builder().add(ItemUnits.pack()).add(FluidUnits.pack())
                .convert(ItemUnits.ingot, FluidUnits.millibucket).forMaterial(iron).byApproximate(144).build();

        MaterialAmount molten = converter.materialAmount(144, FluidUnits.millibucket, iron);
        assertTrue(molten.isExact());

        MaterialAmount back = converter.materialAmount(
            converter.convert(144, FluidUnits.millibucket, ItemUnits.ingot, iron).value(),
            ItemUnits.ingot,
            iron
        );
        assertFalse(back.isExact());
    }
}
