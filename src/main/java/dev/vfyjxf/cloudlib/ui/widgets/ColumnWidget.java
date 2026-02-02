package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionProperty;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;

/**
 * Vertical flexbox container.
 */
public class ColumnWidget extends CompositeWidget<Widget> {

    //region state

    private int spacing = 0;

    //endregion

    //region factory

    public static ColumnWidget create() {
        return new ColumnWidget();
    }

    public static ColumnWidget create(int spacing) {
        return new ColumnWidget().setSpacing(spacing);
    }

    private ColumnWidget() {
        applyStyle(UIStyle.of(UIStyles.flexColumn()));
    }

    //endregion

    //region configuration

    public int spacing() {
        return spacing;
    }

    public ColumnWidget setSpacing(int spacing) {
        this.spacing = spacing;
        applyStyle(UIStyle.of(UIStyles.rowGap(spacing)));
        return this;
    }

    //endregion

    //region children

    public <T extends Widget> T addChild(T widget) {
        return addWidget(widget);
    }

    //endregion

    //region rendering

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        super.renderInternal(canvas, mouseX, mouseY, partialTicks);
    }

    //endregion

    //region inspection

    @Override
    public void collectInspectionInfo(InspectionInfoCollector collector) {
        super.collectInspectionInfo(collector);
        collector.addWithDefault("spacing", spacing, 0, InspectionProperty.CATEGORY_LAYOUT);
        collector.add("children", children().size(), InspectionProperty.CATEGORY_DATA);
    }

    //endregion
}
