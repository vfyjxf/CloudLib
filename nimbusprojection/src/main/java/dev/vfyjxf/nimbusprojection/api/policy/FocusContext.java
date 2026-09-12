package dev.vfyjxf.nimbusprojection.api.policy;

import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanel;
import net.minecraft.world.phys.Vec3;

/**
 * Per-frame focus-scoring input handed to a {@link FocusPolicy}.
 *
 * @param panel       the candidate panel
 * @param eye         camera eye position
 * @param look        normalized camera look direction
 * @param focusPoint  the panel's focus reference point in world space
 *                    (its resolved anchor position)
 * @param angleCos    cos(angle between {@code look} and the eye→focusPoint
 *                    direction) — 1.0 means dead center
 * @param distance    eye→focusPoint distance in blocks
 * @param exactHit    whether the crosshair ray precisely hit the panel's
 *                    anchor block or its presented surface
 * @param incumbent   whether this panel already holds the focus — policies
 *                    may implement their own stickiness, though the runtime
 *                    applies a uniform hysteresis multiplier on top
 * @param partialTick frame interpolation factor
 */
public record FocusContext(
        InworldPanel panel,
        Vec3 eye,
        Vec3 look,
        Vec3 focusPoint,
        double angleCos,
        double distance,
        boolean exactHit,
        boolean incumbent,
        float partialTick) {}
