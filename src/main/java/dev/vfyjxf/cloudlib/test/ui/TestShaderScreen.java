package dev.vfyjxf.cloudlib.test.ui;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.ui.base.BasicScreen;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.taffy.style.TaffyDimension;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.sizeOf;

//@TestScreen
public class TestShaderScreen extends BasicScreen {

    public TestShaderScreen() {
        var editor = new NodeGraphEditorWidget();
        editor.useStyle(UIStyle.of(sizeOf(TaffyDimension.percent(1f), TaffyDimension.percent(1f))));
        mainGroup.addWidget(editor);
    }

    @Override
    protected void init() {
        super.init();
    }

    //region data types

    private static class GraphNode {

        static final int HEADER_H = 18;
        static final int PIN_ROW_H = 14;
        static final int PIN_R = 4;
        static final int BODY_PAD = 3;

        final String title;
        final int w;
        final int headerColor;
        final String[] inputs;
        final String[] outputs;
        final int[] inputColors;
        final int[] outputColors;

        float x, y;

        GraphNode(
                String title, float x, float y, int w, int headerColor,
                String[] inputs, int[] inputColors,
                String[] outputs, int[] outputColors) {
            this.title = title;
            this.x = x;
            this.y = y;
            this.w = w;
            this.headerColor = headerColor;
            this.inputs = inputs;
            this.outputs = outputs;
            this.inputColors = inputColors;
            this.outputColors = outputColors;
        }

        int height() {
            return HEADER_H + BODY_PAD * 2 + Math.max(Math.max(inputs.length, outputs.length), 1) * PIN_ROW_H;
        }

        float inPinX() {
            return x;
        }

        float inPinY(int i) {
            return y + HEADER_H + BODY_PAD + i * PIN_ROW_H + PIN_ROW_H / 2f;
        }

        float outPinX() {
            return x + w;
        }

        float outPinY(int i) {
            return y + HEADER_H + BODY_PAD + i * PIN_ROW_H + PIN_ROW_H / 2f;
        }

        boolean hitHeader(double px, double py) {
            return px >= x && px <= x + w && py >= y && py <= y + HEADER_H;
        }

        boolean hitBody(double px, double py) {
            return px >= x && px <= x + w && py >= y && py <= y + height();
        }
    }

    private record Connection(int srcNode, int srcPin, int dstNode, int dstPin) {
    }

    private static class FloatingPanel {

        static final int TITLE_H = 16;

        final String title;
        final int w, h;
        final int titleColor;
        final String[] lines;

        float x, y;

        FloatingPanel(String title, float x, float y, int w, int h, int titleColor, String... lines) {
            this.title = title;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.titleColor = titleColor;
            this.lines = lines;
        }

        boolean hitTitle(double px, double py) {
            return px >= x && px <= x + w && py >= y && py <= y + TITLE_H;
        }

        boolean hitBody(double px, double py) {
            return px >= x && px <= x + w && py >= y && py <= y + h;
        }
    }

    //endregion

    //region editor widget

    private static class NodeGraphEditorWidget extends Widget {

        static final int DRAG_NONE = 0;
        static final int DRAG_NODE = 1;
        static final int DRAG_PANEL = 2;
        static final int DRAG_CONN = 3;
        static final int DRAG_PAN = 4;

        final List<GraphNode> nodes = new ArrayList<>();
        final List<Connection> connections = new ArrayList<>();
        final List<FloatingPanel> panels = new ArrayList<>();

        int dragType = DRAG_NONE;
        int dragIndex = -1;

        int connSrcNode = -1;
        int connSrcPin = -1;
        boolean connFromOutput = true;
        float connDragX, connDragY;

        float panX = 0, panY = 0;

        int hoverNode = -1;
        int hoverPin = -1;
        boolean hoverIsOutput = false;

        // Connection wire visual toggles
        boolean wireGlow = true;
        boolean wireGradient = true;

        {
            setFocusable(true);
            initGraph();
            registerInputHandlers();
        }

        //region graph setup

        private void initGraph() {
            nodes.add(new GraphNode("Texture Sample", 30, 40, 120, 0xFF1B6B3A,
                    new String[]{}, new int[]{},
                    new String[]{"RGB", "R", "G", "B", "A"},
                    new int[]{0xFFF9E2AF, 0xFFF38BA8, 0xFFA6E3A1, 0xFF89B4FA, 0xFFCDD6F4}));
            nodes.add(new GraphNode("Constant", 30, 180, 100, 0xFF6C3483,
                    new String[]{}, new int[]{},
                    new String[]{"Value"},
                    new int[]{0xFFCBA6F7}));
            nodes.add(new GraphNode("Multiply", 200, 170, 110, 0xFF7D6608,
                    new String[]{"A", "B"}, new int[]{0xFFCDD6F4, 0xFFCDD6F4},
                    new String[]{"Result"},
                    new int[]{0xFFF9E2AF}));
            nodes.add(new GraphNode("Lerp", 210, 50, 110, 0xFF1A5276,
                    new String[]{"A", "B", "Alpha"}, new int[]{0xFFF9E2AF, 0xFFF9E2AF, 0xFFCDD6F4},
                    new String[]{"Result"},
                    new int[]{0xFFF9E2AF}));
            nodes.add(new GraphNode("Add", 200, 260, 110, 0xFF7D6608,
                    new String[]{"A", "B"}, new int[]{0xFFCDD6F4, 0xFFCDD6F4},
                    new String[]{"Result"},
                    new int[]{0xFFA6E3A1}));
            nodes.add(new GraphNode("Material", 400, 30, 130, 0xFF922B21,
                    new String[]{"Base Color", "Metallic", "Roughness", "Normal", "Emissive"},
                    new int[]{0xFFF9E2AF, 0xFFCDD6F4, 0xFFCDD6F4, 0xFF89B4FA, 0xFFA6E3A1},
                    new String[]{}, new int[]{}));

            connections.add(new Connection(0, 0, 3, 0));
            connections.add(new Connection(1, 0, 2, 0));
            connections.add(new Connection(0, 4, 3, 2));
            connections.add(new Connection(3, 0, 5, 0));
            connections.add(new Connection(2, 0, 5, 2));
            connections.add(new Connection(4, 0, 5, 3));

            panels.add(new FloatingPanel("Options", 420, 200, 130, 96, 0xFF585B70,
                    "[G] Toggle glow: ON",
                    "[C] Toggle gradient: ON",
                    "Drag pins to connect",
                    "Node graph editor demo"));
            panels.add(new FloatingPanel("Stats", 420, 300, 120, 60, 0xFF585B70,
                    "Nodes: 6",
                    "Connections: 6",
                    "FPS: --"));
        }

        //endregion

        //region input handling

        private void registerInputHandlers() {
            onMouseClicked((input, ctx) -> {
                if (ctx.bubbling()) return EventDispatch.pass;

                // Request focus so this widget can receive key events
                if (focusNode() != null) focusNode().requestFocus();

                FloatPos local = input.mouseRelative(this);
                double mx = local.x - panX;
                double my = local.y - panY;
                int button = input.key().getValue();

                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    // pins → connection drag
                    for (int ni = nodes.size() - 1; ni >= 0; ni--) {
                        GraphNode n = nodes.get(ni);
                        for (int pi = 0; pi < n.outputs.length; pi++) {
                            if (distSq(mx, my, n.outPinX(), n.outPinY(pi)) <= 64) {
                                beginConnectionDrag(ni, pi, true, mx, my);
                                return EventDispatch.consumed;
                            }
                        }
                        for (int pi = 0; pi < n.inputs.length; pi++) {
                            if (distSq(mx, my, n.inPinX(), n.inPinY(pi)) <= 64) {
                                beginConnectionDrag(ni, pi, false, mx, my);
                                return EventDispatch.consumed;
                            }
                        }
                    }
                    // panels → title drag
                    for (int i = panels.size() - 1; i >= 0; i--) {
                        FloatingPanel p = panels.get(i);
                        if (p.hitTitle(mx, my)) {
                            beginDrag(DRAG_PANEL, i);
                            return EventDispatch.consumed;
                        }
                        if (p.hitBody(mx, my)) return EventDispatch.consumed;
                    }
                    // nodes → header drag
                    for (int i = nodes.size() - 1; i >= 0; i--) {
                        GraphNode n = nodes.get(i);
                        if (n.hitHeader(mx, my)) {
                            beginDrag(DRAG_NODE, i);
                            return EventDispatch.consumed;
                        }
                        if (n.hitBody(mx, my)) return EventDispatch.consumed;
                    }
                }

                if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT || button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                    beginDrag(DRAG_PAN, -1);
                    return EventDispatch.consumed;
                }
                return EventDispatch.pass;
            });

            onMouseDragged((input, deltaX, deltaY, ctx) -> {
                if (ctx.bubbling()) return EventDispatch.pass;
                float dx = (float) deltaX;
                float dy = (float) deltaY;
                switch (dragType) {
                    case DRAG_NODE -> {
                        GraphNode n = nodes.get(dragIndex);
                        n.x += dx;
                        n.y += dy;
                    }
                    case DRAG_PANEL -> {
                        FloatingPanel p = panels.get(dragIndex);
                        p.x += dx;
                        p.y += dy;
                    }
                    case DRAG_CONN -> {
                        connDragX += dx;
                        connDragY += dy;
                    }
                    case DRAG_PAN -> {
                        panX += dx;
                        panY += dy;
                    }
                    default -> {
                        return EventDispatch.pass;
                    }
                }
                return EventDispatch.consumed;
            });

            onMouseReleased((input, ctx) -> {
                if (dragType == DRAG_CONN) tryCompleteConnection();
                resetDrag();
                return EventDispatch.consumed;
            });

            onKeyPressed((input, ctx) -> {
                if (ctx.bubbling()) return EventDispatch.pass;
                int key = input.key().getValue();
                if (key == GLFW.GLFW_KEY_G) {
                    wireGlow = !wireGlow;
                    updateOptionsPanel();
                    return EventDispatch.consumed;
                }
                if (key == GLFW.GLFW_KEY_C) {
                    wireGradient = !wireGradient;
                    updateOptionsPanel();
                    return EventDispatch.consumed;
                }
                return EventDispatch.pass;
            });
        }

        private void beginDrag(int type, int index) {
            dragType = type;
            dragIndex = index;
        }

        private void beginConnectionDrag(int nodeIdx, int pinIdx, boolean fromOutput, double mx, double my) {
            dragType = DRAG_CONN;
            connSrcNode = nodeIdx;
            connSrcPin = pinIdx;
            connFromOutput = fromOutput;
            connDragX = (float) mx;
            connDragY = (float) my;
        }

        private void resetDrag() {
            dragType = DRAG_NONE;
            dragIndex = -1;
            connSrcNode = -1;
            connSrcPin = -1;
        }

        private void updateOptionsPanel() {
            FloatingPanel opts = panels.get(0);
            opts.lines[0] = "[G] Toggle glow: " + (wireGlow ? "ON" : "OFF");
            opts.lines[1] = "[C] Toggle gradient: " + (wireGradient ? "ON" : "OFF");
        }

        private void tryCompleteConnection() {
            for (int ni = 0; ni < nodes.size(); ni++) {
                if (ni == connSrcNode) continue;
                GraphNode n = nodes.get(ni);
                final int nodeIdx = ni;

                if (connFromOutput) {
                    for (int pi = 0; pi < n.inputs.length; pi++) {
                        if (distSq(connDragX, connDragY, n.inPinX(), n.inPinY(pi)) <= 64) {
                            final int pinIdx = pi;
                            connections.removeIf(c -> c.dstNode == nodeIdx && c.dstPin == pinIdx);
                            connections.add(new Connection(connSrcNode, connSrcPin, nodeIdx, pinIdx));
                            return;
                        }
                    }
                } else {
                    for (int pi = 0; pi < n.outputs.length; pi++) {
                        if (distSq(connDragX, connDragY, n.outPinX(), n.outPinY(pi)) <= 64) {
                            final int pinIdx = pi;
                            connections.removeIf(c -> c.dstNode == connSrcNode && c.dstPin == connSrcPin);
                            connections.add(new Connection(nodeIdx, pinIdx, connSrcNode, connSrcPin));
                            return;
                        }
                    }
                }
            }
        }

        //endregion

        //region rendering

        @Override
        protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
            int w = width(), h = height();

            FloatPos local = sceneToLocal(mouseX, mouseY);
            updateHover(local.x - panX, local.y - panY);

            canvas.fill(0, 0, w, h, 0xFF181825);
            canvas.pushClip(0, 0, w, h);
            canvas.pushTransform();
            canvas.translate(panX, panY);

            drawGrid(canvas, w, h);
            drawConnections(canvas);
            drawTempConnection(canvas);
            drawNodes(canvas);
            drawPanels(canvas);

            canvas.popTransform();
            canvas.popClip();

            canvas.drawString("Node Graph Editor", 6, 3, 0xFFCDD6F4, true);
            canvas.drawString("LMB: drag nodes/pins | RMB: pan | G: glow | C: gradient", 6, h - 11, 0x88CDD6F4, false);
        }

        private void drawGrid(SceneCanvas canvas, int w, int h) {
            float vx0 = -panX, vy0 = -panY;
            float vx1 = vx0 + w, vy1 = vy0 + h;
            int step = 100;
            int sx = (int) (Math.floor(vx0 / step) * step);
            int sy = (int) (Math.floor(vy0 / step) * step);
            for (int gx = sx; gx <= (int) vx1; gx += step) {
                canvas.line(gx, (int) vy0, gx, (int) vy1, 0x28CDD6F4);
            }
            for (int gy = sy; gy <= (int) vy1; gy += step) {
                canvas.line((int) vx0, gy, (int) vx1, gy, 0x28CDD6F4);
            }
        }

        private void drawConnections(SceneCanvas canvas) {
            for (Connection c : connections) {
                GraphNode src = nodes.get(c.srcNode);
                GraphNode dst = nodes.get(c.dstNode);
                float x0 = src.outPinX() + GraphNode.PIN_R;
                float y0 = src.outPinY(c.srcPin);
                float x1 = dst.inPinX() - GraphNode.PIN_R;
                float y1 = dst.inPinY(c.dstPin);
                int cSrc = src.outputColors[c.srcPin];
                int cDst = dst.inputColors[c.dstPin];
                int cEnd = wireGradient ? cDst : cSrc;
                float glow = wireGlow ? 6f : 0f;
                int glowColor = wireGlow ? withAlpha(cSrc, 0x22) : 0;
                canvas.horizontalSpline(x0, y0, x1, y1,
                        2.5f, cSrc, cEnd, glow, glowColor);
            }
        }

        private void drawTempConnection(SceneCanvas canvas) {
            if (dragType != DRAG_CONN || connSrcNode < 0) return;
            GraphNode src = nodes.get(connSrcNode);
            float x0, y0;
            int color;
            if (connFromOutput) {
                x0 = src.outPinX() + GraphNode.PIN_R;
                y0 = src.outPinY(connSrcPin);
                color = src.outputColors[connSrcPin];
            } else {
                x0 = src.inPinX() - GraphNode.PIN_R;
                y0 = src.inPinY(connSrcPin);
                color = src.inputColors[connSrcPin];
            }
            float glow = wireGlow ? 4f : 0f;
            int glowColor = wireGlow ? withAlpha(color, 0x18) : 0;
            int cEnd = wireGradient ? 0xFFCDD6F4 : color;
            canvas.horizontalSpline(x0, y0, connDragX, connDragY,
                    2f, color, cEnd, glow, glowColor);
        }

        private void drawNodes(SceneCanvas canvas) {
            for (int i = 0; i < nodes.size(); i++) {
                drawNode(canvas, nodes.get(i), i);
            }
        }

        private void drawNode(SceneCanvas canvas, GraphNode n, int nodeIndex) {
            int nh = n.height();
            int nx = (int) n.x, ny = (int) n.y;
            float r = 5f;

            canvas.shadow(nx, ny, n.w, nh, r, 3f, 10f, 0xAA000000);
            canvas.roundedRect(nx, ny, n.w, nh, r, 0xFF1E1E2E, 1f, darken(n.headerColor, 0.6f));
            canvas.roundedRect(nx, ny, n.w, GraphNode.HEADER_H, r, r, 0f, 0f, n.headerColor);
            canvas.drawString(n.title, nx + 6, ny + 4, 0xFFFFFFFF, true);
            canvas.line(n.x + 1, n.y + GraphNode.HEADER_H, n.x + n.w - 1, n.y + GraphNode.HEADER_H, 1f, 0x60FFFFFF);

            for (int i = 0; i < n.inputs.length; i++) {
                drawPin(canvas, n.inPinX(), n.inPinY(i), n.inputs[i], n.inputColors[i],
                        true, nodeIndex == hoverNode && i == hoverPin && !hoverIsOutput);
            }
            for (int i = 0; i < n.outputs.length; i++) {
                drawPin(canvas, n.outPinX(), n.outPinY(i), n.outputs[i], n.outputColors[i],
                        false, nodeIndex == hoverNode && i == hoverPin && hoverIsOutput);
            }
        }

        private void drawPin(
                SceneCanvas canvas, float px, float py, String label, int color,
                boolean isInput, boolean hovered) {
            int radius = hovered ? GraphNode.PIN_R + 2 : GraphNode.PIN_R;
            canvas.circle(px, py, radius, color);
            if (hovered) {
                canvas.circle(px, py, GraphNode.PIN_R + 5, withAlpha(color, 0x30));
            }
            if (isInput) {
                canvas.drawString(label, (int) px + 7, (int) py - 4, 0xFFBAC2DE, false);
            } else {
                int tw = canvas.font().width(label);
                canvas.drawString(label, (int) px - tw - 7, (int) py - 4, 0xFFBAC2DE, false);
            }
        }

        private void drawPanels(SceneCanvas canvas) {
            for (FloatingPanel p : panels) {
                int px = (int) p.x, py = (int) p.y;
                float r = 6f;
                canvas.shadow(px, py, p.w, p.h, r, 4f, 14f, 0x99000000);
                canvas.roundedRect(px, py, p.w, p.h, r, 0xE6313244);
                canvas.roundedRect(px, py, p.w, FloatingPanel.TITLE_H, r, r, 0f, 0f, p.titleColor);
                canvas.drawString(p.title, px + 5, py + 3, 0xFFFFFFFF, true);
                for (int i = 0; i < p.lines.length; i++) {
                    canvas.drawString(p.lines[i], px + 5, py + FloatingPanel.TITLE_H + 4 + i * 11, 0xFFBAC2DE, false);
                }
            }
        }

        //endregion

        //region utilities

        private void updateHover(double mx, double my) {
            hoverNode = -1;
            hoverPin = -1;
            for (int ni = 0; ni < nodes.size(); ni++) {
                GraphNode n = nodes.get(ni);
                for (int pi = 0; pi < n.outputs.length; pi++) {
                    if (distSq(mx, my, n.outPinX(), n.outPinY(pi)) <= 64) {
                        hoverNode = ni;
                        hoverPin = pi;
                        hoverIsOutput = true;
                        return;
                    }
                }
                for (int pi = 0; pi < n.inputs.length; pi++) {
                    if (distSq(mx, my, n.inPinX(), n.inPinY(pi)) <= 64) {
                        hoverNode = ni;
                        hoverPin = pi;
                        hoverIsOutput = false;
                        return;
                    }
                }
            }
        }

        private static double distSq(double x1, double y1, double x2, double y2) {
            double dx = x1 - x2, dy = y1 - y2;
            return dx * dx + dy * dy;
        }

        private static int darken(int argb, float factor) {
            int a = (argb >> 24) & 0xFF;
            int r = (int) (((argb >> 16) & 0xFF) * factor);
            int g = (int) (((argb >> 8) & 0xFF) * factor);
            int b = (int) ((argb & 0xFF) * factor);
            return (a << 24) | (r << 16) | (g << 8) | b;
        }

        private static int withAlpha(int rgb, int alpha) {
            return (rgb & 0x00FFFFFF) | (alpha << 24);
        }

        //endregion
    }
}
