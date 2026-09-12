package dev.vfyjxf.nimbusprojection.api.provider;

/**
 * Registration-time options for a {@link PanelProvider}.
 *
 * @param sharedDomain whether this provider's panel keys form a
 *                     network-meaningful domain: the provider certifies
 *                     that its {@code PanelKey}s encode world identity
 *                     (e.g. {@code "mymod:chest/x,y,z"}), so two clients
 *                     producing the same key are looking at the same
 *                     world object. Presence on shared-domain panels is
 *                     relayed between watchers — other players' operations
 *                     become visible.
 */
public record ProviderOptions(boolean sharedDomain) {

    public static ProviderOptions defaults() {
        return new ProviderOptions(false);
    }

    /** Marks the provider's keys as a network-meaningful domain (see above). */
    public static ProviderOptions shared() {
        return new ProviderOptions(true);
    }

}
