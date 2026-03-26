package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.unit.conversion.*;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UnitImprovementsTest {

    private static final Namespace INGOT = Namespace.ofMc("ingot");
    private static final Namespace BLOCK = Namespace.ofMc("block");
    private static final Namespace NUGGET = Namespace.ofMc("nugget");

    private static final MatterFamily<AlphaFamily> ALPHA = MatterFamily.create(Namespace.ofMc("alpha"));
    private static final Unit<AlphaFamily> A1 = Unit.create(ALPHA, Namespace.ofMc("a1"));
    private static final Unit<AlphaFamily> A2 = Unit.create(ALPHA, Namespace.ofMc("a2"));
    private static final Unit<AlphaFamily> A3 = Unit.create(ALPHA, Namespace.ofMc("a3"));

    private static final MatterFamily<BetaFamily> BETA = MatterFamily.create(Namespace.ofMc("beta"));
    private static final Unit<BetaFamily> B1 = Unit.create(BETA, Namespace.ofMc("b1"));

    private static final MeasureFamily<TimeFamily> TIME = MeasureFamily.create(Units.familyTime);
    private static final Unit<TimeFamily> TICK = Unit.create(TIME, Units.unitTick);
    private static final Unit<TimeFamily> SECOND = Unit.create(TIME, Units.unitSecond);
    private static final Unit<TimeFamily> MINUTE = Unit.create(TIME, Units.unitMinute);

    private static final MeasureFamily<EnergyFamily> ENERGY = MeasureFamily.create(Units.familyEnergy);
    private static final Unit<EnergyFamily> FE = Unit.create(ENERGY, Units.unitFe);
    private static final Unit<EnergyFamily> EU = Unit.create(ENERGY, Units.unitEu);

    // ===== Auto-inverse tests =====

    @Nested
    class AutoInverse {

        @Test
        void familyRuleAutoInverse() {
            ConversionSchema schema = ConversionSchema.create();
            schema.addFamilyRule(FamilyRule.create(A1, A2, 4.0));
            UnitConverter rules = schema.build();

            assertEquals(4.0, rules.convert(1.0, A1, A2));
            assertEquals(0.25, rules.convert(1.0, A2, A1), 1e-9);
        }

        @Test
        void builtInTimeAutoInverse() {
            UnitConverter rules = ConversionSchema.create().build();

            // Forward: second → tick = 20
            assertEquals(20.0, rules.convert(1.0, SECOND, TICK));
            // Auto-inverse: tick → second = 1/20
            assertEquals(0.05, rules.convert(1.0, TICK, SECOND), 1e-9);
        }

        @Test
        void matterRuleAutoInverse() {
            ConversionSchema schema = ConversionSchema.create();
            schema.addFallbackRule(FallbackRule.create(INGOT, BLOCK, 9.0));
            UnitConverter rules = schema.build();

            Unit<AlphaFamily> ingotUnit = Unit.create(ALPHA, INGOT);
            Unit<AlphaFamily> blockUnit = Unit.create(ALPHA, BLOCK);

            assertEquals(9.0, rules.convert(1.0, ingotUnit, blockUnit), 1e-9);
            // Auto-inverse: block → ingot = 1/9
            assertEquals(1.0 / 9.0, rules.convert(1.0, blockUnit, ingotUnit), 1e-9);
        }
    }

    // ===== canConvert tests =====

    @Nested
    class CanConvert {

        @Test
        void canConvertReturnsTrueForSameUnit() {
            UnitConverter rules = ConversionSchema.create().build();
            assertTrue(rules.canConvert(A1, A1));
        }

        @Test
        void canConvertReturnsTrueForKnownRule() {
            ConversionSchema schema = ConversionSchema.create();
            schema.addFamilyRule(FamilyRule.create(A1, A2, 2.0));
            UnitConverter rules = schema.build();

            assertTrue(rules.canConvert(A1, A2));
            // Auto-inverse should also be resolvable
            assertTrue(rules.canConvert(A2, A1));
        }

        @Test
        void canConvertReturnsFalseForUnknownPair() {
            UnitConverter rules = ConversionSchema.create().build();
            assertFalse(rules.canConvert(A1, A2));
        }
    }

    // ===== ConversionMode tests =====

    @Nested
    class ConversionModeTests {

        @Test
        void exactOnlySucceedsWithExactRatio() {
            ConversionSchema schema = ConversionSchema.create();
            schema.addFamilyRule(FamilyRule.createFraction(A1, A2, 9, 1));
            UnitConverter rules = schema.build();

            assertEquals(9.0, rules.convert(1.0, A1, A2, ConversionMode.exactOnly));
        }

        @Test
        void exactOnlyThrowsForApproximateRatio() {
            ConversionSchema schema = ConversionSchema.create();
            schema.addFamilyRule(FamilyRule.create(A1, A2, 3.14));
            UnitConverter rules = schema.build();

            assertThrows(
                    InexactResultException.class,
                    () -> rules.convert(1.0, A1, A2, ConversionMode.exactOnly)
            );
        }

        @Test
        void approximateOnlyUsesDoubleRatio() {
            ConversionSchema schema = ConversionSchema.create();
            schema.addFamilyRule(FamilyRule.createFraction(A1, A2, 3, 1));
            UnitConverter rules = schema.build();

            assertEquals(6.0, rules.convert(2.0, A1, A2, ConversionMode.approximateOnly));
        }
    }

    // ===== ExactRatio integration tests =====

    @Nested
    class ExactRatioIntegration {

        @Test
        void exactFractionDiscreteConversion() {
            ConversionSchema schema = ConversionSchema.create();
            schema.addFamilyRule(FamilyRule.createFraction(A1, A2, 9, 1));
            UnitConverter rules = schema.build();

            // 3 * 9/1 = 27 (exact)
            assertEquals(27L, rules.convertDiscrete(3L, A1, A2));
        }

        @Test
        void exactFractionWithInverse() {
            ConversionSchema schema = ConversionSchema.create();
            schema.addFamilyRule(FamilyRule.createFraction(A1, A2, 9, 1));
            UnitConverter rules = schema.build();

            // Auto-inverse: A2→A1 = 1/9, so 9 * 1/9 = 1 (exact integer)
            assertEquals(1L, rules.convertDiscrete(9L, A2, A1));
        }

        @Test
        void exactFractionInexactThrows() {
            ConversionSchema schema = ConversionSchema.create();
            schema.addFamilyRule(FamilyRule.createFraction(A1, A2, 9, 1));
            UnitConverter rules = schema.build();

            // 2 * 1/9 is not an integer
            assertThrows(
                    InexactResultException.class,
                    () -> rules.convertDiscrete(2L, A2, A1)
            );
        }

        @Test
        void matterFallbackFractionExactPath() {
            ConversionSchema schema = ConversionSchema.create();
            schema.addFallbackRule(FallbackRule.createFraction(INGOT, NUGGET, 1, 9));
            UnitConverter rules = schema.build();

            Unit<AlphaFamily> ingotUnit = Unit.create(ALPHA, INGOT);
            Unit<AlphaFamily> nuggetUnit = Unit.create(ALPHA, NUGGET);

            // 9 ingots * (1/9) = 1 nugget (exact)
            assertEquals(1L, rules.convertDiscrete(9L, ingotUnit, nuggetUnit));
        }
    }

    // ===== Default rule tests =====

    @Nested
    class DefaultRuleTests {

        @Test
        void clearDefaultsDisablesTimeAndEnergy() {
            ConversionSchema schema = ConversionSchema.create();
            schema.clearDefaults();
            UnitConverter rules = schema.build();

            // Built-in time should no longer work
            assertFalse(rules.canConvert(SECOND, TICK));
            // Built-in energy should no longer work
            assertFalse(rules.canConvert(EU, FE));
        }

        @Test
        void overridableDefaultCanBeOverridden() {
            // Energy EU→FE is overridable (default ratio 4.0)
            ConversionSchema schema = ConversionSchema.create();
            schema.addFamilyRule(FamilyRule.create(EU, FE, 8.0));
            UnitConverter rules = schema.build();

            // Family rule should take priority over default
            assertEquals(8.0, rules.convert(1.0, EU, FE));
        }

        @Test
        void fixedDefaultCannotBeOverriddenWithDifferentRatio() {
            // Time SECOND→TICK is fixed (ratio 20/1)
            ConversionSchema schema = ConversionSchema.create();
            schema.addFamilyRule(FamilyRule.create(SECOND, TICK, 30.0));

            assertThrows(InvalidRuleException.class, schema::build);
        }

        @Test
        void fixedDefaultCanBeReconfirmedWithSameRatio() {
            // Time SECOND→TICK is fixed at 20 — confirming with matching ratio should be fine
            ConversionSchema schema = ConversionSchema.create();
            schema.addFamilyRule(FamilyRule.create(SECOND, TICK, 20.0));
            UnitConverter rules = schema.build();

            assertEquals(20.0, rules.convert(1.0, SECOND, TICK));
        }
    }

    // ===== ConvertContext.with() tests =====

    @Nested
    class ConvertContextWithTests {

        @Test
        void withCreatesNewContextWithAddedKey() {
            ConvertContext ctx = ConvertContext.empty();
            ContextKey<String> key = ContextKey.create(Namespace.ofMc("test_key"), String.class);
            ConvertContext derived = ctx.with(key, "hello");

            assertEquals("hello", derived.find(key).orElse(null));
            // Original is not modified
            assertTrue(ctx.find(key).isEmpty());
        }

        @Test
        void withOverridesExistingKey() {
            ContextKey<String> key = ContextKey.create(Namespace.ofMc("test_key"), String.class);
            ConvertContext ctx1 = ConvertContext.empty().with(key, "first");
            ConvertContext ctx2 = ctx1.with(key, "second");

            assertEquals("second", ctx2.find(key).orElse(null));
            assertEquals("first", ctx1.find(key).orElse(null));
        }
    }

    // ===== Transitive derivation with auto-inverse =====

    @Nested
    class TransitiveDerivedTests {

        @Test
        void builtInTimeDerivedMinuteToTick() {
            // Built-in: SECOND→TICK = 20, MINUTE→SECOND = 60
            // Derived: MINUTE→TICK = 60 * 20 = 1200
            UnitConverter rules = ConversionSchema.create().build();

            assertEquals(1200.0, rules.convert(1.0, MINUTE, TICK), 1e-9);
        }

        @Test
        void builtInTimeReverseDerivedTickToMinute() {
            // tick → minute should resolve somehow (either builtIn auto-inverse or derived)
            UnitConverter rules = ConversionSchema.create().build();

            assertEquals(1.0 / 1200.0, rules.convert(1.0, TICK, MINUTE), 1e-9);
        }
    }

    // ===== Batch add tests =====

    @Nested
    class BatchAddTests {

        @Test
        void batchAddFamilyRulesWorks() {
            ConversionSchema schema = ConversionSchema.create();
            schema.addFamilyRules(java.util.List.of(
                    FamilyRule.create(A1, A2, 2.0),
                    FamilyRule.create(A1, A3, 5.0)
            ));
            UnitConverter rules = schema.build();

            assertEquals(2.0, rules.convert(1.0, A1, A2));
            assertEquals(5.0, rules.convert(1.0, A1, A3));
        }
    }

    // ===== ExactRatio unit tests =====

    @Nested
    class ExactRatioTests {

        @Test
        void inverseWorks() {
            ExactRatio r = ExactRatio.create(3, 7);
            ExactRatio inv = r.inverse();
            assertEquals(7, inv.numerator().longValue());
            assertEquals(3, inv.denominator().longValue());
        }

        @Test
        void multiplyWorks() {
            ExactRatio a = ExactRatio.create(2, 3);
            ExactRatio b = ExactRatio.create(3, 5);
            ExactRatio result = a.multiply(b);
            assertEquals(2, result.numerator().longValue());
            assertEquals(5, result.denominator().longValue());
        }

        @Test
        void divideWorks() {
            ExactRatio a = ExactRatio.create(4, 3);
            ExactRatio b = ExactRatio.create(2, 3);
            ExactRatio result = a.divide(b);
            assertEquals(2, result.numerator().longValue());
            assertEquals(1, result.denominator().longValue());
        }

        @Test
        void applyExactReturnsNullForNonInteger() {
            ExactRatio r = ExactRatio.create(1, 3);
            assertNull(r.applyExact(2));
        }

        @Test
        void applyExactReturnsResultForExactDivision() {
            ExactRatio r = ExactRatio.create(1, 3);
            assertEquals(1L, r.applyExact(3));
        }

        @Test
        void toStringFormats() {
            assertEquals("2/3", ExactRatio.create(2, 3).toString());
            assertEquals("5", ExactRatio.create(5, 1).toString());
        }
    }

    private static final class AlphaFamily {
    }

    private static final class BetaFamily {
    }

    private static final class TimeFamily {
    }

    private static final class EnergyFamily {
    }
}
