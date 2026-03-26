package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.Checks;

import java.util.Objects;

/**
 * A measure-based unit family: time, energy, temperature, etc.
 * <p>
 * Measure families use {@link FamilyRule} or {@link BridgeRule} only,
 * not form-based conversion rules.
 *
 * @param <F> phantom type for compile-time family distinction
 */
public final class MeasureFamily<F> implements UnitFamily<F> {

    private final Namespace id;

    public static <F> MeasureFamily<F> create(Namespace id) {
        return new MeasureFamily<>(id);
    }

    private MeasureFamily(Namespace id) {
        this.id = Checks.checkNotNull(id, "id");
    }

    @Override
    public Namespace id() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MeasureFamily<?> that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "MeasureFamily{" + id + "}";
    }
}
