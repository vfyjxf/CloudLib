package dev.vfyjxf.cloudlib.api.ui.scroll;

import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.SceneContext;
import dev.vfyjxf.cloudlib.api.ui.base.SceneHost;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.tooltip.Tooltip;
import dev.vfyjxf.cloudlib.api.ui.tooltip.TooltipEntry;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScrollEffectHoverRefreshTest {

    @Test
    void contentOffsetRefreshRehitsHoverPathForNewContentUnderMouse() throws Exception {
        TestRoot root = new TestRoot();
        TestRow first = root.addWidget(row("first"));
        TestRow second = root.addWidget(row("second"));

        Scene scene = new Scene(root);
        scene.init();
        scene.mount(SceneContext.create(testSceneHost()));
        root.setTestBounds(0, 0, 80, 40);
        first.setTestBounds(0, 0, 80, 40);
        second.setTestBounds(0, 40, 80, 40);

        scene.mouseMoved(10, 10);
        assertEquals(List.of("first"), hoverTooltipLines(scene));

        root.viewport().setContentOffset(0, 40);
        scene.requestHoverRefresh(10, 10);
        scene.stabilize();

        assertEquals(List.of("second"), hoverTooltipLines(scene));
    }

    private static TestRow row(String label) {
        TestRow widget = new TestRow();
        widget.onHoverTooltip((mouseX, mouseY) -> Tooltip.create().add(Component.literal(label)));
        return widget;
    }

    private static List<String> hoverTooltipLines(Scene scene) throws Exception {
        Field field = Scene.class.getDeclaredField("hoverTooltip");
        field.setAccessible(true);
        Tooltip tooltip = (Tooltip) field.get(scene);
        return tooltip.flatEntries().stream()
            .map(ScrollEffectHoverRefreshTest::entryText)
            .toList();
    }

    private static String entryText(TooltipEntry entry) {
        return switch (entry) {
            case TooltipEntry.TextEntry text -> text.text().getString();
            case TooltipEntry.DynamicEntry dynamic -> dynamic.provider().get().getString();
            case TooltipEntry.ComponentEntry ignored -> "";
        };
    }

    private static SceneHost testSceneHost() {
        return new SceneHost() {
            @Override
            public Font font() {
                return null;
            }

            @Override
            public int width() {
                return 80;
            }

            @Override
            public int height() {
                return 40;
            }
        };
    }

    private static final class TestRoot extends WidgetGroup<Widget> {
        void setTestBounds(int x, int y, int width, int height) {
            setBound(x, y, width, height);
        }
    }

    private static final class TestRow extends Widget {
        void setTestBounds(int x, int y, int width, int height) {
            setBound(x, y, width, height);
        }
    }
}
