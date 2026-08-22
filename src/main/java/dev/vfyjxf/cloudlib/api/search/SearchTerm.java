package dev.vfyjxf.cloudlib.api.search;

import java.util.Objects;

public record SearchTerm(
    SearchField field,
    SearchOperator operator,
    String value,
    String upperBound,
    SearchOperator amountOperator,
    String amount,
    String amountUpperBound,
    boolean strict
) {
    public SearchTerm {
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(operator, "operator");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(upperBound, "upperBound");
        Objects.requireNonNull(amountOperator, "amountOperator");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(amountUpperBound, "amountUpperBound");
    }

    public SearchTerm(SearchField field, SearchOperator operator, String value) {
        this(field, operator, value, "");
    }

    public SearchTerm(SearchField field, SearchOperator operator, String value, String upperBound) {
        this(field, operator, value, upperBound, false);
    }

    public SearchTerm(SearchField field, SearchOperator operator, String value, String upperBound, boolean strict) {
        this(field, operator, value, upperBound, SearchOperator.exact, "", "", strict);
    }

    public SearchTerm(
        SearchField field,
        SearchOperator operator,
        String value,
        String upperBound,
        SearchOperator amountOperator,
        String amount,
        String amountUpperBound
    ) {
        this(field, operator, value, upperBound, amountOperator, amount, amountUpperBound, false);
    }

    public boolean hasAmount() {
        return !amount.isBlank() || !amountUpperBound.isBlank();
    }
}
