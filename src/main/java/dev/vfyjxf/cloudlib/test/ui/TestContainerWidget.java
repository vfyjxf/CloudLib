package dev.vfyjxf.cloudlib.test.ui;

import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;

/**
 * A test-only container widget that exposes add/remove/clear operations.
 * <p>
 * This widget is for testing widget manipulation scenarios.
 * In production code, use proper widget containers with controlled APIs.
 */
public class TestContainerWidget extends CompositeWidget<Widget> {

    private int spacing = 0;

    public static TestContainerWidget create() {
        return new TestContainerWidget();
    }

    public static TestContainerWidget create(int spacing) {
        return new TestContainerWidget().setSpacing(spacing);
    }

    private TestContainerWidget() {
        applyStyle(UIStyle.of(
            UIStyles.flexColumn()
        ));
    }

    public int spacing() {
        return spacing;
    }

    public TestContainerWidget setSpacing(int spacing) {
        this.spacing = spacing;
        applyStyle(UIStyle.of(UIStyles.rowGap(spacing)));
        return this;
    }

    // ==================== Public API for Testing ====================

    public <T extends Widget> T addChild(T widget) {
        return addWidget(widget);
    }

    /**
     * Removes a widget from this container.
     */
    public boolean removeChild(Widget widget) {
        return super.remove(widget);
    }

    /**
     * Removes all children from this container.
     */
    public void clearChildren() {
        super.clear();
    }
}
