package dev.vfyjxf.cloudlib.api.ui.inworld.algorithm;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.FloatRect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcentricColumnTest {

    private static final FloatRect screen = new FloatRect(0, 0, 1920, 1080);

    private static ExcentricColumn columner() {
        return new ExcentricColumn(ExcentricColumn.Config.of(16, 6, 100, 2));
    }

    private static List<ExcentricColumn.Label> labels() {
        return List.of(
                new ExcentricColumn.Label("l1", 80, 20),
                new ExcentricColumn.Label("l2", 120, 24),
                new ExcentricColumn.Label("l3", 60, 18));
    }

    @Test
    void labelsStackInOneAlignedColumnBesideTheFocus() {
        ExcentricColumn column = columner();

        ExcentricColumn.Layout layout = column.layout(new FloatPos(600, 400), labels(), screen);

        assertEquals(ExcentricColumn.Side.right, layout.side());
        assertEquals(616, layout.columnX());
        List<ExcentricColumn.Placed> placed = layout.labels();
        assertEquals(3, placed.size());

        double totalHeight = 20 + 24 + 18 + 6 * 2;
        assertEquals(400 - totalHeight / 2, placed.get(0).topLeft().y(), 1.0e-9);
        for (ExcentricColumn.Placed label : placed) {
            assertEquals(616, label.topLeft().x(), 1.0e-9);
            // connectors sit on the focus-facing (left) edge, at its midpoint
            assertEquals(616, label.connector().x(), 1.0e-9);
        }
        // no vertical overlap: each label clears the previous one plus the gap
        assertTrue(
                placed.get(0).topLeft().y() + 20 + 6 <= placed.get(1).topLeft().y());
        assertTrue(
                placed.get(1).topLeft().y() + 24 + 6 <= placed.get(2).topLeft().y());
        assertEquals(placed.get(1).connector().y(), placed.get(1).topLeft().y() + 12, 1.0e-9);
    }

    @Test
    void leftColumnsAlignTheirRightEdgesOnTheColumnLine() {
        ExcentricColumn column = columner();

        ExcentricColumn.Layout layout = column.layout(new FloatPos(1820, 500), labels(), screen);

        assertEquals(ExcentricColumn.Side.left, layout.side());
        assertEquals(1804, layout.columnX());
        for (int i = 0; i < labels().size(); i++) {
            double width = labels().get(i).width();
            assertEquals(1804 - width, layout.labels().get(i).topLeft().x(), 1.0e-9);
            assertEquals(1804, layout.labels().get(i).connector().x(), 1.0e-9);
        }
    }

    @Test
    void theColumnFollowsTheFocusAndKeepsItsAlignment() {
        ExcentricColumn column = columner();

        ExcentricColumn.Layout before = column.layout(new FloatPos(600, 400), labels(), screen);
        ExcentricColumn.Layout after = column.layout(new FloatPos(700, 460), labels(), screen);

        assertEquals(ExcentricColumn.Side.right, after.side());
        assertEquals(716, after.columnX());
        assertEquals(before.labels().size(), after.labels().size());
        double dy = after.labels().get(0).topLeft().y()
                - before.labels().get(0).topLeft().y();
        assertEquals(60, dy, 1.0e-9);
        for (ExcentricColumn.Placed label : after.labels()) {
            assertEquals(716, label.topLeft().x(), 1.0e-9);
        }
    }

    @Test
    void theColumnIsClampedIntoTheBounds() {
        ExcentricColumn column = columner();

        ExcentricColumn.Layout layout = column.layout(new FloatPos(600, 10), labels(), screen);

        assertTrue(layout.labels().get(0).topLeft().y() >= screen.y());
        assertTrue(layout.labels().get(2).topLeft().y() + 18 <= screen.bottom());
    }

    @Test
    void labelsKeepTheCallersOrder() {
        ExcentricColumn column = columner();

        List<ExcentricColumn.Placed> placed =
                column.layout(new FloatPos(600, 400), labels(), screen).labels();

        assertEquals(
                List.of("l1", "l2", "l3"),
                placed.stream().map(ExcentricColumn.Placed::id).toList());
    }

    @Test
    void focusOscillatingAroundTheCenterKeepsTheSide() {
        ExcentricColumn column = new ExcentricColumn(ExcentricColumn.Config.of(16, 6, 100, 2));
        FloatRect bounds = new FloatRect(0, 0, 1000, 600);

        // first layout at the exact center: room ties go right
        assertEquals(
                ExcentricColumn.Side.right,
                column.layout(new FloatPos(500, 300), labels(), bounds).side());

        // oscillate ±30 px around the center: imbalance swings ±60, inside the band
        for (int i = 0; i < 20; i++) {
            double x = 500 + ((i % 2 == 0) ? 30 : -30);
            assertEquals(
                    ExcentricColumn.Side.right,
                    column.layout(new FloatPos(x, 300), labels(), bounds).side());
        }

        // a real move across the band commits the flip after the dwell
        column.layout(new FloatPos(800, 300), labels(), bounds);
        assertEquals(
                ExcentricColumn.Side.left,
                column.layout(new FloatPos(800, 300), labels(), bounds).side());
    }

    @Test
    void emptyLabelListYieldsAnEmptyColumn() {
        ExcentricColumn.Layout layout = columner().layout(new FloatPos(600, 400), List.of(), screen);

        assertEquals(ExcentricColumn.Side.right, layout.side());
        assertEquals(616, layout.columnX());
        assertEquals(List.of(), layout.labels());
    }

    @Test
    void rejectsInvalidUse() {
        ExcentricColumn column = columner();

        assertThrows(IllegalArgumentException.class, () -> ExcentricColumn.Config.of(-1, 6, 100, 2));
        assertThrows(IllegalArgumentException.class, () -> ExcentricColumn.Config.of(16, -1, 100, 2));
        assertThrows(IllegalArgumentException.class, () -> ExcentricColumn.Config.of(16, 6, 0, 2));
        assertThrows(IllegalArgumentException.class, () -> ExcentricColumn.Config.of(16, 6, 100, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> column.layout(new FloatPos(600, 400), List.of(new ExcentricColumn.Label("bad", 0, 10)), screen));
        assertThrows(
                IllegalArgumentException.class,
                () -> column.layout(
                        new FloatPos(600, 400), List.of(new ExcentricColumn.Label("bad", 10, Double.NaN)), screen));
    }
}
