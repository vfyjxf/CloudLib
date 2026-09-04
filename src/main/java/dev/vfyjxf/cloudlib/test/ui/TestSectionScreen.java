package dev.vfyjxf.cloudlib.test.ui;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.ui.base.BasicScreen;
import dev.vfyjxf.cloudlib.api.ui.base.SceneLayer;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.debug.Inspector;
import dev.vfyjxf.cloudlib.api.ui.scroll.ScrollDirection;
import dev.vfyjxf.cloudlib.api.ui.scroll.ScrollState;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.ui.widget.ButtonWidget;
import dev.vfyjxf.cloudlib.ui.widget.ColumnWidget;
import dev.vfyjxf.cloudlib.ui.widget.LabelWidget;
import dev.vfyjxf.cloudlib.ui.widget.RowWidget;
import dev.vfyjxf.taffy.style.TaffyDimension;

import static dev.vfyjxf.cloudlib.api.ui.effect.UIEffects.scrollable;
import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.*;

/**
 * Test screen demonstrating collapsible section layout.
 * <p>
 * Three sections with expand/collapse functionality,
 * plus vertical scrolling on the root container.
 */
@TestScreen
public class TestSectionScreen extends BasicScreen {

    public TestSectionScreen() {
        buildUI();
    }

    private void buildUI() {
        // Root: vertical column, centered items, full size, scrollable
        var scrollState = ScrollState.create(ScrollDirection.vertical)
                                     .scrollSpeed(12)
                                     .smooth(true)
                                     .smoothSpeed(0.35f)
                                     .trackTexture(new ColorTexture(0x60000000))
                                     .thumbTexture(new ColorTexture(0xCC888888))
                                     .scrollbarWidth(6);

        var root = ColumnWidget.create(8);
        root.useStyle(UIStyle.of(
                flexColumn(),
                alignItemsCenter(),
                padding(8),
                sizeOf(TaffyDimension.percent(1f), TaffyDimension.percent(1f)),
                background(new ColorTexture(0xFF0F172A))
        ));
        root.useEffect(scrollable(scrollState));
        root.onMouseScrolled((mouseX, mouseY, scrollX, scrollY, context) -> {
            scrollState.scrollBy(0, (float) (-scrollY * scrollState.scrollSpeed()));
            return EventDispatch.consumed;
        });

        // === Section 1 ===
        var section1 = createSection("第一个分组", true);
        section1.useStyle(UIStyle.of(
                widthOf(TaffyDimension.percent(0.5f))
        ));
        var label1 = LabelWidget.of("这是第一个分组的内容")
								.setColor(0xFFE2E8F0)
								.setAlign(LabelWidget.TextAlign.CENTER);
        label1.useStyle(UIStyle.of(
                sizeOf(TaffyDimension.percent(1f), TaffyDimension.length(200)),
                background(new ColorTexture(0xFF334155))
        ));
        addSectionContent(section1, label1, true);
        root.addWidget(section1);

        // === Section 2 - 默认展开 ===
        var section2 = createSection("第二个分组 - 默认展开", true);
        section2.useStyle(UIStyle.of(
                widthOf(TaffyDimension.percent(0.5f))
        ));
        var content2 = ColumnWidget.create(4);
        content2.useStyle(UIStyle.of(
                alignItemsFlexStart(),
                sizeOf(TaffyDimension.percent(1f), TaffyDimension.length(150)),
                padding(4),
                background(new ColorTexture(0xFF475569))
        ));
        for (int i = 1; i <= 3; i++) {
            int idx = i;
            var btn = ButtonWidget.of("按钮 " + i, () -> System.out.println("press " + idx))
								  .setColors(0xFF334155, 0xFF475569, 0xFF1E293B);
            btn.useStyle(UIStyle.of(
                    sizeOf(100, 32),
                    flexShrink(0)
            ));
            content2.addWidget(btn);
        }
        addSectionContent(section2, content2, true);
        root.addWidget(section2);

        // === Section 3 - 初始折叠 ===
        var section3 = createSection("第三个分组 - 初始折叠", false);
        section3.useStyle(UIStyle.of(
                widthOf(TaffyDimension.percent(0.5f))
        ));
        var content3 = new Widget();
        content3.useStyle(UIStyle.of(
                sizeOf(TaffyDimension.percent(1f), TaffyDimension.length(180)),
                background(new ColorTexture(0xFF64748B))
        ));
        addSectionContent(section3, content3, false);
        root.addWidget(section3);

        // === 底部提示 ===
        var hint = LabelWidget.of("提示：点击分组标题可以展开/折叠内容")
                              .setColor(0xFF94A3B8)
                              .setAlign(LabelWidget.TextAlign.CENTER);
        hint.useStyle(UIStyle.of(
                sizeOf(TaffyDimension.percent(1f), TaffyDimension.length(30)),
                flexShrink(0)
        ));
        root.addWidget(hint);

        mainGroup().addWidget(root);

        // Inspector for debugging
        mainGroup().addWidget(
                Inspector.create()
                         .setTrackMouse(true)
                         .useStyle(positionAbsolute(), sizeOf(280, 200))
                         .setSceneLayer(SceneLayer.debug)
        );
    }

    /**
     * Creates a collapsible section with a clickable header.
     *
     * @param title    the section title
     * @param expanded whether the section starts expanded
     * @return the section column widget (header + content placeholder)
     */
    private ColumnWidget createSection(String title, boolean expanded) {
        var section = ColumnWidget.create();
        boolean[] state = {expanded};

        // Header row
        var header = RowWidget.create(4);
        header.useStyle(UIStyle.of(
                widthOf(TaffyDimension.percent(1f)),
                padding(6, 8),
                alignItemsCenter(),
                background(new ColorTexture(0xFF1E293B))
        ));

        var arrow = LabelWidget.of(expanded ? "▼" : "▶")
                               .setColor(0xFFE2E8F0);
        arrow.useStyle(UIStyle.of(sizeOf(12, 12)));
        header.addWidget(arrow);

        var titleLabel = LabelWidget.of(title)
                                   .setColor(0xFFE2E8F0)
                                   .setShadow(true);
        header.addWidget(titleLabel);

        section.addWidget(header);

        // Click header to toggle
        header.onMouseClicked((input, context) -> {
            state[0] = !state[0];
            // Find the content widget (second child of section)
            var children = section.children();
            if (children.size() > 1) {
                children.get(1).setVisible(state[0]);
            }
            arrow.setText(state[0] ? "▼" : "▶");
            return EventDispatch.consumed;
        });

        return section;
    }

    /**
     * Adds content to a section created by {@link #createSection}.
     */
    private void addSectionContent(ColumnWidget section, Widget content, boolean expanded) {
        content.setVisible(expanded);
        section.addWidget(content);
    }
}
