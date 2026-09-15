package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
import dev.vfyjxf.cloudlib.api.unit.text.QuantityFormatter;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.Checks;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * An exact value bound to a unit, holding a converter back-reference for chained operations.
 *
 * @param <F> phantom family marker; same-family operations are compile-time enforced
 */
@NotNullByDefault
public final class Quantity<F> {

    public static <F> Quantity<F> of(Ratio value, Unit<F> unit, UnitConverter converter) {
        return new Quantity<>(value, unit, converter);
    }

    private final Ratio value;
    private final Unit<F> unit;
    private final UnitConverter converter;

    private Quantity(Ratio value, Unit<F> unit, UnitConverter converter) {
        Checks.checkNotNull(value, "value");
        Checks.checkNotNull(unit, "unit");
        Checks.checkNotNull(converter, "converter");
        this.value = value;
        this.unit = unit;
        this.converter = converter;
    }

    public Ratio value() {
        return value;
    }

    public Unit<F> unit() {
        return unit;
    }

    public UnitConverter converter() {
        return converter;
    }

    /**
     * Whether the value has an exact origin: false once any hop of the conversion
     * chain that produced it went through an approximate rule.
     */
    public boolean isExact() {
        return value.isExact();
    }

    /**
     * Normalizes this quantity into a {@link MaterialAmount}: converts it to the
     * family's registered base unit (material-specific rules apply) and tags it
     * with the material. Throws if the family has no base unit registered.
     */
    public MaterialAmount toMaterialAmount(Namespace material) {
        Checks.checkNotNull(material, "material");
        return converter.materialAmount(value, unit, material);
    }

    /**
     * Converts to another unit of the same family.
     */
    public Quantity<F> to(Unit<F> target) {
        return to(target, null);
    }

    public Quantity<F> to(Unit<F> target, @Nullable Namespace material) {
        return converter.convert(value, unit, target, material);
    }

    /**
     * Converts to a unit of another family; requires a bridge to exist.
     */
    public <T> Quantity<T> toCross(Unit<T> target) {
        return toCross(target, null);
    }

    public <T> Quantity<T> toCross(Unit<T> target, @Nullable Namespace material) {
        return converter.convert(value, unit, target, material);
    }

    /**
     * Adds another quantity of the same family, auto-converting it to this quantity's unit.
     */
    public Quantity<F> add(Quantity<F> other) {
        Checks.checkNotNull(other, "other");
        Quantity<F> converted = converter.convert(other.value, other.unit, unit);
        return new Quantity<>(value.add(converted.value), unit, converter);
    }

    public Quantity<F> subtract(Quantity<F> other) {
        Checks.checkNotNull(other, "other");
        Quantity<F> converted = converter.convert(other.value, other.unit, unit);
        return new Quantity<>(value.subtract(converted.value), unit, converter);
    }

    public Quantity<F> multiply(Ratio factor) {
        return new Quantity<>(value.multiply(factor), unit, converter);
    }

    public Quantity<F> multiply(long factor) {
        return new Quantity<>(value.multiply(factor), unit, converter);
    }

    public Quantity<F> divide(Ratio divisor) {
        return new Quantity<>(value.divide(divisor), unit, converter);
    }

    public Quantity<F> divide(long divisor) {
        return new Quantity<>(value.divide(divisor), unit, converter);
    }

    /**
     * Returns the exact integral value, or throws if not integral.
     */
    public long toLongExact() {
        return value.toLongExact();
    }

    public long floor() {
        return value.floor();
    }

    public double toDouble() {
        return value.toDouble();
    }

    /**
     * Splits this quantity into whole units of {@code target} plus a remainder
     * expressed in this quantity's unit.
     */
    public DiscreteResult<F> toDiscrete(Unit<F> target) {
        return toDiscrete(target, null);
    }

    public DiscreteResult<F> toDiscrete(Unit<F> target, @Nullable Namespace material) {
        Ratio converted = converter.convert(value, unit, target, material).value();
        long amount = converted.floor();
        Ratio remainderInTarget = converted.subtract(Ratio.of(amount));
        Ratio remainderInSource = converter.convert(remainderInTarget, target, unit, material).value();
        Quantity<F> remainder = new Quantity<>(remainderInSource, unit, converter);
        return new DiscreteResult<>(amount, remainder, remainderInSource.isZero());
    }

    public String format() {
        return converter.formatter().format(this);
    }

    public String format(QuantityFormatter formatter) {
        Checks.checkNotNull(formatter, "formatter");
        return formatter.format(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Quantity<?> other)) return false;
        return value.equals(other.value) && unit.equals(other.unit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, unit);
    }

    @Override
    public String toString() {
        return value + " " + unit.id();
    }
}
