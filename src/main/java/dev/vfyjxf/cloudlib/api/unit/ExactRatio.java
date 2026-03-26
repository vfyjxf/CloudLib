package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.util.Checks;

import java.math.BigInteger;

/**
 * Immutable normalized rational ratio used by exact conversion paths.
 */
public record ExactRatio(BigInteger numerator, BigInteger denominator) {

    /**
     * Creates an exact ratio and normalizes it to the reduced positive-denominator form.
     */
    public static ExactRatio create(long numerator, long denominator) {
        return new ExactRatio(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator));
    }

    public ExactRatio {
        numerator = Checks.checkNotNull(numerator, "numerator");
        denominator = Checks.checkNotNull(denominator, "denominator");

        if (denominator.signum() == 0) {
            throw new InvalidRuleException("Exact ratio denominator must not be zero");
        }

        if (denominator.signum() < 0) {
            numerator = numerator.negate();
            denominator = denominator.negate();
        }

        if (numerator.signum() <= 0) {
            throw new InvalidRuleException("Exact ratio numerator must be > 0");
        }

        BigInteger gcd = numerator.gcd(denominator);
        numerator = numerator.divide(gcd);
        denominator = denominator.divide(gcd);
    }

    public double toDouble() {
        return numerator.doubleValue() / denominator.doubleValue();
    }

    /**
     * Returns the inverse ratio (denominator/numerator).
     */
    public ExactRatio inverse() {
        return new ExactRatio(denominator, numerator);
    }

    /**
     * Multiplies two exact ratios and returns a reduced result.
     */
    public ExactRatio multiply(ExactRatio other) {
        Checks.checkNotNull(other, "other");
        return new ExactRatio(numerator.multiply(other.numerator), denominator.multiply(other.denominator));
    }

    /**
     * Divides this ratio by other and returns a reduced result.
     */
    public ExactRatio divide(ExactRatio other) {
        Checks.checkNotNull(other, "other");
        return multiply(other.inverse());
    }

    /**
     * Applies this ratio to an integer amount. Returns null if the result is not an integer.
     */
    public Long applyExact(long amount) {
        BigInteger product = BigInteger.valueOf(amount).multiply(numerator);
        BigInteger[] divRem = product.divideAndRemainder(denominator);
        if (divRem[1].signum() != 0) {
            return null;
        }
        return divRem[0].longValueExact();
    }

    @Override
    public String toString() {
        if (denominator.equals(BigInteger.ONE)) {
            return numerator.toString();
        }
        return numerator + "/" + denominator;
    }
}
