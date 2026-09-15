package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.unit.exception.InexactResultException;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RatioTest {

    @Test
    void ofReducesToLowestTerms() {
        Ratio ratio = Ratio.of(2, 4);
        assertEquals(BigInteger.ONE, ratio.numerator());
        assertEquals(BigInteger.TWO, ratio.denominator());
        assertEquals(Ratio.of(1, 2), ratio);
    }

    @Test
    void denominatorIsAlwaysPositive() {
        Ratio ratio = Ratio.of(1, -2);
        assertEquals(BigInteger.ONE.negate(), ratio.numerator());
        assertEquals(BigInteger.TWO, ratio.denominator());
    }

    @Test
    void ofRejectsZeroDenominator() {
        assertThrows(IllegalArgumentException.class, () -> Ratio.of(1, 0));
    }

    @Test
    void ofDecimalIsExact() {
        assertEquals(Ratio.of(1, 10), Ratio.ofDecimal(0.1));
        assertEquals(Ratio.of(18, 125), Ratio.ofDecimal(0.144));
        assertEquals(Ratio.of(20), Ratio.ofDecimal(20.0));
    }

    @Test
    void parseAcceptsColonSlashDecimalAndInteger() {
        assertEquals(Ratio.of(9), Ratio.parse("9:1"));
        assertEquals(Ratio.of(1, 9), Ratio.parse("1/9"));
        assertEquals(Ratio.of(18, 125), Ratio.parse("0.144"));
        assertEquals(Ratio.of(20), Ratio.parse("20"));
        assertThrows(IllegalArgumentException.class, () -> Ratio.parse("abc"));
    }

    @Test
    void arithmeticIsExact() {
        assertEquals(Ratio.of(5, 6), Ratio.of(1, 2).add(Ratio.of(1, 3)));
        assertEquals(Ratio.of(1, 6), Ratio.of(1, 2).subtract(Ratio.of(1, 3)));
        assertEquals(Ratio.of(1, 2), Ratio.of(2, 3).multiply(Ratio.of(3, 4)));
        assertEquals(Ratio.of(8, 9), Ratio.of(2, 3).divide(Ratio.of(3, 4)));
        assertEquals(Ratio.of(3, 2), Ratio.of(2, 3).inverse());
        assertEquals(Ratio.of(-1, 2), Ratio.of(1, 2).negate());
    }

    @Test
    void toLongExactThrowsOnNonIntegral() {
        assertEquals(20, Ratio.of(20).toLongExact());
        assertThrows(InexactResultException.class, () -> Ratio.of(1, 2).toLongExact());
    }

    @Test
    void floorRoundsTowardsNegativeInfinity() {
        assertEquals(1, Ratio.of(10, 9).floor());
        assertEquals(-1, Ratio.of(-1, 2).floor());
        assertEquals(20, Ratio.of(20).floor());
    }

    @Test
    void toDoubleApproximates() {
        assertEquals(0.5, Ratio.of(1, 2).toDouble(), 1e-9);
    }

    @Test
    void textForms() {
        assertEquals("9:1", Ratio.of(9).describe());
        assertEquals("1:9", Ratio.of(1, 9).describe());
        assertEquals("1/9", Ratio.of(1, 9).toString());
        assertEquals("20", Ratio.of(20).toString());
    }

    @Test
    void compareToOrdersByValue() {
        assertTrue(Ratio.of(1, 9).compareTo(Ratio.of(1, 2)) < 0);
        assertEquals(0, Ratio.of(2, 4).compareTo(Ratio.of(1, 2)));
        assertTrue(Ratio.of(9).compareTo(Ratio.of(1, 9)) > 0);
    }

    @Test
    void approximateFactoriesAreNumericallyExactButFlagged() {
        Ratio fromDecimal = Ratio.approximate(0.1);
        assertEquals(Ratio.of(1, 10), fromDecimal);
        assertFalse(fromDecimal.isExact());

        Ratio fromFraction = Ratio.approximate(2, 8);
        assertEquals(Ratio.of(1, 4), fromFraction);
        assertFalse(fromFraction.isExact());

        assertTrue(Ratio.of(1, 10).isExact());
        assertTrue(Ratio.ofDecimal(0.1).isExact());
        assertTrue(Ratio.parse("1/9").isExact());
    }

    @Test
    void exactFlagPropagatesByAnd() {
        Ratio exact = Ratio.of(1, 2);
        Ratio approximate = Ratio.approximate(1, 3);

        assertFalse(exact.multiply(approximate).isExact());
        assertFalse(approximate.divide(exact).isExact());
        assertFalse(exact.add(approximate).isExact());
        assertFalse(exact.subtract(approximate).isExact());
        assertTrue(exact.multiply(Ratio.of(1, 3)).isExact());
        assertFalse(approximate.multiply(2).isExact());
        assertFalse(approximate.divide(2).isExact());
        assertFalse(approximate.inverse().isExact());
        assertFalse(approximate.negate().isExact());
        assertTrue(exact.inverse().isExact());
    }

    @Test
    void equalityIgnoresTheExactFlag() {
        Ratio approximate = Ratio.approximate(1, 9);
        Ratio exact = Ratio.of(1, 9);

        assertEquals(exact, approximate);
        assertEquals(exact.hashCode(), approximate.hashCode());
        assertEquals(0, exact.compareTo(approximate));
    }
}
