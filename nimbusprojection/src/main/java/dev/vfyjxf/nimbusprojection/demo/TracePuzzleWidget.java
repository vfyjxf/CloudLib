package dev.vfyjxf.nimbusprojection.demo;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldTraceable;
import dev.vfyjxf.cloudlib.ui.hacker.HackerTheme;
import dev.vfyjxf.taffy.geometry.FloatSize;

import java.util.ArrayList;
import java.util.List;

/**
 * A miniature Witness-style trace puzzle: a 4×4 node grid with a round start
 * node (bottom-left), an exit stub (top-right) and a few broken edges that
 * force a route. Hold the interact key and drag the cursor node-to-node —
 * reaching the exit solves the puzzle and fires {@code onSolve}.
 * <p>
 * The demo deliberately keeps the Witness conventions: paths start on the
 * start circle, follow edges, support backtracking, and refuse
 * self-intersection — but the whole thing is just an {@link InworldTraceable}
 * content widget, so it works on face, floating and inspect-flattened panels.
 */
public final class TracePuzzleWidget extends Widget implements InworldTraceable {

    private static final int COLS = 4, ROWS = 4;
    private static final int PAD = 7, STEP = 13;
    private static final int W = PAD * 2 + (COLS - 1) * STEP;      // 53
    private static final int H = PAD * 2 + (ROWS - 1) * STEP + 12; // grid + status row
    private static final float SNAP = 6.5f;

    private static final int START = node(0, ROWS - 1);
    private static final int EXIT_PRE = node(COLS - 1, 0); //node the exit stub hangs off
    private static final int EXIT = COLS * ROWS;           //virtual node at the stub tip

    /** Broken edges (Witness breakpoints) — packed as two node ids. */
    private static final int[][] BLOCKED = {
            {node(0, 2), node(1, 2)},
            {node(1, 1), node(2, 1)},
            {node(2, 2), node(2, 3)},
            {node(3, 1), node(3, 2)},
    };

    private static int node(int col, int row) {
        return row * COLS + col;
    }

    private static float nx(int id) {
        return id == EXIT ? PAD + (COLS - 1) * STEP : PAD + (id % COLS) * STEP;
    }

    private static float ny(int id) {
        return id == EXIT ? PAD - 9 : PAD + (id / COLS) * STEP;
    }

    private static boolean edgeBetween(int a, int b) {
        int ac = a % COLS, ar = a / COLS, bc = b % COLS, br = b / COLS;
        if (Math.abs(ac - bc) + Math.abs(ar - br) != 1) return false;
        for (int[] pair : BLOCKED) {
            if ((pair[0] == a && pair[1] == b) || (pair[0] == b && pair[1] == a)) return false;
        }
        return true;
    }

    private final Runnable onSolve;
    /** live solve-count readout (synced handle) for the status line — null hides it */
    private final java.util.function.IntSupplier solves;

    private final List<Integer> path = new ArrayList<>();
    private float cursorX, cursorY;
    private boolean tracing;
    private boolean solved;
    private int failFlash;   //ticks of red flash left after an unsolved commit
    private int fade;        //ticks of path fade after cancel/fail

    public TracePuzzleWidget(InworldPanelContext ctx, Runnable onSolve,
                             @org.jetbrains.annotations.Nullable java.util.function.IntSupplier solves) {
        this.onSolve = onSolve;
        this.solves = solves;
        setTickable(true);
        onMount((scene, context, handle) ->
                scene.layoutTree().setMeasureFunc(nodeId(), (style, space) -> new FloatSize(W, H)));
    }

    //region InworldTraceable

    @Override
    public @org.jetbrains.annotations.Nullable FloatPos traceCursorStart() {
        //Witness convention: activating the panel snaps the cursor onto the
        //start circle — the stroke always begins at the node, not wherever
        //the crosshair happened to be
        return new FloatPos(nx(START), ny(START));
    }

    @Override
    public boolean traceBegin(InworldPanelContext context, float x, float y) {
        if (solved) return false; //already solved — let the press fall through to the action
        tracing = true;
        path.clear();
        failFlash = 0;
        fade = 0;
        cursorX = x;
        cursorY = y;
        return true;
    }

    @Override
    public void traceMove(float x, float y) {
        cursorX = x;
        cursorY = y;
        if (solved) return;

        if (path.isEmpty()) {
            if (near(x, y, START)) path.add(START);
            return;
        }

        int tip = path.get(path.size() - 1);
        //exit stub — only reachable off the exit-pre node
        if (tip == EXIT_PRE && near(x, y, EXIT)) {
            path.add(EXIT);
            solved = true;
            onSolve.run();
            return;
        }
        //backtrack: hovering the previous node pops the tip
        if (path.size() >= 2 && near(x, y, path.get(path.size() - 2))) {
            path.remove(path.size() - 1);
            return;
        }
        //extend to an unvisited adjacent node
        for (int id = 0; id < COLS * ROWS; id++) {
            if (path.contains(id) || !edgeBetween(tip, id)) continue;
            if (near(x, y, id)) {
                path.add(id);
                return;
            }
        }
    }

    @Override
    public void traceCommit(InworldPanelContext context) {
        tracing = false;
        if (!solved && !path.isEmpty()) {
            failFlash = 16;
            fade = 16;
        }
        //solved paths stay lit until the next session — the board "remembers"
    }

    @Override
    public void traceCancel() {
        tracing = false;
        if (!solved) fade = 14;
    }

    //endregion

    private static boolean near(float x, float y, int id) {
        float dx = x - nx(id), dy = y - ny(id);
        return dx * dx + dy * dy < SNAP * SNAP;
    }

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        int line = HackerTheme.LINE_DARK;
        int node = 0xFF3A4A55;
        int pathColor = solved ? HackerTheme.ACCENT
                : failFlash > 0 ? 0xFFE06666 : HackerTheme.ACCENT;
        int pathDim = solved || failFlash > 0 ? pathColor : HackerTheme.ACCENT_DIM;

        //edges — blocked ones get a punched-out gap
        for (int a = 0; a < COLS * ROWS; a++) {
            int col = a % COLS, row = a / COLS;
            if (col + 1 < COLS) edge(canvas, a, node(col + 1, row), line);
            if (row + 1 < ROWS) edge(canvas, a, node(col, row + 1), line);
        }
        //nodes
        for (int id = 0; id < COLS * ROWS; id++) {
            canvas.circle(nx(id), ny(id), id == START ? 3f : 1.6f,
                    id == START ? HackerTheme.ACCENT_DIM : node);
        }
        //exit stub off the top-right node
        canvas.line(nx(EXIT_PRE), ny(EXIT_PRE), nx(EXIT), ny(EXIT), 1f, line);
        canvas.circle(nx(EXIT), ny(EXIT), 2.2f, solved ? HackerTheme.ACCENT : node);

        //the traced path + live tip segment
        if (!path.isEmpty() && fade > -8) {
            float alpha = fade > 0 && !solved && !tracing ? fade / 14f : 1f;
            int pc = withAlpha(pathColor, alpha);
            int pd = withAlpha(pathDim, alpha);
            for (int i = 1; i < path.size(); i++) {
                canvas.line(nx(path.get(i - 1)), ny(path.get(i - 1)),
                        nx(path.get(i)), ny(path.get(i)), 2f, pc);
            }
            if (tracing && !solved) {
                int tip = path.get(path.size() - 1);
                canvas.line(nx(tip), ny(tip), cursorX, cursorY, 1.2f, pd);
            }
            for (int id : path) {
                canvas.circle(nx(id), ny(id), 2f, pc);
            }
        }

        //the trace cursor — the Witness "dot"
        if (tracing && !solved) {
            canvas.circle(cursorX, cursorY, 4f, 0x5516D8CF);
            canvas.circle(cursorX, cursorY, 1.8f, HackerTheme.ACCENT);
        }

        //status line
        String status = solved
                ? "SOLVED" + (solves != null ? " ×" + solves.getAsInt() : "")
                : failFlash > 0 ? "ROUTE FAIL"
                : tracing ? "···" : "route ◉→◎";
        int sc = solved ? HackerTheme.ACCENT
                : failFlash > 0 ? 0xFFE06666 : HackerTheme.TEXT_DIM;
        canvas.text(status, 0, H - 9, sc);
    }

    private void edge(SceneCanvas canvas, int a, int b, int color) {
        for (int[] pair : BLOCKED) {
            if ((pair[0] == a && pair[1] == b) || (pair[0] == b && pair[1] == a)) {
                //breakpoint: two stubs with a gap in the middle
                float x1 = nx(a), y1 = ny(a), x2 = nx(b), y2 = ny(b);
                float mx = (x1 + x2) / 2, my = (y1 + y2) / 2;
                canvas.line(x1, y1, x1 + (mx - x1) * 0.55f, y1 + (my - y1) * 0.55f, 1f, color);
                canvas.line(x2, y2, x2 + (mx - x2) * 0.55f, y2 + (my - y2) * 0.55f, 1f, color);
                return;
            }
        }
        canvas.line(nx(a), ny(a), nx(b), ny(b), 1f, color);
    }

    private static int withAlpha(int color, float frac) {
        int a = (int) (((color >>> 24) & 0xFF) * frac);
        return (a << 24) | (color & 0xFFFFFF);
    }

    @Override
    public void tick() {
        super.tick();
        if (failFlash > 0) failFlash--;
        if (fade > 0) {
            fade--;
            if (fade == 0 && !solved) path.clear();
        }
    }
}
