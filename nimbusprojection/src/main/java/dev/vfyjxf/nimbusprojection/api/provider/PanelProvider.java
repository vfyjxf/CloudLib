package dev.vfyjxf.nimbusprojection.api.provider;

import dev.vfyjxf.nimbusprojection.api.panel.PanelSpec;

/**
 * Declarative source of in-world panels.
 * <p>
 * Providers are re-evaluated periodically (see
 * {@link dev.vfyjxf.nimbusprojection.api.NimbusClient#registerProvider});
 * every panel the provider wants alive must be re-offered through the sink
 * each pass. Offers are reconciled by {@link PanelSpec#key()}: new keys
 * create panels, missing keys close them, surviving keys keep their widget
 * instance (and its state).
 * <p>
 * Typical uses:
 * <ul>
 *   <li>a panel for every synced block entity within range;</li>
 *   <li>a panel on the block the player is currently looking at
 *       ({@link ProviderContext#crosshairTarget()});</li>
 *   <li>a panel for every entity matching a filter — Jade-style.</li>
 * </ul>
 */
@FunctionalInterface
public interface PanelProvider {

    void provide(ProviderContext context, PanelSink sink);

}
