package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.unit.exception.RuleConflictException;
import dev.vfyjxf.cloudlib.api.unit.units.EnergyUnits;
import dev.vfyjxf.cloudlib.api.unit.units.FluidUnits;
import dev.vfyjxf.cloudlib.api.unit.units.ItemUnits;
import dev.vfyjxf.cloudlib.api.unit.units.TimeUnits;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PredefinedUnitsTest {

    @Test
    void timeUnits() {
        assertEquals(Namespace.ofMc("time"), TimeUnits.family.id());
        assertEquals(UnitFamily.Kind.measure, TimeUnits.family.kind());
        assertEquals(Namespace.ofMc("tick"), TimeUnits.tick.id());
        assertEquals(Namespace.ofMc("second"), TimeUnits.second.id());
        assertEquals(Namespace.ofMc("minute"), TimeUnits.minute.id());
        assertEquals(Namespace.ofMc("hour"), TimeUnits.hour.id());
        assertEquals(TimeUnits.family, TimeUnits.tick.family());

        UnitConverter converter = UnitConverter.builder().add(TimeUnits.pack()).build();
        assertEquals(Ratio.of(72000), converter.convert(1, TimeUnits.hour, TimeUnits.tick).value());
        assertEquals(Ratio.of(1200), converter.convert(1, TimeUnits.minute, TimeUnits.tick).value());
        assertEquals(Ratio.of(20), converter.convert(1, TimeUnits.second, TimeUnits.tick).value());
    }

    @Test
    void timeRulesAreFixed() {
        UnitConverter.Builder builder = UnitConverter.builder().add(TimeUnits.pack());
        assertThrows(
                RuleConflictException.class,
                () -> builder.convert(TimeUnits.hour, TimeUnits.minute).by(30)
        );
        assertThrows(
                RuleConflictException.class,
                () -> builder.convert(TimeUnits.second, TimeUnits.tick).fixed().by(20)
        );
    }

    @Test
    void energyUnits() {
        assertEquals(Namespace.ofMc("energy"), EnergyUnits.family.id());
        assertEquals(UnitFamily.Kind.measure, EnergyUnits.family.kind());
        assertEquals(Namespace.ofMc("eu"), EnergyUnits.eu.id());
        assertEquals(Namespace.ofMc("fe"), EnergyUnits.fe.id());

        UnitConverter converter = UnitConverter.builder().add(EnergyUnits.pack()).build();
        assertEquals(Ratio.of(4), converter.convert(1, EnergyUnits.eu, EnergyUnits.fe).value());
        assertEquals(Ratio.of(1, 4), converter.convert(1, EnergyUnits.fe, EnergyUnits.eu).value());
    }

    @Test
    void energyRuleIsOverridable() {
        UnitConverter converter = UnitConverter.builder()
                .add(EnergyUnits.pack())
                .convert(EnergyUnits.eu, EnergyUnits.fe).by(10)
                .build();

        assertEquals(Ratio.of(10), converter.convert(1, EnergyUnits.eu, EnergyUnits.fe).value());
    }

    @Test
    void itemUnits() {
        assertEquals(Namespace.ofMc("item"), ItemUnits.family.id());
        assertEquals(UnitFamily.Kind.matter, ItemUnits.family.kind());
        assertEquals(Namespace.ofMc("ingot"), ItemUnits.ingot.id());
        assertEquals(Namespace.ofMc("block"), ItemUnits.block.id());
        assertEquals(Namespace.ofMc("nugget"), ItemUnits.nugget.id());
        assertEquals(Namespace.ofMc("dust"), ItemUnits.dust.id());
        assertEquals(Namespace.ofMc("small_dust"), ItemUnits.smallDust.id());
        assertEquals(Namespace.ofMc("tiny_dust"), ItemUnits.tinyDust.id());
        assertEquals(Namespace.ofMc("gem"), ItemUnits.gem.id());
        assertEquals(Namespace.ofMc("plate"), ItemUnits.plate.id());
        assertEquals(Namespace.ofMc("gear"), ItemUnits.gear.id());
        assertEquals(Namespace.ofMc("rod"), ItemUnits.rod.id());

        UnitConverter converter = UnitConverter.builder().add(ItemUnits.pack()).build();
        assertEquals(Ratio.of(9), converter.convert(1, ItemUnits.ingot, ItemUnits.nugget).value());
        assertEquals(Ratio.of(81), converter.convert(1, ItemUnits.block, ItemUnits.nugget).value());
        assertEquals(Ratio.of(1, 9), converter.convert(1, ItemUnits.ingot, ItemUnits.block).value());
        assertEquals(Ratio.of(4), converter.convert(1, ItemUnits.dust, ItemUnits.smallDust).value());
        assertEquals(Ratio.of(9), converter.convert(1, ItemUnits.dust, ItemUnits.tinyDust).value());
    }

    @Test
    void rulesExposeUnmodifiableJavaListView() {
        assertEquals(TimeUnits.rules.castToList(), TimeUnits.rulesAsList);
        assertEquals(3, TimeUnits.rulesAsList.size());
        assertThrows(UnsupportedOperationException.class, () -> TimeUnits.rulesAsList.clear());

        UnitPack pack = ItemUnits.pack();
        assertEquals(pack.rules().castToList(), pack.rulesAsList());
        assertThrows(UnsupportedOperationException.class, () -> pack.rulesAsList().clear());
    }

    @Test
    void fluidUnits() {
        assertEquals(Namespace.ofMc("fluid"), FluidUnits.family.id());
        assertEquals(UnitFamily.Kind.matter, FluidUnits.family.kind());
        assertEquals(Namespace.ofMc("millibucket"), FluidUnits.millibucket.id());
        assertEquals(Namespace.ofMc("bucket"), FluidUnits.bucket.id());
        assertEquals(Namespace.ofMc("droplet"), FluidUnits.droplet.id());

        UnitConverter converter = UnitConverter.builder().add(FluidUnits.pack()).build();
        assertEquals(Ratio.of(1000), converter.convert(1, FluidUnits.bucket, FluidUnits.millibucket).value());
        assertEquals(Ratio.of(1, 1000), converter.convert(1, FluidUnits.millibucket, FluidUnits.bucket).value());
    }
}
