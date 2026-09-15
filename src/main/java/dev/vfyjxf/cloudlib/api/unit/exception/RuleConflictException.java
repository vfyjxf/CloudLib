package dev.vfyjxf.cloudlib.api.unit.exception;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;

/**
 * Thrown when a rule conflicts with an already registered rule of the same specificity.
 */
@NotNullByDefault
public class RuleConflictException extends UnitConversionException {

    public RuleConflictException(String message) {
        super(message);
    }
}
