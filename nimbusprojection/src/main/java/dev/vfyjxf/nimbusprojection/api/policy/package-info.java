/**
 * Spec-level policy SPIs — open hooks a panel declares to override runtime
 * defaults without replacing the shared interaction model.
 * <ul>
 *   <li>{@link FocusPolicy} — scores the panel's focus candidacy per frame
 *       (the runtime still owns selection and hysteresis).</li>
 *   <li>{@link SuspendPolicy} — per-tick live/suspend/close verdict.</li>
 * </ul>
 */
package dev.vfyjxf.nimbusprojection.api.policy;
