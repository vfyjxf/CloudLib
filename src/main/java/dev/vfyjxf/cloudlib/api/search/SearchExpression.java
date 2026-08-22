package dev.vfyjxf.cloudlib.api.search;

import java.util.List;

public sealed interface SearchExpression permits SearchExpression.All,
                                                 SearchExpression.And,
                                                 SearchExpression.Or,
                                                 SearchExpression.Not,
                                                 SearchExpression.Term {
    default String codecType() {
        return switch (this) {
            case All ignored -> "all";
            case And ignored -> "and";
            case Or ignored -> "or";
            case Not ignored -> "not";
            case Term ignored -> "term";
        };
    }

    record All() implements SearchExpression {
    }

    record And(List<SearchExpression> terms) implements SearchExpression {
    }

    record Or(List<SearchExpression> terms) implements SearchExpression {
    }

    record Not(SearchExpression term) implements SearchExpression {
    }

    record Term(SearchTerm term) implements SearchExpression {
    }
}
