package dev.vfyjxf.cloudlib.api.ui.inworld.space;

import dev.vfyjxf.cloudlib.api.math.Rect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InworldExclusionsTest {

    @AfterEach
    void cleanUp() {
        for (ExclusionProvider provider : InworldExclusions.providers()) {
            InworldExclusions.unregister(provider);
        }
    }

    @Test
    void registerKeepsRegistrationOrder() {
        ExclusionProvider first = context -> List.of();
        ExclusionProvider second = context -> List.of();
        InworldExclusions.register(first);
        InworldExclusions.register(second);

        assertEquals(List.of(first, second), InworldExclusions.providers());
    }

    @Test
    void registerIsIdempotent() {
        ExclusionProvider provider = context -> List.of();

        InworldExclusions.register(provider);
        InworldExclusions.register(provider);

        assertEquals(1, InworldExclusions.providers().size());
    }

    @Test
    void unregisterRemovesOnlyThatProvider() {
        ExclusionProvider first = context -> List.of();
        ExclusionProvider second = context -> List.of();
        InworldExclusions.register(first);
        InworldExclusions.register(second);

        assertTrue(InworldExclusions.unregister(first));
        assertEquals(List.of(second), InworldExclusions.providers());
    }

    @Test
    void collectClipsToViewportAndDropsEmptyRects() {
        InworldExclusions.register(
            context -> List.of(new Rect(-10, -10, 20, 20), new Rect(100, 100, 0, 10), new Rect(500, 0, 40, 40))
        );

        List<Rect> collected = InworldExclusions.collect(new ExclusionContext(200, 200, 0));

        assertEquals(List.of(new Rect(0, 0, 10, 10)), collected);
    }

    @Test
    void collectMergesContainmentIntoOuterRect() {
        InworldExclusions.register(context -> List.of(new Rect(0, 0, 50, 50), new Rect(10, 10, 20, 20)));

        assertEquals(List.of(new Rect(0, 0, 50, 50)), InworldExclusions.collect(new ExclusionContext(200, 200, 0)));
    }

    @Test
    void collectMergesEdgeAlignedNeighbors() {
        InworldExclusions.register(context -> List.of(new Rect(0, 0, 10, 10), new Rect(10, 0, 10, 10)));

        assertEquals(List.of(new Rect(0, 0, 20, 10)), InworldExclusions.collect(new ExclusionContext(200, 200, 0)));
    }

    @Test
    void collectMergesPartialOverlapsThatStayRectangular() {
        // Overlapping by 5 px with matching top edges: the union is a rectangle.
        InworldExclusions.register(context -> List.of(new Rect(0, 0, 15, 10), new Rect(10, 0, 15, 10)));

        assertEquals(List.of(new Rect(0, 0, 25, 10)), InworldExclusions.collect(new ExclusionContext(200, 200, 0)));
    }

    @Test
    void collectKeepsNonRectangularUnionsSeparate() {
        // Staggered overlap: the bounding box would exclude space nobody uses.
        InworldExclusions.register(context -> List.of(new Rect(0, 0, 15, 10), new Rect(10, 5, 15, 10)));

        List<Rect> collected = InworldExclusions.collect(new ExclusionContext(200, 200, 0));

        assertEquals(2, collected.size());
        assertEquals(new Rect(0, 0, 15, 10), collected.get(0));
        assertEquals(new Rect(10, 5, 15, 10), collected.get(1));
    }

    @Test
    void collectMergesAcrossProviders() {
        InworldExclusions.register(context -> List.of(new Rect(0, 0, 10, 10)));
        InworldExclusions.register(context -> List.of(new Rect(10, 0, 10, 10)));

        assertEquals(List.of(new Rect(0, 0, 20, 10)), InworldExclusions.collect(new ExclusionContext(200, 200, 0)));
    }

    @Test
    void collectChainsMergesToClosure() {
        InworldExclusions
                .register(context -> List.of(new Rect(0, 0, 10, 10), new Rect(10, 0, 10, 10), new Rect(20, 0, 10, 10)));

        assertEquals(List.of(new Rect(0, 0, 30, 10)), InworldExclusions.collect(new ExclusionContext(200, 200, 0)));
    }

    @Test
    void collectIgnoresNullRects() {
        InworldExclusions.register(context -> Arrays.asList(new Rect(0, 0, 5, 5), null));

        assertEquals(List.of(new Rect(0, 0, 5, 5)), InworldExclusions.collect(new ExclusionContext(200, 200, 0)));
    }

    @Test
    void collectSortsDeterministically() {
        InworldExclusions
                .register(context -> List.of(new Rect(20, 10, 5, 5), new Rect(0, 10, 5, 5), new Rect(0, 0, 5, 5)));

        List<Rect> collected = InworldExclusions.collect(new ExclusionContext(200, 200, 0));

        assertEquals(List.of(new Rect(0, 0, 5, 5), new Rect(0, 10, 5, 5), new Rect(20, 10, 5, 5)), collected);
    }

    @Test
    void emptyRegistryCollectsNothing() {
        assertEquals(List.of(), InworldExclusions.collect(new ExclusionContext(200, 200, 0)));
    }

    @Test
    void contextRejectsNonPositiveScreens() {
        assertThrows(IllegalArgumentException.class, () -> new ExclusionContext(0, 100, 0));
        assertThrows(IllegalArgumentException.class, () -> new ExclusionContext(100, -5, 0));
    }

    @Test
    void contextExposesItsViewport() {
        assertEquals(new Rect(0, 0, 320, 240), new ExclusionContext(320, 240, 0.5f).viewport());
    }
}
