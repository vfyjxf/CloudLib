package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiVec;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Projector;

import java.util.ArrayList;
import java.util.List;

/** A screen-space panel rect as a leader target — ports at three slots on each of four edges. */
public record HudRect(double x, double y, double width, double height) implements LeaderTarget {

    public HudRect {
        if (width <= 0.0 || height <= 0.0) {
            throw new IllegalArgumentException("rectangle extent");
        }
    }

    @Override
    public boolean world() {
        return false;
    }

    @Override
    public List<GuiVec> polygon(Projector camera) {
        return List.of(
                new GuiVec(x, y),
                new GuiVec(x + width, y),
                new GuiVec(x + width, y + height),
                new GuiVec(x, y + height));
    }

    @Override
    public List<PanelPort> ports(Projector camera, LeaderStyle style) {
        List<PanelPort> ports = new ArrayList<>(12);
        for (int side = 0; side < 4; side++) {
            for (int slot = 0; slot < 3; slot++) {
                double t = new double[] {0.13, 0.5, 0.84}[slot];
                GuiVec point = new GuiVec(
                        x + (side == 0 ? 0.0 : (side == 1 ? width : width * t)),
                        y + (side == 2 ? 0.0 : (side == 3 ? height : height * t)));
                GuiVec outward =
                        switch (side) {
                            case 0 -> new GuiVec(-1.0, 0.0);
                            case 1 -> new GuiVec(1.0, 0.0);
                            case 2 -> new GuiVec(0.0, -1.0);
                            default -> new GuiVec(0.0, 1.0);
                        };
                ports.add(new PanelPort(
                        side, slot, point, point.add(outward.mul(style.terminalLength())), null, null, 0.0, 0.0));
            }
        }
        return ports;
    }
}
