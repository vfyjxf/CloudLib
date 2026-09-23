package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.util.Checks;
import org.jspecify.annotations.NullMarked;

/**
 * The result of a discrete conversion: a whole {@code amount} of the target unit,
 * plus the leftover {@code remainder} expressed in the source unit.
 *
 * @param <F> phantom family marker, see {@link UnitFamily}
 */
@NullMarked
public record DiscreteResult<F>(long amount, Quantity<F> remainder, boolean exact) {

    public DiscreteResult {
        Checks.checkNotNull(remainder, "remainder");
    }
}
