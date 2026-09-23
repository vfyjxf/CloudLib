package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.SceneContext;
import dev.vfyjxf.cloudlib.api.ui.base.SceneHost;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;

/**
 * A mounted scene for the widget tests: a full-size root over a screen of the
 * given size, with {@link #add} dropping a widget into it. The host carries a
 * metric-only {@link Font} — measuring widgets want a width and a line height,
 * never glyphs.
 */
final class WidgetTestScene implements AutoCloseable {

    /**
     * Six px per character. {@code Font}'s provider function is only consulted
     * when a glyph is actually rasterised, which never happens headlessly.
     */
    private static final Font metricFont = new Font(location -> null, false) {
        private static final int advance = 6;

        @Override
        public int width(String text) {
            return text.length() * advance;
        }

        @Override
        public int width(FormattedText text) {
            return width(text.getString());
        }
    };

    final WidgetGroup<Widget> root = new WidgetGroup<>();
    final Scene scene;

    WidgetTestScene(int width, int height) {
        root.useStyle(UIStyles.sizeFull());
        scene = new Scene(root);
        scene.init();
        scene.mount(SceneContext.create(host(width, height)));
        scene.setLayoutArea(width, height);
        scene.stabilize();
    }

    <T extends Widget> T add(T widget) {
        root.addWidget(widget);
        return widget;
    }

    void stabilize() {
        scene.stabilize();
    }

    @Override
    public void close() {
        scene.destroy();
    }

    private static SceneHost host(int width, int height) {
        return new SceneHost() {
            @Override
            public Font font() {
                return metricFont;
            }

            @Override
            public int width() {
                return width;
            }

            @Override
            public int height() {
                return height;
            }
        };
    }
}
