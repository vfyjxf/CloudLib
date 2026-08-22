package dev.vfyjxf.cloudlib.api.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchParserTest {
    private final SearchParser parser = TestSearchFields.parser();

    @Test
    void blankQueryMatchesEverything() {
        assertType(SearchExpression.All.class, parser.parse("  "));
    }

    @Test
    void parsesJeiStyleImplicitAndAndOr() {
        SearchExpression expression = parser.parse("oak plank | stick");

        SearchExpression.Or or = assertType(SearchExpression.Or.class, expression);
        assertEquals(2, or.terms().size());
        SearchExpression.And left = assertType(SearchExpression.And.class, or.terms().getFirst());
        assertTerm(left.terms().getFirst(), TestSearchFields.text, SearchOperator.contains, "oak", "");
        assertTerm(left.terms().get(1), TestSearchFields.text, SearchOperator.contains, "plank", "");
        assertTerm(or.terms().get(1), TestSearchFields.text, SearchOperator.contains, "stick", "");
    }

    @Test
    void parsesJeiPrefixesAndNegation() {
        SearchExpression expression = parser.parse("@minecraft $planks #tooltip -&minecraft:debug");

        SearchExpression.And and = assertType(SearchExpression.And.class, expression);
        assertTerm(and.terms().getFirst(), TestSearchFields.mod, SearchOperator.contains, "minecraft", "");
        assertTerm(and.terms().get(1), TestSearchFields.tag, SearchOperator.contains, "planks", "");
        assertTerm(and.terms().get(2), TestSearchFields.tooltip, SearchOperator.contains, "tooltip", "");

        SearchExpression.Not not = assertType(SearchExpression.Not.class, and.terms().get(3));
        assertTerm(not.term(), TestSearchFields.id, SearchOperator.contains, "minecraft:debug", "");
    }

    @Test
    void parsesFieldAliasesForAdvancedQueries() {
        SearchExpression expression = parser.parse("(in:oak OR out:stick) time>=10s out:stick * 1..64 cat:bucket has:oak");

        SearchExpression.And and = assertType(SearchExpression.And.class, expression);
        assertType(SearchExpression.Or.class, and.terms().getFirst());
        assertTerm(and.terms().get(1), TestSearchFields.time, SearchOperator.greaterThanOrEqual, "10s", "");
        assertTerm(and.terms().get(2), TestSearchFields.out, SearchOperator.contains, "stick", "");
        assertAmount(and.terms().get(2), SearchOperator.range, "1", "64");
        assertTerm(and.terms().get(3), TestSearchFields.cat, SearchOperator.contains, "bucket", "");
        assertTerm(and.terms().get(4), TestSearchFields.contains, SearchOperator.contains, "oak", "");
    }

    @Test
    void parsesExplicitContentIdFields() {
        assertTerm(parser.parse("in.id:minecraft:iron_ingot * >=2"), TestSearchFields.in_id, SearchOperator.contains, "minecraft:iron_ingot", "");
        assertAmount(parser.parse("in.id:minecraft:iron_ingot * >=2"), SearchOperator.greaterThanOrEqual, "2", "");
        assertTerm(parser.parse("out.id=minecraft:furnace"), TestSearchFields.out_id, SearchOperator.exact, "minecraft:furnace", "");
        assertTerm(parser.parse("cat.id:test:bucket"), TestSearchFields.cat_id, SearchOperator.contains, "test:bucket", "");
        assertTerm(parser.parse("byproduct.id:test:slag"), TestSearchFields.byproduct_id, SearchOperator.contains, "test:slag", "");
    }

    @Test
    void parsesNegatedComparisonOperatorsAsNotTerms() {
        SearchExpression.And expression = assertType(SearchExpression.And.class, parser.parse("mod!=minecraft out!~smok*"));

        SearchExpression.Not notMod = assertType(SearchExpression.Not.class, expression.terms().getFirst());
        assertTerm(notMod.term(), TestSearchFields.mod, SearchOperator.exact, "minecraft", "");

        SearchExpression.Not notOutput = assertType(SearchExpression.Not.class, expression.terms().get(1));
        assertTerm(notOutput.term(), TestSearchFields.out, SearchOperator.wildcard, "smok*", "");
    }

    @Test
    void keepsResourceLocationsAsText() {
        SearchExpression expression = parser.parse("minecraft:oak_planks");

        assertTerm(expression, TestSearchFields.text, SearchOperator.contains, "minecraft:oak_planks", "");
    }

    @Test
    void parsesQuotedTextAndWildcards() {
        assertTerm(parser.parse("\"oak planks\""), TestSearchFields.text, SearchOperator.contains, "oak planks", "");
        assertTerm(parser.parse("out:*plank?"), TestSearchFields.out, SearchOperator.wildcard, "*plank?", "");
    }

    @Test
    void quotedValuesMarkSearchTermsAsStrict() {
        assertStrict(parser.parse("\"Oak Planks\""), true);
        assertStrict(parser.parse("out:\"Oak_Planks\""), true);
        assertStrict(parser.parse("out=\"Oak_Planks\""), true);
        assertStrict(parser.parse("out:Oak_Planks"), false);
    }

    @Test
    void parsesContentAmountModifier() {
        SearchExpression expression = parser.parse("has:furnace * >=64");

        assertTerm(expression, TestSearchFields.contains, SearchOperator.contains, "furnace", "");
        assertAmount(expression, SearchOperator.greaterThanOrEqual, "64", "");

        SearchExpression implicitAnyAmount = parser.parse("has:* * 64");
        assertTerm(implicitAnyAmount, TestSearchFields.contains, SearchOperator.exists, "*", "");
        assertAmount(implicitAnyAmount, SearchOperator.exact, "64", "");

        SearchExpression comparedAnyAmount = parser.parse("has:* * >64");
        assertTerm(comparedAnyAmount, TestSearchFields.contains, SearchOperator.exists, "*", "");
        assertAmount(comparedAnyAmount, SearchOperator.greaterThan, "64", "");

        SearchExpression defaultContentAmount = parser.parse("furnace * 64");
        assertTerm(defaultContentAmount, TestSearchFields.contains, SearchOperator.contains, "furnace", "");
        assertAmount(defaultContentAmount, SearchOperator.exact, "64", "");
    }

    @Test
    void bareNumbersAfterContentTermsAreNotParsedAsAmounts() {
        SearchExpression.And expression = assertType(SearchExpression.And.class, parser.parse("has:* 64"));

        assertTerm(expression.terms().getFirst(), TestSearchFields.contains, SearchOperator.exists, "*", "");
        assertTerm(expression.terms().get(1), TestSearchFields.text, SearchOperator.contains, "64", "");
    }

    @Test
    void reportsUnclosedSyntax() {
        assertThrows(SearchParseException.class, () -> parser.parse("\"oak planks"));
        assertThrows(SearchParseException.class, () -> parser.parse("(oak plank"));
        assertThrows(SearchParseException.class, () -> parser.parse("oak |"));
        assertThrows(SearchParseException.class, () -> parser.parse("oak AND"));
        assertThrows(SearchParseException.class, () -> parser.parse("oak &"));
    }

    @Test
    void parseResultExposesSourceSpansForUiHighlighting() {
        SearchParseResult result = parser.parseWithSyntax("@minecraft in:oak time>=10s | -#burn");

        assertSpan(result, 0, 1, SearchSyntaxKind.prefix);
        assertSpan(result, 11, 13, SearchSyntaxKind.field);
        assertSpan(result, 13, 14, SearchSyntaxKind.operator);
        assertSpan(result, 18, 22, SearchSyntaxKind.field);
        assertSpan(result, 22, 24, SearchSyntaxKind.operator);
        assertSpan(result, 28, 29, SearchSyntaxKind.keyword);
        assertSpan(result, 30, 31, SearchSyntaxKind.negation);
        assertSpan(result, 31, 32, SearchSyntaxKind.prefix);
    }

    @Test
    void parseResultSplitsRangeAndWildcardValueSyntax() {
        SearchParseResult result = parser.parseWithSyntax("out:smoker * 1..64 out:smok*");

        assertSpan(result, 0, 3, SearchSyntaxKind.field);
        assertSpan(result, 3, 4, SearchSyntaxKind.operator);
        assertSpan(result, 11, 12, SearchSyntaxKind.operator);
        assertSpan(result, 14, 16, SearchSyntaxKind.operator);
        assertSpan(result, 19, 22, SearchSyntaxKind.field);
        assertSpan(result, 22, 23, SearchSyntaxKind.operator);
        assertSpan(result, 27, 28, SearchSyntaxKind.operator);
    }

    private static void assertTerm(
        SearchExpression expression,
        SearchField field,
        SearchOperator operator,
        String value,
        String upperBound
    ) {
        SearchExpression.Term term = assertType(SearchExpression.Term.class, expression);
        assertEquals(field, term.term().field());
        assertEquals(operator, term.term().operator());
        assertEquals(value, term.term().value());
        assertEquals(upperBound, term.term().upperBound());
    }

    private static void assertAmount(
        SearchExpression expression,
        SearchOperator operator,
        String value,
        String upperBound
    ) {
        SearchExpression.Term term = assertType(SearchExpression.Term.class, expression);
        assertEquals(operator, term.term().amountOperator());
        assertEquals(value, term.term().amount());
        assertEquals(upperBound, term.term().amountUpperBound());
    }

    private static void assertStrict(SearchExpression expression, boolean strict) {
        SearchExpression.Term term = assertType(SearchExpression.Term.class, expression);
        assertEquals(strict, term.term().strict());
    }

    private static <T> T assertType(Class<T> type, Object value) {
        assertTrue(type.isInstance(value), () -> "Expected " + type.getSimpleName() + " but got " + value);
        return type.cast(value);
    }

    private static void assertSpan(SearchParseResult result, int start, int end, SearchSyntaxKind kind) {
        assertTrue(
            result.syntaxSpans().stream().anyMatch(span -> span.start() == start && span.end() == end && span.kind() == kind),
            () -> "Expected span " + kind + " at " + start + ".." + end + " but got " + result.syntaxSpans()
        );
    }
}
