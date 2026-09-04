package dev.vfyjxf.cloudlib.api.unit.text;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
import dev.vfyjxf.cloudlib.api.unit.MaterialAmount;
import dev.vfyjxf.cloudlib.api.unit.Quantity;
import dev.vfyjxf.cloudlib.api.unit.Ratio;
import dev.vfyjxf.cloudlib.api.unit.Unit;
import dev.vfyjxf.cloudlib.api.unit.UnitConverter;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.Checks;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Renders quantities as human readable text. All output is exact unless
 * {@link #formatDecimal} is used explicitly.
 */
@NotNullByDefault
public final class QuantityFormatter {

    private final UnitConverter converter;
    private final UnitNames names;
    private final Map<Unit<?>, String> shortNames;

    public QuantityFormatter(UnitConverter converter, UnitNames names) {
        this(converter, names, Map.of());
    }

    private QuantityFormatter(UnitConverter converter, UnitNames names, Map<Unit<?>, String> shortNames) {
        Checks.checkNotNull(converter, "converter");
        Checks.checkNotNull(names, "names");
        this.converter = converter;
        this.names = names;
        this.shortNames = shortNames;
    }

    /**
     * Returns a copy of this formatter with an additional short name used by {@link #formatDecomposed}.
     */
    public QuantityFormatter withShortName(Unit<?> unit, String shortName) {
        Checks.checkNotNull(unit, "unit");
        Checks.checkNotNull(shortName, "shortName");
        Map<Unit<?>, String> copy = new HashMap<>(shortNames);
        copy.put(unit, shortName);
        return new QuantityFormatter(converter, names, Map.copyOf(copy));
    }

    /**
     * {@code 20 ticks}, {@code 1 block}, {@code 1/9 block}; inexact values get a
     * {@code ≈} prefix: {@code ≈20 ticks}.
     */
    public String format(Quantity<?> quantity) {
        Checks.checkNotNull(quantity, "quantity");
        Ratio value = quantity.value();
        String name = names.name(quantity.unit());
        return withApproxMarker(value, renderBody(value, name));
    }

    /**
     * Renders a material amount with its material-qualified name: {@code 2 iron ingots}.
     */
    public String format(MaterialAmount amount) {
        Checks.checkNotNull(amount, "amount");
        Ratio value = amount.value();
        String name = names.name(amount.baseUnit(), amount.material());
        return withApproxMarker(value, renderBody(value, name));
    }

    private static String withApproxMarker(Ratio value, String body) {
        return value.isExact() ? body : "≈" + body;
    }

    private static String renderBody(Ratio value, String name) {
        if (value.isIntegral()) {
            long amount = value.toLongExact();
            String unitName = amount != 1 && !name.endsWith("s") ? name + "s" : name;
            return amount + " " + unitName;
        }
        return value + " " + name;
    }

    /**
     * Approximate decimal rendering: {@code ≈0.111 block}. Integral values render exactly.
     */
    public String formatDecimal(Quantity<?> quantity, int precision) {
        Checks.checkNotNull(quantity, "quantity");
        Checks.checkArgument(precision > 0, "precision must be positive: %s", precision);
        Ratio value = quantity.value();
        if (value.isIntegral()) return format(quantity);
        String name = names.name(quantity.unit());
        BigDecimal decimal = new BigDecimal(value.numerator())
                .divide(new BigDecimal(value.denominator()), new MathContext(precision, RoundingMode.HALF_UP));
        return "≈" + decimal.stripTrailingZeros().toPlainString() + " " + name;
    }

    /**
     * Exact greedy mixed-unit breakdown along a descending chain:
     * {@code 5000 seconds} with {@code (hour, minute, second)} → {@code [1 hour, 23 minutes, 20 seconds]}.
     * Zero-amount elements are skipped. The chain is compile-time checked to be same-family.
     */
    @SafeVarargs
    public final <F> List<Quantity<F>> decompose(Quantity<F> quantity, Unit<F>... chain) {
        Checks.checkNotNull(quantity, "quantity");
        Checks.checkNotNull(chain, "chain");
        Checks.checkArgument(chain.length > 0, "chain can't be empty");
        List<Quantity<F>> result = new ArrayList<>();
        Ratio remaining = quantity.value();
        Unit<F> remainingUnit = quantity.unit();
        for (int i = 0; i < chain.length; i++) {
            Unit<F> target = Checks.checkNotNull(chain[i], "chain element");
            Ratio converted = converter.convert(remaining, remainingUnit, target).value();
            if (i == chain.length - 1) {
                if (!converted.isZero()) {
                    result.add(Quantity.of(converted, target, converter));
                }
            } else {
                long amount = converted.floor();
                if (amount != 0) {
                    result.add(Quantity.of(Ratio.of(amount), target, converter));
                    Ratio used = converter.convert(Ratio.of(amount), target, remainingUnit).value();
                    remaining = remaining.subtract(used);
                }
            }
        }
        if (result.isEmpty()) {
            result.add(Quantity.of(Ratio.ZERO, chain[chain.length - 1], converter));
        }
        return List.copyOf(result);
    }

    /**
     * Short-form decomposed rendering: {@code "1h 23m 20s"}.
     */
    @SafeVarargs
    public final <F> String formatDecomposed(Quantity<F> quantity, Unit<F>... chain) {
        return decompose(quantity, chain).stream()
                .map(element -> valueText(element.value()) + shortName(element.unit()))
                .collect(Collectors.joining(" "));
    }

    /**
     * The exact ratio between two units: {@code ratioText(ingot, block)} → {@code "1:9"}.
     */
    public String ratioText(Unit<?> a, Unit<?> b) {
        return ratioText(a, b, null);
    }

    public String ratioText(Unit<?> a, Unit<?> b, @Nullable Namespace material) {
        return converter.convert(Ratio.ONE, a, b, material).value().describe();
    }

    public String shortName(Unit<?> unit) {
        String registered = shortNames.get(unit);
        if (registered != null) return registered;
        String name = names.name(unit);
        return name.isEmpty() ? unit.id().path() : name.substring(0, 1);
    }

    private static String valueText(Ratio value) {
        if (value.isIntegral()) return Long.toString(value.toLongExact());
        return value.toString();
    }
}
