package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.util.Namespace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UnitValidationAndDiscreteTest {

    private static final MatterFamily<AlphaFamily> ALPHA = MatterFamily.create(Namespace.ofMc("alpha"));

    private static final Unit<AlphaFamily> A1 = Unit.create(ALPHA, Namespace.ofMc("a1"));
    private static final Unit<AlphaFamily> A2 = Unit.create(ALPHA, Namespace.ofMc("a2"));

    private static final MeasureFamily<TimeFamily> TIME = MeasureFamily.create(Units.familyTime);
    private static final Unit<TimeFamily> TICK = Unit.create(TIME, Units.unitTick);
    private static final Unit<TimeFamily> SECOND = Unit.create(TIME, Units.unitSecond);

    @Test
    void familyRuleRejectsNonPositiveRatio() {
        assertThrows(InvalidRuleException.class, () -> FamilyRule.create(A1, A2, 0.0));
    }

    @SuppressWarnings("unchecked")
    @Test
    void familyRuleRejectsCrossFamilyUnits() {
        assertThrows(InvalidRuleException.class, () -> FamilyRule.create(A1, (Unit) TICK, 1.0));
    }

    @Test
    void bridgeRuleRejectsNonPositiveRatio() {
        assertThrows(InvalidRuleException.class, () -> BridgeRule.create(A1, TICK, 0.0));
    }

    @Test
    void timeFixedConstantRuleCannotBeOverriddenWithDifferentRatio() {
        ConversionSchema schema = ConversionSchema.create();
        schema.addFamilyRule(FamilyRule.create(SECOND, TICK, 30.0));

        assertThrows(InvalidRuleException.class, schema::build);
    }

    @Test
    void convertDiscreteReturnsExactIntegerResult() {
        ConversionSchema schema = ConversionSchema.create();
        schema.addFamilyRule(FamilyRule.create(A1, A2, 2.0));
        UnitConverter rules = schema.build();

        assertEquals(6L, rules.convertDiscrete(3L, A1, A2));
    }

    @Test
    void convertDiscreteThrowsWhenResultIsInexact() {
        ConversionSchema schema = ConversionSchema.create();
        schema.addFamilyRule(FamilyRule.create(A1, A2, 0.5));
        UnitConverter rules = schema.build();

        assertThrows(
                InexactResultException.class,
                () -> rules.convertDiscrete(3L, A1, A2)
        );
    }

    private static final class AlphaFamily {
    }

    private static final class TimeFamily {
    }
}
