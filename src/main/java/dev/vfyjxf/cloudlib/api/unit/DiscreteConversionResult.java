package dev.vfyjxf.cloudlib.api.unit;

public record DiscreteConversionResult(long amount, double remainder, boolean exact) {

    public static DiscreteConversionResult exact(long amount) {
        return new DiscreteConversionResult(amount, 0.0, true);
    }

    public static DiscreteConversionResult inexact(long amount, double remainder) {
        return new DiscreteConversionResult(amount, remainder, false);
    }
}
