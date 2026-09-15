package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.unit.exception.RuleConflictException;
import dev.vfyjxf.cloudlib.api.unit.units.FluidUnits;
import dev.vfyjxf.cloudlib.api.unit.units.GtUnits;
import dev.vfyjxf.cloudlib.api.unit.units.ItemUnits;
import dev.vfyjxf.cloudlib.api.unit.units.TicUnits;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModConventionPacksTest {

    private static final Namespace IRON = Namespace.ofCommon("iron");

    private static UnitConverter ticConverter() {
        return UnitConverter.builder()
                .add(ItemUnits.pack())
                .add(FluidUnits.pack())
                .add(TicUnits.pack())
                .build();
    }

    private static UnitConverter gtConverter() {
        return UnitConverter.builder()
                .add(ItemUnits.pack())
                .add(FluidUnits.pack())
                .add(GtUnits.pack())
                .build();
    }

    @Test
    void ticSmelteryAmounts() {
        UnitConverter converter = ticConverter();

        assertEquals(Ratio.of(90), converter.convert(1, ItemUnits.ingot, FluidUnits.millibucket).value());
        assertEquals(Ratio.of(810), converter.convert(1, ItemUnits.block, FluidUnits.millibucket).value());
        assertEquals(Ratio.of(90), converter.convert(9, ItemUnits.nugget, FluidUnits.millibucket).value());
        assertEquals(Ratio.of(100), converter.convert(1, ItemUnits.gem, FluidUnits.millibucket).value());
    }

    @Test
    void ticReverseConversionIsExact() {
        UnitConverter converter = ticConverter();

        assertEquals(2, converter.convert(180, FluidUnits.millibucket, ItemUnits.ingot).toLongExact());
    }

    @Test
    void gtFluidAmounts() {
        UnitConverter converter = gtConverter();

        assertEquals(Ratio.of(144), converter.convert(1, ItemUnits.ingot, FluidUnits.millibucket).value());
        assertEquals(Ratio.of(1296), converter.convert(1, ItemUnits.block, FluidUnits.millibucket).value());
        assertEquals(Ratio.of(144), converter.convert(1, ItemUnits.dust, FluidUnits.millibucket).value());
        assertEquals(Ratio.of(36), converter.convert(1, ItemUnits.smallDust, FluidUnits.millibucket).value());
        assertEquals(Ratio.of(16), converter.convert(1, ItemUnits.tinyDust, FluidUnits.millibucket).value());
        assertEquals(Ratio.of(72), converter.convert(1, ItemUnits.rod, FluidUnits.millibucket).value());
        assertEquals(Ratio.of(576), converter.convert(1, ItemUnits.gear, FluidUnits.millibucket).value());
        assertEquals(Ratio.of(144), converter.convert(1, ItemUnits.plate, FluidUnits.millibucket).value());
    }

    @Test
    void gtLiterIsOneToOneWithMillibucket() {
        UnitConverter converter = gtConverter();

        assertEquals(Ratio.of(1), converter.convert(1, GtUnits.liter, FluidUnits.millibucket).value());
        assertEquals(Ratio.of(1), converter.convert(1, FluidUnits.millibucket, GtUnits.liter).value());
    }

    @Test
    void gtLiterRuleIsFixed() {
        UnitConverter.Builder builder = UnitConverter.builder().add(GtUnits.pack());

        assertThrows(
                RuleConflictException.class,
                () -> builder.convert(GtUnits.liter, FluidUnits.millibucket).by(2)
        );
        assertThrows(
                RuleConflictException.class,
                () -> builder.convert(FluidUnits.millibucket, GtUnits.liter).by(2)
        );
    }

    @Test
    void gtItemFormRules() {
        UnitConverter converter = gtConverter();

        assertEquals(4, converter.convert(1, ItemUnits.gear, ItemUnits.ingot).toLongExact());
        assertEquals(1, converter.convert(2, ItemUnits.rod, ItemUnits.ingot).toLongExact());
        assertEquals(1, converter.convert(1, ItemUnits.plate, ItemUnits.ingot).toLongExact());
    }

    @Test
    void dustSubdivisions() {
        UnitConverter converter = UnitConverter.builder().add(ItemUnits.pack()).build();

        assertEquals(4, converter.convert(1, ItemUnits.dust, ItemUnits.smallDust).toLongExact());
        assertEquals(9, converter.convert(1, ItemUnits.dust, ItemUnits.tinyDust).toLongExact());
    }

    @Test
    void ticAndGtPacksConflict() {
        assertThrows(
                RuleConflictException.class,
                () -> UnitConverter.builder().add(TicUnits.pack()).add(GtUnits.pack())
        );
        assertThrows(
                RuleConflictException.class,
                () -> UnitConverter.builder().add(GtUnits.pack()).add(TicUnits.pack())
        );
    }

    @Test
    void materialSpecificBridgeCoexistsWithFixedGenericBridge() {
        UnitConverter converter = UnitConverter.builder()
                .add(ItemUnits.pack())
                .add(FluidUnits.pack())
                .add(GtUnits.pack())
                .convert(ItemUnits.ingot, FluidUnits.millibucket).forMaterial(IRON).by(100)
                .build();

        assertEquals(Ratio.of(144), converter.convert(1, ItemUnits.ingot, FluidUnits.millibucket).value());
        assertEquals(Ratio.of(100), converter.convert(1, ItemUnits.ingot, FluidUnits.millibucket, IRON).value());
    }

    @Test
    void materialAmountsAreEquivalentAcrossFamiliesWithTic() {
        UnitConverter converter = ticConverter();

        MaterialAmount ingots = converter.quantity(2, ItemUnits.ingot).toMaterialAmount(IRON);
        MaterialAmount molten = converter.quantity(180, FluidUnits.millibucket).toMaterialAmount(IRON);

        assertTrue(ingots.equivalentTo(molten));
    }

    @Test
    void materialAmountsAreEquivalentAcrossFamiliesWithGt() {
        UnitConverter converter = gtConverter();

        MaterialAmount ingots = converter.quantity(2, ItemUnits.ingot).toMaterialAmount(IRON);
        MaterialAmount molten = converter.quantity(288, FluidUnits.millibucket).toMaterialAmount(IRON);

        assertTrue(ingots.equivalentTo(molten));
    }
}
