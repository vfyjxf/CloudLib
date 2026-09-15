package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.Checks;

/**
 * A concrete unit inside a {@link UnitFamily}.
 *
 * @param <F> phantom family marker, see {@link UnitFamily}
 */
@NotNullByDefault
public record Unit<F>(UnitFamily<F> family, Namespace id) {

    public static <F> Unit<F> of(UnitFamily<F> family, Namespace id) {
        return new Unit<>(family, id);
    }

    public Unit {
        Checks.checkNotNull(family, "family");
        Checks.checkNotNull(id, "id");
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
