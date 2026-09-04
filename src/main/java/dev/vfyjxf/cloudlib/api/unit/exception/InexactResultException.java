package dev.vfyjxf.cloudlib.api.unit.exception;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;

/**
 * Thrown when an exact result was required but the value is not integral.
 */
@NotNullByDefault
public class InexactResultException extends UnitConversionException {

    public InexactResultException(String message) {
        super(message);
    }

    public InexactResultException(String message, Throwable cause) {
        super(message, cause);
    }
}
