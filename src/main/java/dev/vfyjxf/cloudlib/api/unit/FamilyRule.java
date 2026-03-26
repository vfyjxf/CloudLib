package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.util.Checks;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public record FamilyRule<F>(
        Unit<F> from,
        Unit<F> to,
        double ratio,
        boolean fixed,
        Map<ContextKey<?>, Object> requiredContext,
        @Nullable ExactRatio exactRatio
) {

    //region Factory methods

    public static <F> FamilyRule<F> create(Unit<F> from, Unit<F> to, double ratio) {
        return new FamilyRule<>(from, to, ratio, false, Map.of(), null);
    }

    public static <F> FamilyRule<F> create(Unit<F> from, Unit<F> to, double ratio, Map<ContextKey<?>, Object> requiredContext) {
        return new FamilyRule<>(from, to, ratio, false, requiredContext, null);
    }

    public static <F> FamilyRule<F> createFixed(Unit<F> from, Unit<F> to, double ratio) {
        return new FamilyRule<>(from, to, ratio, true, Map.of(), null);
    }

    public static <F> FamilyRule<F> createFraction(Unit<F> from, Unit<F> to, long numerator, long denominator) {
        ExactRatio exactRatio = ExactRatio.create(numerator, denominator);
        return new FamilyRule<>(from, to, exactRatio.toDouble(), false, Map.of(), exactRatio);
    }

    public static <F> FamilyRule<F> createFraction(
            Unit<F> from, Unit<F> to, long numerator, long denominator,
            Map<ContextKey<?>, Object> requiredContext
    ) {
        ExactRatio exactRatio = ExactRatio.create(numerator, denominator);
        return new FamilyRule<>(from, to, exactRatio.toDouble(), false, requiredContext, exactRatio);
    }

    public static <F> FamilyRule<F> createFixedFraction(Unit<F> from, Unit<F> to, long numerator, long denominator) {
        ExactRatio exactRatio = ExactRatio.create(numerator, denominator);
        return new FamilyRule<>(from, to, exactRatio.toDouble(), true, Map.of(), exactRatio);
    }

    //endregion

    public FamilyRule(Unit<F> from, Unit<F> to, double ratio, boolean fixed,
                      Map<ContextKey<?>, Object> requiredContext, @Nullable ExactRatio exactRatio) {
        this.from = Checks.checkNotNull(from, "from");
        this.to = Checks.checkNotNull(to, "to");
        this.fixed = fixed;
        this.requiredContext = Map.copyOf(Checks.checkNotNull(requiredContext, "requiredContext"));
        this.exactRatio = exactRatio;
        if (!from.family().equals(to.family())) {
            throw new InvalidRuleException("Family rule requires same family: " + from + " -> " + to);
        }
        if (ratio <= 0.0) {
            throw new InvalidRuleException("Family rule ratio must be > 0");
        }
        this.ratio = ratio;
    }

    public int specificity() {
        return requiredContext.size();
    }

    @SuppressWarnings("unchecked")
    public boolean matches(ConvertContext context) {
        Checks.checkNotNull(context, "context");
        for (Map.Entry<ContextKey<?>, Object> entry : requiredContext.entrySet()) {
            ContextKey<Object> key = (ContextKey<Object>) entry.getKey();
            Object required = entry.getValue();
            Object actual = context.find(key).orElse(null);
            if (!required.equals(actual)) {
                return false;
            }
        }
        return true;
    }
}
