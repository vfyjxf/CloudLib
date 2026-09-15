package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionProperty;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;

/**
 * Flexible space that expands to fill available space.
 */
public class SpacerWidget extends Widget {

    //region state

    private float minLength = 0;
    private float flexGrow = 1;

    //endregion

    //region factory

    public static SpacerWidget create() {
        return new SpacerWidget();
    }

    public static SpacerWidget create(float minLength) {
        return new SpacerWidget().setMinLength(minLength);
    }

    public static SpacerWidget fixed(float size) {
        return new SpacerWidget().setMinLength(size).setFlexGrow(0);
    }

    private SpacerWidget() {
        useStyle(UIStyle.of(UIStyles.flexGrow(1)));
    }

    //endregion

    //region configuration

    public float minLength() {
        return minLength;
    }

    public SpacerWidget setMinLength(float minLength) {
        this.minLength = minLength;
        useStyle(UIStyle.of(UIStyles.minWidth(minLength), UIStyles.minHeight(minLength)));
        return this;
    }

    public SpacerWidget setFlexGrow(float grow) {
        this.flexGrow = grow;
        useStyle(UIStyle.of(UIStyles.flexGrow(grow)));
        return this;
    }

    //endregion

    //region rendering

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        // Renders nothing
    }

    //endregion

    //region inspection

    @Override
    public void collectInspectionInfo(InspectionInfoCollector collector) {
        super.collectInspectionInfo(collector);
        collector.addWithDefault("minLength", minLength, 0f, InspectionProperty.categoryLayout);
        collector.addWithDefault("flexGrow", flexGrow, 1f, InspectionProperty.categoryLayout);
    }

    //endregion
}
