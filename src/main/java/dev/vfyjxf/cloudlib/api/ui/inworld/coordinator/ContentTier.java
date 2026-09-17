package dev.vfyjxf.cloudlib.api.ui.inworld.coordinator;

/**
 * How much content an {@link InworldVariant} renders — the information-density
 * dial of the degradation ladder. The constants are declared in degradation
 * order (each tier carries strictly less information than the one before),
 * which is the order {@link VariantLadder} enforces down its rungs:
 * <ul>
 *   <li>{@link #full} — everything: full layout, all rows/sections</li>
 *   <li>{@link #compact} — condensed layout: essential fields only</li>
 *   <li>{@link #labelOnly} — one text line</li>
 *   <li>{@link #iconOnly} — a single icon</li>
 *   <li>{@link #pip} — a minimal dot/badge</li>
 *   <li>{@link #directionalOnly} — no content, just a direction cue (the
 *       off-screen indicator end of the ladder)</li>
 * </ul>
 */
public enum ContentTier {
    full,
    compact,
    labelOnly,
    iconOnly,
    pip,
    directionalOnly
}
