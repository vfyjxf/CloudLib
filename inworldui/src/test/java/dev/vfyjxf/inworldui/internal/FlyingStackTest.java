package dev.vfyjxf.inworldui.internal;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fly animation lifecycle: ease-out travel, the mid-flight arc and a
 * deterministic tick-based lifetime.
 */
class FlyingStackTest {

    private static FlyingStack flight() {
        return new FlyingStack(new ItemStack(Items.STONE, 8),
                Vec3.ZERO, new Vec3(4, 2, 0));
    }

    @Test
    void landsAfterExactlyDurationTicks() {
        FlyingStack fs = flight();
        for (int i = 0; i < FlyingStack.DURATION - 1; i++) {
            assertTrue(fs.tick(), "should still be flying at tick " + i);
        }
        assertFalse(fs.tick()); //the DURATION-th tick lands it
        assertTrue(fs.landed());
    }

    @Test
    void flightStartsAtFromAndEndsAtTo() {
        FlyingStack fs = flight();
        assertEquals(Vec3.ZERO, fs.pos()); //age 0 → exact start
        fs.age = FlyingStack.DURATION;
        Vec3 end = fs.pos();
        assertEquals(4.0, end.x, 1e-6);
        assertEquals(2.0, end.y, 1e-6);
    }

    @Test
    void midFlightArcsAboveTheStraightLine() {
        FlyingStack fs = flight();
        fs.age = FlyingStack.DURATION / 2;
        Vec3 mid = fs.pos();
        //straight-line midpoint y would be ~1.3 after easing; the arc adds on top
        assertTrue(mid.y > 1.4, "expected upward arc, got y=" + mid.y);
    }

    @Test
    void scaleShrinksTowardDropSize() {
        FlyingStack fs = flight();
        float s0 = fs.scale();
        fs.age = FlyingStack.DURATION;
        float s1 = fs.scale();
        assertTrue(s0 > s1, "sprite should shrink while flying");
        assertEquals(0.25f, s1, 1e-6f);
    }
}
