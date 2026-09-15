package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiRect;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiVec;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.HudRegion;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutFrame;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelMemory;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelRequest;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Pose;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Projection;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Space;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Screen-space slot generation for screen panels: the fixed rect when the
 * request pins one, rail slots along viewport edges, the previous rect
 * when it still fits, and free-space slots near the preferred point.
 */
final class ScreenSlots {

    private ScreenSlots() {}

    /**
     * Where a screen panel "wants" to be: the projected pose origin, or —
     * when off-screen or behind the camera — the viewport-edge point in
     * the origin's direction from screen center.
     */
    static GuiVec preferredPoint(LayoutFrame frame, Pose pose) {
        Projection projection = frame.camera().project(pose.origin());
        GuiVec center = new GuiVec(frame.camera().width() / 2.0, frame.camera().height() / 2.0);
        GuiVec direction;
        if (projection.valid()) {
            if (ScreenMath.inViewport(projection.point(), frame.camera(), 18.0)) {
                return projection.point();
            }
            direction = projection.point().sub(center);
        } else {
            Vec3 offset = pose.origin().subtract(frame.camera().eye());
            direction = new GuiVec(
                    offset.dot(frame.viewBasis().right()),
                    -offset.dot(frame.viewBasis().up()));
            if (direction.len() < 0.1) {
                direction = new GuiVec(0.0, 1.0);
            }
        }
        double scale = Math.min(
                (center.x() - 18.0) / Math.max(1.0E-6, Math.abs(direction.x())),
                (center.y() - 18.0) / Math.max(1.0E-6, Math.abs(direction.y())));
        return center.add(direction.mul(scale));
    }

    static List<Entry<String, GuiRect>> slots(
            LayoutFrame frame,
            PanelRequest request,
            Pose pose,
            double width,
            double height,
            @Nullable PanelMemory memory) {
        if (request.fixedScreen() != null) {
            return List.of(Map.entry("screen-fixed", request.fixedScreen()));
        }

        List<Entry<String, GuiRect>> slots = new ArrayList<>();
        double margin = frame.config().margin();
        double viewWidth = frame.camera().width();
        double viewHeight = frame.camera().height();
        double[] railX = {
            margin,
            viewWidth - margin - width,
            (viewWidth - width) * 0.5,
            (viewWidth * 0.5 - width) * 0.5,
            (viewWidth * 1.5 - width) * 0.5
        };
        for (int rail = 0; rail < railX.length; rail++) {
            for (int row = 0; row < 16; row++) {
                double y = margin + row * (height + frame.config().gap());
                if (y + height > viewHeight - margin) {
                    break;
                }
                slots.add(Map.entry("rail:" + rail + ":" + row, new GuiRect(railX[rail], y, width, height)));
            }
        }

        if (memory != null && memory.space() == Space.screen && memory.rect() != null) {
            GuiRect old = memory.rect();
            slots.add(0, Map.entry(memory.slot(), new GuiRect(old.x(), old.y(), width, height)));
        }

        List<List<GuiVec>> obstacles = new ArrayList<>();
        for (HudRegion hud : frame.hud()) obstacles.add(hud.polygon());
        for (PanelRequest other : frame.requests()) {
            if (!other.id().equals(request.id()) && other.space() == Space.screen && other.fixedScreen() != null) {
                obstacles.add(other.fixedScreen().polygon());
            }
        }
        slots.addAll(FreeSpace.slots(
                frame,
                width,
                height,
                preferredPoint(frame, pose),
                obstacles,
                Math.max(24, frame.config().candidatesPerTier() * 4)));
        return slots;
    }
}
