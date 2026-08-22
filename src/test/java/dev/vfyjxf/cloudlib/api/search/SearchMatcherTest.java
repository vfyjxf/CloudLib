package dev.vfyjxf.cloudlib.api.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchMatcherTest {
    private final SearchParser parser = TestSearchFields.parser();

    @Test
    void ordinaryWordsMatchDefaultSearchText() {
        SearchDocument document = SearchDocument.builder(TestSearchFields.SET, TestSearchFields.text)
            .searchableText(TestSearchFields.name, "Oak Planks")
            .searchableText(TestSearchFields.in, "minecraft:oak_log")
            .searchableText(TestSearchFields.out, "minecraft:oak_planks")
            .text(TestSearchFields.tag, "minecraft:planks")
            .build();

        assertTrue(matches("oak plank", document));
        assertTrue(matches("minecraft:oak_planks", document));
        assertFalse(matches("spruce plank", document));
    }

    @Test
    void unquotedTextIgnoresCaseAndQuotedTextIsStrict() {
        SearchDocument document = SearchDocument.builder(TestSearchFields.SET, TestSearchFields.text)
            .searchableText(TestSearchFields.name, "Oak Planks")
            .searchableText(TestSearchFields.out, "minecraft:Oak_Planks")
            .text(TestSearchFields.mod, "Minecraft")
            .build();

        assertTrue(matches("oak OAK minecraft:oak_planks", document));
        assertTrue(matches("\"Oak Planks\"", document));
        assertTrue(matches("out:\"Oak_Planks\"", document));
        assertTrue(matches("mod=\"Minecraft\"", document));
        assertFalse(matches("\"oak planks\"", document));
        assertFalse(matches("out:\"oak_planks\"", document));
        assertFalse(matches("mod=\"minecraft\"", document));
    }

    @Test
    void quotedWildcardsAreCaseSensitive() {
        SearchDocument document = SearchDocument.builder(TestSearchFields.SET, TestSearchFields.text)
            .searchableText(TestSearchFields.name, "Oak Planks")
            .build();

        assertTrue(matches("oak*", document));
        assertTrue(matches("\"Oak*\"", document));
        assertFalse(matches("\"oak*\"", document));
    }

    @Test
    void jeiPrefixesFilterDedicatedFieldsOnly() {
        SearchDocument document = SearchDocument.builder(TestSearchFields.SET, TestSearchFields.text)
            .searchableText(TestSearchFields.name, "Oak Planks")
            .text(TestSearchFields.mod, "minecraft")
            .text(TestSearchFields.tag, "minecraft:planks")
            .text(TestSearchFields.tooltip, "Can burn")
            .text(TestSearchFields.id, "minecraft:oak_planks")
            .build();

        assertTrue(matches("@minecraft $planks #burn &minecraft:oak_planks", document));
        assertFalse(matches("@thermal", document));
        assertFalse(matches("$logs", document));
    }

    @Test
    void booleanAndNegativeQueriesWorkLikeJeiFiltering() {
        SearchDocument document = SearchDocument.builder(TestSearchFields.SET, TestSearchFields.text)
            .searchableText(TestSearchFields.name, "Smoker")
            .searchableText(TestSearchFields.in, "minecraft:furnace")
            .searchableText(TestSearchFields.out, "minecraft:smoker")
            .text(TestSearchFields.mod, "minecraft")
            .build();

        assertTrue(matches("(furnace | blast) smoker -@thermal", document));
        assertFalse(matches("smoker -@minecraft", document));
    }

    @Test
    void negatedComparisonOperatorsReuseNotTermSemantics() {
        SearchDocument document = SearchDocument.builder(TestSearchFields.SET, TestSearchFields.text)
            .searchableText(TestSearchFields.out, "minecraft:smoker")
            .text(TestSearchFields.mod, "minecraft")
            .build();

        assertTrue(matches("mod!=thermal out!~blast", document));
        assertFalse(matches("mod!=minecraft", document));
        assertFalse(matches("out!~smok*", document));
    }

    @Test
    void advancedNumericFiltersRemainAvailable() {
        SearchDocument document = SearchDocument.builder(TestSearchFields.SET, TestSearchFields.text)
            .number(TestSearchFields.time, 240)
            .number(TestSearchFields.energy, 1500)
            .number(TestSearchFields.chance, 0.25)
            .build();

        assertTrue(matches("time>=10s energy<2k chance<50%", document));
        assertFalse(matches("time>20s", document));
    }

    @Test
    void catalystAndContentContainsFieldsSearchRecipeContents() {
        SearchDocument document = SearchDocument.builder(TestSearchFields.SET, TestSearchFields.text)
            .searchableText(TestSearchFields.in, "minecraft:furnace")
            .searchableText(TestSearchFields.out, "minecraft:smoker")
            .searchableText(TestSearchFields.cat, "minecraft:bucket")
            .text(TestSearchFields.mod, "minecraft")
            .text(TestSearchFields.db, "saved")
            .build();

        assertTrue(matches("cat:bucket", document));
        assertTrue(matches("has:furnace has:smoker has:bucket", document));
        assertFalse(matches("cat:furnace", document));
        assertFalse(matches("has:saved", document));
    }

    @Test
    void contentAmountModifiersUseStarSyntax() {
        SearchDocument document = SearchDocument.builder(TestSearchFields.SET, TestSearchFields.text)
            .searchableContent(TestSearchFields.in, "minecraft:furnace", 64)
            .searchableContent(TestSearchFields.out, "minecraft:smoker", 1)
            .build();

        assertTrue(matches("has:* * 64", document));
        assertTrue(matches("has:* * >=64", document));
        assertTrue(matches("has:furnace * >=64", document));
        assertTrue(matches("furnace * 64", document));
        assertFalse(matches("has:* * >64", document));
        assertFalse(matches("has:smoker * >=64", document));
        assertFalse(matches("has:* 64", document));
    }

    private boolean matches(String query, SearchDocument document) {
        return SearchMatcher.matches(parser.parse(query), document);
    }
}
