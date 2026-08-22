package dev.vfyjxf.cloudlib.api.search;

import dev.vfyjxf.cloudlib.api.search.SearchTokenizer.Token;
import dev.vfyjxf.cloudlib.api.search.SearchTokenizer.TokenType;


import java.util.ArrayList;
import java.util.List;

public final class SearchParser {
    private final SearchFieldSet fields;
    private final SearchField defaultField;

    /**
     * Parses against a scenario's declared field set; bare tokens resolve to
     * {@code defaultField}, and a bare token with an amount modifier remaps to
     * the set's first virtual (aggregate) field when one exists.
     */
    public SearchParser(SearchFieldSet fields, SearchField defaultField) {
        this.fields = fields;
        this.defaultField = defaultField;
    }

    private List<Token> tokens = List.of();
    private int cursor;
    private final ArrayList<SearchSyntaxSpan> syntaxSpans = new ArrayList<>();

    public SearchExpression parse(String input) {
        return parseWithSyntax(input).expression();
    }

    public SearchParseResult parseWithSyntax(String input) {
        tokens = SearchTokenizer.tokenize(input);
        cursor = 0;
        syntaxSpans.clear();
        if (peek().type() == TokenType.end) {
            return new SearchParseResult(new SearchExpression.All(), List.of());
        }
        SearchExpression expression = parseOr();
        expect(TokenType.end, "Unexpected token");
        return new SearchParseResult(expression, syntaxSpans);
    }

    public List<SearchSyntaxSpan> partialSyntaxSpans() {
        return List.copyOf(syntaxSpans);
    }

    private SearchExpression parseOr() {
        ArrayList<SearchExpression> parts = new ArrayList<>();
        parts.add(parseAnd());
        while (peek().type() == TokenType.or) {
            Token or = advance();
            span(or, SearchSyntaxKind.keyword);
            if (!startsUnary(peek())) {
                throw new SearchParseException("Expected search term after OR", peek().position());
            }
            parts.add(parseAnd());
        }
        return collapseOr(parts);
    }

    private SearchExpression parseAnd() {
        ArrayList<SearchExpression> parts = new ArrayList<>();
        while (startsUnary(peek())) {
            parts.add(parseUnary());
            if (peek().type() == TokenType.and) {
                span(advance(), SearchSyntaxKind.keyword);
                if (!startsUnary(peek())) {
                    throw new SearchParseException("Expected search term after AND", peek().position());
                }
            }
        }
        return collapseAnd(parts);
    }

    private SearchExpression parseUnary() {
        if (peek().type() == TokenType.not) {
            span(advance(), SearchSyntaxKind.negation);
            return new SearchExpression.Not(parseUnary());
        }
        Token token = peek();
        if (!token.quoted()
            && token.type() == TokenType.value
            && (token.text().startsWith("!") || token.text().startsWith("-"))
            && token.text().length() > 1) {
            advance();
            addSpan(token.position(), token.position() + 1, SearchSyntaxKind.negation);
            return new SearchExpression.Not(parseTerm(new Token(
                TokenType.value,
                token.text().substring(1),
                false,
                token.position() + 1,
                token.end()
            )));
        }
        return parsePrimary();
    }

    private SearchExpression parsePrimary() {
        if (peek().type() == TokenType.leftParen) {
            span(advance(), SearchSyntaxKind.keyword);
            SearchExpression expression = parseOr();
            span(expect(TokenType.rightParen, "Unclosed group"), SearchSyntaxKind.keyword);
            return expression;
        }
        return parseTerm(advanceValue("Expected search term"));
    }

    private SearchExpression parseTerm(Token first) {
        ParsedTerm parsed = parseFieldAndValue(first);
        SearchField field = parsed.field();
        String value = parsed.value();
        boolean strict = parsed.strict();
        SearchOperator operator = SearchOperator.contains;
        String upperBound = "";
        AmountComparison amount = null;
        boolean negateOperator = false;

        if (parsed.inlineField() && value.isEmpty() && peek().type() == TokenType.value) {
            Token valueToken = advance();
            value = valueToken.text();
            strict = valueToken.quoted();
            spanValue(valueToken, field);
        }
        if (peek().type() == TokenType.operator) {
            Token op = advance();
            span(op, SearchSyntaxKind.operator);
            if (!parsed.inlineField()) {
                if (!fields.isKnownAlias(value)) {
                    throw new SearchParseException("Unknown search field " + value, first.position());
                }
                field = fields.byAlias(value);
                if (field == null) {
                    throw new SearchParseException("Unknown search field " + value, first.position());
                }
            }
            Token next = advanceValue("Expected value after operator");
            negateOperator = op.text().startsWith("!");
            if (field.contentChannel() && isNumericComparisonOperator(op.text())) {
                throw new SearchParseException("Content amount comparisons must use * amount syntax", op.position());
            } else {
                value = next.text();
                strict = next.quoted();
                spanValue(next, field);
                operator = operatorFrom(op.text(), value);
            }
        } else if ("*".equals(value)) {
            operator = SearchOperator.exists;
        } else if (value.contains("..") && field.numeric()) {
            int index = value.indexOf("..");
            upperBound = value.substring(index + 2);
            value = value.substring(0, index);
            operator = SearchOperator.range;
        } else if (containsWildcard(value)) {
            operator = SearchOperator.wildcard;
        }

        if (amount == null && isContentOrDefaultContentField(field)) {
            amount = tryReadContentAmountModifier(field);
            if (amount != null && field == defaultField && fields.virtualFields().length > 0) {
                field = fields.virtualFields()[0];
            }
        }

        SearchTerm searchTerm = amount == null
            ? new SearchTerm(field, operator, value, upperBound, strict)
            : new SearchTerm(field, operator, value, upperBound, amount.operator(), amount.value(), amount.upperBound(), strict);
        SearchExpression term = new SearchExpression.Term(searchTerm);
        return negateOperator ? new SearchExpression.Not(term) : term;
    }

    private AmountComparison tryReadContentAmountModifier(SearchField field) {
        if (peek().type() != TokenType.value || peek().quoted() || !"*".equals(peek().text())) {
            return null;
        }
        Token star = advance();
        span(star, SearchSyntaxKind.operator);
        if (peek().type() == TokenType.operator) {
            Token op = advance();
            span(op, SearchSyntaxKind.operator);
            Token value = advanceValue("Expected amount after operator");
            return amountFromOperator(op.text(), value);
        }
        Token value = advanceValue("Expected amount after *");
        spanAmountValue(value);
        return amountComparison(SearchOperator.exact, value.text(), "");
    }

    private AmountComparison amountFromOperator(String operatorText, Token value) {
        spanAmountValue(value);
        return amountComparison(amountOperatorFrom(operatorText), value.text(), "");
    }

    private AmountComparison amountComparison(SearchOperator operator, String value, String upperBound) {
        if (value.contains("..")) {
            int index = value.indexOf("..");
            return new AmountComparison(SearchOperator.range, value.substring(0, index), value.substring(index + 2));
        }
        return new AmountComparison(operator, value, upperBound);
    }

    private SearchOperator amountOperatorFrom(String operator) {
        return switch (operator) {
            case "", "=" -> SearchOperator.exact;
            case ">" -> SearchOperator.greaterThan;
            case ">=" -> SearchOperator.greaterThanOrEqual;
            case "<" -> SearchOperator.lessThan;
            case "<=" -> SearchOperator.lessThanOrEqual;
            default -> throw new SearchParseException("Unsupported amount operator " + operator, previous().position());
        };
    }



    private boolean isContentOrDefaultContentField(SearchField field) {
        return field.contentChannel() || field == defaultField;
    }

    private boolean isNumericComparisonOperator(String operator) {
        return switch (operator) {
            case ">", ">=", "<", "<=" -> true;
            default -> false;
        };
    }

    private ParsedTerm parseFieldAndValue(Token token) {
        if (token.quoted()) {
            spanQuoted(token);
            return new ParsedTerm(defaultField, token.text(), false, true);
        }
        String value = token.text();
        SearchField prefixField = value.length() > 1 ? fields.byJeiPrefix(value.charAt(0)) : null;
        if (prefixField != null) {
            addSpan(token.position(), token.position() + 1, SearchSyntaxKind.prefix);
            addValueSpans(token.position() + 1, value.substring(1), prefixField);
            return new ParsedTerm(prefixField, value.substring(1), true, false);
        }

        int colon = value.indexOf(':');
        if (colon > 0) {
            String alias = value.substring(0, colon);
            if (fields.isKnownAlias(alias)) {
                SearchField field = fields.byAlias(alias);
                addSpan(token.position(), token.position() + colon, SearchSyntaxKind.field);
                addSpan(token.position() + colon, token.position() + colon + 1, SearchSyntaxKind.operator);
                addValueSpans(token.position() + colon + 1, value.substring(colon + 1), field);
                return new ParsedTerm(field, value.substring(colon + 1), true, false);
            }
        }
        span(token, fields.isKnownAlias(value) ? SearchSyntaxKind.field : SearchSyntaxKind.text);
        return new ParsedTerm(defaultField, value, false, false);
    }

    private SearchOperator operatorFrom(String operator, String value) {
        return switch (operator) {
            case ":", "~", "!~" -> containsWildcard(value) ? SearchOperator.wildcard : SearchOperator.contains;
            case "=", "!=" -> containsWildcard(value) ? SearchOperator.wildcard : SearchOperator.exact;
            case ">" -> SearchOperator.greaterThan;
            case ">=" -> SearchOperator.greaterThanOrEqual;
            case "<" -> SearchOperator.lessThan;
            case "<=" -> SearchOperator.lessThanOrEqual;
            default -> throw new SearchParseException("Unsupported operator " + operator, previous().position());
        };
    }

    private boolean startsUnary(Token token) {
        return switch (token.type()) {
            case value, leftParen, not -> true;
            default -> false;
        };
    }

    private SearchExpression collapseAnd(List<SearchExpression> parts) {
        if (parts.isEmpty()) {
            return new SearchExpression.All();
        }
        return parts.size() == 1 ? parts.getFirst() : new SearchExpression.And(List.copyOf(parts));
    }

    private SearchExpression collapseOr(List<SearchExpression> parts) {
        return parts.size() == 1 ? parts.getFirst() : new SearchExpression.Or(List.copyOf(parts));
    }

    private boolean containsWildcard(String value) {
        return value.indexOf('*') >= 0 || value.indexOf('?') >= 0;
    }

    private boolean match(TokenType type) {
        if (peek().type() != type) {
            return false;
        }
        cursor++;
        return true;
    }

    private Token expect(TokenType type, String message) {
        Token token = peek();
        if (token.type() != type) {
            throw new SearchParseException(message + ": " + token.text(), token.position());
        }
        cursor++;
        return token;
    }

    private Token advanceValue(String message) {
        Token token = peek();
        if (token.type() != TokenType.value) {
            throw new SearchParseException(message, token.position());
        }
        cursor++;
        return token;
    }

    private Token advance() {
        return tokens.get(cursor++);
    }

    private Token peek() {
        return tokens.get(cursor);
    }

    private Token previous() {
        return tokens.get(Math.max(0, cursor - 1));
    }

    private void span(Token token, SearchSyntaxKind kind) {
        addSpan(token.position(), token.end(), kind);
    }

    private void spanValue(Token token, SearchField field) {
        if (token.quoted()) {
            spanQuoted(token);
            return;
        }
        addValueSpans(token.position(), token.text(), field);
    }

    private void spanAmountValue(Token token) {
        if (token.quoted()) {
            spanQuoted(token);
            return;
        }
        addNumericValueSpans(token.position(), token.text());
    }

    private void addValueSpans(int start, String value, SearchField field) {
        if (value.isEmpty()) {
            return;
        }
        if (field.numeric()) {
            addNumericValueSpans(start, value);
            return;
        }

        int segmentStart = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != '*' && c != '?') {
                continue;
            }
            addSpan(start + segmentStart, start + i, SearchSyntaxKind.text);
            addSpan(start + i, start + i + 1, SearchSyntaxKind.operator);
            segmentStart = i + 1;
        }
        addSpan(start + segmentStart, start + value.length(), SearchSyntaxKind.text);
    }

    private void addNumericValueSpans(int start, String value) {
        if (value.isEmpty()) {
            return;
        }
        int range = value.indexOf("..");
        if (range >= 0) {
            addSpan(start, start + range, SearchSyntaxKind.text);
            addSpan(start + range, start + range + 2, SearchSyntaxKind.operator);
            addNumericValueSpans(start + range + 2, value.substring(range + 2));
            return;
        }
        addSpan(start, start + value.length(), SearchSyntaxKind.text);
    }

    private void spanQuoted(Token token) {
        addSpan(token.position(), token.position() + 1, SearchSyntaxKind.quote);
        if (token.end() > token.position() + 2) {
            addSpan(token.position() + 1, token.end() - 1, SearchSyntaxKind.text);
        }
        if (token.end() > token.position() + 1) {
            addSpan(token.end() - 1, token.end(), SearchSyntaxKind.quote);
        }
    }

    private void addSpan(int start, int end, SearchSyntaxKind kind) {
        if (start >= end) {
            return;
        }
        syntaxSpans.add(new SearchSyntaxSpan(start, end, kind));
    }

    private record ParsedTerm(SearchField field, String value, boolean inlineField, boolean strict) {
    }

    private record AmountComparison(SearchOperator operator, String value, String upperBound) {
    }
}
