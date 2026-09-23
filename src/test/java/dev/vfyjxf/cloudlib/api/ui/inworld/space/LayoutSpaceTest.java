package dev.vfyjxf.cloudlib.api.ui.inworld.space;

import dev.vfyjxf.cloudlib.api.math.Insets;
import dev.vfyjxf.cloudlib.api.math.Rect;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static dev.vfyjxf.cloudlib.testutil.GeometryAsserts.assertRectEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LayoutSpaceTest {

    @Test
    void viewportSafeAreaAndWorkAreaCoincideWithoutInsetsOrStruts() {
        LayoutSpace space = LayoutSpace.of(1920, 1080);

        assertRectEquals(new Rect(0, 0, 1920, 1080), space.viewport());
        assertRectEquals(space.viewport(), space.safeArea());
        assertRectEquals(space.safeArea(), space.workArea());
        assertEquals(Insets.zero, space.strutInsets());
    }

    @Test
    void safeAreaInsetsShrinkTheViewport() {
        LayoutSpace space = LayoutSpace.of(1920, 1080).withSafeInsets(new Insets(10, 20, 30, 40));

        assertRectEquals(new Rect(40, 10, 1860, 1040), space.safeArea());
        assertRectEquals(new Rect(40, 10, 1860, 1040), space.workArea());
        assertEquals(new Rect(40, 10, 1860, 1040), space.safeArea());
    }

    @Test
    void strutsPushOnlyTheEdgesTheyHug() {
        LayoutSpace space = LayoutSpace.of(200, 100)
                .withStruts(List.of(new Rect(0, 0, 30, 100), new Rect(170, 0, 30, 100), new Rect(0, 0, 200, 12)));

        assertEquals(new Insets(12, 30, 0, 30), space.strutInsets());
        assertRectEquals(new Rect(30, 12, 140, 88), space.workArea());
    }

    @Test
    void multipleStrutsOnOneEdgeTakeTheMaximumNotTheSum() {
        LayoutSpace space = LayoutSpace.of(200, 100)
                .withStruts(List.of(new Rect(0, 0, 30, 100), new Rect(0, 10, 10, 80), new Rect(0, 50, 25, 10)));

        assertEquals(new Insets(0, 0, 0, 30), space.strutInsets());
        assertRectEquals(new Rect(30, 0, 170, 100), space.workArea());
    }

    @Test
    void strutsMustOverlapTheEdgeExtent() {
        LayoutSpace space = LayoutSpace.of(200, 100).withStruts(List.of(new Rect(0, 100, 30, 20)));

        assertEquals(Insets.zero, space.strutInsets());
        assertRectEquals(new Rect(0, 0, 200, 100), space.workArea());
    }

    @Test
    void strutsMustTouchTheEdge() {
        LayoutSpace space = LayoutSpace.of(200, 100).withStruts(List.of(new Rect(10, 10, 30, 80)));

        assertEquals(Insets.zero, space.strutInsets());
    }

    @Test
    void interiorStrutsDoNotShrinkWorkArea() {
        LayoutSpace space = LayoutSpace.of(200, 100).withStruts(List.of(new Rect(80, 40, 40, 20)));

        assertEquals(Insets.zero, space.strutInsets());
        assertRectEquals(new Rect(0, 0, 200, 100), space.workArea());
    }

    @Test
    void emptyStrutsAreIgnored() {
        LayoutSpace space = LayoutSpace.of(200, 100)
                .withStruts(List.of(new Rect(0, 0, 0, 100), new Rect(0, 0, 200, 0)));

        assertEquals(Insets.zero, space.strutInsets());
    }

    @Test
    void safeInsetsAndStrutsCompose() {
        LayoutSpace space = LayoutSpace.of(200, 100).withSafeInsets(Insets.symmetric(10, 5))
                .withStruts(List.of(new Rect(5, 10, 40, 80), new Rect(0, 10, 10, 80)));

        // The wide strut claims the left edge (40 px past the safe area's left
        // boundary, thinner than its 80 px vertical span), the narrow one also
        // claims left with 5 px — max wins; top and bottom stay untouched.
        assertEquals(new Insets(0, 0, 0, 40), space.strutInsets());
        assertRectEquals(new Rect(45, 10, 150, 80), space.workArea());
    }

    @Test
    void oversizedInsetsClampToEmpty() {
        LayoutSpace space = LayoutSpace.of(100, 100).withSafeInsets(Insets.uniform(80));

        assertRectEquals(new Rect(80, 80, 0, 0), space.safeArea());
        assertRectEquals(new Rect(80, 80, 0, 0), space.workArea());
    }

    @Test
    void withMethodsReturnIndependentCopies() {
        LayoutSpace original = LayoutSpace.of(200, 100);

        LayoutSpace resized = original.withViewport(400, 300);
        LayoutSpace inset = original.withSafeInsets(Insets.uniform(5));
        LayoutSpace strutted = original.withStruts(List.of(new Rect(0, 0, 200, 10)));

        assertRectEquals(new Rect(0, 0, 200, 100), original.viewport());
        assertRectEquals(new Rect(0, 0, 400, 300), resized.viewport());
        assertEquals(Insets.zero, original.safeInsets());
        assertEquals(Insets.uniform(5), inset.safeInsets());
        assertEquals(List.of(), original.struts());
        assertEquals(List.of(new Rect(0, 0, 200, 10)), strutted.struts());
        assertEquals(List.of(new Rect(0, 0, 200, 10)), strutted.struts());
        assertRectEquals(new Rect(0, 0, 200, 100), resized.withViewport(200, 100).viewport());
    }

    @Test
    void strutsSnapshotIsImmutable() {
        LayoutSpace space = LayoutSpace.of(200, 100);
        List<Rect> struts = new ArrayList<>();
        struts.add(new Rect(0, 0, 200, 10));

        LayoutSpace strutted = space.withStruts(struts);
        struts.clear();

        assertEquals(List.of(new Rect(0, 0, 200, 10)), strutted.struts());
    }
}
