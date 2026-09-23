package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionProperty;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import dev.vfyjxf.cloudlib.ui.Textures;
import dev.vfyjxf.taffy.geometry.FloatSize;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Button with label and icon support.
 */
public class ButtonWidget extends Widget {

    // region state

    private Component label;
    private @Nullable Runnable onClick;
    private boolean enabled = true;
    private boolean pressed = false;

    // endregion

    // region colors

    private int textColor = 0xFF3F3F3F;
    private int disabledTextColor = 0xFF8B8B8B;
    private boolean textShadow = false;

    // endregion

    // region textures

    private VisualTexture normalTexture = Textures.flat;
    private VisualTexture hoverTexture = Textures.outlinedFlat;
    private VisualTexture pressedTexture = Textures.inset;
    private VisualTexture disabledTexture = Textures.dark;
    private @Nullable VisualTexture iconTexture = null;

    // endregion

    // region factory

    public static ButtonWidget of(String label) {
        return new ButtonWidget(Component.literal(label));
    }

    public static ButtonWidget of(Component label) {
        return new ButtonWidget(label);
    }

    public static ButtonWidget of(Component label, Runnable onClick) {
        return new ButtonWidget(label).onClick(onClick);
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

        onMouseClicked((input, context) -> {
            if (enabled) {
                setPressed(true);
            }
            return EventDispatch.pass;
        });

        onMouseReleased((input, context) -> {
            setPressed(false);
            return EventDispatch.pass;
        });

        onMouseLeave((mouseX, mouseY, context) -> {
            setPressed(false);
        });

        onMount((scene, context, handle) -> {
            scene.layoutTree().setMeasureFunc(nodeId(), (style, availableSpace) -> {
                var font = context.font();
                return new FloatSize(font.width(this.label) + 8, font.lineHeight + 4);
            });
        });
    }

    // endregion

    // region configuration

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
        if (this.enabled != enabled) {
            this.enabled = enabled;
            markStyleDirty();
        }
        return this;
    }

    /**
     * The button's own {@link #enabled()} gate is part of the selector surface:
     * a disabled button reports {@code :disabled} (and drops {@code :enabled})
     * instead of claiming to be enabled off the framework's {@code :active}
     * alone.
     */
    @Override
    public Set<String> styleStates() {
        Set<String> states = super.styleStates();
        if (!enabled) {
            states.remove("enabled");
            states.add("disabled");
        }
        return states;
    }

    private void setPressed(boolean pressed) {
        if (this.pressed == pressed) {
            return;
        }
        this.pressed = pressed;
        if (pressed) {
            addStyleState("pressed");
        } else {
            removeStyleState("pressed");
        }
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

    public ButtonWidget setTextShadow(boolean shadow) {
        this.textShadow = shadow;
        return this;
    }

    // endregion

    // region rendering

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        // theme wins: a non-empty resolved background already encodes the
        // current state (button:hover/:pressed/:disabled are cascade-selected)
        VisualTexture texture = style().visualContext().background();
        if (texture == null || texture.isEmpty()) {
            if (!enabled) {
                texture = disabledTexture;
            } else if (pressed) {
                texture = pressedTexture;
            } else if (hovered()) {
                texture = hoverTexture;
            } else {
                texture = normalTexture;
            }
        }

        canvas.texture(texture, 0, 0, width(), height());

        // Icon
        VisualTexture icon = iconTexture != null ? iconTexture : style().visualContext().icon();
        int iconOffset = 0;
        if (icon != null && !icon.isEmpty()) {
            int iconSize = Math.min(height() - 4, 16);
            int iconY = (height() - iconSize) / 2;
            canvas.texture(icon, 4, iconY, iconSize, iconSize);
            iconOffset = iconSize + 4;
        }

        // Label
        var font = context().font();
        int textWidth = font.width(label);
        int textX = iconOffset + (width() - iconOffset - textWidth) / 2;
        int textY = (height() - font.lineHeight) / 2;
        Integer themedColor = style().visualContext().textColor();
        int color = themedColor != null ? themedColor : enabled ? textColor : disabledTextColor;

        canvas.text(label, textX, textY, color, textShadow);
    }

    // endregion

    // region inspection

    @Override
    public void collectInspectionInfo(InspectionInfoCollector collector) {
        super.collectInspectionInfo(collector);
        String text = label.getString();
        if (text.length() > 20) {
            text = text.substring(0, 17) + "...";
        }
        collector.add("label", text, InspectionProperty.categoryData);
        collector.addWithDefault("enabled", enabled, true, InspectionProperty.categoryState);
        collector.addWithDefault("pressed", pressed, false, InspectionProperty.categoryState);
        if (iconTexture != null) {
            collector.add("hasIcon", true, InspectionProperty.categoryVisual);
        }
    }

    // endregion
}
