/**
 * The multi-algorithm layout layer of the inworld coordination system (§3.0):
 * the discrete puzzle pieces that turn "where is there room?" into stable
 * assignments. Each class is one independently testable pure-logic component —
 * no Minecraft types, no wall clock, no randomness — combinable only through
 * the closed {@link AlgorithmProfile} enum.
 * <ul>
 *   <li>{@link DockCursor} — the one-dimensional cursor that manages screen-edge
 *       dock slots (allocation, release, order-preserving compaction)</li>
 *   <li>{@link SlotAssigner} — online slot assignment with a per-epoch recourse
 *       budget: at most {@code K} deliberate relocations, {@code K = 0} being
 *       pure stickiness</li>
 *   <li>{@link OrbitRing} — ring-shaped candidate slots around a world anchor,
 *       freed arcs subtracted via {@link dev.vfyjxf.cloudlib.api.ui.inworld.space.RayFan},
 *       expanding outward ring by ring as capacity runs out</li>
 *   <li>{@link LeaderRouter} — leader-line routing with the s-leader →
 *       po-leader upgrade on crossings and shared-trunk hyperleaders for
 *       clusters, gated against ping-ponging</li>
 *   <li>{@link AngleEncoder} — the ANGLE encoding for off-screen indicators:
 *       the indicator slides around all four screen edges, its position is the
 *       target's angle (Lin et al.'s user-tested optimum)</li>
 *   <li>{@link Clusterer} — evolutionary clustering,
 *       {@code score = α·snapshotQuality + (1−α)·historyConsistency}, with the
 *       merge/split hysteresis that formula buys</li>
 *   <li>{@link ExcentricColumn} — the Y-consistent excentric labeling variant:
 *       near-crosshair labels stack in one aligned column beside the focus</li>
 * </ul>
 * <p>
 * Three rules hold for the whole package, mirroring the package's iron rules.
 * First, it is pure logic: the only inputs are plain values and explicit time
 * parameters (a dt for smoothing, a decision tick for gates) — everything is
 * headless-testable. Second, algorithm combinations are never freely assembled
 * by callers: {@link AlgorithmProfile} is the closed enumeration that binds an
 * algorithm combination with its default parameters, and adding a combination
 * means adding a profile constant. Third, decision frequency is not frame
 * rate: discrete decisions run on epochs, continuous smoothing merely consumes
 * the dt the frame hands it.
 */
@dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault
package dev.vfyjxf.cloudlib.api.ui.inworld.algorithm;
