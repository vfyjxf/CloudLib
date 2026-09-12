package dev.vfyjxf.nimbusprojection.api.presentation;

/**
 * How a presentation participates in inspect mode (the hold-key flat
 * projection).
 */
public enum InspectPolicy {

    /** Flatten to a screen rect via {@link PresentationDriver#flatten} (default). */
    FLATTEN,

    /** Not shown in inspect mode at all. */
    HIDDEN,

    /**
     * The driver renders its own inspect-mode form — the runtime skips the
     * flat rect entirely for this panel.
     */
    CUSTOM,

}
