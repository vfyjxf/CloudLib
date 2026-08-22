package dev.vfyjxf.cloudlib.api.search;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


class SearchTextsTest {

    @AfterEach
    void resetPipeline() {
        SearchTexts.setTransformer(null);
        SearchTexts.setMatcher(null);
        SearchTexts.setTokenizer(SearchTextTokenizer.words());
    }

    @Test
    void lowercasesWithFastPathReturningSameInstance() {
        String alreadyLower = "minecraft:ingot_iron";
        assertSame(alreadyLower, SearchTexts.normalize(alreadyLower));
        assertEquals("iron ingot", SearchTexts.normalize("Iron Ingot"));
    }

    @Test
    void transformerAppliesToBothSidesOfNormalization() {
        SearchTexts.setTransformer(value -> value.replace('铁', 't'));
        assertEquals("t ingot", SearchTexts.normalize("铁 Ingot"));
    }

    @Test
    void matcherReplacesContainmentAndDisablesAcceleration() {
        SearchTexts.setMatcher((haystack, needle) -> true);
        assertTrue(SearchTexts.contains("anything", "nope"));
        assertFalse(SearchTexts.accelerated());
        SearchTexts.setMatcher(null);
        assertFalse(SearchTexts.contains("iron ingot", "gold"));
        assertTrue(SearchTexts.accelerated());
    }

    @Test
    void defaultTokenizerSplitsIdsAndSingleChars() {
        assertEquals(List.of("minecraft", "ingot", "iron"), SearchTexts.tokenize("minecraft:ingot_iron"));
        assertEquals(List.of("iron", "ingot"), SearchTexts.tokenize("Iron_Ingot"));
        assertEquals(List.of("铁", "锭"), SearchTexts.tokenize("铁锭"));
        assertEquals(List.of(), SearchTexts.tokenize(":-_"));
    }

    @Test
    void fieldSetsResolveAliasesPrefixesAndRejectDuplicates() {
        SearchFieldSet set = TestSearchFields.SET;
        assertSame(TestSearchFields.mod, set.byAlias("@mod"));
        assertSame(TestSearchFields.out_id, set.byAlias("result.id"));
        assertNull(set.byAlias("unknown-alias"));
        assertSame(TestSearchFields.tag, set.byJeiPrefix('$'));
        assertTrue(TestSearchFields.in.contentChannel());
        assertTrue(TestSearchFields.time.numeric());
        assertEquals(120, TestSearchFields.name.weight());

        // the aggregate contains field unions the non-virtual content channels
        assertEquals(
            java.util.List.of(
                TestSearchFields.out, TestSearchFields.out_id, TestSearchFields.in, TestSearchFields.in_id,
                TestSearchFields.cat, TestSearchFields.cat_id, TestSearchFields.byproduct, TestSearchFields.byproduct_id
            ),
            set.containsSources());
        assertEquals(1, set.virtualFields().length);
        assertSame(TestSearchFields.contains, set.virtualFields()[0]);

        // scenarios may declare independent sets with colliding names
        SearchField mine = SearchField.of("text", new SearchFieldSpec(9, false, false, null));
        SearchFieldSet other = SearchFieldSet.of(mine);
        assertEquals(0, mine.id());
        assertSame(mine, other.byName("text"));
        assertThrows(IllegalArgumentException.class, () -> SearchFieldSet.of(
            SearchField.of("dup", new SearchFieldSpec(1, false, false, null)),
            SearchField.of("dup", new SearchFieldSpec(1, false, false, null))));
        assertThrows(IllegalArgumentException.class, () -> SearchFieldSet.of(
            SearchField.of("a", new SearchFieldSpec(1, false, false, null)),
            SearchField.of("b", new SearchFieldSpec(1, false, false, null, "a"))));
        // a bound field cannot join another set
        assertThrows(IllegalStateException.class, () -> SearchFieldSet.of(mine));
        assertNotSame(other, set);
    }
}
