package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiVec;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Projection;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Projector;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * A world-space panel quad as a leader target. Ports sit on the quad
 * edges; each port's gate is pushed out along the edge normal in world
 * space, then re-projected — the world length is interpolated so the
 * gate's screen distance equals {@link LeaderStyle#terminalLength()}.
 */
public record WorldRect(Vec3 center, Vec3 right, Vec3 up, double width, double height) implements LeaderTarget {

    public WorldRect {
        right = Vecs.unit(right);
        up = Vecs.unit(up);
        Vecs.finite(center);
        if (width <= 0.0 || height <= 0.0 || Math.abs(right.dot(up)) > 1.0E-6) {
            throw new IllegalArgumentException("orthogonal rectangle axes");
        }
    }

    /** The world-space point at normalized quad coordinates {@code (u, v)}. */
    public Vec3 at(double u, double v) {
        return center.add(right.scale((u - 0.5) * width)).add(up.scale((0.5 - v) * height));
    }

    public List<Vec3> corners() {
        return List.of(at(0.0, 0.0), at(1.0, 0.0), at(1.0, 1.0), at(0.0, 1.0));
    }

    @Override
    public List<GuiVec> polygon(Projector camera) {
        return ScreenMath.projectAll(camera, corners());
    }

    @Override
    public boolean world() {
        return true;
    }

    @Override
    public List<PanelPort> ports(Projector camera, LeaderStyle style) {
        List<PanelPort> ports = new ArrayList<>(12);
        for (int side = 0; side < 4; side++) {
            for (int slot = 0; slot < 3; slot++) {
                double t = new double[] {0.13, 0.5, 0.84}[slot];
                double u = side == 0 ? 0.0 : (side == 1 ? 1.0 : t);
                double v = side == 2 ? 0.0 : (side == 3 ? 1.0 : t);
                Vec3 worldPoint = at(u, v);
                Vec3 outward =
                        switch (side) {
                            case 0 -> right.scale(-1.0);
                            case 1 -> right;
                            case 2 -> up;
                            default -> up.scale(-1.0);
                        };
                Projection point = camera.project(worldPoint);
                Vec3 nearGate = worldPoint.add(outward.scale(0.08));
                Projection gate = camera.project(nearGate);
                if (point.valid() && gate.valid()) {
                    GuiVec direction = gate.point().sub(point.point()).unit();
                    if (!(direction.len() < 0.5)) {
                        GuiVec gatePoint = point.point().add(direction.mul(style.terminalLength()));
                        double depthDelta = gate.depth() - point.depth();
                        double ratio = point.point().distance(gatePoint)
                                / point.point().distance(gate.point());
                        double gateDepth = gate.depth() - ratio * depthDelta;
                        if (!(gateDepth <= 1.0E-8)) {
                            double worldLength = 0.08 * ratio * point.depth() / gateDepth;
                            if (!(worldLength <= 0.0) && !(worldLength > Math.max(width, height) * 2.0)) {
                                Vec3 worldGate = worldPoint.add(outward.scale(worldLength));
                                Projection projected = camera.project(worldGate);
                                if (projected.valid()) {
                                    ports.add(new PanelPort(
                                            side,
                                            slot,
                                            point.point(),
                                            projected.point(),
                                            worldPoint,
                                            worldGate,
                                            point.depth(),
                                            projected.depth()));
                                }
                            }
                        }
                    }
                }
            }
        }
        return ports;
    }
}
