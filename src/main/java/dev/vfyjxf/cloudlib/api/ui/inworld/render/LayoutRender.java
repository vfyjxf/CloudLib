package dev.vfyjxf.cloudlib.api.ui.inworld.render;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiRect;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiVec;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LeaderLine;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.OverflowDock;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.OverflowDrawer;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PreparedLayout;
import dev.vfyjxf.cloudlib.internal.ui.inworld.lines.ScreenStroke;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen-space half of the layout bridge — draws a {@link PreparedLayout}'s
 * screen panels, screen leaders and overflow chrome onto a
 * {@link SceneCanvas} (the HUD/gui pass; call it from
 * {@code RenderGuiEvent.Post}).
 * <p>
 * Draw order follows pointer order: leader strokes under panels, dock and
 * drawer chrome last.
 * <ul>
 *   <li>Screen leaders → {@link ScreenStroke} polylines (depth-free —
 *       {@code XRAY}/{@code TEST} identical on the HUD),</li>
 *   <li>screen panels → host {@link ScreenPanelPainter} per rect (default:
 *       {@link #defaultPanelChrome}),</li>
 *   <li>dock/drawer → host {@link ChromePainter} (default: filled chrome).</li>
 * </ul>
 */
public final class LayoutRender {

    private LayoutRender() {}

    /** Host callback: paints one screen panel's contents into its rect. */
    @FunctionalInterface
    public interface ScreenPanelPainter {
        void paint(SceneCanvas canvas, PanelPlacement panel, GuiRect rect);
    }

    /** Host callback: paints the overflow dock or drawer chrome. */
    @FunctionalInterface
    public interface ChromePainter {
        void paint(SceneCanvas canvas, String id, GuiRect rect, List<String> items);
    }

    private static final int defaultLeaderColor = 0xFF4CC9F0;
    private static final float defaultLeaderWidth = 2.0f;
    private static final int defaultPanelFill = 0xE0181B20;
    private static final int defaultPanelBorder = 0xFF4CC9F0;
    private static final int defaultChromeFill = 0xE0101216;
    private static final int defaultChromeBorder = 0xFF8A8F98;

    /**
     * Draws every screen-space piece of {@code prepared}: leaders, screen
     * panels, then overflow chrome. Null painter/chrome fall back to
     * {@link #defaultPanelChrome}/{@link #defaultChrome}.
     */
    public static void drawScreen(
            SceneCanvas canvas,
            PreparedLayout prepared,
            @Nullable ScreenPanelPainter painter,
            @Nullable ChromePainter chrome) {
        drawScreen(canvas, prepared, painter, chrome, defaultLeaderColor, defaultLeaderWidth, TraceStyle.crisp);
    }

    /** Full-parameter variant — leader colour, width and stroke style. */
    public static void drawScreen(
            SceneCanvas canvas,
            PreparedLayout prepared,
            @Nullable ScreenPanelPainter painter,
            @Nullable ChromePainter chrome,
            int leaderArgb,
            float leaderWidthPx,
            @Nullable TraceStyle leaderStyle) {
        for (LeaderLine leader : prepared.screenLeaders()) {
            strokeLeader(canvas, leader, leaderArgb, leaderWidthPx, leaderStyle);
        }
        ScreenPanelPainter panelPainter = painter == null ? LayoutRender::defaultPanelChrome : painter;
        for (PanelPlacement panel : prepared.screenPanels()) {
            panelPainter.paint(canvas, panel, panel.screenRect());
        }
        ChromePainter chromePainter = chrome == null ? LayoutRender::defaultChrome : chrome;
        OverflowDock dock = prepared.result().dock();
        if (dock != null && dock.rect() != null) {
            chromePainter.paint(canvas, "overflow:dock", dock.rect(), dock.pageItems());
        }
        OverflowDrawer drawer = prepared.result().drawer();
        if (drawer != null && drawer.rect() != null) {
            chromePainter.paint(canvas, "overflow:drawer", drawer.rect(), drawer.items());
        }
    }

    /** Strokes one leader's screen polyline through the canvas. */
    public static void strokeLeader(
            SceneCanvas canvas, LeaderLine leader, int argb, float widthPx, @Nullable TraceStyle style) {
        if (leader.screen().size() < 2) return;
        List<FloatPos> pts = new ArrayList<>(leader.screen().size());
        for (GuiVec p : leader.screen()) {
            pts.add(new FloatPos(p.x(), p.y()));
        }
        ScreenStroke.emit(canvas, pts, widthPx, argb, style);
    }

    /** Minimal panel chrome — fill plus a 1px accent border. */
    public static void defaultPanelChrome(SceneCanvas canvas, PanelPlacement panel, GuiRect rect) {
        canvas.fill((int) rect.x(), (int) rect.y(), (int) rect.width(), (int) rect.height(), defaultPanelFill);
        canvas.strokeRect((int) rect.x(), (int) rect.y(), (int) rect.width(), (int) rect.height(), defaultPanelBorder);
    }

    /** Minimal overflow chrome — fill plus a muted border. */
    public static void defaultChrome(SceneCanvas canvas, String id, GuiRect rect, List<String> items) {
        canvas.fill((int) rect.x(), (int) rect.y(), (int) rect.width(), (int) rect.height(), defaultChromeFill);
        canvas.strokeRect((int) rect.x(), (int) rect.y(), (int) rect.width(), (int) rect.height(), defaultChromeBorder);
    }
}
