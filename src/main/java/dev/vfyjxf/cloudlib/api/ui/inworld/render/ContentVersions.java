package dev.vfyjxf.cloudlib.api.ui.inworld.render;

import org.jspecify.annotations.Nullable;

import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Adapters producing content-version longs for
 * {@link WorldUiPanel#contentVersion(LongSupplier)} — the dirty protocol that
 * lets a renderer skip a panel's surface repaint while the version (and the
 * surface's shape) are unchanged.
 * <p>
 * The long space is the panel author's to define; these adapters only cover
 * the two common shapes: a constant (content built once, never changing) and
 * an equals-compared object token (a record over the live values the painter
 * reads — the version steps once per semantic change, however often the token
 * supplier runs).
 */
public final class ContentVersions {

    private ContentVersions() {}

    /**
     * A version that never changes — content that is fully built at setup and
     * has no live inputs.
     */
    public static LongSupplier fixed(long version) {
        return () -> version;
    }

    /**
     * Adapts an equals-compared token into a monotonic version: the value
     * steps by one each time {@code token.get()} stops {@code equals}-ing the
     * previous observation and stays put otherwise. Make the token a record
     * over every value the painter reads (health, labels, item lists, …) and
     * repaints track content changes exactly — no hashing, no per-frame cost
     * beyond the token construction and one {@code equals}.
     * <p>
     * The adapter is <b>stateful</b>: the long counts this adapter's own
     * observations. Create it once and keep it bound to the panel for its
     * lifetime — a per-frame (or per-offer) recreated adapter degenerates to
     * a constant and never reports changes. Under a rebuild-every-pass
     * lifecycle (e.g. re-offered profiles), prefer a pure stamp of the values
     * packed into a long instead.
     */
    public static LongSupplier of(Supplier<?> token) {
        return new TokenVersion(token);
    }

    private static final class TokenVersion implements LongSupplier {
        private final Supplier<?> token;
        private @Nullable Object last;
        private long version;

        TokenVersion(Supplier<?> token) {
            this.token = token;
        }

        @Override
        public long getAsLong() {
            Object current = token.get();
            if (current == null) {
                if (last != null) {
                    last = null;
                    version++;
                }
                return version;
            }
            if (!current.equals(last)) {
                last = current;
                version++;
            }
            return version;
        }
    }
}
