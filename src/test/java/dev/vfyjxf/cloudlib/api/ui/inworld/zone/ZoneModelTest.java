package dev.vfyjxf.cloudlib.api.ui.inworld.zone;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.Insets;
import dev.vfyjxf.cloudlib.api.math.Rect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoneModelTest {

    private static final Rect screen = new Rect(0, 0, 480, 270);
    private static final AttentionField attention = GaussianAttention.atScreenCenter(480, 270, 100.0);

    private static ZoneModel model(FloatPos anchor, double anchorRadius, double maxDisplacement, double edgeBand) {
        return new ZoneModel(anchor, screen, attention, new ZoneModel.Config(anchorRadius, maxDisplacement, edgeBand));
    }

    @Test
    void screenSafeRectInsetsTheScreen() {
        assertEquals(new Rect(16, 16, 1888, 1048), ZoneModel.screenSafeRect(1920, 1080, Insets.uniform(16)));
        assertEquals(new Rect(40, 10, 420, 230), ZoneModel.screenSafeRect(480, 270, new Insets(10, 20, 30, 40)));
        assertEquals(screen, ZoneModel.screenSafeRect(480, 270, Insets.zero));
    }

    @Test
    void screenSafeRectClampsOversizedInsetsToNonNegativeSize() {
        assertEquals(new Rect(380, 100, 0, 70), ZoneModel.screenSafeRect(480, 270, new Insets(100, 500, 100, 380)));
        assertEquals(new Rect(300, 300, 0, 0), ZoneModel.screenSafeRect(480, 270, new Insets(300, 300, 300, 300)));
    }

    @Test
    void screenSafeRectRejectsNonPositiveScreens() {
        assertThrows(IllegalArgumentException.class, () -> ZoneModel.screenSafeRect(0, 270, Insets.zero));
        assertThrows(IllegalArgumentException.class, () -> ZoneModel.screenSafeRect(480, -1, Insets.zero));
    }

    @Test
    void classifiesTheFourZones() {
        ZoneModel model = model(new FloatPos(100, 100), 20, 80, 30);

        // the anchor neighborhood
        assertEquals(ZoneModel.Region.anchor, model.regionOf(100, 100));
        assertEquals(ZoneModel.Region.anchor, model.regionOf(110, 108));
        assertEquals(ZoneModel.Region.anchor, model.regionOf(new FloatPos(88, 92)));

        // the drift band: within D_max, not anchor, not edge
        assertEquals(ZoneModel.Region.displacement, model.regionOf(150, 100));
        assertEquals(ZoneModel.Region.displacement, model.regionOf(100, 170));

        // the center zone: beyond D_max, clear of the edge band
        assertEquals(ZoneModel.Region.center, model.regionOf(240, 135));

        // the edge band: inside the 30px boundary band
        assertEquals(ZoneModel.Region.edge, model.regionOf(240, 15));
        assertEquals(ZoneModel.Region.edge, model.regionOf(455, 200));
        assertEquals(ZoneModel.Region.edge, model.regionOf(240, 260));
    }

    @Test
    void pointsOutsideTheSafeRectClassifyAsEdge() {
        ZoneModel model = model(new FloatPos(100, 100), 20, 80, 30);
        assertEquals(ZoneModel.Region.edge, model.regionOf(-50, 100));
        assertEquals(ZoneModel.Region.edge, model.regionOf(500, 135));
        assertEquals(ZoneModel.Region.edge, model.regionOf(240, -10));
    }

    @Test
    void anchorOutranksEdgeAndEdgeOutranksDisplacement() {
        ZoneModel model = model(new FloatPos(100, 100), 20, 80, 30);
        // a point 77px from the anchor (inside D_max) but inside the edge band:
        // the edge band wins
        assertEquals(ZoneModel.Region.edge, model.regionOf(25, 120));

        // the anchor itself sits in the edge band: anchor still wins
        ZoneModel nearEdge = model(new FloatPos(25, 100), 20, 80, 30);
        assertEquals(ZoneModel.Region.anchor, nearEdge.regionOf(25, 100));
        assertEquals(ZoneModel.Region.anchor, nearEdge.regionOf(20, 96));
    }

    @Test
    void rectZoneIsTheZoneOfItsCenter() {
        ZoneModel model = model(new FloatPos(100, 100), 20, 80, 30);
        assertEquals(ZoneModel.Region.anchor, model.regionOf(new Rect(90, 95, 20, 10)));
        assertEquals(ZoneModel.Region.displacement, model.regionOf(new Rect(140, 95, 20, 10)));
        assertEquals(ZoneModel.Region.center, model.regionOf(new Rect(230, 130, 20, 10)));
        assertEquals(ZoneModel.Region.edge, model.regionOf(new Rect(230, 0, 20, 10)));
    }

    @Test
    void driftExcessMeasuresHowFarBeyondDMaxTheCenterIs() {
        ZoneModel model = model(new FloatPos(100, 100), 20, 80, 30);

        // within the band: no excess
        assertEquals(0.0, model.driftExcess(new Rect(140, 95, 20, 10)), 1.0e-12);
        // exactly at D_max: still no excess
        assertEquals(0.0, model.driftExcess(new Rect(170, 95, 20, 10)), 1.0e-12);
        // 20px beyond: excess 20
        assertEquals(20.0, model.driftExcess(new Rect(190, 95, 20, 10)), 1.0e-9);
        // diagonal beyond
        double expected = Math.hypot(200, 50) - 80;
        assertEquals(expected, model.driftExcess(new Rect(290, 145, 20, 10)), 1.0e-9);
    }

    @Test
    void touchesEdgeBandDetectsBoundaryContact() {
        ZoneModel model = model(new FloatPos(100, 100), 20, 80, 30);

        assertFalse(model.touchesEdgeBand(new Rect(100, 100, 20, 10)));
        assertTrue(model.touchesEdgeBand(new Rect(200, 5, 20, 10)));
        assertTrue(model.touchesEdgeBand(new Rect(450, 100, 20, 10)));
        assertTrue(model.touchesEdgeBand(new Rect(100, 243, 20, 10)));
        // reaching outside the safe rect certainly touches the band
        assertTrue(model.touchesEdgeBand(new Rect(-10, 100, 20, 10)));
        // flush against the band's inner boundary (x == 30) does not touch
        assertFalse(model.touchesEdgeBand(new Rect(30, 100, 20, 10)));
    }

    @Test
    void exposesItsInputs() {
        ZoneModel.Config config = new ZoneModel.Config(20, 80, 30);
        ZoneModel model = new ZoneModel(new FloatPos(100, 100), screen, attention, config);

        assertEquals(new FloatPos(100, 100), model.anchor());
        assertEquals(screen, model.safeRect());
        assertEquals(attention, model.attention());
        assertEquals(config, model.config());
        assertEquals(80, model.maxDisplacementPx(), 0.0);
    }

    @Test
    void ofUsesDefaultConfig() {
        ZoneModel model = ZoneModel.of(new FloatPos(100, 100), screen, attention);
        assertEquals(ZoneModel.Config.defaults(), model.config());
    }

    @Test
    void rejectsInvalidConfiguration() {
        FloatPos anchor = new FloatPos(100, 100);
        assertThrows(IllegalArgumentException.class, () -> model(anchor, 0, 80, 30));
        assertThrows(IllegalArgumentException.class, () -> model(anchor, 20, 0, 30));
        assertThrows(IllegalArgumentException.class, () -> model(anchor, 20, 80, -1));
        assertThrows(IllegalArgumentException.class, () -> model(anchor, Double.NaN, 80, 30));
        assertThrows(
            IllegalArgumentException.class,
            () -> new ZoneModel(new FloatPos(Double.NaN, 0), screen, attention, ZoneModel.Config.defaults())
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new ZoneModel(anchor, screen, attention, ZoneModel.Config.defaults())
                    .regionOf(new FloatPos(Double.NaN, 0))
        );
    }
}
