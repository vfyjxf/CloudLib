package dev.vfyjxf.cloudlib.api.ui.inworld.algorithm;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.FloatRect;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttachPointResolverTest {

    private static final double eps = 1.0e-9;

    /** The workhorse rect: (200,100)–(360,200), center (280,150). */
    private static final FloatRect rect = new FloatRect(200, 100, 160, 100);

    private static final AttachPointResolver.Config config = AttachPointResolver.Config.ofDefaults();

    private static void assertPoint(FloatPos actual, double x, double y, String what) {
        assertEquals(x, actual.x(), eps, what + " x");
        assertEquals(y, actual.y(), eps, what + " y");
    }

    @Test
    void targetRightPicksTheRightMidPort() {
        AttachPointResolver.Port port = new AttachPointResolver(config).resolve("p", rect, new FloatPos(500, 150), 0.0);

        assertEquals(AttachPointResolver.Face.right, port.face());
        assertPoint(port.point(), 360, 150, "the right edge midpoint");
        assertEquals(1.0, port.normalX(), eps);
        assertEquals(0.0, port.normalY(), eps);
    }

    @Test
    void purePickFaceFollowsTheQuadrantsWithoutHistory() {
        assertEquals(AttachPointResolver.Face.right, AttachPointResolver.pickFace(rect, 500, 150, null, config));
        assertEquals(AttachPointResolver.Face.left, AttachPointResolver.pickFace(rect, 0, 150, null, config));
        assertEquals(AttachPointResolver.Face.top, AttachPointResolver.pickFace(rect, 280, 0, null, config));
        assertEquals(AttachPointResolver.Face.bottom, AttachPointResolver.pickFace(rect, 280, 400, null, config));
    }

    @Test
    void faceIsHeldInsideTheAngularBand() {
        // target direction ~50° off the right normal — inside the 55° hold
        // band even though the nearest face is already bottom
        double x = 280 + 300 * Math.cos(Math.toRadians(50));
        double y = 150 + 300 * Math.sin(Math.toRadians(50));

        AttachPointResolver.Face face = AttachPointResolver
                .pickFace(rect, x, y, AttachPointResolver.Face.right, config);

        assertEquals(AttachPointResolver.Face.right, face, "50° ≤ 55° holds the incumbent");
        assertEquals(AttachPointResolver.Face.bottom, AttachPointResolver.nearestFace(rect, x, y));
    }

    @Test
    void faceSwitchesPastTheBandWhenTheOffsetClearsTheDeadzone() {
        // ~60° off the right normal (30° from bottom — inside the enter band),
        // and 260 px of along-edge offset — well past the 48 px deadzone
        double x = 280 + 300 * Math.cos(Math.toRadians(60));
        double y = 150 + 300 * Math.sin(Math.toRadians(60));

        AttachPointResolver.Face face = AttachPointResolver
                .pickFace(rect, x, y, AttachPointResolver.Face.right, config);

        assertEquals(AttachPointResolver.Face.bottom, face);
    }

    @Test
    void bandSwitchAloneIsNotEnoughInsideTheExitDeadzone() {
        // 67° off the right normal — past the hold band and inside the enter
        // band — but only 47 px along the right edge from its midpoint
        AttachPointResolver.Face held = AttachPointResolver
                .pickFace(rect, 300, 197, AttachPointResolver.Face.right, config);
        assertEquals(AttachPointResolver.Face.right, held, "47 px ≤ 48 px deadzone holds the face");

        // one more pixel of offset and the switch goes through
        AttachPointResolver.Face switched = AttachPointResolver
                .pickFace(rect, 300, 199, AttachPointResolver.Face.right, config);
        assertEquals(AttachPointResolver.Face.bottom, switched, "49 px > 48 px deadzone allows the switch");
    }

    @Test
    void oppositeIncumbentEscapesToTheNearestFaceAtOnce() {
        AttachPointResolver resolver = new AttachPointResolver(config);
        // commit the right face, then swing the anchor dead-left: 180° off
        // the incumbent's normal — a backwards-facing port can never stand,
        // so the committed face switches without waiting out the deadzone
        AttachPointResolver.Port start = resolver.resolve("p", rect, new FloatPos(500, 150), 1.0 / 60.0);
        assertEquals(AttachPointResolver.Face.right, start.face());

        AttachPointResolver.Port port = resolver.resolve("p", rect, new FloatPos(0, 150), 1.0 / 60.0);
        assertEquals(
            AttachPointResolver.Face.left,
            port.face(),
            "the opposite incumbent yields in one resolve — the 48 px deadzone must not trap it"
        );

        // the port point still slides along the perimeter — the escape
        // changes the committed face, not the no-teleport rule
        double step = Math.hypot(port.point().x() - 360, port.point().y() - 150);
        assertTrue(step < AttachPointResolver.perimeter(rect) * 0.2, "no teleport: " + step);
    }

    @Test
    void adjacentIncumbentHysteresisIsUnchanged() {
        // ~50° off the top normal — inside the 55° hold band even though the
        // right face is already nearer: the escape never reaches it
        double x = 280 + 300 * Math.cos(Math.toRadians(-40));
        double y = 150 + 300 * Math.sin(Math.toRadians(-40));
        assertEquals(
            AttachPointResolver.Face.top,
            AttachPointResolver.pickFace(rect, x, y, AttachPointResolver.Face.top, config),
            "50° ≤ 55° holds the adjacent incumbent"
        );
        assertEquals(AttachPointResolver.Face.right, AttachPointResolver.nearestFace(rect, x, y));

        // ~85° off the incumbent — adjacent, not opposite: the escape does
        // not fire, and the 47 px along-edge offset still sits inside the
        // 48 px deadzone
        AttachPointResolver.Face held = AttachPointResolver
                .pickFace(rect, 284, 197, AttachPointResolver.Face.right, config);
        assertEquals(AttachPointResolver.Face.right, held, "85° off + inside the deadzone still holds");
    }

    @Test
    void theOppositeFaceTrapRegresses() {
        // the exact trap geometry: incumbent right, target dead-left at
        // center height — 180° off, 0 px along the exit edge. Before the
        // escape the deadzone held 'right' forever and the leader wrapped
        // to the wrong corner
        assertEquals(
            AttachPointResolver.Face.left,
            AttachPointResolver.pickFace(rect, 0, 150, AttachPointResolver.Face.right, config)
        );
        assertEquals(
            AttachPointResolver.Face.bottom,
            AttachPointResolver.pickFace(rect, 280, 400, AttachPointResolver.Face.top, config)
        );
    }

    @Test
    void faceSwitchSlidesAlongThePerimeterWithoutTeleporting() {
        AttachPointResolver resolver = new AttachPointResolver(config);
        // commit the right face first
        AttachPointResolver.Port start = resolver.resolve("p", rect, new FloatPos(500, 150), 1.0 / 60.0);
        assertPoint(start.point(), 360, 150, "rest at the right mid-port");

        // the target sweeps past the band + deadzone: right → bottom
        AttachPointResolver.Port first = resolver.resolve("p", rect, new FloatPos(430, 410), 1.0 / 60.0);
        double firstStep = Math.hypot(first.point().x() - 360, first.point().y() - 150);
        assertTrue(firstStep < 10.0, "the slide starts gently, no teleport: " + firstStep);
        assertEquals(AttachPointResolver.Face.bottom, first.face(), "the committed face already switched");

        double perimeter = AttachPointResolver.perimeter(rect);
        List<FloatPos> positions = new ArrayList<>();
        positions.add(first.point());
        // 160 ms at 60 fps ≈ 10 frames of travel
        for (int i = 0; i < 9; i++) {
            AttachPointResolver.Port port = resolver.resolve("p", rect, new FloatPos(430, 410), 1.0 / 60.0);
            positions.add(port.point());
        }
        for (int i = 1; i < positions.size(); i++) {
            double step = Math.hypot(
                positions.get(i).x() - positions.get(i - 1).x(),
                positions.get(i).y() - positions.get(i - 1).y()
            );
            assertTrue(step > 0.0, "the slide advances");
            assertTrue(step < perimeter * 0.2, "no step may teleport: " + step);
        }
        // drain the slide to its landing
        for (int i = 0; i < 15; i++) {
            resolver.resolve("p", rect, new FloatPos(430, 410), 1.0 / 60.0);
        }
        AttachPointResolver.Port landed = resolver.resolve("p", rect, new FloatPos(430, 410), 0.0);
        assertPoint(landed.point(), 280, 200, "the slide lands on the bottom mid-port");
    }

    @Test
    void theSlideTakesItsConfiguredDuration() {
        AttachPointResolver resolver = new AttachPointResolver(config);
        resolver.resolve("p", rect, new FloatPos(500, 150), 1.0 / 60.0);

        double slideSeconds = config.slideSeconds();
        resolver.resolve("p", rect, new FloatPos(430, 410), slideSeconds * 0.5);
        AttachPointResolver.Port mid = resolver.resolve("p", rect, new FloatPos(430, 410), 0.0);
        double tMid = AttachPointResolver.paramOf(rect, mid.point());
        double tStart = AttachPointResolver.faceParam(rect, AttachPointResolver.Face.right);
        double arc = AttachPointResolver.wrapArc(
            tStart,
            AttachPointResolver.faceParam(rect, AttachPointResolver.Face.bottom),
            AttachPointResolver.perimeter(rect)
        );
        double traveled = AttachPointResolver.wrapArc(tStart, tMid, AttachPointResolver.perimeter(rect));
        assertEquals(Math.abs(arc) * 0.5, traveled, Math.abs(arc) * 0.05, "half the duration, half the arc");

        resolver.resolve("p", rect, new FloatPos(430, 410), slideSeconds);
        AttachPointResolver.Port done = resolver.resolve("p", rect, new FloatPos(430, 410), 0.0);
        assertPoint(done.point(), 280, 200, "the slide completes after slideSeconds");
    }

    @Test
    void aPanelResizeMovesThePortByAtMostTheRectDelta() {
        AttachPointResolver resolver = new AttachPointResolver(config);
        resolver.resolve("p", rect, new FloatPos(500, 150), 1.0 / 60.0);

        // the panel shifts +4 and widens +24: the right mid-port moves 28 px
        FloatRect moved = new FloatRect(204, 100, 184, 100);
        AttachPointResolver.Port after = resolver.resolve("p", moved, new FloatPos(500, 150), 1.0 / 60.0);
        double step = Math.hypot(after.point().x() - 360, after.point().y() - 150);
        assertTrue(step <= 30.0, "the port moves by at most the rect's own change: " + step);
        assertEquals(AttachPointResolver.Face.right, after.face());

        FloatPos previous = after.point();
        for (int i = 0; i < 20; i++) {
            AttachPointResolver.Port port = resolver.resolve("p", moved, new FloatPos(500, 150), 1.0 / 60.0);
            double glide = Math.hypot(port.point().x() - previous.x(), port.point().y() - previous.y());
            assertTrue(glide < 30.0, "continuous settle onto the resized rect: " + glide);
            previous = port.point();
        }
        assertPoint(previous, 388, 150, "lands on the resized rect's right mid-port");
    }

    @Test
    void theStubConstantIsExposed() {
        assertEquals(16.0, AttachPointResolver.stubPx, 0.0);
    }

    @Test
    void restParamPinsToTheMidPortUnlessHovered() {
        // 40° off the right normal — inside the 55° hold band, far off-axis
        FloatPos offAxis = new FloatPos(
            280 + 300 * Math.cos(Math.toRadians(40)),
            150 + 300 * Math.sin(Math.toRadians(40))
        );

        assertEquals(
            AttachPointResolver.faceParam(rect, AttachPointResolver.Face.right),
            AttachPointResolver.restParam(rect, AttachPointResolver.Face.right, offAxis, false),
            eps,
            "at rest the port never leaves the edge midpoint"
        );
        // hovered: the center→target ray meets the right edge at y≈217, and the
        // 8 px corner margin clamps it to the bottom-right corner's approach
        double hovered = AttachPointResolver.restParam(rect, AttachPointResolver.Face.right, offAxis, true);
        assertPoint(
            AttachPointResolver.pointAt(rect, hovered),
            360,
            192,
            "the hover rest rides the edge towards the target"
        );
        // a target on the face's own axis meets the edge at its midpoint
        assertEquals(
            AttachPointResolver.faceParam(rect, AttachPointResolver.Face.right),
            AttachPointResolver.restParam(rect, AttachPointResolver.Face.right, new FloatPos(500, 150), true),
            eps
        );
        // a target on the face's own line leaves the ray parallel to the edge:
        // the fallback is the midpoint, never a division by zero
        assertEquals(
            AttachPointResolver.faceParam(rect, AttachPointResolver.Face.right),
            AttachPointResolver.restParam(rect, AttachPointResolver.Face.right, new FloatPos(280, 0), true),
            eps
        );
    }

    @Test
    void hoverTracksTheTargetAlongTheCommittedEdge() {
        AttachPointResolver resolver = new AttachPointResolver(config);
        // 40° off the right normal: held on the right face, but far down the edge
        FloatPos target = new FloatPos(
            280 + 300 * Math.cos(Math.toRadians(40)),
            150 + 300 * Math.sin(Math.toRadians(40))
        );
        for (int i = 0; i < 30; i++) {
            AttachPointResolver.Port port = resolver.resolve("p", rect, target, true, 1.0 / 60.0);
            assertEquals(AttachPointResolver.Face.right, port.face(), "hover never leaves the held face");
            assertPoint(port.point(), 360, port.point().y(), "the port stays on the committed edge");
        }

        AttachPointResolver.Port settled = resolver.resolve("p", rect, target, true, 1.0 / 60.0);
        assertPoint(settled.point(), 360, 192, "the port has tracked the target down the edge");
        assertEquals(1.0, settled.normalX(), eps, "the exit normal is still the edge's own");
        assertEquals(0.0, settled.normalY(), eps);
    }

    @Test
    void leavingHoverSlidesThePortBackToTheMidPort() {
        AttachPointResolver resolver = new AttachPointResolver(config);
        FloatPos target = new FloatPos(
            280 + 300 * Math.cos(Math.toRadians(40)),
            150 + 300 * Math.sin(Math.toRadians(40))
        );
        for (int i = 0; i < 30; i++) {
            resolver.resolve("p", rect, target, true, 1.0 / 60.0);
        }
        assertPoint(resolver.resolve("p", rect, target, true, 0.0).point(), 360, 192, "hovered rest");

        // the pointer leaves: the port glides back to the layout-stable midpoint
        AttachPointResolver.Port first = resolver.resolve("p", rect, target, false, 1.0 / 60.0);
        assertTrue(first.point().y() < 192.0, "the return starts immediately");
        assertTrue(first.point().y() > 150.0, "and does not teleport: " + first.point().y());
        for (int i = 0; i < 30; i++) {
            resolver.resolve("p", rect, target, false, 1.0 / 60.0);
        }
        AttachPointResolver.Port settled = resolver.resolve("p", rect, target, false, 0.0);
        assertPoint(settled.point(), 360, 150, "the port is back on the right mid-port");
    }

    @Test
    void theExitDeadzoneHoldsTheFaceAcrossPanelShifts() {
        AttachPointResolver resolver = new AttachPointResolver(config);
        // 67° off the right normal, held only because the along-edge offset
        // (47 px) sits inside the 48 px exit deadzone
        FloatPos nearEdge = new FloatPos(300, 197);
        resolver.resolve("p", rect, new FloatPos(500, 150), 1.0 / 60.0);
        assertEquals(
            AttachPointResolver.Face.right,
            resolver.resolve("p", rect, nearEdge, 0.0).face(),
            "the deadzone holds the face past the angular band"
        );

        // the panel itself lifts 4 px: the same target now sits 51 px along
        // the exit edge — past the deadzone, so the switch commits
        FloatRect lifted = new FloatRect(200, 96, 160, 100);
        assertEquals(
            AttachPointResolver.Face.bottom,
            resolver.resolve("p", lifted, nearEdge, 0.0).face(),
            "once the geometry clears the deadzone the switch commits"
        );
    }

    @Test
    void perimeterHelpersRoundTrip() {
        double perimeter = AttachPointResolver.perimeter(rect);
        assertEquals(520.0, perimeter, eps);

        for (AttachPointResolver.Face face : AttachPointResolver.Face.values()) {
            double t = AttachPointResolver.faceParam(rect, face);
            FloatPos point = AttachPointResolver.pointAt(rect, t);
            assertEquals(face, AttachPointResolver.edgeAt(rect, t), face + " edge at its own param");
            double back = AttachPointResolver.paramOf(rect, point);
            double delta = Math.abs(AttachPointResolver.wrapArc(t, back, perimeter));
            assertTrue(delta < eps || perimeter - delta < eps, face + " param round-trips: " + delta);
        }
    }

    @Test
    void wrapArcPicksTheShorterDirection() {
        double perimeter = 520.0;
        assertEquals(10.0, AttachPointResolver.wrapArc(100, 110, perimeter), eps);
        assertEquals(-10.0, AttachPointResolver.wrapArc(110, 100, perimeter), eps);
        assertEquals(20.0, AttachPointResolver.wrapArc(510, 10, perimeter), eps, "across the wrap point");
        assertEquals(-20.0, AttachPointResolver.wrapArc(10, 510, perimeter), eps);
    }

    @Test
    void replayingTheSameSequenceReproducesTheSamePorts() {
        List<FloatPos> runOne = driveSequence();
        List<FloatPos> runTwo = driveSequence();
        assertEquals(runOne, runTwo);
        assertNotEquals(List.of(), runOne);
    }

    private static List<FloatPos> driveSequence() {
        AttachPointResolver resolver = new AttachPointResolver(config);
        List<FloatPos> positions = new ArrayList<>();
        double sweep = 0;
        for (int i = 0; i < 60; i++) {
            sweep += 6.0;
            positions.add(
                resolver.resolve(
                    "p",
                    rect,
                    new FloatPos(280 + 300 * Math.cos(sweep), 150 + 300 * Math.sin(sweep)),
                    1.0 / 60.0
                ).point().copy()
            );
        }
        return positions;
    }

    @Test
    void forgetDropsTheOwnerState() {
        AttachPointResolver resolver = new AttachPointResolver(config);
        resolver.resolve("p", rect, new FloatPos(500, 150), 1.0 / 60.0);
        resolver.resolve("p", rect, new FloatPos(430, 410), 1.0 / 60.0);

        resolver.forget("p");

        // fresh state: no slide, straight to the bottom mid-port
        AttachPointResolver.Port port = resolver.resolve("p", rect, new FloatPos(430, 410), 1.0 / 60.0);
        assertPoint(port.point(), 280, 200, "no history — no slide");
    }

    @Test
    void rejectsInvalidConfigAndUse() {
        assertThrows(IllegalArgumentException.class, () -> new AttachPointResolver.Config(0, 35, 48, 0.16));
        assertThrows(IllegalArgumentException.class, () -> new AttachPointResolver.Config(55, 95, 48, 0.16));
        assertThrows(IllegalArgumentException.class, () -> new AttachPointResolver.Config(55, 35, -1, 0.16));
        assertThrows(IllegalArgumentException.class, () -> new AttachPointResolver.Config(55, 35, 48, 0));
        assertThrows(IllegalArgumentException.class, () -> {
            new AttachPointResolver(config).resolve("p", rect, new FloatPos(0, 0), Double.NaN);
        });
    }
}
