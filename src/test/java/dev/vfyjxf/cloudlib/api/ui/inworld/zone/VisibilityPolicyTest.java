package dev.vfyjxf.cloudlib.api.ui.inworld.zone;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VisibilityPolicyTest {

    @Test
    void theFivePoliciesExistWithDistinctSemantics() {
        assertEquals(
                Set.of(
                        VisibilityPolicy.hardOcclusion,
                        VisibilityPolicy.fade,
                        VisibilityPolicy.occludedIndicator,
                        VisibilityPolicy.edgeProxy,
                        VisibilityPolicy.semanticVisible),
                EnumSet.allOf(VisibilityPolicy.class));
        assertEquals(5, VisibilityPolicy.values().length);
    }

    @Test
    void lookupByNameIsStableForDataDrivenConfig() {
        assertEquals(VisibilityPolicy.hardOcclusion, VisibilityPolicy.valueOf("hardOcclusion"));
        assertEquals(VisibilityPolicy.fade, VisibilityPolicy.valueOf("fade"));
        assertEquals(VisibilityPolicy.occludedIndicator, VisibilityPolicy.valueOf("occludedIndicator"));
        assertEquals(VisibilityPolicy.edgeProxy, VisibilityPolicy.valueOf("edgeProxy"));
        assertEquals(VisibilityPolicy.semanticVisible, VisibilityPolicy.valueOf("semanticVisible"));
    }
}
