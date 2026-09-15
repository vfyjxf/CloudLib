package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import java.util.List;

/**
 * The chosen placement for one visual: which requests it represents
 * (a merged panel lists all members, {@code active} is the displayed one),
 * its tier and space, the world pose and extent for world panels, and the
 * projected screen polygon every panel carries.
 * <p>
 * {@code screenRect} is the axis-aligned bounds of {@code polygon} for
 * world panels and the actual rect for screen panels.
 */
public record PanelPlacement(
        String visualId,
        List<PanelRequest> members,
        PanelRequest active,
        SourceSnapshot source,
        String slot,
        Tier tier,
        Space space,
        Pose pose,
        double worldWidth,
        double worldHeight,
        GuiRect screenRect,
        List<GuiVec> polygon,
        double score,
        boolean merged) {

    public PanelPlacement {
        members = List.copyOf(members);
        polygon = List.copyOf(polygon);
    }

    /** The centroid of the projected polygon — the panel's anchor point in gui px. */
    public GuiVec center() {
        GuiVec sum = GuiVec.zero;
        for (GuiVec vertex : polygon) {
            sum = sum.add(vertex);
        }
        return sum.mul(1.0 / polygon.size());
    }
}
