package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionProperty;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import dev.vfyjxf.taffy.geometry.FloatSize;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Button with label and icon support.
 */
public class ButtonWidget extends Widget {

    //region state

    private Component label;
    private @Nullable Runnable onClick;
    private boolean enabled = true;
    private boolean pressed = false;

    //endregion

    //region colors

    private int textColor = 0xFFFFFF;
    private int disabledTextColor = 0xA0A0A0;

    //endregion

    //region textures

    private VisualTexture normalTexture = new ColorTexture(0xFF555555);
    private VisualTexture hoverTexture = new ColorTexture(0xFF777777);
    private VisualTexture pressedTexture = new ColorTexture(0xFF333333);
    private VisualTexture disabledTexture = new ColorTexture(0xFF444444);
    private @Nullable VisualTexture iconTexture = null;

    //endregion

    //region factory

    public static ButtonWidget of(String label) {
        return new ButtonWidget(Component.literal(label));
    }

    public static ButtonWidget of(Component label) {
        return new ButtonWidget(label);
    }

    public static ButtonWidget of(String label, Runnable onClick) {
        return new ButtonWidget(Component.literal(label)).onClick(onClick);
    }

    private ButtonWidget(Component label) {
        this.label = label;

        onMouseClick((input, clickCount, context) -> {
            if (enabled && onClick != null) {
                onClick.run();
                return EventDispatch.consumed;
            }
            return EventDispatch.pass;
        });

        onMouseEnter((mouseX, mouseY, context) -> {
            if (enabled) setHovered(true);
        });

        onMouseLeave((mouseX, mouseY, context) -> {
            setHovered(false);
            pressed = false;
        });

        onMount((scene, context, handle) -> {
            scene.layoutTree().setMeasureFunc(nodeId(), (style, availableSpace) -> {
                var font = context.font();
                return new FloatSize(font.width(this.label) + 8, font.lineHeight + 4);
            });
        });
    }

    //endregion

    //region configuration

    public Component label() {
        return label;
    }

    public ButtonWidget setLabel(Component label) {
        this.label = label;
        return this;
    }

    public ButtonWidget setLabel(String label) {
        this.label = Component.literal(label);
        return this;
    }

    public ButtonWidget onClick(@Nullable Runnable onClick) {
        this.onClick = onClick;
        return this;
    }

    public boolean enabled() {
        return enabled;
    }

    public ButtonWidget setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public ButtonWidget setTextures(VisualTexture normal, VisualTexture hover, VisualTexture pressed) {
        this.normalTexture = normal;
        this.hoverTexture = hover;
        this.pressedTexture = pressed;
        return this;
    }

    public ButtonWidget setColors(int normal, int hover, int pressed) {
        this.normalTexture = new ColorTexture(normal);
        this.hoverTexture = new ColorTexture(hover);
        this.pressedTexture = new ColorTexture(pressed);
        return this;
    }

    public ButtonWidget setDisabledTexture(VisualTexture texture) {
        this.disabledTexture = texture;
        return this;
    }

    public ButtonWidget setIconTexture(@Nullable VisualTexture texture) {
        this.iconTexture = texture;
        return this;
    }

    public ButtonWidget setTextColor(int color) {
        this.textColor = color;
        return this;
    }

    public ButtonWidget setDisabledTextColor(int color) {
        this.disabledTextColor = color;
        return this;
    }

    //endregion

    //region rendering

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        VisualTexture texture;
        if (!enabled) {
            texture = disabledTexture;
        } else if (pressed) {
            texture = pressedTexture;
        } else if (hovered()) {
            texture = hoverTexture;
        } else {
            texture = normalTexture;
        }

        canvas.texture(texture, 0, 0, width(), height());

        // Icon
        int iconOffset = 0;
        if (iconTexture != null) {
            int iconSize = Math.min(height() - 4, 16);
            int iconY = (height() - iconSize) / 2;
            canvas.texture(iconTexture, 4, iconY, iconSize, iconSize);
            iconOffset = iconSize + 4;
        }

        // Label
        var font = context().font();
        int textWidth = font.width(label);
        int textX = iconOffset + (width() - iconOffset - textWidth) / 2;
        int textY = (height() - font.lineHeight) / 2;
        int color = enabled ? textColor : disabledTextColor;

        canvas.text(label, textX, textY, color, true);
    }

    //endregion

    //region inspection

    @Override
    public void collectInspectionInfo(InspectionInfoCollector collector) {
        super.collectInspectionInfo(collector);
        String text = label.getString();
        if (text.length() > 20) {
            text = text.substring(0, 17) + "...";
        }
        collector.add("label", text, InspectionProperty.CATEGORY_DATA);
        collector.addWithDefault("enabled", enabled, true, InspectionProperty.CATEGORY_STATE);
        collector.addWithDefault("pressed", pressed, false, InspectionProperty.CATEGORY_STATE);
        if (iconTexture != null) {
            collector.add("hasIcon", true, InspectionProperty.CATEGORY_VISUAL);
        }
    }

    //endregion
}
