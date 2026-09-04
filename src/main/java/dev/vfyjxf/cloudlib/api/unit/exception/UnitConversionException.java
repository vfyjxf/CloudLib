package dev.vfyjxf.cloudlib.api.unit.exception;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;

/**
 * Base class of all unit conversion related exceptions.
 */
@NotNullByDefault
public class UnitConversionException extends RuntimeException {

    public UnitConversionException(String message) {
        super(message);
    }

    public UnitConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
