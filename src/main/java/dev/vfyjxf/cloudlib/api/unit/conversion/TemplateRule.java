package dev.vfyjxf.cloudlib.api.unit.conversion;

import dev.vfyjxf.cloudlib.api.unit.ExactRatio;
import dev.vfyjxf.cloudlib.api.unit.InvalidRuleException;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.Checks;
import org.jetbrains.annotations.Nullable;

public record TemplateRule(Namespace fromUnitId, Namespace toUnitId, double ratio, @Nullable ExactRatio exactRatio) {

    public TemplateRule {
        Checks.checkNotNull(fromUnitId, "fromUnitId");
        Checks.checkNotNull(toUnitId, "toUnitId");
        if (ratio <= 0.0) {
            throw new InvalidRuleException("Template rule ratio must be > 0");
        }
    }

    public static TemplateRule create(Namespace fromUnitId, Namespace toUnitId, double ratio) {
        return new TemplateRule(fromUnitId, toUnitId, ratio, null);
    }

    public static TemplateRule createFraction(Namespace fromUnitId, Namespace toUnitId, long numerator, long denominator) {
        ExactRatio exact = ExactRatio.create(numerator, denominator);
        return new TemplateRule(fromUnitId, toUnitId, exact.toDouble(), exact);
    }
}
