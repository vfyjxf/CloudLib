package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unmounting a subtree that still holds queued-created children. Adding a widget to a
 * mounted parent queues it in the scene's created set; the flush that would mount it
 * runs later. Removing the mounted ancestor before that flush walks the whole subtree
 * — including children that never received a scene — and the walk must release them
 * instead of dereferencing the scene they never got.
 */
class CreatedSubtreeUnmountTest {

    @Test
    void removingAMountedGroupWithQueuedCreatedChildrenDoesNotThrow() {
        try (WidgetTestScene fixture = new WidgetTestScene(120, 40)) {
            WidgetGroup<Widget> middle = fixture.add(new WidgetGroup<>());
            fixture.stabilize();
            // the leaf stays created: it waits in the scene's created set and never
            // received a scene of its own before its ancestor is torn down
            WidgetGroup<Widget> leaf = new WidgetGroup<>();
            middle.addWidget(leaf);

            assertTrue(fixture.root.remove(middle), "the subtree is gone");

            fixture.stabilize();
            assertFalse(middle.lifecycle().mounted());
            assertFalse(leaf.lifecycle().mounted(), "the created leaf was released with its subtree");
        }
    }

    @Test
    void destroyingASceneWithQueuedCreatedChildrenDoesNotThrow() {
        try (WidgetTestScene fixture = new WidgetTestScene(120, 40)) {
            WidgetGroup<Widget> middle = fixture.add(new WidgetGroup<>());
            fixture.stabilize();
            middle.addWidget(new WidgetGroup<>());
            // close() runs the destroy walk, which unmounts the queued-created subtree
        }
    }
}
