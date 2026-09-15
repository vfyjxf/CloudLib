package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.Checks;

/**
 * An amount of a specific material, normalized to the base unit of a matter family.
 * Two amounts of the same material can be compared across families and units
 * (1 iron ingot ≡ 144 mB molten iron ≡ 9 iron nuggets), and converted back to
 * any unit as a conversion hub.
 */
@NotNullByDefault
public final class MaterialAmount {

    private final Namespace material;
    private final Ratio value;
    private final Unit<?> baseUnit;
    private final UnitConverter converter;

    MaterialAmount(Namespace material, Ratio value, Unit<?> baseUnit, UnitConverter converter) {
        Checks.checkNotNull(material, "material");
        Checks.checkNotNull(value, "value");
        Checks.checkNotNull(baseUnit, "baseUnit");
        Checks.checkNotNull(converter, "converter");
        this.material = material;
        this.value = value;
        this.baseUnit = baseUnit;
        this.converter = converter;
    }

    /**
     * The material identity (iron ≠ copper).
     */
    public Namespace material() {
        return material;
    }

    /**
     * The exact value counted in the family's base unit.
     */
    public Ratio value() {
        return value;
    }

    public Unit<?> baseUnit() {
        return baseUnit;
    }

    public boolean isExact() {
        return value.isExact();
    }

    /**
     * Whether the other amount expresses the same amount of the same material:
     * materials must be equal and the values must match after converting the other
     * amount into this amount's base unit (bridges included).
     */
    public boolean equivalentTo(MaterialAmount other) {
        Checks.checkNotNull(other, "other");
        if (!material.equals(other.material)) return false;
        if (baseUnit.equals(other.baseUnit)) return value.equals(other.value);
        Ratio converted = converter.convert(other.value, other.baseUnit, baseUnit, material).value();
        return value.equals(converted);
    }

    /**
     * Converts this amount into any unit; material-specific rules apply.
     */
    public <F> Quantity<F> to(Unit<F> target) {
        Checks.checkNotNull(target, "target");
        return converter.convert(value, baseUnit, target, material);
    }

    @Override
    public String toString() {
        return value + " " + baseUnit.id() + " of " + material;
    }
}
