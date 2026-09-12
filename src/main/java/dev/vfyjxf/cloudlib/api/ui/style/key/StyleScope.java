package dev.vfyjxf.cloudlib.api.ui.style.key;

/**
 * Which style slot a {@link StyleKey} feeds when applied.
 */
public enum StyleScope {

    /** Writes into the taffy layout style ({@code StyleContext.layoutStyle()}). */
    layout,

    /** Writes into the visual context ({@code StyleContext.visualContext()}). */
    visual,

    /**
     * Neither layout nor visual — the value is stored on the context for later
     * retrieval ({@code StyleContext.get(key)}) and drives no subsystem directly.
     */
    custom,
}
