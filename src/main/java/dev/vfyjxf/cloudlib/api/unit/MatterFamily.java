package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.unit.conversion.*;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.Checks;

/**
 * A matter-based unit family: items, fluids, chemicals, etc.
 * <p>
 * Matter families support form-based conversion rules
 * ({@link FallbackRule}, {@link TemplateRule}, {@link MaterialRule}, {@link ObjectRule}).
 *
 * @param <F> phantom type for compile-time family distinction
 */
public record MatterFamily<F>(Namespace id) implements UnitFamily<F> {
    public static <F> MatterFamily<F> create(Namespace id) {
        return new MatterFamily<>(id);
    }

    public MatterFamily {
        Checks.checkNotNull(id, "id");
    }

}
