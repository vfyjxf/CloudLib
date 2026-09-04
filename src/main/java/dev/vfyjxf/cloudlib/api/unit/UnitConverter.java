package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
import dev.vfyjxf.cloudlib.api.unit.exception.NoConversionPathException;
import dev.vfyjxf.cloudlib.api.unit.exception.RuleConflictException;
import dev.vfyjxf.cloudlib.api.unit.text.QuantityFormatter;
import dev.vfyjxf.cloudlib.api.unit.text.UnitNames;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.Checks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Immutable conversion engine. All math is exact, carried on {@link Ratio};
 * conversion paths are resolved by BFS shortest-path over the rule graph and memoized.
 *
 * <p>Built fluently:
 * <pre>{@code
 * UnitConverter converter = UnitConverter.builder()
 *         .add(TimeUnits.pack())
 *         .add(ItemUnits.pack())
 *         .convert(EnergyUnits.eu, EnergyUnits.fe).by(4)                        // 1 eu = 4 fe
 *         .convert(TimeUnits.second, TimeUnits.tick).fixed().by(20)             // fixed: can't be overridden
 *         .convert(ItemUnits.ingot, ItemUnits.block).forMaterial(iron).by(1, 4) // material override
 *         .convert(ItemUnits.ingot, FluidUnits.millibucket).by(144)             // cross-family bridge
 *         .convert(someUnit, otherUnit).byApproximate(0.9)                      // lossy rule
 *         .build();
 * }</pre>
 *
 * <p>Besides the family packs ({@code TimeUnits}, {@code EnergyUnits},
 * {@code FluidUnits}, {@code ItemUnits}), two mutually exclusive mod-convention
 * packs are predefined: {@code TicUnits} (Tinkers' Construct 3, 90 mB per ingot)
 * and {@code GtUnits} (GregTech, 144 L per ingot). Their bridges are fixed, so
 * adding both packs to one converter throws {@link RuleConflictException} instead
 * of silently mixing conventions.
 */
@NotNullByDefault
public final class UnitConverter {

    public static Builder builder() {
        return new Builder();
    }

    private record EdgeKey(Unit<?> from, Unit<?> to, @Nullable Namespace material) {
    }

    private record RuleDef(Ratio ratio, boolean fixed) {
    }

    private record MemoKey(Unit<?> from, Unit<?> to, @Nullable Namespace material) {
    }

    private record SearchNode(Unit<?> unit, Ratio ratio) {
    }

    private final Map<EdgeKey, RuleDef> edges;
    private final Map<UnitFamily<?>, Unit<?>> baseUnits;
    private final Map<MemoKey, Ratio> memo = new ConcurrentHashMap<>();
    private final QuantityFormatter formatter;

    private UnitConverter(Map<EdgeKey, RuleDef> edges, Map<UnitFamily<?>, Unit<?>> baseUnits) {
        this.edges = edges;
        this.baseUnits = baseUnits;
        this.formatter = new QuantityFormatter(this, UnitNames.defaults());
    }

    /**
     * The default formatter backed by {@link UnitNames#defaults()}.
     */
    public QuantityFormatter formatter() {
        return formatter;
    }

    public <F> Quantity<F> quantity(long amount, Unit<F> unit) {
        return Quantity.of(Ratio.of(amount), unit, this);
    }

    public <F> Quantity<F> quantity(Ratio value, Unit<F> unit) {
        return Quantity.of(value, unit, this);
    }

    public <F1, F2> Quantity<F2> convert(long amount, Unit<F1> from, Unit<F2> to) {
        return convert(Ratio.of(amount), from, to, null);
    }

    public <F1, F2> Quantity<F2> convert(long amount, Unit<F1> from, Unit<F2> to, @Nullable Namespace material) {
        return convert(Ratio.of(amount), from, to, material);
    }

    public <F1, F2> Quantity<F2> convert(Ratio value, Unit<F1> from, Unit<F2> to) {
        return convert(value, from, to, null);
    }

    public <F1, F2> Quantity<F2> convert(Ratio value, Unit<F1> from, Unit<F2> to, @Nullable Namespace material) {
        Checks.checkNotNull(value, "value");
        Checks.checkNotNull(from, "from");
        Checks.checkNotNull(to, "to");
        Ratio ratio = resolveRatio(from, to, material);
        return Quantity.of(value.multiply(ratio), to, this);
    }

    /**
     * Normalizes an amount of matter into a {@link MaterialAmount}: converts it to the
     * family's registered base unit (material-specific rules apply) and tags it with the material.
     */
    public <F> MaterialAmount materialAmount(long amount, Unit<F> unit, Namespace material) {
        return materialAmount(Ratio.of(amount), unit, material);
    }

    public <F> MaterialAmount materialAmount(Ratio value, Unit<F> unit, Namespace material) {
        Checks.checkNotNull(value, "value");
        Checks.checkNotNull(unit, "unit");
        Checks.checkNotNull(material, "material");
        Unit<?> base = baseUnits.get(unit.family());
        if (base == null) {
            throw new IllegalStateException("No base unit registered for family " + unit.family().id());
        }
        Ratio baseValue = convert(value, unit, base, material).value();
        return new MaterialAmount(material, baseValue, base, this);
    }

    /**
     * The registered base unit of a matter family, or null if none was registered.
     */
    public @Nullable Unit<?> baseUnit(UnitFamily<?> family) {
        Checks.checkNotNull(family, "family");
        return baseUnits.get(family);
    }

    public boolean canConvert(Unit<?> from, Unit<?> to) {
        return canConvert(from, to, null);
    }

    public boolean canConvert(Unit<?> from, Unit<?> to, @Nullable Namespace material) {
        Checks.checkNotNull(from, "from");
        Checks.checkNotNull(to, "to");
        if (from.equals(to)) return true;
        try {
            resolveRatio(from, to, material);
            return true;
        } catch (NoConversionPathException e) {
            return false;
        }
    }

    /**
     * Resolves the exact ratio such that {@code 1 from = ratio to}.
     */
    public Ratio resolveRatio(Unit<?> from, Unit<?> to, @Nullable Namespace material) {
        if (from.equals(to)) return Ratio.ONE;
        return memo.computeIfAbsent(new MemoKey(from, to, material), key -> bfs(key.from(), key.to(), key.material()));
    }

    private Ratio bfs(Unit<?> from, Unit<?> to, @Nullable Namespace material) {
        Deque<SearchNode> queue = new ArrayDeque<>();
        Set<Unit<?>> visited = new HashSet<>();
        queue.add(new SearchNode(from, Ratio.ONE));
        visited.add(from);
        while (!queue.isEmpty()) {
            SearchNode current = queue.poll();
            // material-specific edges first, then generic ones
            for (boolean specific : new boolean[]{true, false}) {
                for (Map.Entry<EdgeKey, RuleDef> entry : edges.entrySet()) {
                    EdgeKey edge = entry.getKey();
                    if (specific != (edge.material() != null)) continue;
                    if (edge.material() != null && !edge.material().equals(material)) continue;
                    Unit<?> next;
                    Ratio edgeRatio;
                    if (edge.from().equals(current.unit())) {
                        next = edge.to();
                        edgeRatio = entry.getValue().ratio();
                    } else if (edge.to().equals(current.unit())) {
                        next = edge.from();
                        edgeRatio = entry.getValue().ratio().inverse();
                    } else {
                        continue;
                    }
                    Ratio nextRatio = current.ratio().multiply(edgeRatio);
                    if (next.equals(to)) return nextRatio;
                    if (visited.add(next)) {
                        queue.add(new SearchNode(next, nextRatio));
                    }
                }
            }
        }
        throw new NoConversionPathException(from, to);
    }

    public static final class Builder {

        private final Map<EdgeKey, RuleDef> edges = new LinkedHashMap<>();
        private final Map<UnitFamily<?>, Unit<?>> baseUnits = new LinkedHashMap<>();

        private Builder() {
        }

        /**
         * Starts a rule registration: {@code 1 from = ratio to}. At the terminal
         * {@code by*(...)} call the rule is classified: endpoints in the same family
         * become a family rule, endpoints in different families become a bridge.
         */
        public <F1, F2> RuleStep convert(Unit<F1> from, Unit<F2> to) {
            Checks.checkNotNull(from, "from");
            Checks.checkNotNull(to, "to");
            return new RuleStep(from, to);
        }

        /**
         * Registers a predefined bundle: all its rules, then its base unit if present.
         */
        public Builder add(UnitPack pack) {
            Checks.checkNotNull(pack, "pack");
            rules(pack.rules());
            if (pack.baseUnit() != null) {
                baseUnit(pack.baseUnit());
            }
            return this;
        }

        public Builder rules(Iterable<UnitRule> rules) {
            Checks.checkNotNull(rules, "rules");
            for (UnitRule rule : rules) {
                apply(rule);
            }
            return this;
        }

        public Builder rules(UnitRule... rules) {
            return rules(List.of(rules));
        }

        /**
         * Registers the base unit of a matter family, used by {@link MaterialAmount}
         * normalization. One per family: duplicate registration throws
         * {@link RuleConflictException}; a unit of a measure family is rejected.
         */
        public <F> Builder baseUnit(Unit<F> matterUnit) {
            Checks.checkNotNull(matterUnit, "matterUnit");
            Checks.checkArgument(
                    matterUnit.family().isMatter(),
                    "Base unit must belong to a matter family: %s",
                    matterUnit.family().id()
            );
            Unit<?> existing = baseUnits.putIfAbsent(matterUnit.family(), matterUnit);
            if (existing != null) {
                throw new RuleConflictException(
                        "Base unit of family " + matterUnit.family().id() + " already registered: " + existing.id()
                );
            }
            return this;
        }

        public UnitConverter build() {
            return new UnitConverter(
                    Collections.unmodifiableMap(new LinkedHashMap<>(edges)),
                    Collections.unmodifiableMap(new LinkedHashMap<>(baseUnits))
            );
        }

        /**
         * The fluent second stage of {@link #convert(Unit, Unit)}. Modifiers
         * ({@link #fixed()}, {@link #forMaterial(Namespace)}) return the step and are
         * combinable in either order; the {@code by*(...)} terminals finish the
         * registration and return the builder for chaining.
         */
        public final class RuleStep {

            private final Unit<?> from;
            private final Unit<?> to;
            private boolean fixed;
            private @Nullable Namespace material;

            private RuleStep(Unit<?> from, Unit<?> to) {
                this.from = from;
                this.to = to;
            }

            /**
             * The rule can never be overridden by a later rule for the same pair.
             */
            public RuleStep fixed() {
                this.fixed = true;
                return this;
            }

            /**
             * The rule applies only to the given material; wins over the generic rule.
             */
            public RuleStep forMaterial(Namespace material) {
                this.material = Checks.checkNotNull(material, "material");
                return this;
            }

            public Builder by(long ratio) {
                return by(Ratio.of(ratio));
            }

            public Builder by(long numerator, long denominator) {
                return by(Ratio.of(numerator, denominator));
            }

            public Builder by(Ratio ratio) {
                Checks.checkNotNull(ratio, "ratio");
                if (from.family().equals(to.family())) {
                    return putRule(from, to, ratio, fixed, material);
                }
                return putBridge(from, to, ratio, fixed, material);
            }

            public Builder byApproximate(double value) {
                return by(Ratio.approximate(value));
            }

            public Builder byApproximate(long numerator, long denominator) {
                return by(Ratio.approximate(numerator, denominator));
            }
        }

        private void apply(UnitRule rule) {
            switch (rule.kind()) {
                case rule, matter -> putRule(rule.from(), rule.to(), rule.ratio(), rule.fixed(), rule.material());
                case bridge -> putBridge(rule.from(), rule.to(), rule.ratio(), rule.fixed(), rule.material());
            }
        }

        private Builder putRule(Unit<?> from, Unit<?> to, Ratio ratio, boolean fixed, @Nullable Namespace material) {
            checkPositive(ratio);
            Checks.checkArgument(
                    from.family().equals(to.family()),
                    "Rule endpoints must be in the same family: %s vs %s",
                    from.family().id(), to.family().id()
            );
            return put(new EdgeKey(from, to, material), new RuleDef(ratio, fixed));
        }

        private Builder putBridge(Unit<?> from, Unit<?> to, Ratio ratio, boolean fixed, @Nullable Namespace material) {
            checkPositive(ratio);
            Checks.checkArgument(
                    !from.family().equals(to.family()),
                    "Bridge endpoints must be in different families: %s",
                    from.family().id()
            );
            return put(new EdgeKey(from, to, material), new RuleDef(ratio, fixed));
        }

        private Builder put(EdgeKey key, RuleDef def) {
            RuleDef existing = edges.get(key);
            if (existing != null) {
                if (existing.fixed()) {
                    throw new RuleConflictException("Fixed rule " + key.from() + " -> " + key.to() + " can't be overridden");
                }
                if (existing.ratio().equals(def.ratio())) {
                    throw new RuleConflictException("Duplicate rule " + key.from() + " -> " + key.to() + " with ratio " + def.ratio());
                }
                edges.put(key, def);
                return this;
            }
            EdgeKey reverse = new EdgeKey(key.to(), key.from(), key.material());
            RuleDef existingReverse = edges.get(reverse);
            if (existingReverse != null) {
                if (existingReverse.fixed()) {
                    throw new RuleConflictException("Fixed rule " + reverse.from() + " -> " + reverse.to() + " can't be overridden");
                }
                if (existingReverse.ratio().equals(def.ratio().inverse())) {
                    throw new RuleConflictException("Duplicate rule " + key.from() + " -> " + key.to() + " (already defined in reverse)");
                }
                edges.remove(reverse);
            }
            edges.put(key, def);
            return this;
        }

        private static void checkPositive(Ratio ratio) {
            Checks.checkNotNull(ratio, "ratio");
            Checks.checkArgument(ratio.signum() > 0, "Rule ratio must be positive: %s", ratio);
        }
    }
}
