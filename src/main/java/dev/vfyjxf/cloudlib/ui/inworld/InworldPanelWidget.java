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

import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.minWidth;
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
    /** open-animation scale (1 = fully open); driven by the manager each frame */
    float openScale = 1f;
    /** distance falloff scale for non-interactive tags; driven by the manager */
    float distScale = 1f;
    /** merge badge drawn at the top-right corner ("+3") — null = none */
    @Nullable String overflow;
    /** dock-column overflow: render only the chrome (title/hints), content hidden */
    boolean folded;

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
        //tab / shift+tab cycles focus through the panel's focusable content
        onKeyPressed((input, context) -> {
            if (input.isKey(GLFW.GLFW_KEY_TAB)) {
                focusCycle(!input.isShiftDown());
                return EventDispatch.consumed;
            }
            return EventDispatch.pass;
        });
        addWidget(content);
        applyChromeMinWidth();
    }

    /**
     * The panel must be at least as wide as its chrome — title text and the
     * hint row are drawn over the frame, so taffy (which only sees the content)
     * would otherwise under-measure and clip them.
     */
    private void applyChromeMinWidth() {
        var font = Minecraft.getInstance().font;
        int need = 0;
        if (title != null) {
            need = Math.max(need, font.width(title) + 12 + InworldTheme.PADDING);
        }
        if (!hints.isEmpty()) {
            int row = InworldTheme.PADDING * 2;
            for (String hint : hints) {
                int sep = hint.indexOf(':');
                String key = sep > 0 ? hint.substring(0, sep) : hint;
                String label = sep > 0 ? hint.substring(sep + 1) : "";
                row += font.width(key) + 4;
                if (!label.isEmpty()) row += font.width(label) + 3;
                row += 4;
            }
            need = Math.max(need, row - 4);
        }
        if (need > 0) useStyle(minWidth(need));
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

    /**
     * Panel position is driven by the manager ({@link #setScreenPos}), not by
     * taffy — the absolute-positioned node's taffy layout is always (0,0) and
     * unchanged layouts never fire {@code onLayout}, so apply the pending
     * position directly after the super pass.
     */
    @Override
    public void applyLayout() {
        super.applyLayout();
        viewport().setLayout(screenX, screenY);
    }

    void setTitle(@Nullable Component title) {
        this.title = title;
        applyChromeMinWidth();
    }

    void setHints(List<String> hints) {
        this.hints = hints;
        applyChromeMinWidth();
    }

    void setFrameState(boolean focused, boolean pointed) {
        this.focused = focused;
        this.pointed = pointed;
    }

    /**
     * Fold/unfold the panel: folded panels hide their content so only the
     * chrome (title strip + hint chips) remains — the dock column's way of
     * staying on screen when it runs out of vertical room.
     */
    void setFolded(boolean folded) {
        if (this.folded == folded) return;
        this.folded = folded;
        content.setVisible(!folded);
        if (lifecycle().mounted()) {
            scene().layoutTree().markDirty(nodeId());
        }
    }

    //endregion

    //region render

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        int w = width();
        int h = height();

        canvas.fill(0, 0, w, h, focused ? InworldTheme.BG_FOCUSED : InworldTheme.BG);
        //idle panels carry no frame — the leader line is the only chrome; the
        //outline appears only when the panel is focused/pointed
        if (focused || pointed) {
            canvas.strokeRect(0, 0, w, h, InworldTheme.BORDER_FOCUSED);
        }

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

        //merge badge: "+N" chip hanging off the top-right corner
        if (overflow != null) {
            var font = Minecraft.getInstance().font;
            int tw = font.width(overflow) + 5;
            canvas.fill(w - tw - 2, -4, tw, 9, InworldTheme.BG_FOCUSED);
            canvas.strokeRect(w - tw - 2, -4, tw, 9, InworldTheme.ACCENT);
            canvas.text(overflow, w - tw + 1, -3, InworldTheme.ACCENT);
        }
    }

    @Override
    public void render(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        float scale = openScale * distScale;
        boolean animating = scale < 0.999f;
        if (animating) {
            //grow out of the bottom-center — toward the leader line/anchor
            canvas.pushTransform();
            canvas.translate(width() * 0.5f, height());
            canvas.scale(scale);
            canvas.translate(-width() * 0.5f, -height());
        }
        super.render(canvas, mouseX, mouseY, partialTicks);
        if (focused || pointed) {
            drawBrackets(canvas);
        }
        if (animating) {
            canvas.popTransform();
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
