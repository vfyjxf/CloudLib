package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionProperty;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;

/**
 * Visual divider line.
 */
public class DividerWidget extends Widget {

    //region types

    public enum Orientation {
        HORIZONTAL, VERTICAL
    }

    //endregion

    //region state

    private Orientation orientation = Orientation.HORIZONTAL;
    private int thickness = 1;
    private VisualTexture texture = new ColorTexture(0xFFAAAAAA);

    //endregion

    //region factory

    public static DividerWidget horizontal() {
        return new DividerWidget().setOrientation(Orientation.HORIZONTAL);
    }

    public static DividerWidget vertical() {
        return new DividerWidget().setOrientation(Orientation.VERTICAL);
    }

    public static DividerWidget create() {
        return new DividerWidget();
    }

    private DividerWidget() {
    }

    //endregion

    //region configuration

    public Orientation orientation() {
        return orientation;
    }

    public DividerWidget setOrientation(Orientation orientation) {
        this.orientation = orientation;
        return this;
    }

    public int thickness() {
        return thickness;
    }

    public DividerWidget setThickness(int thickness) {
        this.thickness = thickness;
        return this;
    }

    public DividerWidget setTexture(VisualTexture texture) {
        this.texture = texture;
        return this;
    }

    public DividerWidget setColor(int color) {
        this.texture = new ColorTexture(color);
        return this;
    }

    //endregion

    //region rendering

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        int w, h;
        if (orientation == Orientation.HORIZONTAL) {
            w = width();
            h = thickness;
        } else {
            w = thickness;
            h = height();
        }

        int x = (width() - w) / 2;
        int y = (height() - h) / 2;

        canvas.texture(texture, x, y, w, h);
    }

    //endregion

    //region inspection

    @Override
    public void collectInspectionInfo(InspectionInfoCollector collector) {
        super.collectInspectionInfo(collector);
        collector.addWithDefault("orientation", orientation.name(), Orientation.HORIZONTAL.name(), InspectionProperty.categoryVisual);
        collector.addWithDefault("thickness", thickness, 1, InspectionProperty.categoryVisual);
    }

    //endregion
}
