package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.Checks;
import org.jetbrains.annotations.Nullable;

/**
 * A declarative conversion rule, consumable in bulk via
 * {@link UnitConverter.Builder#rules(java.lang.Iterable)}.
 * Endpoints are wildcard-typed so mixed rule lists work; the builder re-validates
 * same/cross-family constraints at build time.
 */
@NotNullByDefault
public record UnitRule(Unit<?> from, Unit<?> to, Ratio ratio, Kind kind, boolean fixed, @Nullable Namespace material) {

    public enum Kind {
        /**
         * Same-family rule for measure families.
         */
        rule,
        /**
         * Same-family rule for matter families, optionally material specific.
         */
        matter,
        /**
         * Cross-family bridge, optionally material specific.
         */
        bridge
    }

    public static <F> UnitRule rule(Unit<F> from, Unit<F> to, Ratio ratio) {
        return new UnitRule(from, to, ratio, Kind.rule, false, null);
    }

    public static <F> UnitRule fixedRule(Unit<F> from, Unit<F> to, Ratio ratio) {
        return new UnitRule(from, to, ratio, Kind.rule, true, null);
    }

    public static <F> UnitRule matter(Unit<F> from, Unit<F> to, Ratio ratio) {
        return new UnitRule(from, to, ratio, Kind.matter, false, null);
    }

    public static <F> UnitRule matter(Namespace material, Unit<F> from, Unit<F> to, Ratio ratio) {
        return new UnitRule(from, to, ratio, Kind.matter, false, material);
    }

    public static <F1, F2> UnitRule bridge(Unit<F1> from, Unit<F2> to, Ratio ratio) {
        return new UnitRule(from, to, ratio, Kind.bridge, false, null);
    }

    public static <F1, F2> UnitRule bridge(Namespace material, Unit<F1> from, Unit<F2> to, Ratio ratio) {
        return new UnitRule(from, to, ratio, Kind.bridge, false, material);
    }

    /**
     * A bridge that can never be overridden by a later rule for the same pair;
     * used by convention packs whose ratios must not mix silently.
     */
    public static <F1, F2> UnitRule fixedBridge(Unit<F1> from, Unit<F2> to, Ratio ratio) {
        return new UnitRule(from, to, ratio, Kind.bridge, true, null);
    }

    public static <F1, F2> UnitRule fixedBridge(Namespace material, Unit<F1> from, Unit<F2> to, Ratio ratio) {
        return new UnitRule(from, to, ratio, Kind.bridge, true, material);
    }

    public UnitRule {
        Checks.checkNotNull(from, "from");
        Checks.checkNotNull(to, "to");
        Checks.checkNotNull(ratio, "ratio");
        Checks.checkNotNull(kind, "kind");
    }
}
