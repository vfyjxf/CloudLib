package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiVec;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Projector;

import java.util.List;

/**
 * What a leader line lands on: the panel's screen polygon plus the edge
 * ports it may attach to, in either world or screen space.
 */
public interface LeaderTarget {

    List<GuiVec> polygon(Projector camera);

    List<PanelPort> ports(Projector camera, LeaderStyle style);

    boolean world();
}
