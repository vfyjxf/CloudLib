package dev.vfyjxf.cloudlib.api.unit.exception;

import dev.vfyjxf.cloudlib.api.unit.Unit;
import org.jspecify.annotations.NullMarked;

/**
 * Thrown when no conversion path exists between two units.
 */
@NullMarked
public class NoConversionPathException extends UnitConversionException {

    public NoConversionPathException(Unit from, Unit to) {
        super(
            "No conversion path from " + from + " (family " + from.family().id() + ") to " + to + " (family "
                    + to.family().id() + ")"
        );
    }
}
