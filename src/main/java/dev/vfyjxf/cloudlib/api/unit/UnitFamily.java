package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.util.Namespace;

/**
 * Sealed family hierarchy: matter-based units vs. measure-based units.
 * <p>
 * Use {@link MatterFamily} for material units (items, fluids, chemicals)
 * and {@link MeasureFamily} for non-material units (time, energy).
 *
 * @param <F> phantom type for compile-time family distinction
 */
public sealed interface UnitFamily<F> permits MatterFamily, MeasureFamily {

    Namespace id();
}
