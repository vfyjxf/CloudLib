package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.unit.exception.NoConversionPathException;
import dev.vfyjxf.cloudlib.api.unit.units.FluidUnits;
import dev.vfyjxf.cloudlib.api.unit.units.ItemUnits;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialAmountTest {

    private static final Namespace IRON = Namespace.ofCommon("iron");
    private static final Namespace COPPER = Namespace.ofCommon("copper");

    private static final UnitFamily<MaterialAmountTest> GAS_FAMILY = UnitFamily.matter(Namespace.ofMc("gas"));
    private static final Unit<MaterialAmountTest> GAS_UNIT = Unit.of(GAS_FAMILY, Namespace.ofMc("gas_unit"));

    private UnitConverter converter() {
        return UnitConverter.builder()
                .add(ItemUnits.pack())
                .add(FluidUnits.pack())
                .baseUnit(GAS_UNIT)
                .convert(ItemUnits.ingot, FluidUnits.millibucket).forMaterial(IRON).by(144)
                .convert(ItemUnits.ingot, GAS_UNIT).forMaterial(IRON).by(2)
                .build();
    }

    @Test
    void sameFamilyNormalizesToBaseUnit() {
        MaterialAmount amount = converter().materialAmount(9, ItemUnits.nugget, IRON);

        assertEquals(ItemUnits.ingot, amount.baseUnit());
        assertEquals(Ratio.of(1), amount.value());
        assertEquals(IRON, amount.material());
    }

    @Test
    void crossFamilyEquivalenceIsThreeWay() {
        UnitConverter converter = converter();
        MaterialAmount ingot = converter.quantity(1, ItemUnits.ingot).toMaterialAmount(IRON);
        MaterialAmount molten = converter.quantity(144, FluidUnits.millibucket).toMaterialAmount(IRON);
        MaterialAmount gas = converter.quantity(2, GAS_UNIT).toMaterialAmount(IRON);

        assertTrue(ingot.equivalentTo(molten));
        assertTrue(molten.equivalentTo(gas));
        assertTrue(ingot.equivalentTo(gas));

        assertFalse(ingot.equivalentTo(converter.materialAmount(1, GAS_UNIT, IRON)));
    }

    @Test
    void differentMaterialsAreNeverEquivalent() {
        UnitConverter converter = converter();
        MaterialAmount iron = converter.materialAmount(1, ItemUnits.ingot, IRON);
        MaterialAmount copper = converter.materialAmount(1, ItemUnits.ingot, COPPER);

        assertFalse(iron.equivalentTo(copper));
    }

    @Test
    void missingBridgeThrows() {
        UnitConverter converter = UnitConverter.builder()
                .add(ItemUnits.pack())
                .baseUnit(GAS_UNIT)
                .build();
        MaterialAmount ingot = converter.materialAmount(1, ItemUnits.ingot, IRON);
        MaterialAmount gas = converter.materialAmount(2, GAS_UNIT, IRON);

        assertThrows(NoConversionPathException.class, () -> ingot.equivalentTo(gas));
    }

    @Test
    void missingBaseUnitThrowsNamingFamily() {
        UnitConverter converter = UnitConverter.builder()
                .rules(FluidUnits.rules)
                .build();

        IllegalStateException e = assertThrows(
                IllegalStateException.class,
                () -> converter.quantity(1, FluidUnits.millibucket).toMaterialAmount(IRON)
        );
        assertTrue(e.getMessage().contains("minecraft:fluid"));
    }

    @Test
    void toTargetActsAsConversionHub() {
        UnitConverter converter = converter();
        MaterialAmount amount = converter.quantity(2, ItemUnits.ingot).toMaterialAmount(IRON);

        assertEquals(Ratio.of(18), amount.to(ItemUnits.nugget).value());
        assertEquals(Ratio.of(288), amount.to(FluidUnits.millibucket).value());
        assertEquals(Ratio.of(4), amount.to(GAS_UNIT).value());
    }

    @Test
    void materialSpecificOverrideAppliesDuringNormalization() {
        UnitConverter converter = UnitConverter.builder()
                .add(ItemUnits.pack())
                .convert(ItemUnits.ingot, ItemUnits.block).forMaterial(IRON).by(1, 4)
                .build();

        assertEquals(Ratio.of(4), converter.materialAmount(1, ItemUnits.block, IRON).value());
        assertEquals(Ratio.of(9), converter.materialAmount(1, ItemUnits.block, COPPER).value());
    }

    @Test
    void inexactnessPropagatesIntoMaterialAmount() {
        UnitConverter converter = UnitConverter.builder()
                .add(ItemUnits.pack())
                .add(FluidUnits.pack())
                .convert(ItemUnits.ingot, FluidUnits.millibucket).forMaterial(IRON).byApproximate(144)
                .build();

        MaterialAmount molten = converter.materialAmount(144, FluidUnits.millibucket, IRON);
        assertTrue(molten.isExact());

        MaterialAmount back = converter.materialAmount(
                converter.convert(144, FluidUnits.millibucket, ItemUnits.ingot, IRON).value(),
                ItemUnits.ingot, IRON
        );
        assertFalse(back.isExact());
    }
}
