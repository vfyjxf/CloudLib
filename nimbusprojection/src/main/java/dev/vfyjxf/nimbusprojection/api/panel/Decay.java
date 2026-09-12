package dev.vfyjxf.nimbusprojection.api.panel;

/**
 * Transient panel lifecycle — "live for a while, then fade and die".
 * <p>
 * The TTL counts from <b>creation</b>: provider re-offers do not extend it
 * (a panel meant to persist simply keeps being offered and never declares
 * a decay). Typical use is imperative toasts and operation feedback.
 *
 * @param ttlTicks      lifetime in client ticks before the panel expires
 * @param fadeTicks     ticks spent fading out before expiry — the last
 *                      {@code fadeTicks} of the TTL ease the panel out
 *                      instead of vanishing hard
 * @param lingerOnHover pause the countdown while the panel is hovered or
 *                      engaged — a player actively looking keeps it alive
 */
public record Decay(int ttlTicks, int fadeTicks, boolean lingerOnHover) {

    public Decay {
        if (ttlTicks <= 0) throw new IllegalArgumentException("ttlTicks must be > 0");
        if (fadeTicks < 0) throw new IllegalArgumentException("fadeTicks must be >= 0");
    }

    /** Dies after {@code ttlTicks} with a 10-tick fade, lingering on hover. */
    public static Decay after(int ttlTicks) {
        return new Decay(ttlTicks, 10, true);
    }

    /** Hard disappearance after {@code ttlTicks} — no fade, no linger. */
    public static Decay hard(int ttlTicks) {
        return new Decay(ttlTicks, 0, false);
    }

}
