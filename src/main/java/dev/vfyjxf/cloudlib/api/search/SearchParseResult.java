package dev.vfyjxf.cloudlib.api.search;

import java.util.List;
import java.util.Objects;

public record SearchParseResult(
    SearchExpression expression,
    List<SearchSyntaxSpan> syntaxSpans
) {
    public SearchParseResult {
        Objects.requireNonNull(expression, "expression");
        syntaxSpans = List.copyOf(Objects.requireNonNull(syntaxSpans, "syntaxSpans"));
    }
}
