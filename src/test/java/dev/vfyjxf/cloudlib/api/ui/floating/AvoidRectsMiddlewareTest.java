package dev.vfyjxf.cloudlib.api.ui.floating;

import dev.vfyjxf.cloudlib.api.math.Rect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvoidRectsMiddlewareTest {

    private static final Rect boundary = new Rect(0, 0, 400, 300);

    private static FloatingState state(double x, double y, int w, int h) {
        return new FloatingState(
                x,
                y,
                FloatingPlacement.right,
                FloatingPlacement.right,
                new Rect(0, 0, 10, 10),
                new Rect(0, 0, w, h),
                boundary);
    }

    private static Rect at(FloatingState s, int w, int h) {
        return new Rect((int) Math.round(s.x()), (int) Math.round(s.y()), w, h);
    }

    @Test
    void noObstaclesKeepsPosition() {
        var s = state(50, 50, 100, 40);
        AvoidRectsMiddleware.create(List::of).run(s);
        assertEquals(50, s.x());
        assertEquals(50, s.y());
    }

    @Test
    void pushesOutAlongCheapestAxis() {
        // panel at (50,50) 100x40; obstacle covers (60,50)-(160,90) →
        // left push is 64px, right push is 60px, up is 44px, down 46px → up wins
        List<Rect> obstacles = List.of(new Rect(60, 50, 100, 40));
        var s = state(50, 50, 100, 40);
        AvoidRectsMiddleware.create(() -> obstacles, 0).run(s);
        assertFalse(at(s, 100, 40).intersects(obstacles.getFirst()));
        assertTrue(s.y() < 50, "expected an upward escape");
    }

    @Test
    void overBudgetOverlapIsAccepted() {
        // obstacle so deep inside the panel that clearing needs >64px
        List<Rect> obstacles = List.of(new Rect(10, 10, 200, 200));
        var s = state(50, 50, 100, 40);
        AvoidRectsMiddleware.create(() -> obstacles, 0).run(s);
        assertTrue(
                at(s, 100, 40).intersects(obstacles.getFirst()),
                "deep overlap should be left alone when clearing exceeds the push budget");
    }

    @Test
    void tinyGrazeIsIgnored() {
        // 3px corner graze — below the tolerance, must not move
        List<Rect> obstacles = List.of(new Rect(147, 87, 40, 40));
        var s = state(50, 50, 100, 40);
        AvoidRectsMiddleware.create(() -> obstacles, 0).run(s);
        assertEquals(50, s.x());
        assertEquals(50, s.y());
    }

    @Test
    void pushesClearOfSecondObstacleToo() {
        // two obstacles sandwich the panel; escaping both needs two pushes
        List<Rect> obstacles = List.of(
                new Rect(50, 50, 100, 40), // exact cover
                new Rect(50, 92, 100, 40) // one row below
                );
        var s = state(50, 50, 100, 40);
        AvoidRectsMiddleware.create(() -> obstacles, 0).run(s);
        Rect r = at(s, 100, 40);
        assertFalse(r.intersects(obstacles.get(0)));
        assertFalse(r.intersects(obstacles.get(1)));
    }

    @Test
    void resultClampedIntoBoundary() {
        // obstacle pushes the panel left past the screen edge → clamped back
        List<Rect> obstacles = List.of(new Rect(0, 10, 80, 40));
        var s = state(30, 10, 100, 40);
        AvoidRectsMiddleware.create(() -> obstacles, 0).run(s);
        assertTrue(s.x() >= 0);
    }
}
