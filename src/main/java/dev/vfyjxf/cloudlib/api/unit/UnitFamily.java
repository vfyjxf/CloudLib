package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.Checks;

import java.util.Objects;

/**
 * A family of units that measure the same kind of thing.
 *
 * <p>The phantom type parameter {@code F} ties units of this family together at compile time.
 * Use the constants-holder class of the family as the marker, so no dedicated marker class
 * is ever needed:
 * <pre>{@code
 * public final class TimeUnits {
 *     public static final UnitFamily<TimeUnits> family = UnitFamily.measure(Namespace.ofMc("time"));
 *     public static final Unit<TimeUnits> tick = Unit.of(family, Namespace.ofMc("tick"));
 * }
 * }</pre>
 * Equality is structural on {@code id} + {@code kind}; the marker type is not part of it.
 *
 * @param <F> phantom family marker, typically the holder class of the family's constants
 */
@NotNullByDefault
public final class UnitFamily<F> {

    public enum Kind {
        /**
         * Abstract measures like time or energy.
         */
        measure,
        /**
         * Physical matter like items or fluids; conversions may depend on the material.
         */
        matter
    }

    public static <F> UnitFamily<F> measure(Namespace id) {
        return new UnitFamily<>(id, Kind.measure);
    }

    public static <F> UnitFamily<F> matter(Namespace id) {
        return new UnitFamily<>(id, Kind.matter);
    }

    public static <F> UnitFamily<F> of(Namespace id, Kind kind) {
        return new UnitFamily<>(id, kind);
    }

    private final Namespace id;
    private final Kind kind;

    private UnitFamily(Namespace id, Kind kind) {
        Checks.checkNotNull(id, "id");
        Checks.checkNotNull(kind, "kind");
        this.id = id;
        this.kind = kind;
    }

    public Namespace id() {
        return id;
    }

    public Kind kind() {
        return kind;
    }

    public boolean isMatter() {
        return kind == Kind.matter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UnitFamily<?> other)) return false;
        return id.equals(other.id) && kind == other.kind;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, kind);
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
