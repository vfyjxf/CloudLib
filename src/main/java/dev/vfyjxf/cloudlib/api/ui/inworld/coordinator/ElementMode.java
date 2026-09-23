package dev.vfyjxf.cloudlib.api.ui.inworld.coordinator;

/**
 * How the coordinator treats an {@link InworldElement}:
 * <ul>
 *   <li>{@link #arbitrated} — the element proposes candidate placements and
 *       the coordinator arbitrates: candidates that fit are granted, rejected
 *       proposals renegotiate down the element's
 *       {@link VariantLadder degradation ladder}</li>
 *   <li>{@link #selfManaged} — the element brings its own layout: it proposes
 *       exactly one authoritative candidate, the coordinator only registers
 *       its occupancy and rejects it on conflict. Self-managed elements are
 *       never renegotiated — handling a rejection (or not) is entirely the
 *       element's business</li>
 * </ul>
 */
public enum ElementMode {
    arbitrated, selfManaged
}
