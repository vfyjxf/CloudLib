package dev.vfyjxf.cloudlib.api.ui.inworld.algorithm;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.ui.inworld.OffscreenProjector;
import dev.vfyjxf.cloudlib.api.ui.inworld.ScreenEdge;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AngleEncoderTest {

    private static final double screenWidth = 1920;
    private static final double screenHeight = 1080;
    private static final double inset = 24;

    private static AngleEncoder encoder(double lambda, double band, int dwell) {
        return new AngleEncoder(screenWidth, screenHeight, inset, AngleEncoder.Config.of(lambda, band, dwell));
    }

    private static OffscreenProjector.Result offscreen(double angle) {
        double dx = Math.cos(angle);
        double dy = Math.sin(angle);
        return new OffscreenProjector.Result(false, false, dx, dy, angle, null, new FloatPos(dx, dy), edgeOf(angle));
    }

    private static ScreenEdge edgeOf(double angle) {
        double halfW = screenWidth * 0.5 - inset;
        double halfH = screenHeight * 0.5 - inset;
        double tx = Math.abs(Math.cos(angle)) < 1.0e-9 ? Double.POSITIVE_INFINITY : halfW / Math.abs(Math.cos(angle));
        double ty = Math.abs(Math.sin(angle)) < 1.0e-9 ? Double.POSITIVE_INFINITY : halfH / Math.abs(Math.sin(angle));
        if (tx <= ty) {
            return Math.cos(angle) < 0 ? ScreenEdge.left : ScreenEdge.right;
        }
        return Math.sin(angle) < 0 ? ScreenEdge.top : ScreenEdge.bottom;
    }

    @Test
    void landsOnTheInsetRectangleAtTheRayAngle() {
        AngleEncoder encoder = encoder(10, 0.2, 1);
        encoder.snap(0);

        AngleEncoder.Output output = encoder.update(offscreen(0), 1.0 / 60.0);

        assertTrue(output.active());
        assertEquals(ScreenEdge.right, output.edge());
        assertEquals(screenWidth / 2 + (screenWidth / 2 - inset), output.position().x(), 1.0e-6);
        assertEquals(screenHeight / 2, output.position().y(), 1.0e-6);
    }

    @Test
    void snapJumpsTheSmoothedAngleWithoutGlide() {
        AngleEncoder encoder = encoder(10, 0.2, 1);
        encoder.snap(Math.PI);

        AngleEncoder.Output output = encoder.update(offscreen(Math.PI), 0.0);

        assertEquals(ScreenEdge.left, output.edge());
        assertEquals(screenWidth / 2 - (screenWidth / 2 - inset), output.position().x(), 1.0e-6);
        assertEquals(screenHeight / 2, output.position().y(), 1.0e-6);
    }

    @Test
    void targetCirclingTheCameraSlidesTheIndicatorAroundAllFourEdges() {
        AngleEncoder encoder = encoder(10, 0.2, 1);
        encoder.snap(0);

        int steps = 720;
        double dt = 1.0 / 60.0;
        List<AngleEncoder.Output> outputs = new ArrayList<>(steps);
        for (int k = 1; k <= steps; k++) {
            double angle = 2 * Math.PI * k / steps;
            outputs.add(encoder.update(offscreen(angle), dt));
        }

        // continuous position: no step larger than a fraction of the perimeter,
        // so no teleport anywhere — corner crossings included
        for (int i = 1; i < outputs.size(); i++) {
            double dx = outputs.get(i).position().x() - outputs.get(i - 1).position().x();
            double dy = outputs.get(i).position().y() - outputs.get(i - 1).position().y();
            double step = Math.sqrt(dx * dx + dy * dy);
            assertTrue(step < 30.0, "teleport of " + step + " px between frames " + (i - 1) + " and " + i);
        }

        // the position always sits on the inset rectangle boundary
        double halfW = screenWidth / 2 - inset;
        double halfH = screenHeight / 2 - inset;
        for (AngleEncoder.Output output : outputs) {
            boolean onVertical = Math.abs(Math.abs(output.position().x() - screenWidth / 2) - halfW) < 1.0e-6;
            boolean onHorizontal = Math.abs(Math.abs(output.position().y() - screenHeight / 2) - halfH) < 1.0e-6;
            assertTrue(onVertical || onHorizontal, "position off the perimeter: " + output.position());
        }

        // the gated edge walks clockwise through all four edges and comes home
        List<ScreenEdge> sequence = new ArrayList<>();
        sequence.add(ScreenEdge.right);
        for (AngleEncoder.Output output : outputs) {
            if (sequence.get(sequence.size() - 1) != output.edge()) {
                sequence.add(output.edge());
            }
        }
        assertEquals(
            List.of(ScreenEdge.right, ScreenEdge.bottom, ScreenEdge.left, ScreenEdge.top, ScreenEdge.right),
            sequence
        );
    }

    @Test
    void cornerJitterDoesNotFlipTheReportedEdge() {
        AngleEncoder encoder = encoder(10, 0.2, 1);
        encoder.snap(0);

        // hover across the right/bottom corner boundary (angle π/4)
        double corner = Math.PI / 4;
        for (int i = 0; i < 50; i++) {
            double angle = corner + ((i % 2 == 0) ? 0.05 : -0.05);
            encoder.update(offscreen(angle), 1.0 / 60.0);
        }

        ScreenEdge settled = encoder.update(offscreen(corner + 0.05), 1.0 / 60.0).edge();
        for (int i = 0; i < 50; i++) {
            double angle = corner + ((i % 2 == 0) ? 0.05 : -0.05);
            assertEquals(settled, encoder.update(offscreen(angle), 1.0 / 60.0).edge());
        }
    }

    @Test
    void behindTheCameraTargetsStayActiveAndContinuous() {
        AngleEncoder encoder = encoder(10, 0.2, 1);
        encoder.snap(Math.PI);

        AngleEncoder.Output first = encoder.update(offscreen(Math.PI), 0.0);
        AngleEncoder.Output second = encoder.update(offscreen(Math.PI + 0.01), 1.0 / 60.0);

        assertTrue(first.active());
        assertTrue(second.active());
        assertTrue(second.edge() == ScreenEdge.left || second.edge() == ScreenEdge.bottom);
        double dx = second.position().x() - first.position().x();
        double dy = second.position().y() - first.position().y();
        assertTrue(Math.sqrt(dx * dx + dy * dy) < 30.0);
    }

    @Test
    void onScreenResultsAreReportedInactive() {
        AngleEncoder encoder = encoder(10, 0.2, 1);
        encoder.snap(0);

        OffscreenProjector.Result onScreen = new OffscreenProjector.Result(
            true,
            false,
            1,
            0,
            0,
            new FloatPos(1500, 540),
            null,
            null
        );
        AngleEncoder.Output output = encoder.update(onScreen, 1.0 / 60.0);

        assertFalse(output.active());
        // the encoder still tracks the angle so re-activation glides from a sane place
        assertEquals(ScreenEdge.right, output.edge());
    }

    @Test
    void rejectsInvalidUse() {
        assertThrows(IllegalArgumentException.class, () -> encoder(-1, 0.2, 1));
        assertThrows(IllegalArgumentException.class, () -> encoder(10, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> encoder(10, 0.2, 0));
        assertThrows(
            IllegalArgumentException.class,
            () -> new AngleEncoder(0, 1080, 24, AngleEncoder.Config.of(10, 0.2, 1))
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new AngleEncoder(1920, 0, 24, AngleEncoder.Config.of(10, 0.2, 1))
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new AngleEncoder(1920, 1080, -1, AngleEncoder.Config.of(10, 0.2, 1))
        );
        assertThrows(IllegalArgumentException.class, () -> encoder(10, 0.2, 1).snap(Double.NaN));
    }
}
