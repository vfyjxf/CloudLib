package dev.vfyjxf.cloudlib.api.search;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SearchMatcher {
    private SearchMatcher() {
    }

    public static boolean matches(SearchExpression expression, SearchableDocument document) {
        return match(expression, document).matched();
    }

    public static SearchMatch match(SearchExpression expression, SearchableDocument document) {
        return switch (expression) {
            case SearchExpression.All ignored -> SearchMatch.YES;
            case SearchExpression.And and -> matchAnd(and, document);
            case SearchExpression.Or or -> matchOr(or, document);
            case SearchExpression.Not not -> match(not.term(), document).matched() ? SearchMatch.NO : SearchMatch.YES;
            case SearchExpression.Term term -> matchTerm(term.term(), document);
        };
    }

    private static SearchMatch matchAnd(SearchExpression.And and, SearchableDocument document) {
        int score = 0;
        for (SearchExpression term : and.terms()) {
            SearchMatch match = match(term, document);
            if (!match.matched()) {
                return SearchMatch.NO;
            }
            score += match.score();
        }
        return new SearchMatch(true, score);
    }

    private static SearchMatch matchOr(SearchExpression.Or or, SearchableDocument document) {
        boolean matched = false;
        int best = 0;
        for (SearchExpression term : or.terms()) {
            SearchMatch match = match(term, document);
            if (match.matched()) {
                matched = true;
                best = Math.max(best, match.score());
            }
        }
        return matched ? new SearchMatch(true, best) : SearchMatch.NO;
    }

    private static SearchMatch matchTerm(SearchTerm term, SearchableDocument document) {
        if (term.hasAmount()) {
            return matchContentAmount(term, document);
        }
        return switch (term.operator()) {
            case exists -> document.hasField(term.field()) ? SearchMatch.YES : SearchMatch.NO;
            case contains -> matchContains(document, term.field(), term.value(), term.strict());
            case exact -> matchExact(document, term.field(), term.value(), term.strict());
            case wildcard -> matchWildcard(document, term.field(), term.value(), term.strict());
            case range -> matchRange(document, term.field(), term.value(), term.upperBound());
            case greaterThan -> matchCompare(document, term.field(), term.value(), value -> value > 0);
            case greaterThanOrEqual -> matchCompare(document, term.field(), term.value(), value -> value >= 0);
            case lessThan -> matchCompare(document, term.field(), term.value(), value -> value < 0);
            case lessThanOrEqual -> matchCompare(document, term.field(), term.value(), value -> value <= 0);
        };
    }

    private static SearchMatch matchContentAmount(SearchTerm term, SearchableDocument document) {
        String comparableNeedle = comparable(term.value(), term.strict());
        int best = 0;
        for (SearchContentValue entry : document.contentEntries(term.field())) {
            String comparableValue = term.strict() ? entry.value() : entry.normalizedValue();
            if (!matchesContentText(term, entry.value(), comparableValue, comparableNeedle)) {
                continue;
            }
            if (!matchesNumber(term.field(), entry.amount(), term.amountOperator(), term.amount(), term.amountUpperBound())) {
                continue;
            }
            best = Math.max(best, score(entry.field(), comparableValue, comparableNeedle) + 40);
        }
        return best == 0 ? SearchMatch.NO : new SearchMatch(true, best);
    }

    private static boolean matchesContentText(SearchTerm term, String rawValue, String comparableValue, String comparableNeedle) {
        return switch (term.operator()) {
            case exists -> true;
            case contains -> SearchTexts.contains(comparableValue, comparableNeedle);
            case exact -> comparableValue.equals(comparableNeedle);
            case wildcard -> wildcardPattern(term.value(), term.strict())
                .matcher(rawValue)
                .find();
            default -> false;
        };
    }

    private static SearchMatch matchContains(SearchableDocument document, SearchField field, String rawNeedle, boolean strict) {
        PreparedNeedle needle = PreparedNeedle.of(rawNeedle, strict);
        if (needle.get().isEmpty()) return SearchMatch.YES;

        SearchDocument.TextBucket bucket;
        if (document instanceof SearchDocument sd) {
            bucket = sd.textBucket(field);
        } else {
            List<SearchTextValue> list = document.textEntries(field);
            bucket = new SearchDocument.TextBucket(
                list.toArray(new SearchTextValue[0]),
                "", "");
        }

        // Negative filter: if needle not in aggregate haystack, no entry can match
        if (bucket.entries().length > 1 && needle.get().length() >= 2) {
            String haystack = strict ? bucket.rawHaystack() : bucket.normalizedHaystack();
            if (!SearchTexts.contains(haystack, needle.get())) {
                return SearchMatch.NO;
            }
        }

        int best = 0;
        for (SearchTextValue entry : bucket.entries()) {
            String value = strict ? entry.value() : entry.normalizedValue();
            if (SearchTexts.contains(value, needle.get())) {
                best = Math.max(best, score(entry.field(), value, needle.get()));
            }
        }
        return best == 0 ? SearchMatch.NO : new SearchMatch(true, best);
    }

    private static SearchMatch matchExact(SearchableDocument document, SearchField field, String rawExpected, boolean strict) {
        Double numericExpected = tryParseNumber(field, rawExpected);
        if (numericExpected != null) {
            for (double value : numericValues(document, field)) {
                if (Double.compare(value, numericExpected) == 0) {
                    return new SearchMatch(true, field.weight() + 80);
                }
            }
        }

        PreparedNeedle expected = PreparedNeedle.of(rawExpected, strict);
        int best = 0;

        SearchDocument.TextBucket bucket;
        if (document instanceof SearchDocument sd) {
            bucket = sd.textBucket(field);
        } else {
            List<SearchTextValue> list = document.textEntries(field);
            bucket = new SearchDocument.TextBucket(
                list.toArray(new SearchTextValue[0]), "", "");
        }

        for (SearchTextValue entry : bucket.entries()) {
            String value = strict ? entry.value() : entry.normalizedValue();
            if (value.equals(expected.get())) {
                best = Math.max(best, entry.field().weight() + 80);
            }
        }
        return best == 0 ? SearchMatch.NO : new SearchMatch(true, best);
    }

    private static SearchMatch matchWildcard(SearchableDocument document, SearchField field, String pattern, boolean strict) {
        Pattern compiled = wildcardPattern(pattern, strict);
        int best = 0;
        for (SearchTextValue entry : document.textEntries(field)) {
            if (compiled.matcher(entry.value()).find()) {
                best = Math.max(best, entry.field().weight() + 40);
            }
        }
        return best == 0 ? SearchMatch.NO : new SearchMatch(true, best);
    }

    private static SearchMatch matchRange(SearchableDocument document, SearchField field, String left, String right) {
        Double min = left.isBlank() ? null : parseNumber(field, left);
        Double max = right.isBlank() ? null : parseNumber(field, right);
        for (double value : numericValues(document, field)) {
            if (matchesRange(field, value, left, right)) {
                return new SearchMatch(true, field.weight() + 40);
            }
        }
        return SearchMatch.NO;
    }

    private static SearchMatch matchCompare(SearchableDocument document, SearchField field, String raw, Comparison comparison) {
        double expected = parseNumber(field, raw);
        for (double value : numericValues(document, field)) {
            if (comparison.matches(Double.compare(value, expected))) {
                return new SearchMatch(true, field.weight() + 40);
            }
        }
        return SearchMatch.NO;
    }

    private static boolean matchesNumber(SearchField field, double actual, SearchOperator operator, String value, String upperBound) {
        return switch (operator) {
            case exact -> Double.compare(actual, parseNumber(field, value)) == 0;
            case range -> matchesRange(field, actual, value, upperBound);
            case greaterThan -> Double.compare(actual, parseNumber(field, value)) > 0;
            case greaterThanOrEqual -> Double.compare(actual, parseNumber(field, value)) >= 0;
            case lessThan -> Double.compare(actual, parseNumber(field, value)) < 0;
            case lessThanOrEqual -> Double.compare(actual, parseNumber(field, value)) <= 0;
            default -> false;
        };
    }

    private static boolean matchesRange(SearchField field, double value, String left, String right) {
        Double min = left.isBlank() ? null : parseNumber(field, left);
        Double max = right.isBlank() ? null : parseNumber(field, right);
        return (min == null || value >= min) && (max == null || value <= max);
    }

    private static Iterable<Double> numericValues(SearchableDocument document, SearchField field) {
        var numbers = document.numberValues(field);
        if (!numbers.isEmpty()) {
            return numbers;
        }
        return document.textValues(field)
            .stream()
            .map(value -> tryParseNumber(field, value))
            .filter(value -> value != null)
            .toList();
    }

    private static double parseNumber(SearchField field, String raw) {
        Double value = tryParseNumber(field, raw);
        if (value == null) {
            throw new SearchParseException("Invalid numeric value " + raw, 0);
        }
        return value;
    }

    private static Double tryParseNumber(SearchField field, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return switch (field.spec().unit()) {
                case time -> (double) UnitParser.parseTime(raw);
                case energy -> (double) UnitParser.parseEnergy(raw);
                case chance -> UnitParser.parseChance(raw);
                default -> Double.parseDouble(raw.trim());
            };
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static int score(SearchField field, String value, String needle) {
        int score = field.weight();
        if (value.equals(needle)) {
            return score + 80;
        }
        if (value.startsWith(needle)) {
            return score + 50;
        }
        if (value.contains(":" + needle) || value.contains("_" + needle) || value.contains(" " + needle)) {
            return score + 30;
        }
        return score + 10;
    }

    private static String wildcardRegex(String pattern) {
        StringBuilder regex = new StringBuilder();
        for (int index = 0; index < pattern.length(); index++) {
            char c = pattern.charAt(index);
            switch (c) {
                case '*' -> regex.append(".*");
                case '?' -> regex.append('.');
                default -> regex.append(Pattern.quote(Character.toString(c)));
            }
        }
        return regex.toString();
    }

    private static Pattern wildcardPattern(String pattern, boolean strict) {
        int flags = strict ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        return Pattern.compile(wildcardRegex(pattern), flags);
    }

    private static String comparable(String value, boolean strict) {
        return strict ? value : normalize(value);
    }

    private static String normalize(String value) {
        return SearchTexts.normalize(value);
    }

    private record PreparedNeedle(String raw, String normalized, boolean strict) {
        String get() { return strict ? raw : normalized; }
        static PreparedNeedle of(String raw, boolean strict) {
            return new PreparedNeedle(raw, strict ? raw : normalize(raw), strict);
        }
    }

    private interface Comparison {
        boolean matches(int value);
    }
}
