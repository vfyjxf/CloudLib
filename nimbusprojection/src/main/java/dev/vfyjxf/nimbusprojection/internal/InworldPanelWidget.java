package dev.vfyjxf.nimbusprojection.internal;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.ui.base.FocusScopeNode;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetTree;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetTree.TraversalControl;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.style.StyleContext;
import dev.vfyjxf.cloudlib.api.ui.style.Styles;
import dev.vfyjxf.cloudlib.api.ui.style.VisualContext;
import dev.vfyjxf.cloudlib.api.ui.texture.BorderTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import dev.vfyjxf.nimbusprojection.internal.NimbusPalette;
import dev.vfyjxf.nimbusprojection.api.Nimbus;
import dev.vfyjxf.nimbusprojection.api.panel.PanelKeySink;
import dev.vfyjxf.nimbusprojection.api.panel.PanelSpec;
import dev.vfyjxf.nimbusprojection.api.sync.PresenceInfo;
import dev.vfyjxf.nimbusprojection.api.sync.PresenceKind;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.display;
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

    // per-frame chrome state, driven by the manager
    boolean focused;
    boolean pointed;
    int screenX;
    int screenY;
    /** open-animation scale (1 = fully open); driven by the manager each frame */
    float openScale = 1f;
    /** distance falloff scale for non-interactive tags; driven by the manager */
    float distScale = 1f;
    /** merge badge drawn at the top-right corner ("+3") — null = none */
    @Nullable
    String overflow;
    /** dock-column overflow: render only the chrome (title/hints), content hidden */
    boolean folded;

    public InworldPanelWidget(PanelRuntime runtime, PanelSpec spec, Widget content) {
        this.runtime = runtime;
        this.content = content;
        this.title = spec.title();
        this.hints = spec.hints();

        setFocusNode(new FocusScopeNode());
        useStyle(
                positionAbsolute(),
                padding(
                        spec.title() != null ? NimbusPalette.titleHeight + 2 : NimbusPalette.padding,
                        NimbusPalette.padding,
                        spec.hints().isEmpty() ? NimbusPalette.padding : NimbusPalette.hintHeight + 2,
                        NimbusPalette.padding));
        // tab / shift+tab cycles focus through the panel's focusable content;
        // unconsumed keys fall through to the content's PanelKeySink — the
        // chrome owns scene focus, so content sees keys only via this handoff
        onKeyPressed((input, context) -> {
            if (input.isKey(GLFW.GLFW_KEY_TAB)) {
                focusCycle(!input.isShiftDown());
                return EventDispatch.consumed;
            }
            if (content instanceof PanelKeySink sink) {
                return sink.keyPressed(input);
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
            need = Math.max(need, font.width(title) + 12 + NimbusPalette.padding);
        }
        if (!hints.isEmpty()) {
            int row = NimbusPalette.padding * 2;
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

    // region runtime drive

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
        // display:none collapses the content out of layout as well — an
        // invisible-but-laid-out subtree would leave a full-height ghost that
        // swallows clicks meant for the panel stacked below this dock slot
        content.useStyle(display(folded ? TaffyDisplay.NONE : TaffyDisplay.FLEX));
        if (lifecycle().mounted()) {
            scene().layoutTree().markDirty(nodeId());
        }
    }

    // endregion

    // region render

    /**
     * Visible chrome height while folded — the layout bounds stay full-height
     * (folding must not remeasure or the dock solver would flap between
     * folded/unfolded every frame), so the strip is drawn explicitly and the
     * remaining bounds stay visually transparent.
     */
    private int foldHeightPx() {
        int padTop = title != null ? NimbusPalette.titleHeight + 2 : NimbusPalette.padding;
        int padBottom = hints.isEmpty() ? NimbusPalette.padding : NimbusPalette.hintHeight + 2;
        return padTop + padBottom;
    }

    /**
     * Theme-resolved chrome ink: the theme's {@code background} /
     * {@code border-texture} / {@code color} / {@code accent} / {@code text-dim}
     * win when present; unthemed panels keep the hacker palette.
     */
    private static int dimOf(StyleContext style, int fallback) {
        Integer themed = style.get(Styles.textDim);
        return themed != null ? themed : fallback;
    }

    private int borderOf(VisualContext vc) {
        VisualTexture bt = vc.getProperty("border-texture", VisualTexture.class);
        if (bt instanceof BorderTexture border) return border.colorTop();
        if (vc.borderColor() != 0) return vc.borderColor();
        return focused ? NimbusPalette.borderFocused : NimbusPalette.border;
    }

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        int w = width();
        // folded: draw only the chrome strip — filling the full-height bounds
        // would paint a translucent box over whatever stacks below this slot
        int h = folded ? foldHeightPx() : height();

        StyleContext style = style();
        VisualContext vc = style.visualContext();
        Integer themedAccent = style.get(Styles.accent);
        int accent = themedAccent != null ? themedAccent : NimbusPalette.accent;
        // dim = the accent at ~40% alpha; unthemed keeps the tuned hacker value
        int accentDim = themedAccent != null ? (themedAccent & 0x00FFFFFF) | 0x66000000 : NimbusPalette.accentDim;
        int textC = vc.textColor() != null ? vc.textColor() : NimbusPalette.text;
        int dimC = dimOf(style, NimbusPalette.textDim);

        VisualTexture bg = vc.background();
        if (!bg.isEmpty()) {
            canvas.texture(bg, 0, 0, w, h);
        } else {
            canvas.fill(0, 0, w, h, focused ? NimbusPalette.bgFocused : NimbusPalette.bg);
        }
        // idle panels carry no frame — the leader line is the only chrome; the
        // outline appears only when the panel is focused/pointed
        if (focused || pointed) {
            canvas.strokeRect(0, 0, w, h, borderOf(vc));
        }

        if (title != null) {
            // header: accent chip + text + hairline rule
            canvas.fill(3, 5, 3, 3, accent);
            canvas.text(title, 9, 3, textC);
            canvas.fill(0, NimbusPalette.titleHeight + 1, w, 1, NimbusPalette.titleRule);
        }

        // hint strip only while the panel is actually selected — chrome that
        // every visible panel shouts is noise; on-focus keeps it a contextual
        // prompt. Plain text, no chip boxes.
        if (!hints.isEmpty() && (focused || pointed)) {
            var font = Minecraft.getInstance().font;
            int hx = w - NimbusPalette.padding;
            int hy = h - NimbusPalette.hintHeight + 1;
            for (int i = hints.size() - 1; i >= 0; i--) {
                String hint = hints.get(i);
                int sep = hint.indexOf(':');
                String key = sep > 0 ? hint.substring(0, sep) : hint;
                String label = sep > 0 ? hint.substring(sep + 1) : "";
                int keyW = font.width(key);
                int labelW = label.isEmpty() ? 0 : font.width(label);
                hx -= keyW + labelW + (labelW > 0 ? 3 : 0);
                canvas.text(key, hx, hy, accent);
                if (labelW > 0) {
                    canvas.text(label, hx + keyW + 3, hy, dimC);
                }
                hx -= 6;
            }
        }

        // merge badge: "+N" chip hanging off the top-right corner
        if (overflow != null) {
            var font = Minecraft.getInstance().font;
            int tw = font.width(overflow) + 5;
            canvas.fill(w - tw - 2, -4, tw, 9, focused ? NimbusPalette.bgFocused : NimbusPalette.bg);
            canvas.strokeRect(w - tw - 2, -4, tw, 9, accent);
            canvas.text(overflow, w - tw + 1, -3, accent);
        }

        drawPresence(canvas, w);
    }

    /** Pips cap before collapsing into a "+N" suffix on the last pip. */
    private static final int maxPips = 5;

    private static final int draggingColor = 0xFFD84315;
    private static final int tracingColor = 0xFFAB47BC;

    /**
     * Remote-presence ghosts: a pip per remote player interacting with this
     * panel's key, right-aligned in the title strip, most active first.
     * While the panel is focused, name chips stack upward off the top-right
     * corner so the watcher can see <em>who</em> is engaged, not just that
     * someone is.
     */
    private void drawPresence(SceneCanvas canvas, int w) {
        var client = Nimbus.client();
        if (client == null) return;
        List<PresenceInfo> infos = new ArrayList<>(client.presence(runtime.spec.key()));
        if (infos.isEmpty()) return;
        infos.sort(Comparator.comparingInt(i -> -kindRank(i.kind())));

        var font = Minecraft.getInstance().font;
        var conn = Minecraft.getInstance().getConnection();
        int x = w - NimbusPalette.padding - 4;
        int chipY = -13;
        for (int i = 0; i < infos.size(); i++) {
            PresenceInfo info = infos.get(i);
            if (i >= maxPips) {
                canvas.text("+" + (infos.size() - maxPips), x - 6, 4, NimbusPalette.textDim);
                break;
            }
            int color = kindColor(info.kind());
            canvas.fill(x, 4, 4, 4, color);
            x -= 6;
            if (focused && conn != null && i < 3) {
                PlayerInfo who = conn.getPlayerInfo(info.playerId());
                String name = who != null ? who.getProfile().getName() : "?";
                int tw = font.width(name) + 10;
                canvas.fill(w - tw, chipY, tw, 9, NimbusPalette.bgFocused);
                canvas.strokeRect(w - tw, chipY, tw, 9, color);
                canvas.fill(w - tw + 2, chipY + 2, 4, 4, color);
                canvas.text(name, w - tw + 9, chipY + 1, NimbusPalette.text);
                chipY -= 10;
            }
        }
    }

    private static int kindRank(PresenceKind kind) {
        return switch (kind) {
            case dragging -> 3;
            case tracing -> 2;
            case engaged -> 1;
            case watching -> 0;
        };
    }

    private static int kindColor(PresenceKind kind) {
        return switch (kind) {
            case dragging -> draggingColor;
            case tracing -> tracingColor;
            case engaged -> NimbusPalette.accent;
            case watching -> NimbusPalette.textDim;
        };
    }

    @Override
    public void render(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        float scale = openScale * distScale;
        boolean animating = scale < 0.999f;
        if (animating) {
            // grow out of the bottom-center — toward the leader line/anchor
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
        int h = folded ? foldHeightPx() : height();
        int b = NimbusPalette.bracket;
        int color = focused ? NimbusPalette.accent : NimbusPalette.accentDim;
        // top-left
        canvas.fill(-2, -2, b, 1, color);
        canvas.fill(-2, -2, 1, b, color);
        // top-right
        canvas.fill(w - b + 2, -2, b, 1, color);
        canvas.fill(w + 1, -2, 1, b, color);
        // bottom-left
        canvas.fill(-2, h + 1, b, 1, color);
        canvas.fill(-2, h - b + 2, 1, b, color);
        // bottom-right
        canvas.fill(w - b + 2, h + 1, b, 1, color);
        canvas.fill(w + 1, h - b + 2, 1, b, color);
    }

    // endregion

    // region misc

    @Override
    public String inspectionTypeName() {
        return "InworldPanel";
    }

    @Override
    public String toString() {
        return "InworldPanel[" + runtime.key() + "]";
    }

    // endregion
}
