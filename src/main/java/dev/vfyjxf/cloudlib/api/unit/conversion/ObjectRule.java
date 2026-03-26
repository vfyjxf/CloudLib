package dev.vfyjxf.cloudlib.api.unit.conversion;

import dev.vfyjxf.cloudlib.api.unit.ExactRatio;
import dev.vfyjxf.cloudlib.api.unit.InvalidRuleException;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.Checks;
import org.jetbrains.annotations.Nullable;

public record ObjectRule(Namespace objectId, Namespace fromUnitId, Namespace toUnitId, double ratio, @Nullable ExactRatio exactRatio) {

    public ObjectRule {
        Checks.checkNotNull(objectId, "objectId");
        Checks.checkNotNull(fromUnitId, "fromUnitId");
        Checks.checkNotNull(toUnitId, "toUnitId");
        if (ratio <= 0.0) {
            throw new InvalidRuleException("Object rule ratio must be > 0");
        }
    }

    public static ObjectRule create(Namespace objectId, Namespace fromUnitId, Namespace toUnitId, double ratio) {
        return new ObjectRule(objectId, fromUnitId, toUnitId, ratio, null);
    }

    public static ObjectRule createFraction(Namespace objectId, Namespace fromUnitId, Namespace toUnitId, long numerator, long denominator) {
        ExactRatio exact = ExactRatio.create(numerator, denominator);
        return new ObjectRule(objectId, fromUnitId, toUnitId, exact.toDouble(), exact);
    }
}
