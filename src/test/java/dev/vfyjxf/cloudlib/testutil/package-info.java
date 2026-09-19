/**
 * Headless test infrastructure for the inworld layout system: geometry
 * assertions with epsilon, a manually advanced frame clock, a frame-sequence
 * replay harness, a programmable {@link dev.vfyjxf.cloudlib.api.ui.inworld.Projection}
 * factory, deterministic LCG noise and the perception-derived acceptance
 * assertions for screen-space stability. Nothing here may touch Minecraft
 * client singletons.
 */
package dev.vfyjxf.cloudlib.testutil;
