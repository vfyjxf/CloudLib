package dev.vfyjxf.cloudlib.api.search;

import com.mojang.serialization.Codec;

public enum SearchOperator {
    contains,
    exact,
    wildcard,
    range,
    greaterThan,
    greaterThanOrEqual,
    lessThan,
    lessThanOrEqual,
    exists;

    public static final Codec<SearchOperator> CODEC = dev.vfyjxf.cloudlib.api.util.Codecs.lowerCaseEnum(SearchOperator.class);
}
