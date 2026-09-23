package dev.vfyjxf.cloudlib.api.unit.exception;

import org.jspecify.annotations.NullMarked;

/**
 * Thrown when a rule conflicts with an already registered rule of the same specificity.
 */
@NullMarked
public class RuleConflictException extends UnitConversionException {

    public RuleConflictException(String message) {
        super(message);
    }
}
