package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.util.Namespace;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UnitBridgeConflictTest {

    private static final MatterFamily<A> AF = MatterFamily.create(Namespace.ofMc("af"));
    private static final MeasureFamily<MeasureKind> MEASURE = MeasureFamily.create(Namespace.ofMc("measure"));

    private static final Unit<A> A1 = Unit.create(AF, Namespace.ofMc("a1"));
    private static final Unit<MeasureKind> B1 = Unit.create(MEASURE, Namespace.ofMc("b1"));

    @Test
    void sameContextConflictThrowsOnFreeze() {
        ConversionSchema schema = ConversionSchema.create();
        schema.addBridgeRule(BridgeRule.create(A1, B1, 2.0));
        schema.addBridgeRule(BridgeRule.create(A1, B1, 3.0));

        assertThrows(RuleConflictException.class, schema::build);
    }

    @Test
    void sameContextWithMaterialConflictThrowsOnFreeze() {
        Namespace iron = Namespace.ofMc("iron");
        ConversionSchema schema = ConversionSchema.create();
        schema.addBridgeRule(BridgeRule.create(A1, B1, 2.0, iron));
        schema.addBridgeRule(BridgeRule.create(A1, B1, 3.0, iron));

        assertThrows(RuleConflictException.class, schema::build);
    }

    @Test
    void differentContextBridgesAreAllowed() {
        ConversionSchema schema = ConversionSchema.create();
        // Specificity 0 (no context)
        schema.addBridgeRule(BridgeRule.create(A1, B1, 2.0));
        // Specificity 1 (has context)
        ContextKey<String> key = ContextKey.create(Namespace.ofMc("k"), String.class);
        schema.addBridgeRule(BridgeRule.create(A1, B1, 5.0, null, Map.of(key, "v")));

        assertDoesNotThrow(schema::build);
    }

    private static final class A {
    }

    private static final class MeasureKind {
    }
}
