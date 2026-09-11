package dev.vfyjxf.cloudlib.test.inworld;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.ui.inworld.InworldTheme;
import dev.vfyjxf.taffy.geometry.FloatSize;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * A tiny hacker-mode action chip: outline-only rect + short label.
 * Used by the in-world demo panels instead of the chunky vanilla-style
 * {@link dev.vfyjxf.cloudlib.ui.widget.ButtonWidget}.
 */
public class ChipWidget extends Widget {

    private Component label;
    private Runnable onClick;
    private boolean pressed;

    public static ChipWidget of(String label, Runnable onClick) {
        return new ChipWidget(Component.literal(label), onClick);
    }

    private ChipWidget(Component label, Runnable onClick) {
        this.label = label;
        this.onClick = onClick;
        setFocusable(true);
        onMouseClick((input, clickCount, context) -> {
            if (this.onClick != null) {
                this.onClick.run();
                return EventDispatch.consumed;
            }
            return EventDispatch.pass;
        });
        onMouseLeave((x, y, context) -> pressed = false);
        onMouseClicked((input, context) -> {
            pressed = true;
            return EventDispatch.pass;
        });
        onMouseReleased((input, context) -> {
            pressed = false;
            return EventDispatch.pass;
        });
        //keyboard/controller activation: enter or space presses the chip
        onKeyPressed((input, context) -> {
            if (input.isKey(GLFW.GLFW_KEY_ENTER) || input.isKey(GLFW.GLFW_KEY_KP_ENTER)
                    || input.isKey(GLFW.GLFW_KEY_SPACE)) {
                if (this.onClick != null) {
                    this.onClick.run();
                    return EventDispatch.consumed;
                }
            }
            return EventDispatch.pass;
        });
        onMount((scene, context, handle) ->
                scene.layoutTree().setMeasureFunc(nodeId(), (style, space) ->
                        new FloatSize(context.font().width(this.label) + 8, context.font().lineHeight + 4)));
    }

    public ChipWidget setLabel(String label) {
        this.label = Component.literal(label);
        return this;
    }

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        int w = width();
        int h = height();
        boolean hot = hovered() || focused();
        int border = hot ? InworldTheme.ACCENT : InworldTheme.BORDER;
        if (pressed) {
            canvas.fill(0, 0, w, h, InworldTheme.ACCENT_DIM);
        } else if (hot) {
            canvas.fill(0, 0, w, h, 0x3335D6D0);
        }
        canvas.strokeRect(0, 0, w, h, border);
        var font = context().font();
        canvas.text(label, (w - font.width(label)) / 2, (h - font.lineHeight) / 2 + 1,
                hot ? InworldTheme.TEXT : InworldTheme.TEXT_DIM);
    }
}
