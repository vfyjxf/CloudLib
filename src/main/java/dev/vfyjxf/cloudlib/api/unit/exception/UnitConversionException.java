package dev.vfyjxf.cloudlib.api.unit.exception;

import org.jspecify.annotations.NullMarked;

/**
 * Base class of all unit conversion related exceptions.
 */
@NullMarked
public class UnitConversionException extends RuntimeException {

    public UnitConversionException(String message) {
        super(message);
    }

    public UnitConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
