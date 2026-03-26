package dev.vfyjxf.cloudlib.api.unit.conversion;

import dev.vfyjxf.cloudlib.api.unit.ExactRatio;
import dev.vfyjxf.cloudlib.api.unit.InvalidRuleException;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.Checks;
import org.jetbrains.annotations.Nullable;

public record DomainRule(String domain, Namespace fromUnitId, Namespace toUnitId, double ratio,
                         @Nullable ExactRatio exactRatio) {

    public DomainRule {
        Checks.checkNotNull(domain, "domain");
        Checks.checkNotNull(fromUnitId, "fromUnitId");
        Checks.checkNotNull(toUnitId, "toUnitId");
        if (ratio <= 0.0) {
            throw new InvalidRuleException("Domain rule ratio must be > 0");
        }
    }

    public static DomainRule create(String domain, Namespace fromUnitId, Namespace toUnitId, double ratio) {
        return new DomainRule(domain, fromUnitId, toUnitId, ratio, null);
    }

    public static DomainRule createFraction(String domain, Namespace fromUnitId, Namespace toUnitId, long numerator, long denominator) {
        ExactRatio exact = ExactRatio.create(numerator, denominator);
        return new DomainRule(domain, fromUnitId, toUnitId, exact.toDouble(), exact);
    }
}
