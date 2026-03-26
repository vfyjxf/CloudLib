package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.util.Namespace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnitRegistryKeysTest {

    @Test
    void familyKeysUseNamespaceConstants() {
        assertEquals(Namespace.ofMc("time"), Units.familyTime);
        assertEquals(Namespace.ofMc("energy"), Units.familyEnergy);
        assertEquals(Namespace.ofMc("matter"), Units.familyMatter);
        assertEquals(Namespace.ofMc("item"), Units.familyItem);
        assertEquals(Namespace.ofMc("fluid"), Units.familyFluid);
    }

    @Test
    void unitKeysTimeUseNamespaceConstants() {
        assertEquals(Namespace.ofMc("tick"), Units.unitTick);
        assertEquals(Namespace.ofMc("second"), Units.unitSecond);
        assertEquals(Namespace.ofMc("minute"), Units.unitMinute);
        assertEquals(Namespace.ofMc("hour"), Units.unitHour);
    }

    @Test
    void unitKeysEnergyUseNamespaceConstants() {
        assertEquals(Namespace.ofMc("eu"), Units.unitEu);
        assertEquals(Namespace.ofMc("fe"), Units.unitFe);
    }

    @Test
    void unitKeysItemFormsUseNamespaceConstants() {
        assertEquals(Namespace.ofMc("ingot"), Units.unitIngot);
        assertEquals(Namespace.ofMc("block"), Units.unitBlock);
        assertEquals(Namespace.ofMc("nugget"), Units.unitNugget);
        assertEquals(Namespace.ofMc("dust"), Units.unitDust);
        assertEquals(Namespace.ofMc("plate"), Units.unitPlate);
        assertEquals(Namespace.ofMc("gear"), Units.unitGear);
        assertEquals(Namespace.ofMc("rod"), Units.unitRod);
    }

    @Test
    void unitKeysFluidMeasuresUseNamespaceConstants() {
        assertEquals(Namespace.ofMc("millibucket"), Units.unitMillibucket);
        assertEquals(Namespace.ofMc("bucket"), Units.unitBucket);
        assertEquals(Namespace.ofMc("droplet"), Units.unitDroplet);
    }

    @Test
    void builtInTimeHourConversion() {
        MeasureFamily<TimeKind> TIME = MeasureFamily.create(Units.familyTime);
        Unit<TimeKind> TICK = Unit.create(TIME, Units.unitTick);
        Unit<TimeKind> HOUR = Unit.create(TIME, Units.unitHour);

        UnitConverter rules = ConversionSchema.create().build();
        assertEquals(72000.0, rules.convert(1.0, HOUR, TICK));
        assertEquals(1.0 / 72000.0, rules.convert(1.0, TICK, HOUR), 1e-12);
    }

    private static final class TimeKind {
    }
}
