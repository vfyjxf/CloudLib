package dev.vfyjxf.cloudlib.api.search;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Set-scoped serialization codecs for search terms and expressions: fields
 * serialize by canonical name and resolve against the owning
 * {@link SearchFieldSet}, so persisted queries stay valid as long as the
 * scenario's field vocabulary does.
 */
public final class SearchCodecs {
    private final SearchFieldSet fields;

    private SearchCodecs(SearchFieldSet fields) {
        this.fields = fields;
    }

    public static SearchCodecs of(SearchFieldSet fields) {
        return new SearchCodecs(fields);
    }

    public Codec<SearchTerm> term() {
        return RecordCodecBuilder.create(instance -> instance.group(
            fields.fieldCodec().fieldOf("field").forGetter(SearchTerm::field),
            SearchOperator.CODEC.fieldOf("operator").forGetter(SearchTerm::operator),
            Codec.STRING.fieldOf("value").forGetter(SearchTerm::value),
            Codec.STRING.optionalFieldOf("upper_bound", "").forGetter(SearchTerm::upperBound),
            SearchOperator.CODEC.optionalFieldOf("amount_operator", SearchOperator.exact).forGetter(SearchTerm::amountOperator),
            Codec.STRING.optionalFieldOf("amount", "").forGetter(SearchTerm::amount),
            Codec.STRING.optionalFieldOf("amount_upper_bound", "").forGetter(SearchTerm::amountUpperBound),
            Codec.BOOL.fieldOf("strict").forGetter(SearchTerm::strict)
        ).apply(instance, SearchTerm::new));
    }

    public Codec<SearchExpression> expression() {
        return Codec.recursive(
            "search_expression",
            self -> Codec.STRING.dispatch("type", SearchExpression::codecType, type -> codecForType(type, self))
        );
    }

    private MapCodec<? extends SearchExpression> codecForType(String type, Codec<SearchExpression> self) {
        return switch (type) {
            case "all" -> MapCodec.unit(new SearchExpression.All());
            case "and" -> self.listOf().fieldOf("terms").xmap(SearchExpression.And::new, SearchExpression.And::terms);
            case "or" -> self.listOf().fieldOf("terms").xmap(SearchExpression.Or::new, SearchExpression.Or::terms);
            case "not" -> self.fieldOf("term").xmap(SearchExpression.Not::new, SearchExpression.Not::term);
            case "term" -> term().fieldOf("term").xmap(SearchExpression.Term::new, SearchExpression.Term::term);
            default -> throw new IllegalArgumentException("Unknown search expression type: " + type);
        };
    }
}
