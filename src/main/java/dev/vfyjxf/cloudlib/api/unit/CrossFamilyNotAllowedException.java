package dev.vfyjxf.cloudlib.api.unit;

public class CrossFamilyNotAllowedException extends IllegalArgumentException {

    public CrossFamilyNotAllowedException() {
    }

    public CrossFamilyNotAllowedException(String message) {
        super(message);
    }
}
