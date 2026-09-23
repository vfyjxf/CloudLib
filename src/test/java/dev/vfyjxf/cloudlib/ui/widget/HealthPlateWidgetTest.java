package dev.vfyjxf.cloudlib.ui.widget;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HealthPlateWidget's geometry, colour and number rules — the pure half of
 * the Neat bar semantics; the render half only wires these into the canvas.
 */
class HealthPlateWidgetTest {

    // region colour

    @Test
    void hueFollowsTheNeatFormula() {
        // zero health clamps the negative branch to pure red
        assertEquals(0.0, HealthPlateWidget.hue(0.0), 1.0e-9);
        // hue = max(0, fraction/3 - 0.07)
        assertEquals(1.0 / 3 - 0.07, HealthPlateWidget.hue(1.0), 1.0e-9);
        // the low half stays red until the fraction passes 0.21
        assertEquals(0.0, HealthPlateWidget.hue(0.21), 1.0e-9);
        assertTrue(HealthPlateWidget.hue(0.22) > 0);
    }

    @Test
    void fillColorIsFullSaturationHsvAtAlpha127() {
        // fraction 0 → hue 0 → pure red
        assertEquals(0x7FFF0000, HealthPlateWidget.fillColor(0.0));
        // the reachable hue band is [0, 1/3 - 0.07]: full health lands at
        // hue 0.2633 — segment 1 at f=0.58 → (0.42, 1, 0), a green-leaning yellow
        assertEquals(0x7F6BFF00, HealthPlateWidget.fillColor(1.0));
        // out-of-range fractions clamp before colouring
        assertEquals(HealthPlateWidget.fillColor(1.0), HealthPlateWidget.fillColor(1.7));
        assertEquals(HealthPlateWidget.fillColor(0.0), HealthPlateWidget.fillColor(-3));
    }

    @Test
    void hsvToRgbWalksTheColorWheel() {
        assertEquals(0xFF0000, HealthPlateWidget.hsvToRgb(0.0));
        // 1/6 of the wheel from red is yellow, 1/3 is green, 2/3 is blue
        assertEquals(0xFFFF00, HealthPlateWidget.hsvToRgb(1.0 / 6));
        assertEquals(0x00FF00, HealthPlateWidget.hsvToRgb(1.0 / 3));
        assertEquals(0x0000FF, HealthPlateWidget.hsvToRgb(2.0 / 3));
        assertEquals(0xFF0000, HealthPlateWidget.hsvToRgb(1.0));
        // halfway between red and yellow rounds its green half up
        assertEquals(0xFF8000, HealthPlateWidget.hsvToRgb(1.0 / 12));
    }

    // endregion

    // region bar geometry

    @Test
    void fillWidthTracksTheFractionExactly() {
        assertEquals(24, HealthPlateWidget.fillWidth(48, 0.5));
        assertEquals(48, HealthPlateWidget.fillWidth(48, 1.0));
        assertEquals(0, HealthPlateWidget.fillWidth(48, 0.0));
        // clamped both ways — no negative or overflowing fills
        assertEquals(48, HealthPlateWidget.fillWidth(48, 1.5));
        assertEquals(0, HealthPlateWidget.fillWidth(48, -0.25));
    }

    @Test
    void boardWidthIsFiftyTwoUnlessTheNameIsWider() {
        assertEquals(52, HealthPlateWidget.panelWidth(0));
        assertEquals(52, HealthPlateWidget.panelWidth(40));
        // a 60px name needs 60 + 2*2 padding
        assertEquals(64, HealthPlateWidget.panelWidth(60));
    }

    @Test
    void boardHeightFollowsNameAndArmor() {
        // nameless, no armor: 2 top + 4 bar + 6 bottom
        assertEquals(12, HealthPlateWidget.panelHeight(0, 0));
        // named (9px line): 6 top + 9 name + 4 bar + 6 bottom
        assertEquals(25, HealthPlateWidget.panelHeight(9, 0));
        // each armor row adds a 16px icon line
        assertEquals(28, HealthPlateWidget.panelHeight(0, 1));
        assertEquals(57, HealthPlateWidget.panelHeight(9, 2));
    }

    // endregion

    // region armor

    @Test
    void armorDrawsOneIconPerFivePoints() {
        assertEquals(0, HealthPlateWidget.armorIcons(0));
        assertEquals(0, HealthPlateWidget.armorIcons(4));
        assertEquals(1, HealthPlateWidget.armorIcons(5));
        assertEquals(1, HealthPlateWidget.armorIcons(7));
        assertEquals(4, HealthPlateWidget.armorIcons(20));
        assertEquals(0, HealthPlateWidget.armorIcons(-5));
    }

    @Test
    void armorRowsWrapAtTheBoardWidth() {
        // the 52px board fits 3 icons per row (48px inner width)
        assertEquals(0, HealthPlateWidget.armorRows(4, 52));
        assertEquals(1, HealthPlateWidget.armorRows(15, 52));
        // 4 icons (20 armor) wrap to a second row
        assertEquals(2, HealthPlateWidget.armorRows(20, 52));
        // 25 armor = 5 icons = 2 rows; on a wide-enough board it stays one row
        assertEquals(2, HealthPlateWidget.armorRows(25, 52));
        assertEquals(1, HealthPlateWidget.armorRows(25, 100));
    }

    // endregion

    // region numbers

    @Test
    void numbersFormatUpToTwoDecimalsWithoutTrailingZeros() {
        assertEquals("20", HealthPlateWidget.formatNumber(20.0));
        assertEquals("19.5", HealthPlateWidget.formatNumber(19.5));
        assertEquals("0.27", HealthPlateWidget.formatNumber(0.2659));
        assertEquals("100", HealthPlateWidget.formatNumber(100));
        assertEquals("0.01", HealthPlateWidget.formatNumber(0.01));
        assertEquals("0", HealthPlateWidget.formatNumber(0.004));
    }

    // endregion

    // region switches

    @Test
    void numberAndArmorSwitchesDefaultOnExceptArmor() {
        HealthPlateWidget plate = new HealthPlateWidget(() -> 10, () -> 20, () -> null);
        assertTrue(plate.showCurrentHealth());
        assertTrue(plate.showMaxHealth());
        assertTrue(plate.showPercentage());
        assertFalse(plate.showArmor());

        plate.setShowCurrentHealth(false).setShowMaxHealth(false).setShowPercentage(false).setShowArmor(true);
        assertFalse(plate.showCurrentHealth());
        assertFalse(plate.showMaxHealth());
        assertFalse(plate.showPercentage());
        assertTrue(plate.showArmor());
    }

    @Test
    void fractionClampsAgainstZeroAndOverflowingMaxima() {
        HealthPlateWidget plate = new HealthPlateWidget(() -> 10, () -> 20, () -> null);
        assertEquals(0.5, plate.fraction(), 1.0e-9);

        HealthPlateWidget over = new HealthPlateWidget(() -> 30, () -> 20, () -> null);
        assertEquals(1.0, over.fraction(), 1.0e-9);

        HealthPlateWidget deadMax = new HealthPlateWidget(() -> 30, () -> 0, () -> null);
        assertEquals(0.0, deadMax.fraction(), 1.0e-9);
    }

    // endregion
}
