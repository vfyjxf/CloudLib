package dev.vfyjxf.cloudlib.ui.inworld;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.ui.base.FocusScopeNode;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetTree.TraversalControl;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetTree;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.padding;
import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.positionAbsolute;

/**
 * The chrome widget wrapping an in-world panel's content: translucent dark
 * background, thin border, an optional title strip, hint chips, and the
 * corner-bracket focus frame.
 * <p>
 * The widget is absolutely positioned inside the in-world scene; the owning
 * {@link PanelRuntime} pushes its resolved screen position every frame via
 * {@link #setScreenPos(int, int)}.
 */
public final class InworldPanelWidget extends WidgetGroup<Widget> {

    final PanelRuntime runtime;
    private final Widget content;

    private @Nullable Component title;
    private List<String> hints;

    //per-frame chrome state, driven by the manager
    boolean focused;
    boolean pointed;
    int screenX;
    int screenY;

    public InworldPanelWidget(PanelRuntime runtime, InworldPanelSpec spec, Widget content) {
        this.runtime = runtime;
        this.content = content;
        this.title = spec.title();
        this.hints = spec.hints();

        setFocusNode(new FocusScopeNode());
        useStyle(
                positionAbsolute(),
                padding(
                        spec.title() != null ? InworldTheme.TITLE_HEIGHT + 2 : InworldTheme.PADDING,
                        InworldTheme.PADDING,
                        spec.hints().isEmpty() ? InworldTheme.PADDING : InworldTheme.HINT_HEIGHT + 2,
                        InworldTheme.PADDING
                )
        );
        onLayout((widget, scope) -> {
            scope.useTaffy();
            scope.setPosition(screenX, screenY);
        });
        //tab / shift+tab cycles focus through the panel's focusable content
        onKeyPressed((input, context) -> {
            if (input.isKey(GLFW.GLFW_KEY_TAB)) {
                focusCycle(!input.isShiftDown());
                return EventDispatch.consumed;
            }
            return EventDispatch.pass;
        });
        addWidget(content);
    }

    /**
     * Moves the scene's focus to the next/previous focusable widget inside the
     * panel content — keyboard/controller-style traversal independent of the
     * pointer.
     */
    private void focusCycle(boolean forward) {
        if (!lifecycle().mounted()) return;
        List<Widget> focusables = new ArrayList<>();
        WidgetTree.walkPreOrder(content, true, -1, (widget, depth) -> {
            if (widget != content && widget.focusable() && widget.visible() && widget.active()) {
                focusables.add(widget);
            }
            return TraversalControl.proceed;
        });
        if (focusables.isEmpty()) return;
        Widget current = scene().focusingWidget();
        int idx = focusables.indexOf(current);
        int next = idx < 0
                ? (forward ? 0 : focusables.size() - 1)
                : (idx + (forward ? 1 : -1) + focusables.size()) % focusables.size();
        scene().requestFocus(focusables.get(next));
    }

    public Widget content() {
        return content;
    }

    //region runtime drive

    /**
     * Sets the scene-space position applied by the next layout pass.
     * Marks the node dirty when the position actually moved.
     */
    void setScreenPos(int x, int y) {
        if (screenX != x || screenY != y) {
            screenX = x;
            screenY = y;
            if (lifecycle().mounted()) {
                scene().layoutTree().markDirty(nodeId());
            }
        }
    }

    void setTitle(@Nullable Component title) {
        this.title = title;
    }

    void setHints(List<String> hints) {
        this.hints = hints;
    }

    void setFrameState(boolean focused, boolean pointed) {
        this.focused = focused;
        this.pointed = pointed;
    }

    //endregion

    //region render

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        int w = width();
        int h = height();

        canvas.fill(0, 0, w, h, focused ? InworldTheme.BG_FOCUSED : InworldTheme.BG);
        canvas.strokeRect(0, 0, w, h, focused || pointed ? InworldTheme.BORDER_FOCUSED : InworldTheme.BORDER);

        if (title != null) {
            //header: accent chip + text + hairline rule
            canvas.fill(3, 5, 3, 3, InworldTheme.ACCENT);
            canvas.text(title, 9, 3, InworldTheme.TEXT);
            canvas.fill(0, InworldTheme.TITLE_HEIGHT + 1, w, 1, InworldTheme.TITLE_RULE);
        }

        if (!hints.isEmpty()) {
            var font = Minecraft.getInstance().font;
            int hx = w - InworldTheme.PADDING;
            int hy = h - InworldTheme.HINT_HEIGHT + 1;
            for (int i = hints.size() - 1; i >= 0; i--) {
                String hint = hints.get(i);
                int sep = hint.indexOf(':');
                String key = sep > 0 ? hint.substring(0, sep) : hint;
                String label = sep > 0 ? hint.substring(sep + 1) : "";
                int keyW = font.width(key) + 4;
                int labelW = label.isEmpty() ? 0 : font.width(label) + 3;
                hx -= keyW + labelW;
                canvas.strokeRect(hx, hy - 1, keyW, 9, InworldTheme.ACCENT_DIM);
                canvas.text(key, hx + 2, hy, InworldTheme.HINT_KEY);
                if (labelW > 0) {
                    canvas.text(label, hx + keyW + 3, hy, InworldTheme.TEXT_DIM);
                }
                hx -= 4;
            }
        }
    }

    @Override
    public void render(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        super.render(canvas, mouseX, mouseY, partialTicks);
        if (focused || pointed) {
            drawBrackets(canvas);
        }
    }

    /**
     * The focus frame: four corner brackets slightly outside the panel bounds.
     */
    private void drawBrackets(SceneCanvas canvas) {
        int w = width();
        int h = height();
        int b = InworldTheme.BRACKET;
        int color = focused ? InworldTheme.ACCENT : InworldTheme.ACCENT_DIM;
        //top-left
        canvas.fill(-2, -2, b, 1, color);
        canvas.fill(-2, -2, 1, b, color);
        //top-right
        canvas.fill(w - b + 2, -2, b, 1, color);
        canvas.fill(w + 1, -2, 1, b, color);
        //bottom-left
        canvas.fill(-2, h + 1, b, 1, color);
        canvas.fill(-2, h - b + 2, 1, b, color);
        //bottom-right
        canvas.fill(w - b + 2, h + 1, b, 1, color);
        canvas.fill(w + 1, h - b + 2, 1, b, color);
    }

    //endregion

    //region misc

    @Override
    public String inspectionTypeName() {
        return "InworldPanel";
    }

    @Override
    public String toString() {
        return "InworldPanel[" + runtime.key() + "]";
    }

    //endregion
}
