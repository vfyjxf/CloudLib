package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

/**
 * What the solver remembers about a placed panel between frames — slot,
 * tier, pose and rect plus when it last changed and was seen. Stored per
 * request id in {@link LayoutState#panels}.
 */
public record PanelMemory(
        String slot,
        Tier tier,
        Pose pose,
        GuiRect rect,
        Space space,
        double changedAt,
        double seenAt,
        long generation) {}
