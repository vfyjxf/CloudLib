package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.unit.units.ItemUnits;
import dev.vfyjxf.cloudlib.api.unit.units.TimeUnits;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class QuantityTest {

    private final UnitConverter converter = UnitConverter.builder()
            .add(TimeUnits.pack())
            .add(ItemUnits.pack())
            .build();

    @Test
    void fluentConversionIsExact() {
        Quantity<TimeUnits> ticks = converter.quantity(2, TimeUnits.hour).to(TimeUnits.tick);

        assertEquals(Ratio.of(144000), ticks.value());
        assertEquals(TimeUnits.tick, ticks.unit());
        assertEquals(144000, ticks.toLongExact());
    }

    @Test
    void toKeepsFractionsExact() {
        Quantity<ItemUnits> blocks = converter.quantity(1, ItemUnits.ingot).to(ItemUnits.block);

        assertEquals(Ratio.of(1, 9), blocks.value());
    }

    @Test
    void addConvertsRightHandSideToLeftUnit() {
        Quantity<TimeUnits> sum = converter.quantity(1, TimeUnits.minute)
                .add(converter.quantity(30, TimeUnits.second));

        assertEquals(Ratio.of(3, 2), sum.value());
        assertEquals(TimeUnits.minute, sum.unit());
    }

    @Test
    void subtractConvertsRightHandSideToLeftUnit() {
        Quantity<TimeUnits> difference = converter.quantity(90, TimeUnits.second)
                .subtract(converter.quantity(1, TimeUnits.minute));

        assertEquals(Ratio.of(30), difference.value());
        assertEquals(TimeUnits.second, difference.unit());
    }

    @Test
    void multiplyAndDivideKeepTheUnit() {
        Quantity<TimeUnits> quantity = converter.quantity(2, TimeUnits.tick);

        assertEquals(Ratio.of(6), quantity.multiply(3).value());
        assertEquals(Ratio.of(6), quantity.multiply(Ratio.of(3)).value());
        assertEquals(Ratio.of(1), quantity.divide(2).value());
        assertEquals(Ratio.of(4), quantity.divide(Ratio.of(1, 2)).value());
        assertEquals(TimeUnits.tick, quantity.multiply(3).unit());
    }

    @Test
    void exits() {
        Quantity<ItemUnits> quantity = converter.quantity(Ratio.of(10, 9), ItemUnits.ingot);

        assertEquals(1, quantity.floor());
        assertEquals(10.0 / 9.0, quantity.toDouble(), 1e-9);
    }

    @Test
    void equalityIsStructural() {
        Quantity<TimeUnits> a = converter.quantity(2, TimeUnits.tick);
        Quantity<TimeUnits> b = converter.quantity(2, TimeUnits.tick);
        Quantity<TimeUnits> c = converter.quantity(3, TimeUnits.tick);
        Quantity<TimeUnits> d = converter.quantity(2, TimeUnits.second);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, d);
    }
}
