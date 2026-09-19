/**
 * StabilityKit — the anti-flicker toolkit of the inworld layout system (G7/G14):
 * everything a low-frequency discrete decision needs so that, once it reaches
 * the screen, it does not look like flicker.
 * <ul>
 *   <li>{@link Smoothing} — frame-rate-independent exponential damping of
 *       continuous quantities</li>
 *   <li>{@link Spring2} — critically damped 2D spring for following moving
 *       anchors and being pushed back into place</li>
 *   <li>{@link SwitchGate} — the triple gate (hysteresis band + dwell frames +
 *       dead-zone pixels, plus a minimum hold) that suppresses ping-ponging on
 *       discrete switches such as slots, corners, visibility and modes</li>
 *   <li>{@link FlipPlanner} — constant-speed FLIP morphs for discrete rect
 *       changes</li>
 *   <li>{@link VisibilityTracker} — the appear/linger/fade state machine that
 *       keeps rejected elements on screen for one decision epoch instead of
 *       blinking them away</li>
 *   <li>{@link OcclusionFade} — the occlusion-fade policy value object
 *       (hysteresis thresholds + frame gate) plus its drive constants and
 *       easing helpers</li>
 *   <li>{@link OneEuroFilter} — the speed-adaptive low-pass (Casiez CHI 2012,
 *       plus a cutoff ceiling) for noisy per-frame screen positions</li>
 *   <li>{@link PixelStabilizer} — the rest/move pixel-space quantizer with
 *       hysteresis snapping, dwell and an ease-out landing that keeps
 *       quasi-static panels on a locked integer lattice</li>
 *   <li>{@link FollowStabilizer} — the facade wiring two 1€ filters into a
 *       pixel stabilizer: the follow-panel screen-position stabilizer</li>
 * </ul>
 * <p>
 * Two rules hold for the whole package. First, time is always an explicit
 * parameter — a dt on {@link Smoothing}/{@link Spring2}, an absolute time on
 * {@link FlipPlanner}/{@link VisibilityTracker}, a decision tick on
 * {@link SwitchGate} — never a wall clock or an MC singleton; everything here
 * is pure logic on plain values and is headless-testable. Second, decision
 * frequency is not frame rate: the discrete tools run on decision ticks or
 * epochs, the continuous tools merely consume whatever dt the frame hands
 * them, and both stay correct when sampling rates change.
 */
@dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault
package dev.vfyjxf.cloudlib.api.ui.inworld.stability;
