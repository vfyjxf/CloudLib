package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.util.Namespace;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CrossFamilyConversionTest {

    private static final MatterFamily<ItemKind> ITEM = MatterFamily.create(Namespace.ofMc("item"));
    private static final MatterFamily<FluidKind> FLUID = MatterFamily.create(Namespace.ofMc("fluid"));
    private static final MeasureFamily<TimeKind> TIME = MeasureFamily.create(Namespace.ofMc("time"));

    private static final Namespace INGOT = Namespace.ofMc("ingot");
    private static final Namespace MB = Namespace.ofMc("mb");
    private static final Namespace BUCKET = Namespace.ofMc("bucket");

    private static final Unit<ItemKind> ITEM_INGOT = Unit.create(ITEM, INGOT);
    private static final Unit<FluidKind> FLUID_MB = Unit.create(FLUID, MB);
    private static final Unit<ItemKind> ITEM_BUCKET = Unit.create(ITEM, BUCKET);

    private static final Namespace IRON = Namespace.ofMc("iron");
    private static final Namespace GOLD = Namespace.ofMc("gold");
    private static final Namespace WATER = Namespace.ofMc("water");

    // ===== Basic cross-family auto-resolve =====

    @Nested
    class AutoResolve {

        @Test
        void ingotToMoltenFluidWithMaterial() {
            // 1 iron ingot = 144mB molten iron (standard Tinkers' ratio)
            ConversionSchema schema = ConversionSchema.create();
            schema.addBridgeRule(BridgeRule.createFraction(
                    ITEM_INGOT, FLUID_MB, 144, 1, IRON
            ));
            UnitConverter rules = schema.build();

            assertEquals(144.0, rules.convertCross(1.0, ITEM_INGOT, FLUID_MB, IRON));
        }

        @Test
        void differentMaterialDifferentRatio() {
            ConversionSchema schema = ConversionSchema.create();
            schema.addBridgeRule(BridgeRule.createFraction(
                    ITEM_INGOT, FLUID_MB, 144, 1, IRON
            ));
            schema.addBridgeRule(BridgeRule.createFraction(
                    ITEM_INGOT, FLUID_MB, 200, 1, GOLD
            ));
            UnitConverter rules = schema.build();

            assertEquals(144.0, rules.convertCross(1.0, ITEM_INGOT, FLUID_MB, IRON));
            assertEquals(200.0, rules.convertCross(1.0, ITEM_INGOT, FLUID_MB, GOLD));
        }

        @Test
        void fallbackBridgeUsedWhenNoContextMatch() {
            // Default: 1 ingot = 144mB for any material
            // Override: 1 gold ingot = 200mB
            ConversionSchema schema = ConversionSchema.create();
            schema.addBridgeRule(BridgeRule.createFraction(ITEM_INGOT, FLUID_MB, 144, 1));
            schema.addBridgeRule(BridgeRule.createFraction(
                    ITEM_INGOT, FLUID_MB, 200, 1, GOLD
            ));
            UnitConverter rules = schema.build();

            // Gold uses specific rule
            assertEquals(200.0, rules.convertCross(1.0, ITEM_INGOT, FLUID_MB, GOLD));

            // Iron falls back to default
            assertEquals(144.0, rules.convertCross(1.0, ITEM_INGOT, FLUID_MB, IRON));

            // No material also falls back to default
            assertEquals(144.0, rules.convertCross(1.0, ITEM_INGOT, FLUID_MB));
        }

        @Test
        void noMatchingBridgeThrows() {
            ConversionSchema schema = ConversionSchema.create();
            schema.addBridgeRule(BridgeRule.createFraction(
                    ITEM_INGOT, FLUID_MB, 144, 1, IRON
            ));
            UnitConverter rules = schema.build();

            // No bridge for gold
            assertThrows(
                    CrossFamilyNotAllowedException.class,
                    () -> rules.convertCross(1.0, ITEM_INGOT, FLUID_MB, GOLD)
            );
        }

        @Test
        void waterBucketToFluid() {
            // 1 water bucket = 1000mB water
            ConversionSchema schema = ConversionSchema.create();
            schema.addBridgeRule(BridgeRule.createFraction(
                    ITEM_BUCKET, FLUID_MB, 1000, 1, WATER
            ));
            UnitConverter rules = schema.build();

            assertEquals(1000.0, rules.convertCross(1.0, ITEM_BUCKET, FLUID_MB, WATER));
        }
    }

    // ===== Auto-inverse =====

    @Nested
    class BridgeAutoInverse {

        @Test
        void autoInverseFluidToIngot() {
            ConversionSchema schema = ConversionSchema.create();
            schema.addBridgeRule(BridgeRule.createFraction(ITEM_INGOT, FLUID_MB, 144, 1));
            UnitConverter rules = schema.build();

            // Reverse: 144mB → 1 ingot
            assertEquals(1.0, rules.convertCross(144.0, FLUID_MB, ITEM_INGOT), 1e-9);
        }

        @Test
        void autoInverseExactDiscrete() {
            ConversionSchema schema = ConversionSchema.create();
            schema.addBridgeRule(BridgeRule.createFraction(ITEM_INGOT, FLUID_MB, 144, 1));
            UnitConverter rules = schema.build();

            // 144mB → 1 ingot (exact integer via ExactRatio inverse)
            assertEquals(1L, rules.convertCrossDiscrete(144L, FLUID_MB, ITEM_INGOT));
        }
    }

    // ===== canConvertCross =====

    @Nested
    class CanConvertCross {

        @Test
        void returnsTrueWhenBridgeExists() {
            ConversionSchema schema = ConversionSchema.create();
            schema.addBridgeRule(BridgeRule.create(ITEM_INGOT, FLUID_MB, 144.0));
            UnitConverter rules = schema.build();

            assertTrue(rules.canConvertCross(ITEM_INGOT, FLUID_MB));
        }

        @Test
        void returnsTrueForAutoInverse() {
            ConversionSchema schema = ConversionSchema.create();
            schema.addBridgeRule(BridgeRule.create(ITEM_INGOT, FLUID_MB, 144.0));
            UnitConverter rules = schema.build();

            assertTrue(rules.canConvertCross(FLUID_MB, ITEM_INGOT));
        }

        @Test
        void returnsFalseWhenNoBridge() {
            UnitConverter rules = ConversionSchema.create().build();

            assertFalse(rules.canConvertCross(ITEM_INGOT, FLUID_MB));
        }

        @Test
        void returnsFalseWhenContextDoesNotMatch() {
            ConversionSchema schema = ConversionSchema.create();
            schema.addBridgeRule(BridgeRule.createFraction(
                    ITEM_INGOT, FLUID_MB, 144, 1, IRON
            ));
            UnitConverter rules = schema.build();

            // Gold context doesn't match iron bridge
            assertFalse(rules.canConvertCross(ITEM_INGOT, FLUID_MB, GOLD));
        }
    }

    // ===== Discrete conversion =====

    @Nested
    class DiscreteConversion {

        @Test
        void exactFractionDiscrete() {
            ConversionSchema schema = ConversionSchema.create();
            schema.addBridgeRule(BridgeRule.createFraction(ITEM_INGOT, FLUID_MB, 144, 1));
            UnitConverter rules = schema.build();

            // 3 ingots = 432 mB (exact)
            assertEquals(432L, rules.convertCrossDiscrete(3L, ITEM_INGOT, FLUID_MB));
        }

        @Test
        void inexactDiscreteThrows() {
            ConversionSchema schema = ConversionSchema.create();
            schema.addBridgeRule(BridgeRule.create(ITEM_INGOT, FLUID_MB, 100.3));
            UnitConverter rules = schema.build();

            assertThrows(
                    InexactResultException.class,
                    () -> rules.convertCrossDiscrete(3L, ITEM_INGOT, FLUID_MB)
            );
        }
    }

    // ===== Explicit bridge still works =====

    @Nested
    class ExplicitBridge {

        @Test
        void explicitBridgeStillWorks() {
            ConversionSchema schema = ConversionSchema.create();
            BridgeRule<ItemKind, FluidKind> bridge = BridgeRule.create(ITEM_INGOT, FLUID_MB, 144.0);
            schema.addBridgeRule(bridge);
            UnitConverter rules = schema.build();

            assertEquals(288.0, rules.convertCross(2.0, ITEM_INGOT, bridge, FLUID_MB));
        }
    }

    private static final class ItemKind {
    }

    private static final class FluidKind {
    }

    private static final class TimeKind {
    }
}
