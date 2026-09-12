package dev.vfyjxf.nimbusprojection.demo;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelContext;
import dev.vfyjxf.cloudlib.ui.hacker.HackerTheme;
import dev.vfyjxf.taffy.geometry.FloatSize;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The tracker's expand-panel content — a small surveillance console:
 * <ul>
 *   <li>a radar scope fed by the <em>actual</em> entities around the anchor
 *       block (re-scanned every 5 ticks; blips flare when the sweep arm
 *       crosses them and fade out after);</li>
 *   <li>a stat column bound to the synced handles (entity count, nearest
 *       name, ping count, alert lamp);</li>
 *   <li>a crowd-threshold bar that flips to warn color when the live count
 *       crosses it;</li>
 *   <li>a sparkline of the entity-count history sampled client-side;</li>
 *   <li>jittering signal bars — pure eye-candy animation.</li>
 * </ul>
 * Everything animates from gameTime+partialTick so the panel exercises
 * per-frame in-world rendering.
 */
public final class ScanConsoleWidget extends Widget {

    private static final int W = 182;
    private static final int H = 108;
    private static final int WARN = 0xFFE06666;
    private static final int BG_PANEL = 0x55061012;
    private static final double TAU = Math.PI * 2;

    private final @Nullable BlockPos anchor;
    private final @Nullable TrackerBlockEntity be;

    private List<Blip> blips = List.of();
    private final int[] history = new int[48];
    private int histHead;
    private int histSize;

    private record Blip(float nx, float nz, int color) {
    }

    public ScanConsoleWidget(InworldPanelContext ctx) {
        this.anchor = ctx.panel().blockPos();
        this.be = ctx.blockEntity(TrackerBlockEntity.class);
        onMount((scene, context, handle) ->
                scene.layoutTree().setMeasureFunc(nodeId(), (style, space) -> new FloatSize(W, H)));
        setTickable(true);
        onTick(this::refresh);
    }

    /** Re-scans entities around the anchor at 4hz and appends to the sparkline. */
    private void refresh() {
        var mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null || anchor == null) {
            blips = List.of();
            return;
        }
        if (level.getGameTime() % 5 != 0) return;

        int range = TrackerBlockEntity.RANGE;
        var box = AABB.ofSize(Vec3.atCenterOf(anchor), range * 2.0, 12, range * 2.0);
        List<Blip> found = new ArrayList<>();
        for (var e : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (e == mc.player) continue;
            float nx = (float) ((e.getX() - anchor.getX() - 0.5) / range);
            float nz = (float) ((e.getZ() - anchor.getZ() - 0.5) / range);
            int color = e instanceof Monster ? WARN : HackerTheme.ACCENT;
            found.add(new Blip(nx, nz, color));
        }
        blips = found;
        history[histHead] = found.size();
        histHead = (histHead + 1) % history.length;
        histSize = Math.min(histSize + 1, history.length);
    }

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        var level = Minecraft.getInstance().level;
        float now = level != null ? level.getGameTime() + partialTicks : 0;
        var font = context().font();

        renderScope(canvas, now);
        renderStats(canvas, font);
        renderSparkline(canvas, font);
        renderSignalBars(canvas, now);
    }

    /** Left: square-ring radar scope with real entity blips. */
    private void renderScope(SceneCanvas canvas, float now) {
        int cx = 50;
        int cy = 46;
        int r = 40;

        for (int i = 1; i <= 3; i++) {
            int rr = r * i / 3;
            canvas.strokeRect(cx - rr, cy - rr, rr * 2, rr * 2, 0x4435D6D0);
        }
        canvas.fill(cx - r, cy, r * 2, 1, 0x2200FFE0);
        canvas.fill(cx, cy - r, 1, r * 2, 0x2200FFE0);

        double sweep = now * 0.11;
        for (int i = 0; i < 3; i++) {
            double a = sweep - i * 0.16;
            int color = ((int) ((1f - i * 0.33f) * 200) << 24) | (HackerTheme.ACCENT & 0xFFFFFF);
            canvas.line(cx, cy, (float) (cx + Math.cos(a) * r), (float) (cy + Math.sin(a) * r), 1f, color);
        }

        for (Blip b : blips) {
            double ba = Math.atan2(b.nz(), b.nx());
            double since = (sweep - ba) % TAU;
            if (since < 0) since += TAU;
            float glow = (float) Math.exp(-since * 1.4);
            int alpha = Math.min(255, 0x40 + (int) (glow * 0xBF));
            int color = (alpha << 24) | (b.color() & 0xFFFFFF);
            int bx = (int) (cx + b.nx() * r);
            int by = (int) (cy + b.nz() * r);
            canvas.fill(bx - 1, by - 1, 3, 3, color);
            if (glow > 0.75f) {
                canvas.strokeRect(bx - 3, by - 3, 7, 7, color);
            }
        }
        canvas.strokeRect(cx - r - 2, cy - r - 2, r * 2 + 4, r * 2 + 4, HackerTheme.BORDER);
    }

    /** Right column: synced stats + threshold bar + alert lamp. */
    private void renderStats(SceneCanvas canvas, net.minecraft.client.gui.Font font) {
        int x = 102;
        int y = 6;
        if (be == null) {
            canvas.text("NO LINK", x, y + 20, HackerTheme.TEXT_DIM);
            return;
        }
        int ents = be.entities().get();
        int thr = be.threshold().get();

        canvas.text("ENTS " + ents, x, y, HackerTheme.TEXT);
        canvas.text("NEAR " + be.nearest().get(), x, y + 11, HackerTheme.TEXT_DIM);
        canvas.text("PING " + be.pings().get(), x, y + 22, HackerTheme.TEXT_DIM);

        //crowd threshold bar — flips to warn color when live count crosses it
        int barY = y + 35;
        canvas.text("THR", x, barY - 1, HackerTheme.TEXT_DIM);
        int bx = x + 20;
        int bw = 44;
        boolean tripped = ents >= thr && ents > 0;
        int fill = tripped ? WARN : HackerTheme.ACCENT;
        canvas.fill(bx, barY, bw, 5, BG_PANEL);
        canvas.strokeRect(bx, barY, bw, 5, 0x5535D6D0);
        int fw = (int) ((bw - 2) * (thr / (double) TrackerBlockEntity.MAX_THRESHOLD));
        canvas.fill(bx + 1, barY + 1, fw, 3, fill);
        //live count marker riding on the same bar
        int mark = bx + 1 + (int) ((bw - 2) * Math.min(1, ents / (double) TrackerBlockEntity.MAX_THRESHOLD));
        canvas.fill(mark, barY - 1, 1, 7, HackerTheme.TEXT);
        canvas.text(thr + "", bx + bw + 3, barY - 1, tripped ? WARN : HackerTheme.TEXT_DIM);

        canvas.text(be.alert().get() ? "■ ALRT" : "· idle", x, y + 46,
                be.alert().get() ? WARN : HackerTheme.TEXT_DIM);
    }

    /** Bottom-left: entity-count history sparkline. */
    private void renderSparkline(SceneCanvas canvas, net.minecraft.client.gui.Font font) {
        int sx = 6;
        int sy = 97;
        int sw = 88;
        int sh = 8;
        canvas.text("HIST", sx, sy - 8, HackerTheme.TEXT_DIM);
        canvas.fill(sx, sy, sw, sh, BG_PANEL);
        canvas.strokeRect(sx, sy, sw, sh, 0x5535D6D0);
        if (histSize < 2) return;

        int prevX = -1, prevY = -1;
        for (int i = 0; i < histSize; i++) {
            int v = history[(histHead - histSize + i + history.length) % history.length];
            int px = sx + 1 + i * (sw - 2) / Math.max(1, history.length - 1);
            int py = sy + sh - 1 - (int) (Math.min(v, 32) / 32.0 * (sh - 2));
            if (prevX >= 0) {
                canvas.line(prevX, prevY, px, py, HackerTheme.ACCENT_DIM);
            }
            prevX = px;
            prevY = py;
        }
    }

    /** Bottom-right: jittering signal bars. */
    private void renderSignalBars(SceneCanvas canvas, float now) {
        int bars = 11;
        int bw = 4;
        int x = 102;
        int base = H - 5;
        for (int i = 0; i < bars; i++) {
            float amp = (float) (Math.sin(now * 0.3 + i * 0.9) * 0.5 + 0.5);
            int bh = 2 + (int) (amp * 7);
            canvas.fill(x + i * (bw + 2), base - bh, bw, bh,
                    amp > 0.6f ? HackerTheme.ACCENT : HackerTheme.ACCENT_DIM);
        }
        canvas.text("SIG", x, base - 16, HackerTheme.TEXT_DIM);
    }
}
