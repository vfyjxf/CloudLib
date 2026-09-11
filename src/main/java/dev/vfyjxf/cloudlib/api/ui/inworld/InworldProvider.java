package dev.vfyjxf.cloudlib.api.ui.inworld;

/**
 * Declarative source of in-world panels.
 * <p>
 * Providers are re-evaluated periodically (see
 * {@link InworldUiApi#registerProvider}); every panel the provider wants alive
 * must be re-offered through the sink each pass. Offers are reconciled by
 * {@link InworldPanelSpec#key()}: new keys create panels, missing keys close
 * them, surviving keys keep their widget instance (and its state).
 * <p>
 * Typical uses:
 * <ul>
 *   <li>a panel for every synced block entity within range;</li>
 *   <li>a panel on the block the player is currently looking at
 *       ({@link InworldContext#crosshairTarget()}).</li>
 * </ul>
 */
@FunctionalInterface
public interface InworldProvider {

    void provide(InworldContext context, InworldSink sink);

}
