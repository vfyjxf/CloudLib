package dev.vfyjxf.cloudlib.api.unit;

public class RuleConflictException extends IllegalStateException {
    public RuleConflictException() {
    }

    public RuleConflictException(String message) {
        super(message);
    }
}
