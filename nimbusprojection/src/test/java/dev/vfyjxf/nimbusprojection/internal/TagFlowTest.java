package dev.vfyjxf.nimbusprojection.internal;

import net.minecraft.client.renderer.Rect2i;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coverage of {@link TagFlow}: the stay/slide/rail ladder, direction
 * stickiness, the slide budget, and rail packing.
 */
class TagFlowTest {

    private static final int W = 400, H = 240;
    /** a foreground chrome rect — the kind of thing tags must yield to */
    private static final Rect2i PANEL = new Rect2i(180, 100, 80, 40);

    private static TagFlow.Result resolve(int x, int y, int w, int h, List<Rect2i> obs) {
        return TagFlow.resolve(x, y, w, h, obs, -1, W, H);
    }

    //---- STAY ----

    @Test
    void clearHomeStays() {
        var r = resolve(40, 40, 50, 12, List.of(PANEL));
        assertEquals(TagFlow.Outcome.STAY, r.outcome());
        assertEquals(40, r.x());
        assertEquals(40, r.y());
    }

    @Test
    void halfCoveredStaysBehindChrome() {
        //tag right half under the panel — still readable, don't move it
        var r = resolve(155, 105, 50, 12, List.of(PANEL)); //x 155..205 vs 180..260 → 25px = 50%
        assertEquals(TagFlow.Outcome.STAY, r.outcome());
    }

    @Test
    void grazeStays() {
        //4px bottom graze — a sliver, not worth moving
        var r = resolve(190, 92, 50, 12, List.of(PANEL)); //y 92..104 vs 100..140 → 4px deep
        assertEquals(TagFlow.Outcome.STAY, r.outcome());
    }

    //---- SLIDE ----

    @Test
    void deepCoverSlidesOffTheBlocker() {
        //tag 90% inside the panel, sticking out the left side — but the top
        //escape keeps a 10px graze (acceptable) and is only 7px away
        var r = resolve(175, 105, 50, 12, List.of(PANEL));
        assertEquals(TagFlow.Outcome.SLIDED, r.outcome());
        assertEquals(0, r.slideDir()); //above
        assertEquals(PANEL.getY() - 12 + TagFlow.ESCAPE_TOLERANCE, r.y());
        assertEquals(175, r.x());
    }

    @Test
    void slidePicksCheapestSide() {
        var blocker = new Rect2i(120, 60, 60, 30);
        //tag y-band fully inside the blocker (depth 12) → must move;
        //the top escape is 4px away, right escape 20px → goes up
        var r = resolve(150, 62, 50, 12, List.of(blocker));
        assertEquals(TagFlow.Outcome.SLIDED, r.outcome());
        assertEquals(0, r.slideDir());
        assertEquals(58, r.y()); //60 - 12 + 10 graze
    }

    @Test
    void lastDirectionSticks() {
        //vertical escapes covered by ceiling/floor walls → the contest is
        //left (44px) vs right (40px): right wins fresh, left wins when sticky
        var blocker = new Rect2i(150, 105, 60, 30);
        var ceiling = new Rect2i(0, 95, 400, 20);
        var floor = new Rect2i(0, 125, 400, 20);
        var obs = List.of(blocker, ceiling, floor);
        var fresh = TagFlow.resolve(160, 110, 44, 12, obs, -1, W, H);
        assertEquals(TagFlow.Outcome.SLIDED, fresh.outcome());
        assertEquals(3, fresh.slideDir()); //right is cheaper
        var stickLeft = TagFlow.resolve(160, 110, 44, 12, obs, 2, W, H);
        assertEquals(2, stickLeft.slideDir()); //stickiness flips the near-tie
        assertEquals(blocker.getX() - 44 + TagFlow.ESCAPE_TOLERANCE, stickLeft.x());
    }

    @Test
    void coveredButNotBuriedStaysPut() {
        //70% covered: not acceptable, not buried. Every escape fails — above
        //and below are walled by other chrome, left lands on a pocket blocker,
        //right is >48px away → the tag accepts staying behind the chrome.
        var wall = new Rect2i(150, 90, 60, 60);
        var ceiling = new Rect2i(0, 80, 400, 20);
        var floor = new Rect2i(0, 140, 400, 20);
        var pocket = new Rect2i(85, 95, 50, 25);
        var r = resolve(135, 100, 50, 12, List.of(wall, ceiling, floor, pocket));
        assertEquals(TagFlow.Outcome.STAY, r.outcome());
        assertEquals(135, r.x());
        assertEquals(100, r.y());
    }

    //---- RAIL ----

    @Test
    void buriedTagGoesToRail() {
        //fully inside a huge blocker — every escape is >48px away → rail
        var wall = new Rect2i(60, 20, 280, 200);
        var r = resolve(180, 90, 50, 12, List.of(wall));
        assertEquals(TagFlow.Outcome.RAIL, r.outcome());
    }

    @Test
    void railPacksDownFromTopExtent() {
        var occupied = new ArrayList<Rect2i>();
        var rails = new TagFlow.Rails(W, H, 60, 0, occupied);
        var s1 = rails.claim(0, 50, 12);
        var s2 = rails.claim(0, 50, 12);
        assertEquals(8, s1[0]);
        assertEquals(66, s1[1]); //topExtent 60 + 6
        assertEquals(s1[1] + 12 + 4, s2[1]); //packed below the first
    }

    @Test
    void railSideFollowsAnchorHalf() {
        var rails = new TagFlow.Rails(W, H, 0, 0, new ArrayList<>());
        assertEquals(0, rails.side(100));
        assertEquals(1, rails.side(300));
        assertEquals(1, rails.side(Double.NaN));
    }

    @Test
    void fullRailReturnsNull() {
        var occupied = new ArrayList<Rect2i>();
        var rails = new TagFlow.Rails(W, H, 0, 0, occupied);
        //fill the left rail until it runs out of vertical room
        int claims = 0;
        while (rails.claim(0, 50, 30) != null) claims++;
        assertTrue(claims > 0 && claims < H / 30);
    }

    @Test
    void railSkipsOccupiedSlots() {
        var occupied = new ArrayList<Rect2i>();
        occupied.add(new Rect2i(8, 24, 50, 12)); //pre-occupied rail slot
        var rails = new TagFlow.Rails(W, H, 0, 0, occupied);
        var s = rails.claim(0, 50, 12);
        assertTrue(s[1] > 24); //hopped over the occupied slot
    }

    //---- deterministic ordering ----

    @Test
    void resolveIsDeterministic() {
        var obs = List.of(PANEL);
        var a = resolve(190, 88, 50, 12, obs);
        var b = resolve(190, 88, 50, 12, obs);
        assertEquals(a.x(), b.x());
        assertEquals(a.y(), b.y());
        assertEquals(a.outcome(), b.outcome());
        assertEquals(a.slideDir(), b.slideDir());
    }

    @Test
    void slideBeyondBudgetFallsThrough() {
        //a huge blocker: every side is >48px away → no affordable slide;
        //fully covered → buried → rail
        var wall = new Rect2i(60, 20, 280, 200);
        var r = resolve(180, 90, 50, 12, List.of(wall));
        assertEquals(TagFlow.Outcome.RAIL, r.outcome());
    }

}
