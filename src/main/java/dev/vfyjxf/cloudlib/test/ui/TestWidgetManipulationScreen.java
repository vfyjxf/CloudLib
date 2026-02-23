package dev.vfyjxf.cloudlib.test.ui;

import dev.vfyjxf.cloudlib.api.ui.base.BasicScreen;
import dev.vfyjxf.cloudlib.api.ui.base.SceneLayer;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetTree;
import dev.vfyjxf.cloudlib.api.ui.debug.Inspector;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.ui.widget.*;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

/**
 * Test screen for widget manipulation operations.
 * <p>
 * Tests:
 * <ul>
 *   <li><b>Add</b> - Dynamically adding widgets to containers</li>
 *   <li><b>Remove</b> - Removing widgets (single, batch, clear all)</li>
 *   <li><b>Move</b> - Re-parenting widgets between containers</li>
 *   <li><b>Reuse</b> - Detaching and reattaching the same widget instance</li>
 *   <li><b>Destroy</b> - Bottom-up destruction using WidgetTree</li>
 * </ul>
 */
//@TestScreen
public class TestWidgetManipulationScreen extends BasicScreen {

    // ==================== State ====================
    private int widgetCounter = 0;
    private final MutableList<LabelWidget> createdWidgets = Lists.mutable.empty();

    // ==================== Containers ====================
    private TestContainerWidget containerA;
    private TestContainerWidget containerB;
    private LabelWidget statusLabel;
    private LabelWidget statsLabel;

    // ==================== Reusable Widget ====================
    /**
     * A single widget instance that can be moved between containers
     */
    private LabelWidget reusableWidget;
    private int reuseCount = 0;
    private boolean reusableInA = true;

    public TestWidgetManipulationScreen() {
        buildUI();
    }

    private void buildUI() {
        var mainContainer = ColumnWidget.create(8);
        mainContainer.useStyle(UIStyle.of(
                UIStyles.padding(16),
                UIStyles.background(new ColorTexture(0xCC222222))
        ));

        // Header
        var header = createHeader();
        mainContainer.addWidget(header);

        mainContainer.addWidget(createDivider());

        // Control Panel
        var controlPanel = createControlPanel();
        mainContainer.addWidget(controlPanel);

        mainContainer.addWidget(createDivider());

        // Container Area
        var containerArea = createContainerArea();
        mainContainer.addWidget(containerArea);

        mainContainer.addWidget(createDivider());

        // Status Bar
        var statusBar = createStatusBar();
        mainContainer.addWidget(statusBar);

        mainGroup().addWidget(mainContainer);

        mainGroup().addWidget(
                Inspector.create()
                        .setTrackMouse(true)
                        .setDisplayMode(Inspector.DisplayMode.FULL)
                        .useStyle(UIStyles.positionAbsolute(), UIStyles.sizeOf(280, 180))
                        .setSceneLayer(SceneLayer.debug)
        );

        // Create the reusable widget
        reusableWidget = LabelWidget.of("★ REUSABLE ★")
                .setColor(0xFFFF00FF);
        reusableWidget.onInit((self) -> {
            System.out.println("Reusable widget initialized");
            reuseCount++;
        });
        reusableWidget.onMount(((scene, context, handle) -> {
            System.out.println("Reusable widget mounted");
            reuseCount++;
            reusableWidget.setText("★ REUSABLE ★ #" + reuseCount);
            handle.onCleanup(() -> System.out.println("Reusable widget cleaned up"));
        }));
        reusableWidget.onUnmount(() -> {
            System.out.println("Reusable widget unmounted");
        });
        reusableWidget.onDestroy((self) -> {
            System.out.println("Reusable widget destroyed");
        });
        reusableWidget.useStyle(UIStyle.of(
                UIStyles.padding(4),
                UIStyles.background(new ColorTexture(0x80FF00FF))
        ));
    }

    // ==================== UI Creation ====================

    private Widget createHeader() {
        var header = RowWidget.create(16);
        header.useStyle(UIStyle.of(UIStyles.alignItemsCenter()));

        var title = LabelWidget.of("Widget Manipulation Test")
                .setColor(0xFFFFAA00)
                .setShadow(true);
        title.useStyle(UIStyle.of(UIStyles.sizeOf(200, 12)));
        header.addWidget(title);

        header.addWidget(SpacerWidget.create());

        statsLabel = LabelWidget.of("Widgets: 0")
                .setColor(0xFF88FF88);
        statsLabel.useStyle(UIStyle.of(UIStyles.sizeOf(100, 12)));
        header.addWidget(statsLabel);

        return header;
    }

    private Widget createControlPanel() {
        var panel = ColumnWidget.create(8);
        panel.useStyle(UIStyle.of(
                UIStyles.padding(8),
                UIStyles.background(new ColorTexture(0x40000000))
        ));

        // Row 1: Add Operations
        var addRow = RowWidget.create(4);
        addRow.addWidget(LabelWidget.of("Add:").setColor(0xFFAAAA00));

        addRow.addWidget(createButton("Add to A", this::addWidgetToA, 0xFF0066CC));
        addRow.addWidget(createButton("Add to B", this::addWidgetToB, 0xFF006600));
        addRow.addWidget(createButton("Add 5 to A", () -> {
            for (int i = 0; i < 5; i++) addWidgetToA();
        }, 0xFF0088FF));
        addRow.addWidget(createButton("Add 5 to B", () -> {
            for (int i = 0; i < 5; i++) addWidgetToB();
        }, 0xFF00AA00));
        panel.addWidget(addRow);

        // Row 2: Remove Operations
        var removeRow = RowWidget.create(4);
        removeRow.addWidget(LabelWidget.of("Remove:").setColor(0xFFAA0000));

        removeRow.addWidget(createButton("Remove First A", this::removeFirstFromA, 0xFFCC6600));
        removeRow.addWidget(createButton("Remove Last B", this::removeLastFromB, 0xFFCC6600));
        removeRow.addWidget(createButton("Clear A", this::clearA, 0xFFCC0000));
        removeRow.addWidget(createButton("Clear B", this::clearB, 0xFFCC0000));
        removeRow.addWidget(createButton("Clear All", this::clearAll, 0xFF990000));
        panel.addWidget(removeRow);

        // Row 3: Move Operations
        var moveRow = RowWidget.create(4);
        moveRow.addWidget(LabelWidget.of("Move:").setColor(0xFF00AAAA));

        moveRow.addWidget(createButton("Move First A→B", this::moveFirstAToB, 0xFF008888));
        moveRow.addWidget(createButton("Move Last B→A", this::moveLastBToA, 0xFF008888));
        moveRow.addWidget(createButton("Swap All A↔B", this::swapContainers, 0xFF00AAAA));
        panel.addWidget(moveRow);

        // Row 4: Reuse Operations
        var reuseRow = RowWidget.create(4);
        reuseRow.addWidget(LabelWidget.of("Reuse:").setColor(0xFFFF00FF));

        reuseRow.addWidget(createButton("Toggle Reusable", this::toggleReusableWidget, 0xFFAA00AA));
        reuseRow.addWidget(createButton("Detach Reusable", this::detachReusableWidget, 0xFF880088));
        reuseRow.addWidget(createButton("Attach to A", this::attachReusableToA, 0xFF660066));
        reuseRow.addWidget(createButton("Attach to B", this::attachReusableToB, 0xFF660066));
        panel.addWidget(reuseRow);

        // Row 5: Destroy Operations
        var destroyRow = RowWidget.create(4);
        destroyRow.addWidget(LabelWidget.of("Destroy:").setColor(0xFFFF4444));

        destroyRow.addWidget(createButton("Destroy A (bottomUp)", this::destroyContainerA, 0xFFAA0000));
        destroyRow.addWidget(createButton("Destroy B (deepestFirst)", this::destroyContainerB, 0xFFAA0000));
        panel.addWidget(destroyRow);

        return panel;
    }

    private Widget createContainerArea() {
        var row = RowWidget.create(16);
        row.useStyle(UIStyle.of(UIStyles.flexGrow(1)));

        // Container A
        var boxA = BoxWidget.create();
        boxA.useStyle(UIStyle.of(
                UIStyles.flexGrow(1),
                UIStyles.padding(12),
                UIStyles.background(new ColorTexture(0x30336699))
        ));

        var columnA = ColumnWidget.create(8);
        columnA.useStyle(UIStyle.of(UIStyles.sizeOf(-1, -1)));

        var labelA = LabelWidget.of("▼ Container A")
                .setColor(0xFF88AAFF)
                .setShadow(true);
        labelA.useStyle(UIStyle.of(UIStyles.heightOf(14)));
        columnA.addWidget(labelA);

        containerA = TestContainerWidget.create(4);
        containerA.useStyle(UIStyle.of(
                UIStyles.flexGrow(1),
                UIStyles.padding(8),
                UIStyles.background(new ColorTexture(0x18FFFFFF))
        ));
        columnA.addWidget(containerA);

        boxA.addChild(columnA);
        row.addWidget(boxA);

        // Vertical Divider
        var divider = DividerWidget.vertical().setColor(0xFF666666);
        divider.useStyle(UIStyle.of(UIStyles.widthOf(2)));
        row.addWidget(divider);

        // Container B
        var boxB = BoxWidget.create();
        boxB.useStyle(UIStyle.of(
                UIStyles.flexGrow(1),
                UIStyles.padding(12),
                UIStyles.background(new ColorTexture(0x30336633))
        ));

        var columnB = ColumnWidget.create(8);
        columnB.useStyle(UIStyle.of(UIStyles.sizeOf(-1, -1)));

        var labelB = LabelWidget.of("▼ Container B")
                .setColor(0xFF88FF88)
                .setShadow(true);
        labelB.useStyle(UIStyle.of(UIStyles.heightOf(14)));
        columnB.addWidget(labelB);

        containerB = TestContainerWidget.create(4);
        containerB.useStyle(UIStyle.of(
                UIStyles.flexGrow(1),
                UIStyles.padding(8),
                UIStyles.background(new ColorTexture(0x18FFFFFF))
        ));
        columnB.addWidget(containerB);

        boxB.addChild(columnB);
        row.addWidget(boxB);

        return row;
    }

    private Widget createStatusBar() {
        var statusBar = RowWidget.create(8);
        statusBar.useStyle(UIStyle.of(UIStyles.alignItemsCenter()));

        statusLabel = LabelWidget.of("Ready")
                .setColor(0xFFAAAAAA);
        statusLabel.useStyle(UIStyle.of(UIStyles.flexGrow(1)));
        statusBar.addWidget(statusLabel);

        return statusBar;
    }

    private Widget createDivider() {
        var divider = DividerWidget.horizontal().setColor(0xFF555555);
        divider.useStyle(UIStyle.of(UIStyles.heightOf(2)));
        return divider;
    }

    private ButtonWidget createButton(String label, Runnable action, int color) {
        var btn = ButtonWidget.of(label, action)
                .setColors(color, lighten(color), darken(color));
        btn.useStyle(UIStyle.of(UIStyles.minWidth(90), UIStyles.heightOf(18), UIStyles.padding(2)));
        return btn;
    }

    // ==================== Add Operations ====================

    private void addWidgetToA() {
        var widget = createNumberedWidget(widgetCounter++, 0xFF6699FF);
        containerA.addChild(widget);
        createdWidgets.add(widget);
        updateStatus("Added widget #" + (widgetCounter - 1) + " to Container A");
        updateStats();
    }

    private void addWidgetToB() {
        var widget = createNumberedWidget(widgetCounter++, 0xFF66FF66);
        containerB.addChild(widget);
        createdWidgets.add(widget);
        updateStatus("Added widget #" + (widgetCounter - 1) + " to Container B");
        updateStats();
    }

    private LabelWidget createNumberedWidget(int number, int color) {
        var widget = LabelWidget.of("#" + number)
                .setColor(color)
                .setShadow(true);
        widget.useStyle(UIStyle.of(
                UIStyles.padding(4, 8),
                UIStyles.background(new ColorTexture(0x30000000))
        ));
        return widget;
    }

    // ==================== Remove Operations ====================

    private void removeFirstFromA() {
        if (containerA.children().isEmpty()) {
            updateStatus("Container A is empty!");
            return;
        }
        var child = containerA.children().getFirst();
        containerA.removeChild(child);
        createdWidgets.remove(child);
        updateStatus("Removed first widget from Container A");
        updateStats();
    }

    private void removeLastFromB() {
        if (containerB.children().isEmpty()) {
            updateStatus("Container B is empty!");
            return;
        }
        var child = containerB.children().getLast();
        containerB.removeChild(child);
        createdWidgets.remove(child);
        updateStatus("Removed last widget from Container B");
        updateStats();
    }

    private void clearA() {
        int count = containerA.children().size();
        for (var child : containerA.children().toList()) {
            createdWidgets.remove(child);
        }
        containerA.clearChildren();
        updateStatus("Cleared " + count + " widgets from Container A");
        updateStats();
    }

    private void clearB() {
        int count = containerB.children().size();
        for (var child : containerB.children().toList()) {
            createdWidgets.remove(child);
        }
        containerB.clearChildren();
        updateStatus("Cleared " + count + " widgets from Container B");
        updateStats();
    }

    private void clearAll() {
        int count = containerA.children().size() + containerB.children().size();
        containerA.clearChildren();
        containerB.clearChildren();
        createdWidgets.clear();
        updateStatus("Cleared all " + count + " widgets");
        updateStats();
    }

    // ==================== Move Operations ====================

    private void moveFirstAToB() {
        if (containerA.children().isEmpty()) {
            updateStatus("Container A is empty!");
            return;
        }
        var child = containerA.children().getFirst();
        containerA.removeChild(child);
        containerB.addChild(child);
        updateStatus("Moved first widget from A to B");
        updateStats();
    }

    private void moveLastBToA() {
        if (containerB.children().isEmpty()) {
            updateStatus("Container B is empty!");
            return;
        }
        var child = containerB.children().getLast();
        containerB.removeChild(child);
        containerA.addChild(child);
        updateStatus("Moved last widget from B to A");
        updateStats();
    }

    private void swapContainers() {
        // Collect all children
        var childrenA = containerA.children().toList();
        var childrenB = containerB.children().toList();

        // Clear both containers
        containerA.clearChildren();
        containerB.clearChildren();

        // Add to opposite containers
        for (var child : childrenA) {
            containerB.addChild(child);
        }
        for (var child : childrenB) {
            containerA.addChild(child);
        }

        updateStatus("Swapped " + childrenA.size() + " and " + childrenB.size() + " widgets between containers");
        updateStats();
    }

    // ==================== Reuse Operations ====================

    private void toggleReusableWidget() {
        if (reusableWidget.parent() == null) {
            // Not attached, attach to appropriate container
            if (reusableInA) {
                containerA.addChild(reusableWidget);
            } else {
                containerB.addChild(reusableWidget);
            }
            updateStatus("Attached reusable widget to Container " + (reusableInA ? "A" : "B"));
        } else {
            // Already attached, move to other container
            var currentParent = reusableWidget.parent();
            if (currentParent instanceof TestContainerWidget container) {
                container.removeChild(reusableWidget);
                scene().reuse(reusableWidget);
            }

            reusableInA = !reusableInA;
            if (reusableInA) {
                containerA.addChild(reusableWidget);
            } else {
                containerB.addChild(reusableWidget);
            }
            updateStatus("Moved reusable widget to Container " + (reusableInA ? "A" : "B"));
        }
        updateStats();
    }

    private void detachReusableWidget() {
        if (reusableWidget.parent() == null) {
            updateStatus("Reusable widget is already detached");
            return;
        }
        var parent = reusableWidget.parent();
        if (parent instanceof TestContainerWidget container) {
            container.removeChild(reusableWidget);
            scene().reuse(reusableWidget);
            updateStatus("Detached reusable widget");
        }
        updateStats();
    }

    private void attachReusableToA() {
        if (reusableWidget.parent() != null) {
            var parent = reusableWidget.parent();
            if (parent instanceof TestContainerWidget container) {
                container.removeChild(reusableWidget);
                scene().reuse(reusableWidget);
            }
        }
        containerA.addChild(reusableWidget);
        reusableInA = true;
        updateStatus("Attached reusable widget to Container A");
        updateStats();
    }

    private void attachReusableToB() {
        if (reusableWidget.parent() != null) {
            var parent = reusableWidget.parent();
            if (parent instanceof TestContainerWidget container) {
                container.removeChild(reusableWidget);
                scene().reuse(reusableWidget);
            }
        }
        containerB.addChild(reusableWidget);
        reusableInA = false;
        updateStatus("Attached reusable widget to Container B");
        updateStats();
    }

    // ==================== Destroy Operations ====================

    private void destroyContainerA() {
        int count = WidgetTree.countAll(containerA, false);
        WidgetTree.bottomUp(containerA, false, widget -> {
            // Simulate destruction - in real use, this might call dispose() etc.
            System.out.println("Destroying (bottomUp): " + widget);
        });
        containerA.clearChildren();
        updateStatus("Destroyed " + count + " widgets in A using bottomUp traversal");
        updateStats();
    }

    private void destroyContainerB() {
        int count = WidgetTree.countAll(containerB, false);
        WidgetTree.deepestFirst(containerB, false, widget -> {
            // Simulate destruction - in real use, this might call dispose() etc.
            System.out.println("Destroying (deepestFirst): " + widget);
        });
        containerB.clearChildren();
        updateStatus("Destroyed " + count + " widgets in B using deepestFirst traversal");
        updateStats();
    }

    // ==================== UI Helpers ====================

    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    private void updateStats() {
        int totalA = containerA.children().size();
        int totalB = containerB.children().size();
        int total = totalA + totalB;
        boolean hasReusable = reusableWidget.parent() != null;
        statsLabel.setText("A:" + totalA + " B:" + totalB + " Total:" + total + (hasReusable ? " +R" : ""));
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
