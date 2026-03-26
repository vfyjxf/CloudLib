package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.Checks;

public record Unit<F>(UnitFamily<F> family, Namespace id) {

    public Unit {
        Checks.checkNotNull(family, "family");
        Checks.checkNotNull(id, "id");
    }

    public static <F> Unit<F> create(UnitFamily<F> family, Namespace id) {
        return new Unit<>(family, id);
    }

    @Override
    public String toString() {
        return family.id() + "/" + id;
    }
}
