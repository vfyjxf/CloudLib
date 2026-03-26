package dev.vfyjxf.cloudlib.api.unit.conversion;

import dev.vfyjxf.cloudlib.api.unit.ExactRatio;
import dev.vfyjxf.cloudlib.api.unit.InvalidRuleException;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.Checks;
import org.jetbrains.annotations.Nullable;

public record MaterialRule(Namespace materialId, Namespace fromUnitId, Namespace toUnitId, double ratio, @Nullable ExactRatio exactRatio) {

    public MaterialRule {
        Checks.checkNotNull(materialId, "materialId");
        Checks.checkNotNull(fromUnitId, "fromUnitId");
        Checks.checkNotNull(toUnitId, "toUnitId");
        if (ratio <= 0.0) {
            throw new InvalidRuleException("Material rule ratio must be > 0");
        }
    }

    public static MaterialRule create(Namespace materialId, Namespace fromUnitId, Namespace toUnitId, double ratio) {
        return new MaterialRule(materialId, fromUnitId, toUnitId, ratio, null);
    }

    public static MaterialRule createFraction(Namespace materialId, Namespace fromUnitId, Namespace toUnitId, long numerator, long denominator) {
        ExactRatio exact = ExactRatio.create(numerator, denominator);
        return new MaterialRule(materialId, fromUnitId, toUnitId, exact.toDouble(), exact);
    }
}
