package dev.vfyjxf.cloudlib.api.search;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchCodecTest {
    @Test
    void searchExpressionRoundTripsThroughCodec() {
        SearchExpression expression = new SearchExpression.And(java.util.List.of(
            new SearchExpression.Term(new SearchTerm(TestSearchFields.text, SearchOperator.contains, "oak")),
            new SearchExpression.Not(new SearchExpression.Term(new SearchTerm(TestSearchFields.tag, SearchOperator.exact, "blocked"))),
            new SearchExpression.Term(new SearchTerm(TestSearchFields.out, SearchOperator.wildcard, "Oak*", "", true))
        ));

        var encoded = TestSearchFields.codecs().expression().encodeStart(JsonOps.INSTANCE, expression).result().orElseThrow();
        SearchExpression decoded = TestSearchFields.codecs().expression().parse(JsonOps.INSTANCE, encoded).result().orElseThrow();

        assertEquals(expression, decoded);
    }
}
