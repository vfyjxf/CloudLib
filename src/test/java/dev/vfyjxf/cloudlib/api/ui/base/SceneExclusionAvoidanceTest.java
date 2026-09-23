package dev.vfyjxf.cloudlib.api.ui.base;

import dev.vfyjxf.cloudlib.api.math.Insets;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.ExclusionProvider;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.InworldExclusions;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.taffy.geometry.FloatSize;
import net.minecraft.client.gui.Font;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class SceneExclusionAvoidanceTest {

    private static final int screenW = 480;
    private static final int screenH = 270;

    @Test
    void rootShrinksAroundRegisteredExclusionsAndRelayoutsOnChange() {
        WidgetGroup<Widget> root = new WidgetGroup<>();
        root.useStyle(UIStyles.sizeFull());
        Scene scene = newScene(root);

        // no exclusions registered → the root fills the whole layout area
        assertEquals(screenW, root.width());
        assertEquals(screenH, root.height());

        // a bottom HUD bar and a left HUD column register as exclusion areas
        Rect hotbar = new Rect(0, screenH - 22, screenW, 22);
        Rect leftColumn = new Rect(0, 0, 62, screenH);
        ExclusionProvider provider = context -> {
            assertEquals(screenW, context.screenWidth());
            assertEquals(screenH, context.screenHeight());
            return List.of(hotbar, leftColumn);
        };
        InworldExclusions.register(provider);
        try {
            scene.setLayoutArea(screenW, screenH);
            scene.stabilize();

            assertEquals(screenW - 62, root.width(), "root must shrink by the left exclusion column");
            assertEquals(screenH - 22, root.height(), "root must shrink by the bottom exclusion bar");
            assertEquals(62, root.absolutePos().x());
            assertEquals(0, root.absolutePos().y());

            Rect rootRect = root.bounds();
            assertFalse(rootRect.intersects(hotbar), "root must not cover the hotbar exclusion");
            assertFalse(rootRect.intersects(leftColumn), "root must not cover the left column exclusion");
        } finally {
            InworldExclusions.unregister(provider);
        }

        // removing the exclusion areas must restore the full layout area
        scene.setLayoutArea(screenW, screenH);
        scene.stabilize();

        assertEquals(screenW, root.width());
        assertEquals(screenH, root.height());
        assertEquals(0, root.absolutePos().x());
        assertEquals(0, root.absolutePos().y());
    }

    @Test
    void exclusionInsetsCombineWithDebugInsetsPerEdge() {
        WidgetGroup<Widget> root = new WidgetGroup<>();
        root.useStyle(UIStyles.sizeFull());
        Scene scene = newScene(root);

        Rect hotbar = new Rect(0, screenH - 22, screenW, 22);
        ExclusionProvider provider = context -> List.of(hotbar);
        InworldExclusions.register(provider);
        try {
            scene.setDebugInsets(Insets.symmetric(0, 40));
            scene.setLayoutArea(screenW, screenH);
            scene.stabilize();

            // left/right 40 from the debug dock, bottom 22 from the exclusion
            // strut — the wider side wins, they are never summed
            assertEquals(screenW - 80, root.width());
            assertEquals(screenH - 22, root.height());
            assertEquals(40, root.absolutePos().x());
        } finally {
            InworldExclusions.unregister(provider);
            scene.setDebugInsets(Insets.zero);
        }
    }

    @Test
    void interiorExclusionDoesNotShrinkTheRoot() {
        WidgetGroup<Widget> root = new WidgetGroup<>();
        root.useStyle(UIStyles.sizeFull());
        Scene scene = newScene(root);

        Rect interior = new Rect(200, 100, 80, 40);
        ExclusionProvider provider = context -> List.of(interior);
        InworldExclusions.register(provider);
        try {
            scene.setLayoutArea(screenW, screenH);
            scene.stabilize();

            // an interior rect hugs no edge, so the single root rectangle keeps
            // its full size — interior avoidance belongs to the floating pipeline
            assertEquals(screenW, root.width());
            assertEquals(screenH, root.height());
            assertEquals(Insets.zero, scene.exclusionInsets());
        } finally {
            InworldExclusions.unregister(provider);
        }
    }

    @Test
    void sceneSpaceLayerWidgetIsExemptFromRootStrutTranslation() {
        WidgetGroup<Widget> root = new WidgetGroup<>();
        root.useStyle(UIStyles.sizeFull());
        Scene scene = newScene(root);

        // a regular content child — the strut must keep protecting it
        Widget content = new Widget();
        root.addWidget(content);

        // a self-positioned overlay in the floating layer, laid out in scene
        // space the way floating effects and the in-world panel layer do
        Widget overlay = new Widget();
        overlay.useStyle(UIStyles.positionAbsolute());
        overlay.setCoordinateSpace(CoordinateSpace.scene);
        overlay.setSceneLayer(SceneLayer.floating);
        overlay.onMount(
            (s, context, handle) -> s.layoutTree()
                    .setMeasureFunc(overlay.nodeId(), (style, availableSpace) -> new FloatSize(120, 50))
        );
        root.addWidget(overlay);

        // a chat-like column hugging the left edge only — a full-height column
        // would claim the top edge instead (the thinnest-hugged-edge rule)
        Rect chatColumn = new Rect(0, 30, 320, 180);
        ExclusionProvider provider = context -> List.of(chatColumn);
        InworldExclusions.register(provider);
        try {
            scene.setLayoutArea(screenW, screenH);
            scene.stabilize();

            // the strut is active and still pushes regular scene content
            // clear of the exclusion
            assertEquals(320, scene.exclusionInsets().left());
            assertEquals(320, root.absolutePos().x());
            assertEquals(320, content.absolutePos().x());

            // the overlay's manager-driven scene-space position
            overlay.viewport().setLayout(8, 40);

            // the overlay's absolute transform — exactly what the scene-space
            // layer render path pushes — is the raw scene position: the root's
            // strut translation must not enter it
            assertEquals(8, overlay.absolutePos().x(), 0.5);
            assertEquals(40, overlay.absolutePos().y(), 0.5);

            // the input path agrees: the scene-space layer hit test resolves
            // in scene space, at the overlay's own rect — not at rect + strut
            assertSame(overlay, scene.hitTest(20, 60));
        } finally {
            InworldExclusions.unregister(provider);
        }
    }

    private static Scene newScene(WidgetGroup<Widget> root) {
        Scene scene = new Scene(root);
        scene.init();
        scene.mount(SceneContext.create(testSceneHost()));
        scene.setLayoutArea(screenW, screenH);
        scene.stabilize();
        return scene;
    }

    private static SceneHost testSceneHost() {
        return new SceneHost() {
            @Override
            @SuppressWarnings("NullAway")
            public Font font() {
                return null;
            }

            @Override
            public int width() {
                return screenW;
            }

            @Override
            public int height() {
                return screenH;
            }
        };
    }
}
