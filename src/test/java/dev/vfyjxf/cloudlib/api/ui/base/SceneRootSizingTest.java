package dev.vfyjxf.cloudlib.api.ui.base;

import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import net.minecraft.client.gui.Font;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SceneRootSizingTest {

    @Test
    void fullSizedRootFillsLayoutAreaAndHitTests() {
        WidgetGroup<Widget> root = new WidgetGroup<>();
        root.useStyle(UIStyles.sizeFull());

        Scene scene = new Scene(root);
        scene.init();
        scene.mount(SceneContext.create(testSceneHost()));
        scene.setLayoutArea(480, 270);
        scene.stabilize();

        assertEquals(480, root.width(), "root width after stabilize");
        assertEquals(270, root.height(), "root height after stabilize");
        assertNotNull(scene.hitTest(36, 80), "hitTest inside window");
    }

    private static SceneHost testSceneHost() {
        return new SceneHost() {
            @Override
            public Font font() {
                return null;
            }

            @Override
            public int width() {
                return 480;
            }

            @Override
            public int height() {
                return 270;
            }
        };
    }
}
