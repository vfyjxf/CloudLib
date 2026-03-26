package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.unit.conversion.*;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MatterRulePriorityTest {

    private static final MatterFamily<MatterKind> MATTER = MatterFamily.create(Namespace.ofMc("matter"));
    private static final Namespace INGOT = Namespace.ofMc("ingot");
    private static final Namespace BLOCK = Namespace.ofMc("block");

    private static final Namespace QUARTZ = Namespace.ofMc("quartz");
    private static final Namespace IRON = Namespace.ofMc("iron");

    private static final Unit<MatterKind> INGOT_UNIT = Unit.create(MATTER, INGOT);
    private static final Unit<MatterKind> BLOCK_UNIT = Unit.create(MATTER, BLOCK);

    @Test
    void objectRuleHasHighestPriority() {
        ConversionSchema schema = ConversionSchema.create();
        schema.addFallbackRule(FallbackRule.create(INGOT, BLOCK, 9.0));
        schema.addTemplateRule(TemplateRule.create(INGOT, BLOCK, 8.0));
        schema.addMaterialRule(MaterialRule.create(QUARTZ, INGOT, BLOCK, 4.0));
        schema.addObjectRule(ObjectRule.create(QUARTZ, INGOT, BLOCK, 2.0));

        UnitConverter rules = schema.build();

        assertEquals(2.0, rules.convert(1.0, INGOT_UNIT, BLOCK_UNIT, QUARTZ));
    }

    @Test
    void materialRuleUsedWhenNoObjectRule() {
        ConversionSchema schema = ConversionSchema.create();
        schema.addFallbackRule(FallbackRule.create(INGOT, BLOCK, 9.0));
        schema.addTemplateRule(TemplateRule.create(INGOT, BLOCK, 8.0));
        schema.addMaterialRule(MaterialRule.create(QUARTZ, INGOT, BLOCK, 4.0));

        UnitConverter rules = schema.build();

        assertEquals(4.0, rules.convert(1.0, INGOT_UNIT, BLOCK_UNIT, QUARTZ));
    }

    @Test
    void templateRuleUsedWhenNoSpecificRules() {
        ConversionSchema schema = ConversionSchema.create();
        schema.addFallbackRule(FallbackRule.create(INGOT, BLOCK, 9.0));
        schema.addTemplateRule(TemplateRule.create(INGOT, BLOCK, 8.0));

        UnitConverter rules = schema.build();

        assertEquals(8.0, rules.convert(1.0, INGOT_UNIT, BLOCK_UNIT, QUARTZ));
    }

    @Test
    void fallbackRuleUsedAsLastResort() {
        ConversionSchema schema = ConversionSchema.create();
        schema.addFallbackRule(FallbackRule.create(INGOT, BLOCK, 9.0));

        UnitConverter rules = schema.build();

        assertEquals(9.0, rules.convert(1.0, INGOT_UNIT, BLOCK_UNIT));
    }

    @Test
    void objectRuleConflictThrowsOnFreeze() {
        ConversionSchema schema = ConversionSchema.create();
        schema.addObjectRule(ObjectRule.create(QUARTZ, INGOT, BLOCK, 2.0));
        schema.addObjectRule(ObjectRule.create(QUARTZ, INGOT, BLOCK, 3.0));

        assertThrows(RuleConflictException.class, schema::build);
    }

    @Test
    void materialRuleConflictThrowsOnFreeze() {
        ConversionSchema schema = ConversionSchema.create();
        schema.addMaterialRule(MaterialRule.create(QUARTZ, INGOT, BLOCK, 4.0));
        schema.addMaterialRule(MaterialRule.create(QUARTZ, INGOT, BLOCK, 5.0));

        assertThrows(RuleConflictException.class, schema::build);
    }

    @Test
    void templateRuleConflictThrowsOnFreeze() {
        ConversionSchema schema = ConversionSchema.create();
        schema.addTemplateRule(TemplateRule.create(INGOT, BLOCK, 8.0));
        schema.addTemplateRule(TemplateRule.create(INGOT, BLOCK, 7.0));

        assertThrows(RuleConflictException.class, schema::build);
    }

    @Test
    void fallbackRuleConflictThrowsOnFreeze() {
        ConversionSchema schema = ConversionSchema.create();
        schema.addFallbackRule(FallbackRule.create(INGOT, BLOCK, 9.0));
        schema.addFallbackRule(FallbackRule.create(INGOT, BLOCK, 10.0));

        assertThrows(RuleConflictException.class, schema::build);
    }

    private static final class MatterKind {
    }
}
