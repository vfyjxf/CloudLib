package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

/**
 * The result of projecting a world point through a {@link Projector}:
 * the gui-px point and its view depth, or {@code valid == false} when
 * the point sits behind the near plane.
 */
public record Projection(GuiVec point, double depth, boolean valid) {}
