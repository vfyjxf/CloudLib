package dev.vfyjxf.cloudlib.api.ui.base;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.taffy.geometry.FloatSize;
import net.minecraft.client.gui.Font;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code Scene}'s pointer-capture input model: while a press is active the
 * drag stays with the widget the press hit (no re-hitTest — the same capture
 * rule {@code mouseReleased} applies to its click determination), and key
 * events resolve against the synthetic pointer — the last position any
 * pointer entry recorded — instead of polling the OS mouse, so a scene driven
 * through synthesized input (world-mode crosshair) keys off the position it
 * actually presented.
 */
class ScenePointerCaptureTest {

    private static final int screenW = 480;
    private static final int screenH = 270;

    private Scene scene;
    private WidgetGroup<Widget> root;

    @BeforeEach
    void setUp() {
        root = new WidgetGroup<>();
        root.useStyle(UIStyles.sizeFull());
        scene = new Scene(root);
        scene.init();
        scene.mount(SceneContext.create(testSceneHost()));
        scene.setLayoutArea(screenW, screenH);
        scene.stabilize();
    }

    /** A fixed-size widget placed at a scene-space rect (the floating-layer convention). */
    private Widget panel(int w, int h) {
        Widget widget = new Widget();
        widget.useStyle(UIStyles.positionAbsolute());
        widget.setCoordinateSpace(CoordinateSpace.scene);
        widget.setSceneLayer(SceneLayer.floating);
        widget.onMount((s, context, handle) ->
                s.layoutTree().setMeasureFunc(widget.nodeId(), (style, availableSpace) -> new FloatSize(w, h)));
        root.addWidget(widget);
        return widget;
    }

    @Test
    void dragStaysWithThePressedWidgetUntilRelease() {
        Widget a = panel(60, 20);
        Widget b = panel(60, 20);
        scene.stabilize();
        a.viewport().setLayout(10, 10);
        b.viewport().setLayout(10, 60);

        List<String> drags = new ArrayList<>();
        a.onMouseDragged((input, deltaX, deltaY, context) -> {
            drags.add("a@" + (int) input.mouseX() + "," + (int) input.mouseY());
            return EventDispatch.handled;
        });
        b.onMouseDragged((input, deltaX, deltaY, context) -> {
            drags.add("b@" + (int) input.mouseX() + "," + (int) input.mouseY());
            return EventDispatch.handled;
        });

        // press on A, then drag while the pointer travels over B — the drag
        // must keep dispatching to A (capture), with the event's coordinates
        assertSame(a, scene.hitTest(20, 15), "the press lands on A");
        scene.mouseClicked(20, 15, GLFW.GLFW_MOUSE_BUTTON_LEFT);
        assertTrue(scene.mouseDragged(30, 70, GLFW.GLFW_MOUSE_BUTTON_LEFT, 10, 55), "the drag is dispatched");
        assertEquals(
                List.of("a@30,70"), drags, "the pressed widget receives the drag, not the widget under the pointer");

        // release ends the capture (mouseReleased clears the pressed target)
        scene.mouseReleased(30, 70, GLFW.GLFW_MOUSE_BUTTON_LEFT);

        // the next drag re-hitTests: the pointer is over B now
        assertTrue(scene.mouseDragged(32, 72, GLFW.GLFW_MOUSE_BUTTON_LEFT, 2, 2));
        assertEquals(List.of("a@30,70", "b@32,72"), drags, "after release the drag follows the fresh hitTest again");

        // and with no press active, a drag over empty space dispatches nothing
        assertFalse(scene.mouseDragged(200, 200, GLFW.GLFW_MOUSE_BUTTON_LEFT, 1, 1));
        assertEquals(2, drags.size());
    }

    @Test
    void keyEventsUseTheSyntheticPointerPosition() {
        Widget focused = panel(60, 20);
        scene.stabilize();
        focused.viewport().setLayout(10, 10);
        focused.setFocusable(true);
        scene.requestFocus(focused);

        List<String> keys = new ArrayList<>();
        focused.onKeyPressed((input, context) -> {
            keys.add((int) input.mouseX() + "," + (int) input.mouseY());
            return EventDispatch.handled;
        });

        // the pointer the scene last saw — a mouseMoved — is what the key reads
        scene.mouseMoved(123, 45);
        assertTrue(scene.keyPressed(GLFW.GLFW_KEY_Z, 0, 0));
        assertEquals(List.of("123,45"), keys, "key events resolve against the last recorded pointer position");

        // any pointer entry updates it — even a drag that dispatches nowhere
        assertFalse(scene.mouseDragged(200, 100, GLFW.GLFW_MOUSE_BUTTON_LEFT, 1, 1));
        scene.keyReleased(GLFW.GLFW_KEY_Z, 0, 0);
        assertTrue(scene.keyPressed(GLFW.GLFW_KEY_X, 0, 0));
        assertEquals(List.of("123,45", "200,100"), keys);
    }

    @Test
    void keyEventsFallBackToTheLayoutAreaCenterBeforeAnyPointerInput() {
        Widget focused = panel(60, 20);
        scene.stabilize();
        focused.viewport().setLayout(10, 10);
        focused.setFocusable(true);
        scene.requestFocus(focused);

        double[] seen = new double[2];
        focused.onKeyPressed((input, context) -> {
            seen[0] = input.mouseX();
            seen[1] = input.mouseY();
            return EventDispatch.handled;
        });

        // no pointer event yet — the synthetic pointer rests at the layout
        // area's center, the pointer-less world mode's rest position
        assertTrue(scene.keyPressed(GLFW.GLFW_KEY_Z, 0, 0));
        assertEquals(screenW * 0.5, seen[0], 1.0e-9);
        assertEquals(screenH * 0.5, seen[1], 1.0e-9);
    }

    private static SceneHost testSceneHost() {
        return new SceneHost() {
            @Override
            public Font font() {
                return null; // headless — no text measure goes through the host
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
