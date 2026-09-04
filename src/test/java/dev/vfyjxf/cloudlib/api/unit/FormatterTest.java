package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.unit.text.QuantityFormatter;
import dev.vfyjxf.cloudlib.api.unit.text.UnitNames;
import dev.vfyjxf.cloudlib.api.unit.units.EnergyUnits;
import dev.vfyjxf.cloudlib.api.unit.units.FluidUnits;
import dev.vfyjxf.cloudlib.api.unit.units.ItemUnits;
import dev.vfyjxf.cloudlib.api.unit.units.TimeUnits;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormatterTest {

    private static final Namespace IRON = Namespace.ofCommon("iron");

    private final UnitConverter converter = UnitConverter.builder()
            .add(TimeUnits.pack())
            .add(ItemUnits.pack())
            .build();

    @Test
    void defaultNamesDeriveFromUnitId() {
        UnitNames names = UnitNames.defaults();

        assertEquals("tick", names.name(TimeUnits.tick));
        assertEquals("millibucket", names.name(FluidUnits.millibucket));
    }

    @Test
    void customAndMaterialQualifiedNames() {
        UnitNames names = UnitNames.builder()
                .name(ItemUnits.dust, "pulverized dust")
                .name(IRON, ItemUnits.ingot, "iron ingot")
                .build();

        assertEquals("pulverized dust", names.name(ItemUnits.dust));
        assertEquals("iron ingot", names.name(ItemUnits.ingot, IRON));
        assertEquals("ingot", names.name(ItemUnits.ingot));
        assertEquals("ingot", names.name(ItemUnits.ingot, Namespace.ofCommon("copper")));
    }

    @Test
    void formatIntegralValuesPluralizes() {
        QuantityFormatter formatter = converter.formatter();

        assertEquals("20 ticks", formatter.format(converter.quantity(20, TimeUnits.tick)));
        assertEquals("1 tick", formatter.format(converter.quantity(1, TimeUnits.tick)));
        assertEquals("1 block", formatter.format(converter.quantity(1, ItemUnits.block)));
        assertEquals("20 ticks", converter.quantity(1, TimeUnits.second).to(TimeUnits.tick).format());
    }

    @Test
    void formatNonIntegralValuesAsFraction() {
        QuantityFormatter formatter = converter.formatter();

        assertEquals(
                "1/9 block",
                formatter.format(converter.quantity(1, ItemUnits.ingot).to(ItemUnits.block))
        );
    }

    @Test
    void formatDecimalUsesApproximationMarker() {
        QuantityFormatter formatter = converter.formatter();

        assertEquals(
                "≈0.111 block",
                formatter.formatDecimal(converter.quantity(1, ItemUnits.ingot).to(ItemUnits.block), 3)
        );
        assertEquals(
                "1 block",
                formatter.formatDecimal(converter.quantity(9, ItemUnits.ingot).to(ItemUnits.block), 3)
        );
    }

    @Test
    void decomposeBreaksDownAlongChain() {
        QuantityFormatter formatter = converter.formatter();

        List<Quantity<TimeUnits>> parts = formatter.decompose(
                converter.quantity(5000, TimeUnits.second),
                TimeUnits.hour, TimeUnits.minute, TimeUnits.second
        );

        assertEquals(3, parts.size());
        assertEquals(Quantity.of(Ratio.of(1), TimeUnits.hour, converter), parts.get(0));
        assertEquals(Quantity.of(Ratio.of(23), TimeUnits.minute, converter), parts.get(1));
        assertEquals(Quantity.of(Ratio.of(20), TimeUnits.second, converter), parts.get(2));
    }

    @Test
    void decomposeSkipsZeroElements() {
        QuantityFormatter formatter = converter.formatter();

        List<Quantity<TimeUnits>> parts = formatter.decompose(
                converter.quantity(60, TimeUnits.second),
                TimeUnits.hour, TimeUnits.minute, TimeUnits.second
        );

        assertEquals(1, parts.size());
        assertEquals(Quantity.of(Ratio.of(1), TimeUnits.minute, converter), parts.get(0));
    }

    @Test
    void formatDecomposedUsesShortNames() {
        QuantityFormatter formatter = converter.formatter();

        assertEquals(
                "1h 23m 20s",
                formatter.formatDecomposed(
                        converter.quantity(5000, TimeUnits.second),
                        TimeUnits.hour, TimeUnits.minute, TimeUnits.second
                )
        );
    }

    @Test
    void ratioTextDescribesExactRatio() {
        QuantityFormatter formatter = converter.formatter();

        assertEquals("1:9", formatter.ratioText(ItemUnits.ingot, ItemUnits.block));
        assertEquals("20:1", formatter.ratioText(TimeUnits.second, TimeUnits.tick));
        assertEquals("1:20", formatter.ratioText(TimeUnits.tick, TimeUnits.second));
    }

    @Test
    void formatPrefixesInexactValuesWithApproxMarker() {
        UnitConverter lossy = UnitConverter.builder()
                .convert(EnergyUnits.eu, EnergyUnits.fe).byApproximate(3.5)
                .build();
        QuantityFormatter formatter = lossy.formatter();

        assertEquals("≈7 fes", formatter.format(lossy.convert(2, EnergyUnits.eu, EnergyUnits.fe)));
        assertEquals(
                "≈1/9 block",
                converter.formatter().format(
                        Quantity.of(Ratio.approximate(1, 9), ItemUnits.block, converter)
                )
        );
        assertEquals("20 ticks", converter.formatter().format(converter.quantity(20, TimeUnits.tick)));
    }

    @Test
    void formatMaterialAmountUsesMaterialQualifiedName() {
        UnitConverter withBase = UnitConverter.builder()
                .add(ItemUnits.pack())
                .build();
        UnitNames names = UnitNames.builder()
                .name(IRON, ItemUnits.ingot, "iron ingot")
                .build();
        QuantityFormatter formatter = new QuantityFormatter(withBase, names);

        MaterialAmount amount = withBase.materialAmount(18, ItemUnits.nugget, IRON);
        assertEquals("2 iron ingots", formatter.format(amount));
        assertEquals("2 ingots", withBase.formatter().format(amount));
    }
}
