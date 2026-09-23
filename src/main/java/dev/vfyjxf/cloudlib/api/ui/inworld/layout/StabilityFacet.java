package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.stability.SwitchGate;
import dev.vfyjxf.cloudlib.api.ui.inworld.stability.VisibilityTracker;

import java.util.Objects;

/**
 * The stability facet (G7/G14): the element's declared stability posture —
 * the three-part gate parameters for discrete switches, the follow spring
 * and FLIP morph rates for continuous motion, the linger/fade budget for
 * appearance, and whether the element arbitrates sticky (prefers its
 * incumbent placement).
 * <p>
 * Where each part acts: {@code stickySlot} wires directly into the
 * coordinator's sticky arbitration and the pipeline's incumbent-first
 * ranking; the gate/spring/FLIP/linger values are the per-element posture
 * the render adapter smooths with ({@code SwitchGate}/{@code Spring2}/
 * {@code FlipPlanner}/{@code VisibilityTracker} are all pure and
 * constructible from these numbers) — the coordinator itself runs one
 * global stability config, and the posture here is the per-element
 * declaration the adapter applies on top of the committed placements.
 *
 * @param switchGate the discrete-switch triple gate (hysteresis band,
 *        dwell frames, dead zone)
 * @param followSpringOmega the critically damped follow spring's stiffness
 * @param flipSpeedPixelsPerSecond the constant-speed FLIP morph rate for
 *        discrete rect changes
 * @param visibility the linger/fade budget (appear, fade-out, linger)
 * @param stickySlot whether the element prefers its incumbent placement
 *        when arbitrating
 */
public record StabilityFacet(
    SwitchGate.Config switchGate,
    double followSpringOmega,
    double flipSpeedPixelsPerSecond,
    VisibilityTracker.Config visibility,
    boolean stickySlot
) {

    public StabilityFacet {
        Objects.requireNonNull(switchGate, "switchGate");
        Objects.requireNonNull(visibility, "visibility");
        if (!Double.isFinite(followSpringOmega) || followSpringOmega <= 0) {
            throw new IllegalArgumentException("followSpringOmega must be finite and positive: " + followSpringOmega);
        }
        if (!Double.isFinite(flipSpeedPixelsPerSecond) || flipSpeedPixelsPerSecond <= 0) {
            throw new IllegalArgumentException(
                "flipSpeedPixelsPerSecond must be finite and positive: " + flipSpeedPixelsPerSecond
            );
        }
    }

    /** The WoW-nameplate baseline: snappy spring, sticky, generous linger. */
    public static StabilityFacet nameplateBaseline() {
        return new StabilityFacet(
            SwitchGate.Config.of(16.0, 1, 2.0, 0),
            30.0,
            900.0,
            VisibilityTracker.Config.of(0.15, 0.25, 0.25),
            true
        );
    }

    /** The transient baseline: no stickiness, quick fades, near-open gate. */
    public static StabilityFacet transientBaseline() {
        return new StabilityFacet(
            SwitchGate.Config.of(2.0, 1, 0.0, 0),
            60.0,
            1400.0,
            VisibilityTracker.Config.of(0.05, 0.15, 0.10),
            false
        );
    }

    /** The fixed-panel baseline: dwell-heavy gate, calm spring, sticky. */
    public static StabilityFacet fixedBaseline() {
        return new StabilityFacet(
            SwitchGate.Config.of(24.0, 2, 4.0, 1),
            20.0,
            700.0,
            VisibilityTracker.Config.of(0.20, 0.25, 0.40),
            true
        );
    }
}
