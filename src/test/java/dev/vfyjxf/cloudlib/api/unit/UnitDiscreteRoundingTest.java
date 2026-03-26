package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.util.Namespace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UnitDiscreteRoundingTest {

    private static final MatterFamily<DiscreteFamily> FAMILY = MatterFamily.create(Namespace.ofMc("discrete"));
    private static final Unit<DiscreteFamily> FROM = Unit.create(FAMILY, Namespace.ofMc("from"));
    private static final Unit<DiscreteFamily> TO = Unit.create(FAMILY, Namespace.ofMc("to"));

    @Test
    void strictModeRejectsInexactResult() {
        ConversionSchema schema = ConversionSchema.create();
        schema.addFamilyRule(FamilyRule.create(FROM, TO, 0.5));
        UnitConverter rules = schema.build();

        assertThrows(
                InexactResultException.class,
                () -> rules.convertDiscrete(3L, FROM, TO, DiscreteRoundingMode.strict)
        );
    }

    @Test
    void floorWithRemainderModeReturnsRemainder() {
        ConversionSchema schema = ConversionSchema.create();
        schema.addFamilyRule(FamilyRule.create(FROM, TO, 0.5));
        UnitConverter rules = schema.build();

        DiscreteConversionResult result = rules.convertDiscrete(
                3L,
                FROM,
                TO,
                DiscreteRoundingMode.floorWithRemainder
        );

        assertEquals(1L, result.amount());
        assertEquals(0.5, result.remainder());
        assertEquals(false, result.exact());
    }

    @Test
    void approximateModeRoundsToNearest() {
        ConversionSchema schema = ConversionSchema.create();
        schema.addFamilyRule(FamilyRule.create(FROM, TO, 0.5));
        UnitConverter rules = schema.build();

        DiscreteConversionResult result = rules.convertDiscrete(
                3L,
                FROM,
                TO,
                DiscreteRoundingMode.approximate
        );

        assertEquals(2L, result.amount());
        assertEquals(-0.5, result.remainder());
        assertEquals(false, result.exact());
    }

    private static final class DiscreteFamily {
    }
}
