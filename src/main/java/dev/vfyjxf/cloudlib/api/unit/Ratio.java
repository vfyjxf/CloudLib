package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
import dev.vfyjxf.cloudlib.api.unit.exception.InexactResultException;
import dev.vfyjxf.cloudlib.util.Checks;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * An immutable, exact rational number backed by {@link BigInteger}.
 * Always stored in lowest terms with a positive denominator.
 *
 * <p>The {@code exact} flag is provenance metadata: values built with {@link #approximate}
 * carry {@code exact=false}, and arithmetic propagates it by AND-ing the operands.
 * The flag is not part of equality — an approximate 1/9 equals an exact 1/9.
 */
@NotNullByDefault
public final class Ratio implements Comparable<Ratio> {

    public static final Ratio ZERO = new Ratio(BigInteger.ZERO, BigInteger.ONE, true);
    public static final Ratio ONE = new Ratio(BigInteger.ONE, BigInteger.ONE, true);

    public static Ratio of(long value) {
        return new Ratio(BigInteger.valueOf(value), BigInteger.ONE, true);
    }

    public static Ratio of(long numerator, long denominator) {
        if (denominator == 0) throw new IllegalArgumentException("denominator can't be zero");
        return create(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator), true);
    }

    /**
     * Exact decimal conversion: {@code ofDecimal(0.1)} is exactly 1/10.
     */
    public static Ratio ofDecimal(double value) {
        return fromDecimal(value, true);
    }

    /**
     * A lossy rule ratio: numerically exact (1/10 for {@code 0.1}), but flagged as
     * approximate in origin. The flag propagates through arithmetic and conversions.
     */
    public static Ratio approximate(double value) {
        return fromDecimal(value, false);
    }

    /**
     * A lossy rule ratio from a fraction, flagged as approximate in origin.
     */
    public static Ratio approximate(long numerator, long denominator) {
        if (denominator == 0) throw new IllegalArgumentException("denominator can't be zero");
        return create(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator), false);
    }

    private static Ratio fromDecimal(double value, boolean exact) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("value must be finite: " + value);
        BigDecimal decimal = BigDecimal.valueOf(value);
        if (decimal.signum() == 0) return create(BigInteger.ZERO, BigInteger.ONE, exact);
        BigInteger numerator = decimal.unscaledValue();
        int scale = decimal.scale();
        if (scale <= 0) {
            return create(numerator.multiply(BigInteger.TEN.pow(-scale)), BigInteger.ONE, exact);
        }
        return create(numerator, BigInteger.TEN.pow(scale), exact);
    }

    /**
     * Accepts {@code "9:1"}, {@code "1/9"}, {@code "0.144"} or a plain integer {@code "20"}.
     */
    public static Ratio parse(String str) {
        Checks.checkNotNull(str, "str");
        String trimmed = str.trim();
        int colon = trimmed.indexOf(':');
        int slash = trimmed.indexOf('/');
        try {
            if (colon >= 0) {
                long num = Long.parseLong(trimmed.substring(0, colon).trim());
                long den = Long.parseLong(trimmed.substring(colon + 1).trim());
                return of(num, den);
            }
            if (slash >= 0) {
                long num = Long.parseLong(trimmed.substring(0, slash).trim());
                long den = Long.parseLong(trimmed.substring(slash + 1).trim());
                return of(num, den);
            }
            if (trimmed.indexOf('.') >= 0 || trimmed.indexOf('e') >= 0 || trimmed.indexOf('E') >= 0) {
                return ofDecimal(Double.parseDouble(trimmed));
            }
            return of(Long.parseLong(trimmed));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a valid ratio: " + str, e);
        }
    }

    private static Ratio create(BigInteger numerator, BigInteger denominator, boolean exact) {
        if (denominator.signum() == 0) throw new IllegalArgumentException("denominator can't be zero");
        if (numerator.signum() == 0) {
            return exact ? ZERO : new Ratio(BigInteger.ZERO, BigInteger.ONE, false);
        }
        if (denominator.signum() < 0) {
            numerator = numerator.negate();
            denominator = denominator.negate();
        }
        BigInteger gcd = numerator.gcd(denominator);
        if (!gcd.equals(BigInteger.ONE)) {
            numerator = numerator.divide(gcd);
            denominator = denominator.divide(gcd);
        }
        if (exact && denominator.equals(BigInteger.ONE) && numerator.equals(BigInteger.ONE)) {
            return ONE;
        }
        return new Ratio(numerator, denominator, exact);
    }

    private final BigInteger numerator;
    private final BigInteger denominator;
    private final boolean exact;

    private Ratio(BigInteger numerator, BigInteger denominator, boolean exact) {
        this.numerator = numerator;
        this.denominator = denominator;
        this.exact = exact;
    }

    public BigInteger numerator() {
        return numerator;
    }

    public BigInteger denominator() {
        return denominator;
    }

    /**
     * Whether this value has an exact origin (not derived from any approximate rule).
     */
    public boolean isExact() {
        return exact;
    }

    public boolean isZero() {
        return numerator.signum() == 0;
    }

    public boolean isIntegral() {
        return denominator.equals(BigInteger.ONE);
    }

    public int signum() {
        return numerator.signum();
    }

    public Ratio multiply(Ratio other) {
        Checks.checkNotNull(other, "other");
        return create(
                numerator.multiply(other.numerator),
                denominator.multiply(other.denominator),
                exact && other.exact
        );
    }

    public Ratio multiply(long value) {
        return create(numerator.multiply(BigInteger.valueOf(value)), denominator, exact);
    }

    public Ratio divide(Ratio other) {
        Checks.checkNotNull(other, "other");
        if (other.isZero()) throw new IllegalArgumentException("can't divide by zero");
        return create(
                numerator.multiply(other.denominator),
                denominator.multiply(other.numerator),
                exact && other.exact
        );
    }

    public Ratio divide(long value) {
        if (value == 0) throw new IllegalArgumentException("can't divide by zero");
        return create(numerator, denominator.multiply(BigInteger.valueOf(value)), exact);
    }

    public Ratio inverse() {
        if (isZero()) throw new IllegalArgumentException("zero has no inverse");
        return create(denominator, numerator, exact);
    }

    public Ratio add(Ratio other) {
        Checks.checkNotNull(other, "other");
        return create(
                numerator.multiply(other.denominator).add(other.numerator.multiply(denominator)),
                denominator.multiply(other.denominator),
                exact && other.exact
        );
    }

    public Ratio subtract(Ratio other) {
        Checks.checkNotNull(other, "other");
        return create(
                numerator.multiply(other.denominator).subtract(other.numerator.multiply(denominator)),
                denominator.multiply(other.denominator),
                exact && other.exact
        );
    }

    public Ratio negate() {
        return create(numerator.negate(), denominator, exact);
    }

    /**
     * Returns the exact integral value, or throws if this ratio is not integral.
     */
    public long toLongExact() {
        if (!isIntegral()) throw new InexactResultException("Ratio " + this + " is not an integer");
        try {
            return numerator.longValueExact();
        } catch (ArithmeticException e) {
            throw new InexactResultException("Ratio " + this + " overflows long", e);
        }
    }

    public long floor() {
        BigInteger[] divRem = numerator.divideAndRemainder(denominator);
        BigInteger quotient = divRem[0];
        if (divRem[1].signum() != 0 && numerator.signum() < 0) {
            quotient = quotient.subtract(BigInteger.ONE);
        }
        return quotient.longValue();
    }

    public double toDouble() {
        return numerator.doubleValue() / denominator.doubleValue();
    }

    /**
     * Renders as {@code "9:1"}.
     */
    public String describe() {
        return numerator + ":" + denominator;
    }

    @Override
    public String toString() {
        if (isIntegral()) return numerator.toString();
        return numerator + "/" + denominator;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ratio other)) return false;
        return numerator.equals(other.numerator) && denominator.equals(other.denominator);
    }

    @Override
    public int hashCode() {
        return 31 * numerator.hashCode() + denominator.hashCode();
    }

    @Override
    public int compareTo(Ratio other) {
        return numerator.multiply(other.denominator).compareTo(other.numerator.multiply(denominator));
    }
}
