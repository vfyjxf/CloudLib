package dev.vfyjxf.cloudlib.api.unit.conversion;

import dev.vfyjxf.cloudlib.api.unit.ExactRatio;
import dev.vfyjxf.cloudlib.api.unit.InvalidRuleException;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.Checks;
import org.jetbrains.annotations.Nullable;

public record FallbackRule(Namespace fromUnitId, Namespace toUnitId, double ratio, @Nullable ExactRatio exactRatio) {

    public FallbackRule {
        Checks.checkNotNull(fromUnitId, "fromUnitId");
        Checks.checkNotNull(toUnitId, "toUnitId");
        if (ratio <= 0.0) {
            throw new InvalidRuleException("Fallback rule ratio must be > 0");
        }
    }

    public static FallbackRule create(Namespace fromUnitId, Namespace toUnitId, double ratio) {
        return new FallbackRule(fromUnitId, toUnitId, ratio, null);
    }

    public static FallbackRule createFraction(Namespace fromUnitId, Namespace toUnitId, long numerator, long denominator) {
        ExactRatio exact = ExactRatio.create(numerator, denominator);
        return new FallbackRule(fromUnitId, toUnitId, exact.toDouble(), exact);
    }
}
