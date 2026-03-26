package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.unit.conversion.*;
import dev.vfyjxf.cloudlib.util.Checks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ConversionSchema {

    private final List<FamilyRule<?>> familyRules = new ArrayList<>();
    private final List<BridgeRule<?, ?>> bridgeRules = new ArrayList<>();
    private final List<FallbackRule> fallbackRules = new ArrayList<>();
    private final List<TemplateRule> templateRules = new ArrayList<>();
    private final List<MaterialRule> materialRules = new ArrayList<>();
    private final List<ObjectRule> objectRules = new ArrayList<>();
    private final List<DomainRule> domainRules = new ArrayList<>();
    private boolean useDefaults = true;

    public static ConversionSchema create() {
        return new ConversionSchema();
    }

    private ConversionSchema() {
    }

    //region Single-add methods

    public ConversionSchema addFamilyRule(FamilyRule<?> rule) {
        Checks.checkNotNull(rule, "rule");
        familyRules.add(rule);
        return this;
    }

    public ConversionSchema addBridgeRule(BridgeRule<?, ?> rule) {
        Checks.checkNotNull(rule, "rule");
        bridgeRules.add(rule);
        return this;
    }

    public ConversionSchema addFallbackRule(FallbackRule rule) {
        Checks.checkNotNull(rule, "rule");
        fallbackRules.add(rule);
        return this;
    }

    public ConversionSchema addTemplateRule(TemplateRule rule) {
        Checks.checkNotNull(rule, "rule");
        templateRules.add(rule);
        return this;
    }

    public ConversionSchema addMaterialRule(MaterialRule rule) {
        Checks.checkNotNull(rule, "rule");
        materialRules.add(rule);
        return this;
    }

    public ConversionSchema addObjectRule(ObjectRule rule) {
        Checks.checkNotNull(rule, "rule");
        objectRules.add(rule);
        return this;
    }

    public ConversionSchema addDomainRule(DomainRule rule) {
        Checks.checkNotNull(rule, "rule");
        domainRules.add(rule);
        return this;
    }

    //endregion

    //region Batch-add methods

    public ConversionSchema addFamilyRules(Collection<? extends FamilyRule<?>> rules) {
        Checks.checkNotNull(rules, "rules");
        familyRules.addAll(rules);
        return this;
    }

    public ConversionSchema addBridgeRules(Collection<? extends BridgeRule<?, ?>> rules) {
        Checks.checkNotNull(rules, "rules");
        bridgeRules.addAll(rules);
        return this;
    }

    public ConversionSchema addFallbackRules(Collection<? extends FallbackRule> rules) {
        Checks.checkNotNull(rules, "rules");
        fallbackRules.addAll(rules);
        return this;
    }

    public ConversionSchema addTemplateRules(Collection<? extends TemplateRule> rules) {
        Checks.checkNotNull(rules, "rules");
        templateRules.addAll(rules);
        return this;
    }

    public ConversionSchema addMaterialRules(Collection<? extends MaterialRule> rules) {
        Checks.checkNotNull(rules, "rules");
        materialRules.addAll(rules);
        return this;
    }

    public ConversionSchema addObjectRules(Collection<? extends ObjectRule> rules) {
        Checks.checkNotNull(rules, "rules");
        objectRules.addAll(rules);
        return this;
    }

    public ConversionSchema addDomainRules(Collection<? extends DomainRule> rules) {
        Checks.checkNotNull(rules, "rules");
        domainRules.addAll(rules);
        return this;
    }

    //endregion

    //region Default control

    public ConversionSchema clearDefaults() {
        useDefaults = false;
        return this;
    }

    //endregion

    //region Build

    public UnitConverter build() {
        return UnitConverter.build(this);
    }

    //endregion

    //region Package-private accessors

    List<FamilyRule<?>> familyRules() {
        return familyRules;
    }

    List<BridgeRule<?, ?>> bridgeRules() {
        return bridgeRules;
    }

    List<FallbackRule> fallbackRules() {
        return fallbackRules;
    }

    List<TemplateRule> templateRules() {
        return templateRules;
    }

    List<MaterialRule> materialRules() {
        return materialRules;
    }

    List<ObjectRule> objectRules() {
        return objectRules;
    }

    List<DomainRule> domainRules() {
        return domainRules;
    }

    boolean useDefaults() {
        return useDefaults;
    }

    //endregion
}
