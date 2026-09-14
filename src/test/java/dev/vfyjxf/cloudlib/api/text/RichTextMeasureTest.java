package dev.vfyjxf.cloudlib.api.text;

import dev.vfyjxf.cloudlib.api.text.layout.LaidOutText;
import dev.vfyjxf.cloudlib.api.text.layout.RichTextMeasure;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class RichTextMeasureTest {

    private static final FloatSize UNKNOWN = new FloatSize(Float.NaN, Float.NaN);

    private static RichTextMeasure measureOf(String text) {
        return new RichTextMeasure(RichText.of(text), TestTextEnvs.layouter());
    }

    private static FloatSize measure(RichTextMeasure measure, AvailableSpace width) {
        return measure.measure(UNKNOWN, new TaffySize<>(width, AvailableSpace.MAX_CONTENT));
    }

    @Test
    void definiteSpaceWrapsAndReportsWidestLine() {
        // "hello world" at 36px wraps into two 30px lines.
        FloatSize size = measure(measureOf("hello world"), AvailableSpace.definite(36));
        assertEquals(30f, size.width);
        assertEquals(18f, size.height);
    }

    @Test
    void definiteSpaceWiderThanContentShrinksToContent() {
        FloatSize size = measure(measureOf("hi"), AvailableSpace.definite(100));
        assertEquals(12f, size.width);
        assertEquals(9f, size.height);
    }

    @Test
    void maxContentReportsNaturalWidth() {
        FloatSize size = measure(measureOf("hello world"), AvailableSpace.MAX_CONTENT);
        assertEquals(66f, size.width);
        assertEquals(9f, size.height);
    }

    @Test
    void minContentReportsLongestWord() {
        FloatSize size = measure(measureOf("hello world"), AvailableSpace.MIN_CONTENT);
        assertEquals(30f, size.width);
        assertEquals(18f, size.height);
    }

    @Test
    void knownWidthWinsAndWraps() {
        FloatSize size = measureOf("hello world")
                .measure(new FloatSize(60, Float.NaN), new TaffySize<>(AvailableSpace.MAX_CONTENT, AvailableSpace.MAX_CONTENT));
        assertEquals(60f, size.width);
        assertEquals(18f, size.height);
    }

    @Test
    void knownHeightWins() {
        FloatSize size = measureOf("hello world")
                .measure(new FloatSize(Float.NaN, 42), new TaffySize<>(AvailableSpace.MAX_CONTENT, AvailableSpace.MAX_CONTENT));
        assertEquals(66f, size.width);
        assertEquals(42f, size.height);
    }

    @Test
    void noWrapIgnoresDefiniteSpace() {
        FloatSize size = measure(measureOf("hello world").withWrap(false), AvailableSpace.definite(10));
        assertEquals(66f, size.width);
        assertEquals(9f, size.height);
    }

    @Test
    void layoutAtCachesPerWidth() {
        RichTextMeasure measure = measureOf("hello world");
        LaidOutText first = measure.layoutAt(36);
        assertSame(first, measure.layoutAt(36));
        assertNotSame(first, measure.layoutAt(60));
    }

    @Test
    void invalidateDropsCache() {
        RichTextMeasure measure = measureOf("hello world");
        LaidOutText first = measure.layoutAt(36);
        measure.invalidate();
        assertNotSame(first, measure.layoutAt(36));
    }

    @Test
    void worksAsTaffyMeasureFunc() {
        TaffyTree tree = new TaffyTree();
        RichTextMeasure measure = measureOf("hello world");
        NodeId leaf = tree.newLeafWithMeasure(new TaffyStyle(), measure);

        tree.computeLayout(leaf, new TaffySize<>(AvailableSpace.definite(36), AvailableSpace.MAX_CONTENT));

        FloatSize size = tree.getLayout(leaf).size();
        assertEquals(30f, size.width);
        assertEquals(18f, size.height);
    }

    @Test
    void emptyTextMeasuresZero() {
        FloatSize size = measure(new RichTextMeasure(RichText.empty(), TestTextEnvs.layouter()), AvailableSpace.MAX_CONTENT);
        assertEquals(0f, size.width);
        assertEquals(0f, size.height);
    }
}
