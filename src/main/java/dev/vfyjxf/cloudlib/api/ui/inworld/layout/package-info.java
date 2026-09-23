/**
 * Layout contracts — resolving where an in-world UI surface lands for a frame.
 * <p>
 * <strong>The public API (plan §7, option B — the pipeline-aspect
 * surface).</strong> An element declares its layout behavior as seven
 * immutable facets on an {@link dev.vfyjxf.cloudlib.api.ui.inworld.layout.ElementSpec}
 * — anchor, orientation, spaces, avoidance, stability, degrade, group —
 * bound to one closed {@link dev.vfyjxf.cloudlib.api.ui.inworld.layout.InworldProfile}
 * (the algorithm combination). The
 * {@link dev.vfyjxf.cloudlib.api.ui.inworld.layout.PipelineAssembler}
 * assembles the spec into an element the
 * {@link dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.InworldCoordinator}
 * drives through the stages ANCHOR → PROJECT → CANDIDATES → AVOID → RANK →
 * ARBITRATE → STABILIZE → COMMIT. Stage behavior comes from named
 * strategies registered in
 * {@link dev.vfyjxf.cloudlib.api.ui.inworld.layout.StageCatalogs} — third
 * parties register named strategies, they never touch the pipeline.
 * Scenarios the facets cannot express embed an
 * {@link dev.vfyjxf.cloudlib.api.ui.inworld.layout.InworldLayouter}
 * escape hatch through {@code ElementSpec.custom(...)}.
 * <p>
 * Pure logic: no Minecraft types, no clock — headless-testable. Illegal
 * facet combinations are rejected at construction
 * ({@link dev.vfyjxf.cloudlib.api.ui.inworld.layout.FacetRules} documents
 * the table). The 22-class type-catalog mapping lives in
 * {@code docs/inworld-type-catalog-mapping.md}.
 */
@org.jspecify.annotations.NullMarked
package dev.vfyjxf.cloudlib.api.ui.inworld.layout;
