package dev.vfyjxf.cloudlib.api.search;

import java.util.Objects;

public final class SearchContentValue {
    private final SearchField field;
    private final String value;
    private final double amount;
    private final String normalizedValue;

    public SearchContentValue(SearchField field, String value, double amount) {
        this.field = field;
        this.value = value;
        this.amount = amount;
        this.normalizedValue = SearchTexts.normalize(value);
    }

    public SearchField field() {
        return field;
    }

    public String value() {
        return value;
    }

    public double amount() {
        return amount;
    }

    public String normalizedValue() {
        return normalizedValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SearchContentValue that)) return false;
        return Double.compare(amount, that.amount) == 0
            && Objects.equals(field, that.field)
            && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, value, amount);
    }

    @Override
    public String toString() {
        return "SearchContentValue[field=" + field + ", value=" + value + ", amount=" + amount + "]";
    }
}
