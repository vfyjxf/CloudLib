package dev.vfyjxf.cloudlib.test.ui;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.ui.base.BasicScreen;
import dev.vfyjxf.cloudlib.api.ui.base.SceneLayer;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.debug.Inspector;
import dev.vfyjxf.cloudlib.api.ui.scroll.ScrollDirection;
import dev.vfyjxf.cloudlib.api.ui.scroll.ScrollState;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.ui.widget.BoxWidget;
import dev.vfyjxf.cloudlib.ui.widget.ButtonWidget;
import dev.vfyjxf.cloudlib.ui.widget.ColumnWidget;
import dev.vfyjxf.cloudlib.ui.widget.DividerWidget;
import dev.vfyjxf.cloudlib.ui.widget.LabelWidget;
import dev.vfyjxf.cloudlib.ui.widget.RowWidget;
import dev.vfyjxf.cloudlib.ui.widget.SpacerWidget;

import static dev.vfyjxf.cloudlib.api.ui.effect.UIEffects.scrollable;

/**
 * Test screen for ScrollEffect.
 * <p>
 * Demonstrates:
 * <ul>
 *   <li>Vertical scroll with auto content size</li>
 *   <li>Horizontal scroll</li>
 *   <li>Smooth scrolling toggle</li>
 *   <li>Dynamic content add/remove</li>
 *   <li>Custom scrollbar textures</li>
 * </ul>
 */
@TestScreen
public class TestScrollScreen extends BasicScreen {

    // ========== State ==========
    private int itemCounter = 0;

    // ========== Scroll States ==========
    private ScrollState verticalState;
    private ScrollState horizontalState;

    // ========== Containers ==========
    private TestContainerWidget verticalContent;
    private TestContainerWidget horizontalContent;
    private LabelWidget statusLabel;

    public TestScrollScreen() {
        buildUI();
    }

    private void buildUI() {
        // Root layout: vertical column
        var root = ColumnWidget.create(8);
        root.useStyle(UIStyle.of(
            UIStyles.flex(1),
            UIStyles.minSize(0, 0),
            UIStyles.padding(12),
            UIStyles.background(new ColorTexture(0xCC222222))
        ));

        // Title
        root.addWidget(createTitle());
        root.addWidget(createDivider());

        // Controls
        root.addWidget(createControlPanel());
        root.addWidget(createDivider());

        // Scroll demo area: two panels side by side
        root.addWidget(createScrollDemoArea());
        root.addWidget(createDivider());

        // Status bar
        root.addWidget(createStatusBar());

        mainGroup().addWidget(
            Inspector.create()
                     .setTrackMouse(true)
                     .setDisplayMode(Inspector.DisplayMode.FULL)
                     .useStyle(UIStyles.positionAbsolute(), UIStyles.sizeOf(280, 180))
                     .setSceneLayer(SceneLayer.debug)
        );

        mainGroup().addWidget(root);

        // Pre-populate enough items to clearly overflow the viewport
        for (int i = 0; i < 40; i++) {
            addVerticalItem();
        }
        for (int i = 0; i < 30; i++) {
            addHorizontalItem();
        }
    }

    // ========== Title ==========

    private Widget createTitle() {
        var header = RowWidget.create(8);
        header.useStyle(UIStyle.of(UIStyles.alignItemsCenter()));

        var title = LabelWidget.of("ScrollEffect Test")
                               .setColor(0xFFFFAA00)
                               .setShadow(true);
        title.useStyle(UIStyle.of(UIStyles.sizeOf(200, 12)));
        header.addWidget(title);

        return header;
    }

    // ========== Controls ==========

    private Widget createControlPanel() {
        var panel = ColumnWidget.create(6);
        panel.useStyle(UIStyle.of(
            UIStyles.padding(8),
            UIStyles.background(new ColorTexture(0x40000000))
        ));

        // Row 1: Add/Remove items
        var row1 = RowWidget.create(4);
        row1.useStyle(UIStyle.of(UIStyles.alignItemsCenter(), UIStyles.flexWrap(), UIStyles.rowGap(4)));
        row1.addWidget(LabelWidget.of("Items:").setColor(0xFFAAAA00));
        row1.addWidget(createButton("+ V Item", this::addVerticalItem, 0xFF0066CC));
        row1.addWidget(createButton("+ 10 V", () -> {
            for (int i = 0; i < 10; i++) addVerticalItem();
        }, 0xFF0088FF));
        row1.addWidget(createButton("Clear V", this::clearVertical, 0xFFCC0000));
        row1.addWidget(SpacerWidget.create().useStyle(UIStyle.of(UIStyles.widthOf(16))));
        row1.addWidget(createButton("+ H Item", this::addHorizontalItem, 0xFF006600));
        row1.addWidget(createButton("+ 10 H", () -> {
            for (int i = 0; i < 10; i++) addHorizontalItem();
        }, 0xFF00AA00));
        row1.addWidget(createButton("Clear H", this::clearHorizontal, 0xFFCC0000));
        panel.addWidget(row1);

        // Row 2: Scroll controls
        var row2 = RowWidget.create(4);
        row2.useStyle(UIStyle.of(UIStyles.alignItemsCenter(), UIStyles.flexWrap(), UIStyles.rowGap(4)));
        row2.addWidget(LabelWidget.of("Scroll:").setColor(0xFF00AAAA));
        row2.addWidget(createButton("Toggle Smooth", this::toggleSmooth, 0xFF008888));
        row2.addWidget(createButton("Speed+", this::increaseSpeed, 0xFF666600));
        row2.addWidget(createButton("Speed-", this::decreaseSpeed, 0xFF666600));
        row2.addWidget(createButton("Reset V", () -> verticalState.resetScroll(), 0xFF884400));
        row2.addWidget(createButton("Reset H", () -> horizontalState.resetScroll(), 0xFF884400));
        row2.addWidget(createButton("Jump V→50%", () -> verticalState.jumpTo(0, verticalState.maxScrollY() * 0.5f), 0xFF446688));
        row2.addWidget(createButton("Jump V→End", () -> verticalState.jumpTo(0, verticalState.maxScrollY()), 0xFF446688));
        row2.addWidget(createButton("Toggle Drag", this::toggleDrag, 0xFF880088));
        panel.addWidget(row2);

        return panel;
    }

    // ========== Scroll Demo Area ==========

    private Widget createScrollDemoArea() {
        var row = RowWidget.create(12);
        row.useStyle(UIStyle.of(UIStyles.flex(1), UIStyles.minSize(0, 0)));

        // Left: Vertical scroll panel
        row.addWidget(createVerticalScrollPanel());

        // Divider
        var divider = DividerWidget.vertical().setColor(0xFF666666);
        divider.useStyle(UIStyle.of(UIStyles.widthOf(2)));
        row.addWidget(divider);

        // Right: Horizontal scroll panel
        row.addWidget(createHorizontalScrollPanel());

        return row;
    }

    private Widget createVerticalScrollPanel() {
        var box = BoxWidget.create();
        box.useStyle(UIStyle.of(
            UIStyles.flex(1),
            UIStyles.minSize(0, 0),
            UIStyles.padding(0)
        ));

        var column = ColumnWidget.create(4);
        column.useStyle(UIStyle.of(UIStyles.flex(1), UIStyles.minSize(0, 0)));

        // Header
        var label = LabelWidget.of("▼ Vertical Scroll (mouse wheel)")
                               .setColor(0xFF88AAFF)
                               .setShadow(true);
        label.useStyle(UIStyle.of(UIStyles.heightOf(14)));
        column.addWidget(label);

        // Scrollable container
        verticalState = ScrollState.create(ScrollDirection.vertical)
            .scrollSpeed(12)
            .smooth(true)
            .smoothSpeed(0.35f)
            .trackTexture(new ColorTexture(0x60000000))
            .thumbTexture(new ColorTexture(0xCC8888FF))
            .scrollbarWidth(6);

        verticalContent = TestContainerWidget.create(4);
        verticalContent.useStyle(UIStyle.of(
            UIStyles.flexGrow(1),
            UIStyles.minHeight(0),
            UIStyles.padding(4),
            UIStyles.background(new ColorTexture(0x20336699))
        ));
        verticalContent.useEffect(scrollable(verticalState));

        // Widget listens to its own scroll event
        verticalContent.onMouseScrolled((mouseX, mouseY, scrollX, scrollY, context) -> {
            verticalState.scrollBy(0, (float) (-scrollY * verticalState.scrollSpeed()));
            return EventDispatch.consumed;
        });

        column.addWidget(verticalContent);
        box.addChild(column);
        return box;
    }

    private Widget createHorizontalScrollPanel() {
        var box = BoxWidget.create();
        box.useStyle(UIStyle.of(
            UIStyles.flex(1),
            UIStyles.minSize(0, 0),
            UIStyles.padding(0)
        ));

        var column = ColumnWidget.create(4);
        column.useStyle(UIStyle.of(UIStyles.flex(1), UIStyles.minSize(0, 0)));

        // Header
        var label = LabelWidget.of("▶ Horizontal Scroll (shift + wheel)")
                               .setColor(0xFF88FF88)
                               .setShadow(true);
        label.useStyle(UIStyle.of(UIStyles.heightOf(14)));
        column.addWidget(label);

        // Scrollable container
        horizontalState = ScrollState.create(ScrollDirection.horizontal)
            .scrollSpeed(15)
            .smooth(true)
            .smoothSpeed(0.3f)
            .trackTexture(new ColorTexture(0x60000000))
            .thumbTexture(new ColorTexture(0xCC88FF88))
            .scrollbarWidth(6);

        horizontalContent = TestContainerWidget.create(0);
        horizontalContent.useStyle(UIStyle.of(
            UIStyles.flexGrow(1),
            UIStyles.minSize(0, 0),
            UIStyles.flexRow(),
            UIStyles.padding(4),
            UIStyles.columnGap(4),
            UIStyles.background(new ColorTexture(0x20336633))
        ));
        horizontalContent.useEffect(scrollable(horizontalState));

        // Widget listens to its own scroll event (scrollX for horizontal)
        horizontalContent.onMouseScrolled((mouseX, mouseY, scrollX, scrollY, context) -> {
            // Use scrollY (vertical wheel) to scroll horizontally
            horizontalState.scrollBy((float) (-scrollY * horizontalState.scrollSpeed()), 0);
            return EventDispatch.consumed;
        });

        column.addWidget(horizontalContent);
        box.addChild(column);
        return box;
    }

    // ========== Status Bar ==========

    private Widget createStatusBar() {
        var bar = RowWidget.create(8);
        bar.useStyle(UIStyle.of(UIStyles.alignItemsCenter()));

        statusLabel = LabelWidget.of("Ready — scroll inside the panels")
                                 .setColor(0xFFAAAAAA);
        statusLabel.useStyle(UIStyle.of(UIStyles.flexGrow(1)));
        bar.addWidget(statusLabel);

        return bar;
    }

    // ========== Actions ==========

    private void addVerticalItem() {
        int idx = itemCounter++;
        // Varied height items to demonstrate dynamic content
        int colorVariant = (idx * 37) & 0xFF;
        int color = 0xFF000000 | (0x44 + colorVariant / 2) << 16 | (0x66 + colorVariant / 3) << 8 | 0xFF;

        var item = LabelWidget.of("Item #" + idx)
                              .setColor(0xFFDDDDFF)
                              .setShadow(true);
        int height = 18 + (idx % 3) * 4; // 18, 22, 26 px alternating
        item.useStyle(UIStyle.of(
            UIStyles.padding(4, 8),
            UIStyles.heightOf(height),
            UIStyles.background(new ColorTexture(color & 0x40FFFFFF))
        ));
        verticalContent.addChild(item);
        updateStatus("Added vertical item #" + idx + " (h=" + height + ")");
    }

    private void addHorizontalItem() {
        int idx = itemCounter++;
        int colorVariant = (idx * 53) & 0xFF;
        int color = 0xFF000000 | 0x44 << 16 | (0x88 + colorVariant / 3) << 8 | (0x44 + colorVariant / 2);

        var item = LabelWidget.of("H#" + idx)
                              .setColor(0xFFDDFFDD)
                              .setShadow(true);
        int width = 50 + (idx % 4) * 10; // 50, 60, 70, 80 px
        item.useStyle(UIStyle.of(
            UIStyles.padding(4, 6),
            UIStyles.widthOf(width),
            UIStyles.background(new ColorTexture(color & 0x40FFFFFF))
        ));
        horizontalContent.addChild(item);
        updateStatus("Added horizontal item #" + idx + " (w=" + width + ")");
    }

    private void clearVertical() {
        int count = verticalContent.children().size();
        verticalContent.clearChildren();
        verticalState.resetScroll();
        updateStatus("Cleared " + count + " vertical items");
    }

    private void clearHorizontal() {
        int count = horizontalContent.children().size();
        horizontalContent.clearChildren();
        horizontalState.resetScroll();
        updateStatus("Cleared " + count + " horizontal items");
    }

    private void toggleSmooth() {
        boolean newSmooth = !verticalState.smooth();
        verticalState.smooth(newSmooth);
        horizontalState.smooth(newSmooth);
        updateStatus("Smooth scrolling: " + (newSmooth ? "ON" : "OFF"));
    }

    private void increaseSpeed() {
        int newSpeed = verticalState.scrollSpeed() + 4;
        verticalState.scrollSpeed(newSpeed);
        horizontalState.scrollSpeed(newSpeed);
        updateStatus("Scroll speed: " + newSpeed);
    }

    private void decreaseSpeed() {
        int newSpeed = Math.max(1, verticalState.scrollSpeed() - 4);
        verticalState.scrollSpeed(newSpeed);
        horizontalState.scrollSpeed(newSpeed);
        updateStatus("Scroll speed: " + newSpeed);
    }

    private void toggleDrag() {
        boolean newDrag = !verticalState.draggable();
        verticalState.draggable(newDrag);
        horizontalState.draggable(newDrag);
        updateStatus("Scrollbar drag: " + (newDrag ? "ON" : "OFF"));
    }

    // ========== Helpers ==========

    private void updateStatus(String message) {
        if (statusLabel != null) {
            String vInfo = String.format("V: %.0f/%.0f", verticalState.scrollY(), (float) verticalState.maxScrollY());
            String hInfo = String.format("H: %.0f/%.0f", horizontalState.scrollX(), (float) horizontalState.maxScrollX());
            statusLabel.setText(message + "  |  " + vInfo + "  " + hInfo);
        }
    }

    private Widget createDivider() {
        var divider = DividerWidget.horizontal().setColor(0xFF555555);
        divider.useStyle(UIStyle.of(UIStyles.heightOf(2)));
        return divider;
    }

    private ButtonWidget createButton(String label, Runnable action, int color) {
        var btn = ButtonWidget.of(label, action)
                              .setColors(color, lighten(color), darken(color));
        btn.useStyle(UIStyle.of(UIStyles.minWidth(70), UIStyles.heightOf(16), UIStyles.padding(2)));
        return btn;
    }

    private static int lighten(int color) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, ((color >> 16) & 0xFF) + 40);
        int g = Math.min(255, ((color >> 8) & 0xFF) + 40);
        int b = Math.min(255, (color & 0xFF) + 40);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int darken(int color) {
        int a = (color >> 24) & 0xFF;
        int r = Math.max(0, ((color >> 16) & 0xFF) - 40);
        int g = Math.max(0, ((color >> 8) & 0xFF) - 40);
        int b = Math.max(0, (color & 0xFF) - 40);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
