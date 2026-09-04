package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.unit.exception.InexactResultException;
import dev.vfyjxf.cloudlib.api.unit.units.ItemUnits;
import dev.vfyjxf.cloudlib.api.unit.units.TimeUnits;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscreteTest {

    private final UnitConverter converter = UnitConverter.builder()
            .add(TimeUnits.pack())
            .add(ItemUnits.pack())
            .build();

    @Test
    void toDiscreteSplitsIntoWholeUnitsPlusRemainder() {
        DiscreteResult<ItemUnits> result = converter.quantity(10, ItemUnits.ingot).toDiscrete(ItemUnits.block);

        assertEquals(1, result.amount());
        assertEquals(Ratio.of(1), result.remainder().value());
        assertEquals(ItemUnits.ingot, result.remainder().unit());
        assertFalse(result.exact());
    }

    @Test
    void toDiscreteReportsExactConversions() {
        DiscreteResult<ItemUnits> result = converter.quantity(9, ItemUnits.ingot).toDiscrete(ItemUnits.block);

        assertEquals(1, result.amount());
        assertEquals(Ratio.ZERO, result.remainder().value());
        assertTrue(result.exact());
    }

    @Test
    void toDiscreteHandlesFractionalSources() {
        DiscreteResult<ItemUnits> result = converter.quantity(Ratio.of(1, 2), ItemUnits.ingot)
                .toDiscrete(ItemUnits.nugget);

        assertEquals(4, result.amount());
        assertEquals(Ratio.of(1, 18), result.remainder().value());
        assertEquals(ItemUnits.ingot, result.remainder().unit());
        assertFalse(result.exact());
    }

    @Test
    void toDiscreteHandlesExactZero() {
        DiscreteResult<ItemUnits> result = converter.quantity(0, ItemUnits.ingot).toDiscrete(ItemUnits.block);

        assertEquals(0, result.amount());
        assertTrue(result.exact());
    }

    @Test
    void toLongExactIsStrict() {
        assertEquals(20, converter.quantity(1, TimeUnits.second).to(TimeUnits.tick).toLongExact());
        assertThrows(
                InexactResultException.class,
                () -> converter.quantity(1, ItemUnits.ingot).to(ItemUnits.block).toLongExact()
        );
    }
}
