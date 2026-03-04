package dev.vfyjxf.cloudlib.test.ui;

import dev.vfyjxf.cloudlib.api.ui.base.BasicScreen;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.ui.widget.ColumnWidget;
import dev.vfyjxf.cloudlib.ui.widget.LabelWidget;
import dev.vfyjxf.cloudlib.ui.widget.RowWidget;
import dev.vfyjxf.taffy.style.TaffyDimension;

import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.*;

/**
 * Test screen for verifying Taffy layout with maxSize + aspectRatio constraints.
 * <p>
 * Two test cases side by side:
 * <p>
 * Case 1 (Bug case — width=100%, maxSize=100x100, aspectRatio=1):
 * <ul>
 *   <li>Parent (green): auto height, should be 100 (child's constrained height)</li>
 *   <li>Child (red): 100x100 (maxSize constrains the width=100% down to 100, aspect ratio keeps it square)</li>
 *   <li>Bug was: parent height = 500 (used child's unconstrained width for height), should be 100</li>
 * </ul>
 * <p>
 * Case 2 (Baseline — width=100px, aspectRatio=1, no maxSize):
 * <ul>
 *   <li>Parent (green): auto height, should be 100</li>
 *   <li>Child (red): 100x100 (fixed width, aspect ratio determines height)</li>
 * </ul>
 */
@TestScreen
public class TestNestedLayoutScreen extends BasicScreen {

    public TestNestedLayoutScreen() {
        buildUI();
    }

    private void buildUI() {
        var root = RowWidget.create();
        root.setSpacing(10);
        root.useStyle(UIStyle.of(padding(10)));

        // === Case 1: width=100.pct, maxSize(100, 100), aspectRatio(1) ===
        var case1 = createCase1();
        root.addWidget(case1);

        // === Case 2: width=100.px, aspectRatio(1), no maxSize ===
        var case2 = createCase2();
        root.addWidget(case2);

        mainGroup().addWidget(root);
    }

    /**
     * Case 1: Bug reproduction — width=100%, maxSize(100,100), aspectRatio(1)
     * <p>
     * Expected: child=100x100, parent height=100 (NOT 200)
     */
    private Widget createCase1() {
        var wrapper = ColumnWidget.create();
        wrapper.setSpacing(4);

        var label = LabelWidget.of("Case1: w=100%,max=100x100,ar=1")
                               .setColor(0xFFFFFFFF).setShadow(true);
        label.useStyle(UIStyle.of(sizeOf(200, 12)));
        wrapper.addWidget(label);

        // Grandparent: Column, fixed 200x200
        var grandparent = ColumnWidget.create();
        grandparent.useStyle(UIStyle.of(
                sizeOf(200, 200),
                background(new ColorTexture(0xFF333333))
        ));

        // Parent: green, auto height
        var parent = new WidgetGroup<>();
        parent.useStyle(UIStyle.of(
                background(new ColorTexture(0xFF00FF00))
        ));

        // Child: red, width=100%, maxSize(100,100), aspectRatio=1
        var child = new Widget();
        child.useStyle(UIStyle.of(
                widthOf(TaffyDimension.percent(1f)),
                maxWidth(100),
                maxHeight(100),
                aspectRatio(1.0f),
                background(new ColorTexture(0xFFFF0000))
        ));

        parent.addWidget(child);
        grandparent.addWidget(parent);
        wrapper.addWidget(grandparent);

        return wrapper;
    }

    /**
     * Case 2: Baseline — width=100px, aspectRatio(1), no maxSize
     * <p>
     * Expected: child=100x100, parent height=100
     */
    private Widget createCase2() {
        var wrapper = ColumnWidget.create();
        wrapper.setSpacing(4);

        var label = LabelWidget.of("Case2: w=100px,ar=1 (no max)")
                               .setColor(0xFFFFFFFF).setShadow(true);
        label.useStyle(UIStyle.of(sizeOf(200, 12)));
        wrapper.addWidget(label);

        // Grandparent: Column, fixed 200x200
        var grandparent = ColumnWidget.create();
        grandparent.useStyle(UIStyle.of(
                sizeOf(200, 200),
                background(new ColorTexture(0xFF333333))
        ));

        // Parent: green, auto height
        var parent = new WidgetGroup<>();
        parent.useStyle(UIStyle.of(
                background(new ColorTexture(0xFF00FF00))
        ));

        // Child: red, width=100px, aspectRatio=1
        var child = new Widget();
        child.useStyle(UIStyle.of(
                widthOf(100),
                aspectRatio(1.0f),
                background(new ColorTexture(0xFFFF0000))
        ));

        parent.addWidget(child);
        grandparent.addWidget(parent);
        wrapper.addWidget(grandparent);

        return wrapper;
    }
}
