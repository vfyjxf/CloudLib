package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.canvas.EntityPreviewRenderer;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import net.minecraft.world.entity.Entity;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EntityPreviewWidget's geometry and supplier contract — the headless half of the
 * widget. The draw itself needs a live client level (the inventory-entity pipeline,
 * the entity render dispatcher), so it is verified on screen; these tests pin the box
 * the measure function declares, the scale defaults and the per-frame supplier.
 */
class EntityPreviewWidgetTest {

    @Test
    void theBoxIsTheSizeItWasDeclared() {
        // a fit-content parent that does not stretch its child, so what is measured
        // is what the widget declares — the fixture's own root would stretch the
        // cross axis, which is the parent's choice, not the widget's size
        try (WidgetTestScene fixture = new WidgetTestScene(220, 60)) {
            WidgetGroup<Widget> holder = new WidgetGroup<>();
            holder.useStyle(UIStyle.of(UIStyles.flexColumn(), UIStyles.alignItemsFlexStart()));
            fixture.add(holder);
            EntityPreviewWidget preview = holder.addWidget(new EntityPreviewWidget(() -> null, 40, 30));
            fixture.stabilize();
            assertEquals(40, preview.width(), "the measure function returns the declared box");
            assertEquals(30, preview.height());
        }
    }

    @Test
    void aStretchingParentStillGetsTheDeclaredPreference() {
        // the root stretches the cross axis: the width (main axis) is still measured
        try (WidgetTestScene fixture = new WidgetTestScene(220, 60)) {
            EntityPreviewWidget preview = fixture.add(new EntityPreviewWidget(() -> null, 40, 30));
            fixture.stabilize();
            assertEquals(40, preview.width());
        }
    }

    @Test
    void aBoxIsAtLeastOnePixelAndAnExplicitScaleAtLeastOne() {
        EntityPreviewWidget degenerate = new EntityPreviewWidget(() -> null, 0, -5);
        assertEquals(1, degenerate.previewWidth());
        assertEquals(1, degenerate.previewHeight());

        assertEquals(
            0,
            new EntityPreviewWidget(() -> null, 40, 30, -7, false).scale(),
            "a negative scale reads as auto"
        );
    }

    @Test
    void theDefaultScaleFitsTheEntityAndTheDefaultFacingIsFront() {
        EntityPreviewWidget preview = new EntityPreviewWidget(() -> null, 40, 30);
        assertEquals(0, preview.scale(), "the default is the auto-fit scale, decided per frame by the entity's box");
        assertFalse(preview.followMouse(), "a portrait faces front unless asked to track the pointer");
        assertEquals(40, preview.previewWidth());
        assertEquals(30, preview.previewHeight());
    }

    @Test
    void anExplicitScaleAndFollowMouseSurvive() {
        EntityPreviewWidget preview = new EntityPreviewWidget(() -> null, 40, 30, 25, true);
        assertEquals(25, preview.scale());
        assertTrue(preview.followMouse());
    }

    @Test
    void theWidgetHoldsTheLiveSupplierAndNeverPullsItBeforeARender() {
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<Entity> current = new AtomicReference<>();
        Supplier<Entity> supplier = () -> {
            calls.incrementAndGet();
            return current.get();
        };
        EntityPreviewWidget preview = new EntityPreviewWidget(supplier, 32, 32);
        assertSame(supplier, preview.entity(), "the supplier itself is held, not an entity captured once");

        try (WidgetTestScene fixture = new WidgetTestScene(220, 60)) {
            fixture.add(preview);
            fixture.stabilize();
            assertEquals(32, preview.width());
        }
        assertEquals(0, calls.get(), "building, mounting and measuring never resolve the entity");
    }

    @Test
    @SuppressWarnings("NullAway")
    void aPreviewNeedsASupplier() {
        assertThrows(NullPointerException.class, () -> new EntityPreviewWidget(null, 16, 16));
    }

    @Test
    void theAutoFitScaleFillsTheBoxForBigAndSmallAlike() {
        // a player in the panel's portrait box: height-bound — the fit works against the box the stance
        // camera *projects*, so it is slightly smaller than the raw bbHeight fit would ask for and the feet
        // stay inside
        assertEquals(13, EntityPreviewRenderer.fitScale(0.6f, 1.8f, 22, 28));
        // a golem-sized box: width-bound, and nothing is clipped
        assertEquals(4, EntityPreviewRenderer.fitScale(3.0f, 3.0f, 22, 28));
        // a chicken-sized box: free to zoom in until it fills the same box
        assertEquals(46, EntityPreviewRenderer.fitScale(0.3f, 0.4f, 22, 28));
        // a degenerate box never asks for a zero scale
        assertEquals(1, EntityPreviewRenderer.fitScale(100f, 100f, 22, 28));
    }
}
