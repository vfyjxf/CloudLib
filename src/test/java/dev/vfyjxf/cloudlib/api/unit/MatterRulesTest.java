package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.unit.exception.RuleConflictException;
import dev.vfyjxf.cloudlib.api.unit.units.ItemUnits;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MatterRulesTest {

    private static final Namespace IRON = Namespace.ofCommon("iron");
    private static final Namespace COPPER = Namespace.ofCommon("copper");

    @Test
    void defaultMatterRuleAppliesWithoutMaterial() {
        UnitConverter converter = UnitConverter.builder()
                .add(ItemUnits.pack())
                .build();

        assertEquals(Ratio.of(9), converter.convert(1, ItemUnits.ingot, ItemUnits.nugget).value());
    }

    @Test
    void defaultMatterRuleAppliesToUnknownMaterial() {
        UnitConverter converter = UnitConverter.builder()
                .add(ItemUnits.pack())
                .build();

        assertEquals(
                Ratio.of(9),
                converter.convert(1, ItemUnits.ingot, ItemUnits.nugget, COPPER).value()
        );
    }

    @Test
    void materialOverrideBeatsDefault() {
        UnitConverter converter = UnitConverter.builder()
                .add(ItemUnits.pack())
                .convert(ItemUnits.ingot, ItemUnits.block).forMaterial(IRON).by(1, 4)
                .build();

        assertEquals(
                Ratio.of(1, 9),
                converter.convert(1, ItemUnits.ingot, ItemUnits.block).value()
        );
        assertEquals(
                Ratio.of(1, 4),
                converter.convert(1, ItemUnits.ingot, ItemUnits.block, IRON).value()
        );
    }

    @Test
    void materialOverrideAppliesInReverse() {
        UnitConverter converter = UnitConverter.builder()
                .add(ItemUnits.pack())
                .convert(ItemUnits.ingot, ItemUnits.block).forMaterial(IRON).by(1, 4)
                .build();

        assertEquals(
                Ratio.of(4),
                converter.convert(1, ItemUnits.block, ItemUnits.ingot, IRON).value()
        );
    }

    @Test
    void duplicateDefaultMatterRuleConflicts() {
        UnitConverter.Builder builder = UnitConverter.builder().add(ItemUnits.pack());
        assertThrows(
                RuleConflictException.class,
                () -> builder.convert(ItemUnits.ingot, ItemUnits.nugget).by(9)
        );
    }

    @Test
    void duplicateMaterialRuleConflicts() {
        UnitConverter.Builder builder = UnitConverter.builder()
                .convert(ItemUnits.ingot, ItemUnits.block).forMaterial(IRON).by(1, 4);
        assertThrows(
                RuleConflictException.class,
                () -> builder.convert(ItemUnits.ingot, ItemUnits.block).forMaterial(IRON).by(1, 4)
        );
    }

    @Test
    void differentMaterialsDoNotConflict() {
        UnitConverter converter = UnitConverter.builder()
                .add(ItemUnits.pack())
                .convert(ItemUnits.ingot, ItemUnits.block).forMaterial(IRON).by(1, 4)
                .convert(ItemUnits.ingot, ItemUnits.block).forMaterial(COPPER).by(1, 9)
                .build();

        assertEquals(Ratio.of(1, 4), converter.convert(1, ItemUnits.ingot, ItemUnits.block, IRON).value());
        assertEquals(Ratio.of(1, 9), converter.convert(1, ItemUnits.ingot, ItemUnits.block, COPPER).value());
    }
}
