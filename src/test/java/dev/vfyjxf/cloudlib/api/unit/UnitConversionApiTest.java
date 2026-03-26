package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.util.Namespace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UnitConversionApiTest {

    private static final MeasureFamily<TimeFamily> TIME = MeasureFamily.create(Namespace.ofMc("time"));
    private static final Unit<TimeFamily> TICK = Unit.create(TIME, Namespace.ofMc("tick"));
    private static final Unit<TimeFamily> SECOND = Unit.create(TIME, Namespace.ofMc("second"));
    private static final Unit<TimeFamily> MINUTE = Unit.create(TIME, Namespace.ofMc("minute"));

    private static final MeasureFamily<EnergyFamily> ENERGY = MeasureFamily.create(Namespace.ofMc("energy"));
    private static final Unit<EnergyFamily> FE = Unit.create(ENERGY, Namespace.ofMc("fe"));
    private static final Unit<EnergyFamily> EU = Unit.create(ENERGY, Namespace.ofMc("eu"));

    private static final MatterFamily<ChemicalFamily> CHEMICAL = MatterFamily.create(Namespace.ofMc("chemical"));
    private static final Unit<ChemicalFamily> MB_CHEM = Unit.create(CHEMICAL, Namespace.ofMc("chemical_mb"));

    private static final MatterFamily<FluidFamily> FLUID = MatterFamily.create(Namespace.ofMc("fluid"));
    private static final Unit<FluidFamily> MB_FLUID = Unit.create(FLUID, Namespace.ofMc("fluid_mb"));

    @Test
    void convertWithinFamilyUsesFamilyRule() {
        ConversionSchema schema = ConversionSchema.create();
        schema.addFamilyRule(FamilyRule.create(SECOND, TICK, 20.0));
        UnitConverter rules = schema.build();

        double result = rules.convert(2.0, SECOND, TICK);

        assertEquals(40.0, result);
    }

    @Test
    void convertCrossRequiresExplicitBridgeAndSingleStep() {
        ConversionSchema schema = ConversionSchema.create();
        BridgeRule<TimeFamily, EnergyFamily> bridge = BridgeRule.create(TICK, FE, 5.0);
        schema.addBridgeRule(bridge);
        UnitConverter rules = schema.build();

        double result = rules.convertCross(2.0, TICK, bridge, FE);
        assertEquals(10.0, result);

        assertThrows(
                CrossFamilyNotAllowedException.class,
                () -> rules.convertCross(2.0, SECOND, bridge, FE)
        );
    }

    @Test
    void freezeCreatesReadOnlyRuleSnapshot() {
        // Use chemical family which has no built-in providers
        Unit<ChemicalFamily> CHEM_A = Unit.create(CHEMICAL, Namespace.ofMc("chem_a"));
        Unit<ChemicalFamily> CHEM_B = Unit.create(CHEMICAL, Namespace.ofMc("chem_b"));

        ConversionSchema schema = ConversionSchema.create();
        schema.addFamilyRule(FamilyRule.create(CHEM_A, CHEM_B, 2.0));
        UnitConverter rules = schema.build();

        // Adding a completely new rule after freeze should not affect frozen rules
        Unit<ChemicalFamily> CHEM_C = Unit.create(CHEMICAL, Namespace.ofMc("chem_c"));
        schema.addFamilyRule(FamilyRule.create(CHEM_A, CHEM_C, 3.0));

        // Original rule still works
        assertEquals(2.0, rules.convert(1.0, CHEM_A, CHEM_B));
        // Auto-inverse also works
        assertEquals(0.5, rules.convert(1.0, CHEM_B, CHEM_A), 1e-9);
        // Newly-added rule should NOT be visible in frozen snapshot
        assertThrows(
                NoRuleMatchedException.class,
                () -> rules.convert(1.0, CHEM_A, CHEM_C)
        );
    }

    @Test
    void sameLevelConflictThrowsOnFreeze() {
        // Use chemical family (no built-in defaults) to avoid hitting fixed rule validation first
        Unit<ChemicalFamily> CHEM_X = Unit.create(CHEMICAL, Namespace.ofMc("chem_x"));
        Unit<ChemicalFamily> CHEM_Y = Unit.create(CHEMICAL, Namespace.ofMc("chem_y"));

        ConversionSchema schema = ConversionSchema.create();
        schema.addFamilyRule(FamilyRule.create(CHEM_X, CHEM_Y, 2.0));
        schema.addFamilyRule(FamilyRule.create(CHEM_X, CHEM_Y, 3.0));

        assertThrows(RuleConflictException.class, schema::build);
    }

    @Test
    void matterAndMeasureAreNotInteroperableWithoutBridge() {
        ConversionSchema schema = ConversionSchema.create();
        UnitConverter rules = schema.build();
        BridgeRule<ChemicalFamily, TimeFamily> bridge = BridgeRule.create(MB_CHEM, TICK, 1.0);

        assertThrows(
                CrossFamilyNotAllowedException.class,
                () -> rules.convertCross(1.0, MB_CHEM, bridge, SECOND)
        );
    }

    @Test
    void chemicalAndFluidAreNotInteroperableByDefault() {
        ConversionSchema schema = ConversionSchema.create();
        UnitConverter rules = schema.build();
        BridgeRule<ChemicalFamily, FluidFamily> bridge = BridgeRule.create(MB_CHEM, MB_FLUID, 1.0);

        assertThrows(
                CrossFamilyNotAllowedException.class,
                () -> rules.convertCross(1.0, MB_CHEM, bridge, MB_FLUID)
        );
    }

    @Test
    void timeUsesBuiltInConstantRelations() {
        UnitConverter rules = ConversionSchema.create().build();

        assertEquals(20.0, rules.convert(1.0, SECOND, TICK));
        assertEquals(60.0, rules.convert(1.0, MINUTE, SECOND));
    }

    @Test
    void energyBuiltInCanBeOverridden() {
        ConversionSchema schema = ConversionSchema.create();
        UnitConverter builtIn = schema.build();

        assertEquals(4.0, builtIn.convert(1.0, EU, FE));

        schema.addFamilyRule(FamilyRule.create(EU, FE, 5.0));
        UnitConverter overridden = schema.build();

        assertEquals(5.0, overridden.convert(1.0, EU, FE));
    }

    @Test
    void crossFamilyConversionRequiresMatchingEndpoints() {
        ConversionSchema schema = ConversionSchema.create();
        BridgeRule<TimeFamily, EnergyFamily> t2e = BridgeRule.create(TICK, FE, 5.0);
        schema.addBridgeRule(t2e);
        UnitConverter rules = schema.build();

        // Target unit doesn't match bridge target
        assertThrows(
                CrossFamilyNotAllowedException.class,
                () -> rules.convertCross(1.0, TICK, t2e, EU)
        );
    }

    private static final class TimeFamily {
    }

    private static final class EnergyFamily {
    }

    private static final class ChemicalFamily {
    }

    private static final class FluidFamily {
    }
}
