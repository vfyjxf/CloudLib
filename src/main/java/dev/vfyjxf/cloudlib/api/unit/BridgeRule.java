package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.Checks;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

public record BridgeRule<F1, F2>(
        Unit<F1> from,
        Unit<F2> to,
        double ratio,
        @Nullable Namespace requiredMaterialId,
        Map<ContextKey<?>, Object> requiredContext,
        @Nullable ExactRatio exactRatio
) {

    public BridgeRule {
        Checks.checkNotNull(from, "from");
        Checks.checkNotNull(to, "to");
        Checks.checkNotNull(requiredContext, "requiredContext");
        if (ratio <= 0.0) {
            throw new InvalidRuleException("Bridge rule ratio must be > 0");
        }
        requiredContext = Map.copyOf(requiredContext);
    }

    //region Factory methods

    public static <F1, F2> BridgeRule<F1, F2> create(Unit<F1> from, Unit<F2> to, double ratio) {
        return new BridgeRule<>(from, to, ratio, null, Map.of(), null);
    }

    public static <F1, F2> BridgeRule<F1, F2> create(Unit<F1> from, Unit<F2> to, double ratio, Namespace materialId) {
        Checks.checkNotNull(materialId, "materialId");
        return new BridgeRule<>(from, to, ratio, materialId, Map.of(), null);
    }

    public static <F1, F2> BridgeRule<F1, F2> create(
            Unit<F1> from, Unit<F2> to, double ratio,
            @Nullable Namespace materialId, Map<ContextKey<?>, Object> requiredContext
    ) {
        return new BridgeRule<>(from, to, ratio, materialId, requiredContext, null);
    }

    public static <F1, F2> BridgeRule<F1, F2> createFraction(
            Unit<F1> from, Unit<F2> to, long numerator, long denominator
    ) {
        ExactRatio exact = ExactRatio.create(numerator, denominator);
        return new BridgeRule<>(from, to, exact.toDouble(), null, Map.of(), exact);
    }

    public static <F1, F2> BridgeRule<F1, F2> createFraction(
            Unit<F1> from, Unit<F2> to, long numerator, long denominator,
            Namespace materialId
    ) {
        Checks.checkNotNull(materialId, "materialId");
        ExactRatio exact = ExactRatio.create(numerator, denominator);
        return new BridgeRule<>(from, to, exact.toDouble(), materialId, Map.of(), exact);
    }

    public static <F1, F2> BridgeRule<F1, F2> createFraction(
            Unit<F1> from, Unit<F2> to, long numerator, long denominator,
            @Nullable Namespace materialId, Map<ContextKey<?>, Object> requiredContext
    ) {
        ExactRatio exact = ExactRatio.create(numerator, denominator);
        return new BridgeRule<>(from, to, exact.toDouble(), materialId, requiredContext, exact);
    }

    //endregion

    public int specificity() {
        return (requiredMaterialId != null ? 1 : 0) + requiredContext.size();
    }

    @SuppressWarnings("unchecked")
    public boolean matches(@Nullable Namespace material, ConvertContext context) {
        if (requiredMaterialId != null && !requiredMaterialId.equals(material)) {
            return false;
        }
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
