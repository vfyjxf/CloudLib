package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionProperty;

/**
 * Stack/overlay container. Children render on top of each other.
 */
public class BoxWidget extends CompositeWidget<Widget> {

    //region factory

    public static BoxWidget create() {
        return new BoxWidget();
    }

    private BoxWidget() {
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
        collector.add("children", children().size(), InspectionProperty.categoryData);
    }

    //endregion
}
