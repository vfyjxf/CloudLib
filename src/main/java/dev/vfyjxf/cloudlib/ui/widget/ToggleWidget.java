package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionProperty;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Toggle/switch for boolean states.
 */
public class ToggleWidget extends Widget {

    //region state

    private boolean toggled = false;
    private @Nullable Consumer<Boolean> onToggle;

    //endregion

    //region textures

    private VisualTexture offTexture = new ColorTexture(0xFF666666);
    private VisualTexture onTexture = new ColorTexture(0xFF00AA00);
    private @Nullable VisualTexture hoverTexture = null;

    //endregion

    //region factory

    public static ToggleWidget create() {
        return new ToggleWidget();
    }

    public static ToggleWidget create(boolean initial) {
        return new ToggleWidget().setToggled(initial);
    }

    private ToggleWidget() {
        onMouseClick((input, clickCount, context) -> {
            toggle();
            return EventDispatch.consumed;
        });
    }

    //endregion

    //region configuration

    public boolean toggled() {
        return toggled;
    }

    public ToggleWidget setToggled(boolean toggled) {
        this.toggled = toggled;
        return this;
    }

    public ToggleWidget toggle() {
        this.toggled = !this.toggled;
        if (onToggle != null) {
            onToggle.accept(toggled);
        }
        return this;
    }

    public ToggleWidget onToggle(@Nullable Consumer<Boolean> callback) {
        this.onToggle = callback;
        return this;
    }

    public ToggleWidget setTextures(VisualTexture offTexture, VisualTexture onTexture) {
        this.offTexture = offTexture;
        this.onTexture = onTexture;
        return this;
    }

    public ToggleWidget setColors(int offColor, int onColor) {
        this.offTexture = new ColorTexture(offColor);
        this.onTexture = new ColorTexture(onColor);
        return this;
    }

    public ToggleWidget setHoverTexture(@Nullable VisualTexture texture) {
        this.hoverTexture = texture;
        return this;
    }

    //endregion

    //region rendering

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        super.renderInternal(canvas, mouseX, mouseY, partialTicks);

        VisualTexture texture;
        if (hovered() && hoverTexture != null) {
            texture = hoverTexture;
        } else {
            texture = toggled ? onTexture : offTexture;
        }

        canvas.texture(texture, 0, 0, width(), height());
    }

    //endregion

    //region inspection

    @Override
    public void collectInspectionInfo(InspectionInfoCollector collector) {
        super.collectInspectionInfo(collector);
        collector.addWithDefault("toggled", toggled, false, InspectionProperty.categoryState);
    }

    //endregion
}
