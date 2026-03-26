package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.unit.conversion.*;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class UnitConverter {

    //region Internal key types

    private record UnitPairKey(Unit<?> from, Unit<?> to) {
    }

    private record UnitIdPairKey(Namespace fromUnitId, Namespace toUnitId) {
    }

    private record MaterialUnitIdPairKey(Namespace materialId, Namespace fromUnitId, Namespace toUnitId) {
    }

    private record DomainUnitIdPairKey(String domain, Namespace fromUnitId, Namespace toUnitId) {
    }

    record ResolvedRatio(double ratio, @Nullable ExactRatio exactRatio) {
        static ResolvedRatio approximate(double ratio) {
            return new ResolvedRatio(ratio, null);
        }

        static ResolvedRatio exact(ExactRatio exactRatio) {
            return new ResolvedRatio(exactRatio.toDouble(), exactRatio);
        }

        static ResolvedRatio of(double ratio, @Nullable ExactRatio exactRatio) {
            return new ResolvedRatio(ratio, exactRatio);
        }

        ResolvedRatio inverse() {
            return new ResolvedRatio(1.0 / ratio, exactRatio != null ? exactRatio.inverse() : null);
        }

        ResolvedRatio multiply(ResolvedRatio other) {
            @Nullable ExactRatio combined = (this.exactRatio != null && other.exactRatio != null)
                    ? this.exactRatio.multiply(other.exactRatio) : null;
            return new ResolvedRatio(this.ratio * other.ratio, combined);
        }
    }

    //endregion

    //region Frozen data

    private final Map<UnitPairKey, List<FamilyRule<?>>> familyRules;
    private final Map<UnitPairKey, List<BridgeRule<?, ?>>> bridgeRules;
    private final Map<UnitIdPairKey, FallbackRule> fallbackRules;
    private final Map<UnitIdPairKey, TemplateRule> templateRules;
    private final Map<MaterialUnitIdPairKey, MaterialRule> materialRules;
    private final Map<MaterialUnitIdPairKey, ObjectRule> objectRules;
    private final Map<DomainUnitIdPairKey, DomainRule> domainRules;
    private final Set<Namespace> allUnitIds;

    private UnitConverter(
            Map<UnitPairKey, List<FamilyRule<?>>> familyRules,
            Map<UnitPairKey, List<BridgeRule<?, ?>>> bridgeRules,
            Map<UnitIdPairKey, FallbackRule> fallbackRules,
            Map<UnitIdPairKey, TemplateRule> templateRules,
            Map<MaterialUnitIdPairKey, MaterialRule> materialRules,
            Map<MaterialUnitIdPairKey, ObjectRule> objectRules,
            Map<DomainUnitIdPairKey, DomainRule> domainRules,
            Set<Namespace> allUnitIds
    ) {
        this.familyRules = familyRules;
        this.bridgeRules = bridgeRules;
        this.fallbackRules = fallbackRules;
        this.templateRules = templateRules;
        this.materialRules = materialRules;
        this.objectRules = objectRules;
        this.domainRules = domainRules;
        this.allUnitIds = allUnitIds;
    }

    //endregion

    //region Build from ConversionSchema

    static UnitConverter build(ConversionSchema mutable) {
        // --- Combine family rules: defaults + user ---
        List<FamilyRule<?>> allFamilyRules = new ArrayList<>();
        if (mutable.useDefaults()) {
            List<FamilyRule<?>> defaults = Units.defaultRules();
            // Validate: user rules cannot contradict fixed default rules
            for (FamilyRule<?> userRule : mutable.familyRules()) {
                for (FamilyRule<?> defRule : defaults) {
                    if (defRule.fixed()
                            && userRule.from().equals(defRule.from())
                            && userRule.to().equals(defRule.to())
                            && Math.abs(userRule.ratio() - defRule.ratio()) > 1e-9) {
                        throw new InvalidRuleException(
                                "Fixed rule " + defRule.from() + " -> " + defRule.to() +
                                        " is fixed at " + defRule.ratio() + ", cannot override with " + userRule.ratio()
                        );
                    }
                }
            }
            // Add defaults that are not overridden by user rules (same pair + specificity)
            for (FamilyRule<?> defRule : defaults) {
                boolean overridden = false;
                for (FamilyRule<?> userRule : mutable.familyRules()) {
                    if (userRule.from().equals(defRule.from())
                            && userRule.to().equals(defRule.to())
                            && userRule.specificity() == defRule.specificity()) {
                        overridden = true;
                        break;
                    }
                }
                if (!overridden) {
                    allFamilyRules.add(defRule);
                }
            }
        }
        allFamilyRules.addAll(mutable.familyRules());

        // --- Family rules: group by pair, detect conflicts ---
        Map<UnitPairKey, List<FamilyRule<?>>> familyMap = new HashMap<>();
        for (FamilyRule<?> rule : allFamilyRules) {
            UnitPairKey key = new UnitPairKey(rule.from(), rule.to());
            familyMap.computeIfAbsent(key, k -> new ArrayList<>()).add(rule);
        }
        for (var entry : familyMap.entrySet()) {
            List<FamilyRule<?>> rules = entry.getValue();
            Set<Integer> seenSpecificities = new HashSet<>();
            for (FamilyRule<?> rule : rules) {
                if (!seenSpecificities.add(rule.specificity())) {
                    throw new RuleConflictException(
                            "Family rule conflict at same specificity: " + entry.getKey().from() + " -> " + entry.getKey().to()
                    );
                }
            }
            rules.sort(Comparator.<FamilyRule<?>>comparingInt(FamilyRule::specificity).reversed());
        }

        // --- Bridge rules: group by pair, detect conflicts with same material + context ---
        Map<UnitPairKey, List<BridgeRule<?, ?>>> bridgeMap = new HashMap<>();
        for (BridgeRule<?, ?> rule : mutable.bridgeRules()) {
            UnitPairKey key = new UnitPairKey(rule.from(), rule.to());
            List<BridgeRule<?, ?>> existing = bridgeMap.computeIfAbsent(key, k -> new ArrayList<>());
            for (BridgeRule<?, ?> prev : existing) {
                if (Objects.equals(prev.requiredMaterialId(), rule.requiredMaterialId())
                        && prev.requiredContext().equals(rule.requiredContext())) {
                    throw new RuleConflictException(
                            "Bridge rule conflict (same material + context): " + rule.from() + " -> " + rule.to()
                    );
                }
            }
            existing.add(rule);
        }
        for (var entry : bridgeMap.entrySet()) {
            entry.getValue().sort(Comparator.<BridgeRule<?, ?>>comparingInt(BridgeRule::specificity).reversed());
        }

        // --- Object rules: detect conflicts ---
        Map<MaterialUnitIdPairKey, ObjectRule> objMap = new HashMap<>();
        for (ObjectRule rule : mutable.objectRules()) {
            MaterialUnitIdPairKey key = new MaterialUnitIdPairKey(rule.objectId(), rule.fromUnitId(), rule.toUnitId());
            if (objMap.put(key, rule) != null) {
                throw new RuleConflictException("Object rule conflict: " + rule.objectId() + " " + rule.fromUnitId() + " -> " + rule.toUnitId());
            }
        }

        // --- Material rules: detect conflicts ---
        Map<MaterialUnitIdPairKey, MaterialRule> matMap = new HashMap<>();
        for (MaterialRule rule : mutable.materialRules()) {
            MaterialUnitIdPairKey key = new MaterialUnitIdPairKey(rule.materialId(), rule.fromUnitId(), rule.toUnitId());
            if (matMap.put(key, rule) != null) {
                throw new RuleConflictException("Material rule conflict: " + rule.materialId() + " " + rule.fromUnitId() + " -> " + rule.toUnitId());
            }
        }

        // --- Template rules: detect conflicts ---
        Map<UnitIdPairKey, TemplateRule> tmplMap = new HashMap<>();
        for (TemplateRule rule : mutable.templateRules()) {
            UnitIdPairKey key = new UnitIdPairKey(rule.fromUnitId(), rule.toUnitId());
            if (tmplMap.put(key, rule) != null) {
                throw new RuleConflictException("Template rule conflict: " + rule.fromUnitId() + " -> " + rule.toUnitId());
            }
        }

        // --- Fallback rules: detect conflicts ---
        Map<UnitIdPairKey, FallbackRule> fbMap = new HashMap<>();
        for (FallbackRule rule : mutable.fallbackRules()) {
            UnitIdPairKey key = new UnitIdPairKey(rule.fromUnitId(), rule.toUnitId());
            if (fbMap.put(key, rule) != null) {
                throw new RuleConflictException("Fallback rule conflict: " + rule.fromUnitId() + " -> " + rule.toUnitId());
            }
        }

        // --- Domain rules: detect conflicts ---
        Map<DomainUnitIdPairKey, DomainRule> domMap = new HashMap<>();
        for (DomainRule rule : mutable.domainRules()) {
            DomainUnitIdPairKey key = new DomainUnitIdPairKey(rule.domain(), rule.fromUnitId(), rule.toUnitId());
            if (domMap.put(key, rule) != null) {
                throw new RuleConflictException("Domain rule conflict: " + rule.domain() + " " + rule.fromUnitId() + " -> " + rule.toUnitId());
            }
        }

        // --- Collect all known unit IDs for auto-derivation ---
        Set<Namespace> allUnitIds = new HashSet<>();
        for (ObjectRule r : objMap.values()) {
            allUnitIds.add(r.fromUnitId());
            allUnitIds.add(r.toUnitId());
        }
        for (MaterialRule r : matMap.values()) {
            allUnitIds.add(r.fromUnitId());
            allUnitIds.add(r.toUnitId());
        }
        for (DomainRule r : domMap.values()) {
            allUnitIds.add(r.fromUnitId());
            allUnitIds.add(r.toUnitId());
        }
        for (TemplateRule r : tmplMap.values()) {
            allUnitIds.add(r.fromUnitId());
            allUnitIds.add(r.toUnitId());
        }
        for (FallbackRule r : fbMap.values()) {
            allUnitIds.add(r.fromUnitId());
            allUnitIds.add(r.toUnitId());
        }
        // Also collect unit IDs from family rules for transitive derivation
        for (var entry : familyMap.entrySet()) {
            allUnitIds.add(entry.getKey().from().id());
            allUnitIds.add(entry.getKey().to().id());
        }

        Map<UnitPairKey, List<FamilyRule<?>>> immutableFamilyMap = new HashMap<>();
        for (var entry : familyMap.entrySet()) {
            immutableFamilyMap.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        Map<UnitPairKey, List<BridgeRule<?, ?>>> immutableBridgeMap = new HashMap<>();
        for (var entry : bridgeMap.entrySet()) {
            immutableBridgeMap.put(entry.getKey(), List.copyOf(entry.getValue()));
        }

        return new UnitConverter(
                Collections.unmodifiableMap(immutableFamilyMap),
                Collections.unmodifiableMap(immutableBridgeMap),
                Map.copyOf(fbMap),
                Map.copyOf(tmplMap),
                Map.copyOf(matMap),
                Map.copyOf(objMap),
                Map.copyOf(domMap),
                Set.copyOf(allUnitIds)
        );
    }

    //endregion

    //region Public conversion API

    // --- convert ---

    public <F> double convert(double amount, Unit<F> from, Unit<F> to) {
        return convert(amount, from, to, null, ConvertContext.empty(), ConversionMode.exactFirstThenApprox);
    }

    public <F> double convert(double amount, Unit<F> from, Unit<F> to, Namespace material) {
        return convert(amount, from, to, material, ConvertContext.empty(), ConversionMode.exactFirstThenApprox);
    }

    public <F> double convert(double amount, Unit<F> from, Unit<F> to, ConversionMode mode) {
        return convert(amount, from, to, null, ConvertContext.empty(), mode);
    }

    public <F> double convert(double amount, Unit<F> from, Unit<F> to, @Nullable Namespace material, ConvertContext context) {
        return convert(amount, from, to, material, context, ConversionMode.exactFirstThenApprox);
    }

    public <F> double convert(double amount, Unit<F> from, Unit<F> to,
                              @Nullable Namespace material, ConvertContext context, ConversionMode mode) {
        if (from.equals(to)) return amount;

        ResolvedRatio resolved = resolveFullRatio(from, to, material, context);
        if (resolved == null) {
            throw new NoRuleMatchedException("No conversion rule: " + from + " -> " + to);
        }

        return switch (mode) {
            case exactFirstThenApprox -> amount * resolved.ratio();
            case exactOnly -> {
                if (resolved.exactRatio() == null) {
                    throw new InexactResultException(
                            "No exact ratio path: " + from + " -> " + to + " (approximate ratio=" + resolved.ratio() + ")"
                    );
                }
                yield amount * resolved.ratio();
            }
            case approximateOnly -> amount * resolved.ratio();
        };
    }

    // --- canConvert ---

    public <F> boolean canConvert(Unit<F> from, Unit<F> to) {
        return canConvert(from, to, null, ConvertContext.empty());
    }

    public <F> boolean canConvert(Unit<F> from, Unit<F> to, Namespace material) {
        return canConvert(from, to, material, ConvertContext.empty());
    }

    public <F> boolean canConvert(Unit<F> from, Unit<F> to, @Nullable Namespace material, ConvertContext context) {
        if (from.equals(to)) return true;
        return resolveFullRatio(from, to, material, context) != null;
    }

    // --- convertDiscrete (strict) ---

    public <F> long convertDiscrete(long amount, Unit<F> from, Unit<F> to) {
        return convertDiscrete(amount, from, to, null, ConvertContext.empty());
    }

    public <F> long convertDiscrete(long amount, Unit<F> from, Unit<F> to, Namespace material) {
        return convertDiscrete(amount, from, to, material, ConvertContext.empty());
    }

    public <F> long convertDiscrete(long amount, Unit<F> from, Unit<F> to,
                                    @Nullable Namespace material, ConvertContext context) {
        if (from.equals(to)) return amount;

        ResolvedRatio resolved = resolveFullRatio(from, to, material, context);
        if (resolved == null) {
            throw new NoRuleMatchedException("No conversion rule: " + from + " -> " + to);
        }

        // Try exact path first
        if (resolved.exactRatio() != null) {
            Long exact = resolved.exactRatio().applyExact(amount);
            if (exact != null) return exact;
        }

        double result = amount * resolved.ratio();
        long rounded = Math.round(result);
        if (Math.abs(result - rounded) > 1e-9) {
            throw new InexactResultException(
                    "Discrete conversion inexact: " + amount + " " + from + " -> " + to + " = " + result
            );
        }
        return rounded;
    }

    // --- convertDiscrete (with rounding mode) ---

    public <F> DiscreteConversionResult convertDiscrete(
            long amount, Unit<F> from, Unit<F> to, DiscreteRoundingMode mode
    ) {
        return convertDiscrete(amount, from, to, null, ConvertContext.empty(), mode);
    }

    public <F> DiscreteConversionResult convertDiscrete(
            long amount, Unit<F> from, Unit<F> to, Namespace material, DiscreteRoundingMode mode
    ) {
        return convertDiscrete(amount, from, to, material, ConvertContext.empty(), mode);
    }

    public <F> DiscreteConversionResult convertDiscrete(
            long amount, Unit<F> from, Unit<F> to,
            @Nullable Namespace material, ConvertContext context, DiscreteRoundingMode mode
    ) {
        if (from.equals(to)) return DiscreteConversionResult.exact(amount);

        ResolvedRatio resolved = resolveFullRatio(from, to, material, context);
        if (resolved == null) {
            throw new NoRuleMatchedException("No conversion rule: " + from + " -> " + to);
        }

        // Try exact integer path
        if (resolved.exactRatio() != null) {
            Long exact = resolved.exactRatio().applyExact(amount);
            if (exact != null) return DiscreteConversionResult.exact(exact);
        }

        double result = amount * resolved.ratio();
        long rounded = Math.round(result);
        boolean exact = Math.abs(result - rounded) <= 1e-9;

        return switch (mode) {
            case strict -> {
                if (!exact) {
                    throw new InexactResultException("Discrete conversion inexact: " + result);
                }
                yield DiscreteConversionResult.exact(rounded);
            }
            case floorWithRemainder -> {
                long floor = (long) Math.floor(result);
                double remainder = result - floor;
                yield exact
                        ? DiscreteConversionResult.exact(floor)
                        : DiscreteConversionResult.inexact(floor, remainder);
            }
            case approximate -> {
                double remainder = result - rounded;
                yield exact
                        ? DiscreteConversionResult.exact(rounded)
                        : DiscreteConversionResult.inexact(rounded, remainder);
            }
        };
    }

    //endregion

    //region Cross-family API

    // --- Explicit bridge ---

    public <F1, F2> double convertCross(
            double amount, Unit<F1> from, BridgeRule<F1, F2> bridge, Unit<F2> to
    ) {
        if (!from.equals(bridge.from())) {
            throw new CrossFamilyNotAllowedException(
                    "Source unit " + from + " does not match bridge source " + bridge.from()
            );
        }
        if (!to.equals(bridge.to())) {
            throw new CrossFamilyNotAllowedException(
                    "Target unit " + to + " does not match bridge target " + bridge.to()
            );
        }
        UnitPairKey bridgeKey = new UnitPairKey(bridge.from(), bridge.to());
        if (!bridgeRules.containsKey(bridgeKey)) {
            throw new CrossFamilyNotAllowedException(
                    "Bridge rule not registered: " + bridge.from() + " -> " + bridge.to()
            );
        }
        return amount * bridge.ratio();
    }

    // --- Auto-resolve cross-family ---

    public double convertCross(double amount, Unit<?> from, Unit<?> to) {
        return convertCross(amount, from, to, null, ConvertContext.empty());
    }

    public double convertCross(double amount, Unit<?> from, Unit<?> to, Namespace material) {
        return convertCross(amount, from, to, material, ConvertContext.empty());
    }

    public double convertCross(double amount, Unit<?> from, Unit<?> to,
                               @Nullable Namespace material, ConvertContext context) {
        ResolvedRatio resolved = resolveBridgeRatio(from, to, material, context);
        if (resolved == null) {
            throw new CrossFamilyNotAllowedException(
                    "No bridge rule: " + from + " -> " + to
            );
        }
        return amount * resolved.ratio();
    }

    public boolean canConvertCross(Unit<?> from, Unit<?> to) {
        return canConvertCross(from, to, null, ConvertContext.empty());
    }

    public boolean canConvertCross(Unit<?> from, Unit<?> to, Namespace material) {
        return canConvertCross(from, to, material, ConvertContext.empty());
    }

    public boolean canConvertCross(Unit<?> from, Unit<?> to,
                                   @Nullable Namespace material, ConvertContext context) {
        return resolveBridgeRatio(from, to, material, context) != null;
    }

    public long convertCrossDiscrete(long amount, Unit<?> from, Unit<?> to) {
        return convertCrossDiscrete(amount, from, to, null, ConvertContext.empty());
    }

    public long convertCrossDiscrete(long amount, Unit<?> from, Unit<?> to, Namespace material) {
        return convertCrossDiscrete(amount, from, to, material, ConvertContext.empty());
    }

    public long convertCrossDiscrete(long amount, Unit<?> from, Unit<?> to,
                                     @Nullable Namespace material, ConvertContext context) {
        ResolvedRatio resolved = resolveBridgeRatio(from, to, material, context);
        if (resolved == null) {
            throw new CrossFamilyNotAllowedException(
                    "No bridge rule: " + from + " -> " + to
            );
        }
        if (resolved.exactRatio() != null) {
            Long exact = resolved.exactRatio().applyExact(amount);
            if (exact != null) return exact;
        }
        double result = amount * resolved.ratio();
        long rounded = Math.round(result);
        if (Math.abs(result - rounded) > 1e-9) {
            throw new InexactResultException(
                    "Discrete cross-family conversion inexact: " + amount + " " + from + " -> " + to + " = " + result
            );
        }
        return rounded;
    }

    //endregion

    //region Internal resolution

    private static final int MAX_DERIVATION_DEPTH = 2;

    /**
     * Main resolver: tries forward → auto-inverse → transitive derivation.
     */
    private <F> @Nullable ResolvedRatio resolveFullRatio(Unit<F> from, Unit<F> to,
                                                         @Nullable Namespace material, ConvertContext context) {
        return resolveFullRatio(from, to, material, context, MAX_DERIVATION_DEPTH);
    }

    private <F> @Nullable ResolvedRatio resolveFullRatio(Unit<F> from, Unit<F> to,
                                                         @Nullable Namespace material, ConvertContext context,
                                                         int maxDepth) {
        // 1. Forward direction
        ResolvedRatio forward = resolveForwardRatio(from, to, material, context);
        if (forward != null) return forward;

        // 2. Auto-inverse: try reverse direction and invert
        ResolvedRatio reverse = resolveForwardRatio(to, from, material, context);
        if (reverse != null) return reverse.inverse();

        // 3. Transitive derivation via intermediate form (depth-limited)
        if (maxDepth <= 0) return null;
        return resolveDerivedRatio(from, to, material, context, maxDepth - 1);
    }

    /**
     * Resolves a ratio in the forward direction only (no auto-inverse).
     * Priority: matter rules (MatterFamily only) > family rules.
     */
    private <F> @Nullable ResolvedRatio resolveForwardRatio(Unit<F> from, Unit<F> to,
                                                            @Nullable Namespace material, ConvertContext context) {
        if (from.family() instanceof MatterFamily<?>) {
            ResolvedRatio matter = resolveMatterRatio(from, to, material);
            if (matter != null) return matter;
        }

        return resolveFamilyRuleRatio(from, to, context);
    }

    /**
     * Resolves matter rules with priority: object > material > domain > template > fallback.
     */
    private <F> @Nullable ResolvedRatio resolveMatterRatio(Unit<F> from, Unit<F> to,
                                                           @Nullable Namespace material) {
        Namespace fromId = from.id();
        Namespace toId = to.id();

        if (material != null) {
            // Object rule (highest priority)
            ObjectRule obj = objectRules.get(new MaterialUnitIdPairKey(material, fromId, toId));
            if (obj != null) return ResolvedRatio.of(obj.ratio(), obj.exactRatio());

            // Material rule
            MaterialRule mat = materialRules.get(new MaterialUnitIdPairKey(material, fromId, toId));
            if (mat != null) return ResolvedRatio.of(mat.ratio(), mat.exactRatio());

            // Domain rule (matches by material's root/domain)
            DomainRule dom = domainRules.get(new DomainUnitIdPairKey(material.root(), fromId, toId));
            if (dom != null) return ResolvedRatio.of(dom.ratio(), dom.exactRatio());
        }

        // Template rule
        TemplateRule tmpl = templateRules.get(new UnitIdPairKey(fromId, toId));
        if (tmpl != null) return ResolvedRatio.of(tmpl.ratio(), tmpl.exactRatio());

        // Fallback rule (lowest priority)
        FallbackRule fb = fallbackRules.get(new UnitIdPairKey(fromId, toId));
        if (fb != null) return ResolvedRatio.of(fb.ratio(), fb.exactRatio());

        return null;
    }

    private <F> @Nullable ResolvedRatio resolveFamilyRuleRatio(Unit<F> from, Unit<F> to, ConvertContext context) {
        UnitPairKey key = new UnitPairKey(from, to);
        List<FamilyRule<?>> rules = familyRules.get(key);
        if (rules == null) return null;

        for (FamilyRule<?> rule : rules) {
            if (rule.matches(context)) {
                return ResolvedRatio.of(rule.ratio(), rule.exactRatio());
            }
        }
        return null;
    }

    private <F> @Nullable ResolvedRatio resolveDerivedRatio(Unit<F> from, Unit<F> to,
                                                            @Nullable Namespace material, ConvertContext context,
                                                            int maxDepth) {
        for (Namespace midId : allUnitIds) {
            if (midId.equals(from.id()) || midId.equals(to.id())) continue;

            Unit<F> mid = Unit.create(from.family(), midId);
            ResolvedRatio r1 = resolveFullRatio(from, mid, material, context, maxDepth);
            ResolvedRatio r2 = resolveFullRatio(mid, to, material, context, maxDepth);

            if (r1 != null && r2 != null) {
                return r1.multiply(r2);
            }
        }
        return null;
    }

    private @Nullable ResolvedRatio resolveBridgeRatio(Unit<?> from, Unit<?> to,
                                                       @Nullable Namespace material, ConvertContext context) {
        UnitPairKey key = new UnitPairKey(from, to);
        List<BridgeRule<?, ?>> rules = bridgeRules.get(key);
        if (rules != null) {
            for (BridgeRule<?, ?> rule : rules) {
                if (rule.matches(material, context)) {
                    return ResolvedRatio.of(rule.ratio(), rule.exactRatio());
                }
            }
        }
        // Auto-inverse: try reverse direction
        UnitPairKey reverseKey = new UnitPairKey(to, from);
        List<BridgeRule<?, ?>> reverseRules = bridgeRules.get(reverseKey);
        if (reverseRules != null) {
            for (BridgeRule<?, ?> rule : reverseRules) {
                if (rule.matches(material, context)) {
                    return ResolvedRatio.of(rule.ratio(), rule.exactRatio()).inverse();
                }
            }
        }
        return null;
    }

    //endregion
}
