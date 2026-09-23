/* Ported from katana-parser (MIT, (c) 2015 Hackers and Painters) — see LICENSE-katana.txt */
package dev.vfyjxf.cloudlib.api.css;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Hand-written implementation of the CSS Syntax Module Level 3 tokenizer ("consume a token"),
 * replacing katana's flex-generated {@code katana.lex.c}. Works on a cursor over the preprocessed
 * input (CR/FF/CRLF folded to LF, NUL replaced by U+FFFD) and never throws: bad strings, bad urls
 * and unclosed constructs produce error entries plus their spec-mandated recovery tokens.
 */
final class CssTokenizer {

    /** All CSS Syntax L3 token kinds. */
    enum TokenKind {
        ident, function, atKeyword, hash, string, badString, url, badUrl, delim, number, percentage, dimension, whitespace, cdo, cdc, colon, semicolon, comma, leftParen, rightParen, leftSquare, rightSquare, leftCurly, rightCurly, unicodeRange, eof
    }

    /**
     * A token. Fields are populated per kind:
     *
     * <ul>
     *   <li>{@code string} — decoded value for ident/function/atKeyword/hash/string/url; the raw
     *       numeric text for number/percentage/dimension; raw text for unicodeRange
     *   <li>{@code number}/{@code unit}/{@code integer}/{@code signed} — numeric tokens
     *   <li>{@code delim} — the code point for {@link TokenKind#delim}
     *   <li>{@code idFlag} — whether a hash would start an identifier
     *   <li>{@code rangeStart}/{@code rangeEnd} — unicode ranges
     * </ul>
     */
    record Token(
        TokenKind kind,
        @Nullable String string,
        double number,
        @Nullable String unit,
        boolean integer,
        boolean signed,
        char delim,
        boolean idFlag,
        int rangeStart,
        int rangeEnd,
        int start,
        int end,
        int line,
        int column
    ) {

        static Token bare(TokenKind kind, int start, int end, int line, int column) {
            return new Token(kind, null, 0, null, false, false, '\0', false, 0, 0, start, end, line, column);
        }

        static Token text(TokenKind kind, String string, int start, int end, int line, int column) {
            return new Token(kind, string, 0, null, false, false, '\0', false, 0, 0, start, end, line, column);
        }

        static Token delimiter(char c, int start, int end, int line, int column) {
            return new Token(TokenKind.delim, null, 0, null, false, false, c, false, 0, 0, start, end, line, column);
        }
    }

    private final String input;
    private final List<CssError> errors;
    private final List<Token> tokens = new ArrayList<>();

    private int pos;
    private int line = 1;
    private int column = 1;
    private int tokenStart;
    private int tokenLine;
    private int tokenColumn;

    private CssTokenizer(String input, List<CssError> errors) {
        this.input = preprocess(input);
        this.errors = errors;
    }

    /** Tokenizes {@code input}; the returned list always ends with an {@link TokenKind#eof} token. */
    static List<Token> tokenize(String input, List<CssError> errors) {
        return new CssTokenizer(input, errors).run();
    }

    // region input preprocessing
    /** CSS Syntax L3 §3.3: CRLF/CR/FF → LF, NUL → U+FFFD. */
    private static String preprocess(String raw) {
        StringBuilder out = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\r') {
                if (i + 1 < raw.length() && raw.charAt(i + 1) == '\n') {
                    i++;
                }
                out.append('\n');
            } else if (c == '\f') {
                out.append('\n');
            } else if (c == '\0') {
                out.append('\uFFFD');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    // endregion

    // region cursor helpers
    private char at(int i) {
        return i < input.length() ? input.charAt(i) : '\0';
    }

    private boolean eof() {
        return pos >= input.length();
    }

    private boolean eofAt(int i) {
        return i >= input.length();
    }

    /** Consumes one char, tracking line/column. */
    private char advance() {
        char c = input.charAt(pos++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    private void advance(int n) {
        for (int i = 0; i < n; i++) {
            advance();
        }
    }

    private void error(String message) {
        errors.add(new CssError(tokenLine, tokenColumn, message));
    }

    private void errorHere(String message) {
        errors.add(new CssError(line, column, message));
    }

    private void mark() {
        tokenStart = pos;
        tokenLine = line;
        tokenColumn = column;
    }

    private void emit(TokenKind kind) {
        tokens.add(Token.bare(kind, tokenStart, pos, tokenLine, tokenColumn));
    }

    private void emit(TokenKind kind, String value) {
        tokens.add(Token.text(kind, value, tokenStart, pos, tokenLine, tokenColumn));
    }

    private void emitDelim(char c) {
        tokens.add(Token.delimiter(c, tokenStart, pos, tokenLine, tokenColumn));
    }

    // endregion

    // region character classes
    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\n' || c == '\t';
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isHexDigit(char c) {
        return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private static boolean isNonAscii(char c) {
        return c >= 0x80;
    }

    private static boolean isIdentStart(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || isNonAscii(c);
    }

    private static boolean isIdentChar(char c) {
        return isIdentStart(c) || isDigit(c) || c == '-';
    }

    private static boolean isNonPrintable(char c) {
        return (c <= 0x08) || c == 0x0B || (c >= 0x0E && c <= 0x1F) || c == 0x7F;
    }

    /** Spec "check if two code points are a valid escape": {@code \} not followed by newline. */
    private boolean validEscapeAt(int i) {
        return at(i) == '\\' && (eofAt(i + 1) || at(i + 1) != '\n');
    }

    /** Spec "check if three code points would start an ident sequence", starting at {@code i}. */
    private boolean wouldStartIdent(int i) {
        char c1 = at(i);
        if (c1 == '-') {
            char c2 = at(i + 1);
            return isIdentStart(c2) || c2 == '-' || (c2 == '\\' && validEscapeAt(i + 1));
        }
        if (isIdentStart(c1)) {
            return true;
        }
        return c1 == '\\' && validEscapeAt(i);
    }

    /** Spec "check if three code points would start a number", starting at {@code i}. */
    private boolean wouldStartNumber(int i) {
        char c1 = at(i);
        if (c1 == '+' || c1 == '-') {
            return isDigit(at(i + 1)) || (at(i + 1) == '.' && isDigit(at(i + 2)));
        }
        if (c1 == '.') {
            return isDigit(at(i + 1));
        }
        return isDigit(c1);
    }

    /** Spec "check if three code points would start a unicode range" (cursor is on u/U). */
    private boolean wouldStartUnicodeRange() {
        return at(pos + 1) == '+' && (isHexDigit(at(pos + 2)) || at(pos + 2) == '?');
    }

    // endregion

    // region main loop
    private List<Token> run() {
        while (true) {
            consumeComments();
            if (eof()) {
                mark();
                emit(TokenKind.eof);
                return tokens;
            }
            mark();
            consumeToken();
        }
    }

    /** Spec "consume comments": runs of {@code /* … *}{@code /}; an unclosed comment ends input. */
    private void consumeComments() {
        while (at(pos) == '/' && at(pos + 1) == '*') {
            int commentLine = line;
            int commentColumn = column;
            advance(2);
            boolean closed = false;
            while (!eof()) {
                if (at(pos) == '*' && at(pos + 1) == '/') {
                    advance(2);
                    closed = true;
                    break;
                }
                advance();
            }
            if (!closed) {
                errors.add(new CssError(commentLine, commentColumn, "unclosed comment"));
            }
        }
    }

    /** Spec "consume a token". */
    private void consumeToken() {
        char c = at(pos);
        switch (c) {
            case ' ', '\n', '\t' -> {
                while (isWhitespace(at(pos))) {
                    advance();
                }
                emit(TokenKind.whitespace);
            }
            case '"', '\'' -> consumeString(c);
            case '#' -> consumeHash();
            case '(' -> {
                advance();
                emit(TokenKind.leftParen);
            }
            case ')' -> {
                advance();
                emit(TokenKind.rightParen);
            }
            case '[' -> {
                advance();
                emit(TokenKind.leftSquare);
            }
            case ']' -> {
                advance();
                emit(TokenKind.rightSquare);
            }
            case '{' -> {
                advance();
                emit(TokenKind.leftCurly);
            }
            case '}' -> {
                advance();
                emit(TokenKind.rightCurly);
            }
            case ',' -> {
                advance();
                emit(TokenKind.comma);
            }
            case ':' -> {
                advance();
                emit(TokenKind.colon);
            }
            case ';' -> {
                advance();
                emit(TokenKind.semicolon);
            }
            case '+' -> {
                if (wouldStartNumber(pos)) {
                    consumeNumericToken();
                } else {
                    advance();
                    emitDelim('+');
                }
            }
            case '-' -> {
                if (wouldStartNumber(pos)) {
                    consumeNumericToken();
                } else if (input.startsWith("-->", pos)) {
                    advance(3);
                    emit(TokenKind.cdc);
                } else if (wouldStartIdent(pos)) {
                    consumeIdentLike();
                } else {
                    advance();
                    emitDelim('-');
                }
            }
            case '.' -> {
                if (wouldStartNumber(pos)) {
                    consumeNumericToken();
                } else {
                    advance();
                    emitDelim('.');
                }
            }
            case '<' -> {
                if (input.startsWith("<!--", pos)) {
                    advance(4);
                    emit(TokenKind.cdo);
                } else {
                    advance();
                    emitDelim('<');
                }
            }
            case '@' -> {
                advance();
                if (wouldStartIdent(pos)) {
                    emit(TokenKind.atKeyword, consumeName());
                } else {
                    emitDelim('@');
                }
            }
            case '\\' -> {
                if (validEscapeAt(pos)) {
                    consumeIdentLike();
                } else {
                    error("stray backslash");
                    advance();
                    emitDelim('\\');
                }
            }
            case 'u', 'U' -> {
                if (wouldStartUnicodeRange()) {
                    consumeUnicodeRange();
                } else {
                    consumeIdentLike();
                }
            }
            default -> {
                if (isDigit(c)) {
                    consumeNumericToken();
                } else if (isIdentStart(c)) {
                    consumeIdentLike();
                } else {
                    advance();
                    emitDelim(c);
                }
            }
        }
    }

    // endregion

    // region token consumers
    /** Spec "consume an ident-like token". */
    private void consumeIdentLike() {
        String name = consumeName();
        if (at(pos) == '(') {
            advance();
            if (name.equalsIgnoreCase("url")) {
                int j = pos;
                while (isWhitespace(at(j))) {
                    j++;
                }
                if (at(j) == '"' || at(j) == '\'') {
                    emit(TokenKind.function, name);
                    return;
                }
                consumeUrlToken();
                return;
            }
            emit(TokenKind.function, name);
            return;
        }
        emit(TokenKind.ident, name);
    }

    /** Spec "consume an ident sequence" — handles escapes. */
    private String consumeName() {
        StringBuilder out = new StringBuilder();
        while (!eof()) {
            char c = at(pos);
            if (isIdentChar(c)) {
                out.append(c);
                advance();
            } else if (c == '\\' && validEscapeAt(pos)) {
                advance();
                out.appendCodePoint(consumeEscapedCodePoint());
            } else {
                break;
            }
        }
        return out.toString();
    }

    /**
     * Spec "consume an escaped code point" — the cursor is just past the backslash.
     *
     * @return the decoded code point, or U+FFFD on EOF/surrogate/out-of-range
     */
    private int consumeEscapedCodePoint() {
        if (eof()) {
            error("escape at end of input");
            return 0xFFFD;
        }
        char c = at(pos);
        if (isHexDigit(c)) {
            int value = 0;
            int digits = 0;
            while (digits < 6 && isHexDigit(at(pos))) {
                value = value * 16 + Character.digit(at(pos), 16);
                advance();
                digits++;
            }
            if (isWhitespace(at(pos))) {
                advance();
            }
            if (value == 0 || value > 0x10FFFF || (value >= 0xD800 && value <= 0xDFFF)) {
                error("invalid escaped code point");
                return 0xFFFD;
            }
            return value;
        }
        return advance();
    }

    /** Spec "consume a string token" — the cursor is on the opening {@code ending} quote. */
    private void consumeString(char ending) {
        advance();
        StringBuilder out = new StringBuilder();
        while (true) {
            if (eof()) {
                error("unclosed string");
                emit(TokenKind.string, out.toString());
                return;
            }
            char c = at(pos);
            if (c == ending) {
                advance();
                emit(TokenKind.string, out.toString());
                return;
            }
            if (c == '\n') {
                error("bad string (newline inside string)");
                emit(TokenKind.badString, out.toString());
                return;
            }
            if (c == '\\') {
                if (eofAt(pos + 1)) {
                    advance();
                    continue;
                }
                if (at(pos + 1) == '\n') {
                    advance(2);
                    continue;
                }
                advance();
                out.appendCodePoint(consumeEscapedCodePoint());
                continue;
            }
            out.append(c);
            advance();
        }
    }

    /** Spec "consume a hash token" — the cursor is on {@code #}. */
    private void consumeHash() {
        advance();
        if (isIdentChar(at(pos)) || validEscapeAt(pos)) {
            boolean id = wouldStartIdent(pos);
            String value = consumeName();
            tokens.add(
                new Token(
                    TokenKind.hash,
                    value,
                    0,
                    null,
                    false,
                    false,
                    '\0',
                    id,
                    0,
                    0,
                    tokenStart,
                    pos,
                    tokenLine,
                    tokenColumn
                )
            );
            return;
        }
        emitDelim('#');
    }

    /**
     * Spec "consume a url token" — {@code url(} has been consumed and the next non-whitespace code
     * point is not a quote.
     */
    private void consumeUrlToken() {
        while (isWhitespace(at(pos))) {
            advance();
        }
        StringBuilder out = new StringBuilder();
        while (true) {
            if (eof()) {
                error("unclosed url");
                emit(TokenKind.url, out.toString());
                return;
            }
            char c = at(pos);
            if (c == ')') {
                advance();
                emit(TokenKind.url, out.toString());
                return;
            }
            if (isWhitespace(c)) {
                while (isWhitespace(at(pos))) {
                    advance();
                }
                if (eof()) {
                    error("unclosed url");
                    emit(TokenKind.url, out.toString());
                    return;
                }
                if (at(pos) == ')') {
                    advance();
                    emit(TokenKind.url, out.toString());
                    return;
                }
                error("whitespace inside url");
                consumeBadUrlRemnants();
                return;
            }
            if (c == '"' || c == '\'' || c == '(' || isNonPrintable(c)) {
                error("bad url");
                consumeBadUrlRemnants();
                return;
            }
            if (c == '\\') {
                if (validEscapeAt(pos)) {
                    advance();
                    out.appendCodePoint(consumeEscapedCodePoint());
                } else {
                    error("bad escape in url");
                    consumeBadUrlRemnants();
                    return;
                }
                continue;
            }
            out.append(c);
            advance();
        }
    }

    /** Spec "consume the remnants of a bad url". */
    private void consumeBadUrlRemnants() {
        while (!eof()) {
            char c = at(pos);
            if (c == ')') {
                advance();
                emit(TokenKind.badUrl, "");
                return;
            }
            if (c == '\\' && validEscapeAt(pos)) {
                advance();
                consumeEscapedCodePoint();
            } else {
                advance();
            }
        }
        emit(TokenKind.badUrl, "");
    }

    /** Spec "consume a numeric token". */
    private void consumeNumericToken() {
        consumeNumber();
        String raw = input.substring(tokenStart, pos);
        boolean signed = raw.startsWith("+") || raw.startsWith("-");
        boolean integer = raw.indexOf('.') < 0 && raw.indexOf('e') < 0 && raw.indexOf('E') < 0;
        double value;
        try {
            value = Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            value = Double.NaN;
        }
        if (wouldStartIdent(pos)) {
            String unit = consumeName();
            tokens.add(
                new Token(
                    TokenKind.dimension,
                    raw,
                    value,
                    unit,
                    integer,
                    signed,
                    '\0',
                    false,
                    0,
                    0,
                    tokenStart,
                    pos,
                    tokenLine,
                    tokenColumn
                )
            );
        } else if (at(pos) == '%') {
            advance();
            tokens.add(
                new Token(
                    TokenKind.percentage,
                    raw,
                    value,
                    "%",
                    integer,
                    signed,
                    '\0',
                    false,
                    0,
                    0,
                    tokenStart,
                    pos,
                    tokenLine,
                    tokenColumn
                )
            );
        } else {
            tokens.add(
                new Token(
                    TokenKind.number,
                    raw,
                    value,
                    null,
                    integer,
                    signed,
                    '\0',
                    false,
                    0,
                    0,
                    tokenStart,
                    pos,
                    tokenLine,
                    tokenColumn
                )
            );
        }
    }

    /** Spec "consume a number" — moves the cursor past the numeric text. */
    private void consumeNumber() {
        if (at(pos) == '+' || at(pos) == '-') {
            advance();
        }
        while (isDigit(at(pos))) {
            advance();
        }
        if (at(pos) == '.' && isDigit(at(pos + 1))) {
            advance();
            while (isDigit(at(pos))) {
                advance();
            }
        }
        char e = at(pos);
        if ((e == 'e' || e == 'E')
                && (isDigit(at(pos + 1)) || ((at(pos + 1) == '+' || at(pos + 1) == '-') && isDigit(at(pos + 2))))) {
            advance();
            if (at(pos) == '+' || at(pos) == '-') {
                advance();
            }
            while (isDigit(at(pos))) {
                advance();
            }
        }
    }

    /** Spec "consume a unicode-range token" — the cursor is on {@code u}/{@code U}. */
    private void consumeUnicodeRange() {
        advance();
        advance();
        int length = 0;
        StringBuilder hex = new StringBuilder();
        while (length < 6 && isHexDigit(at(pos))) {
            hex.append(at(pos));
            advance();
            length++;
        }
        int questionMarks = 0;
        while (length + questionMarks < 6 && at(pos) == '?') {
            advance();
            questionMarks++;
        }
        int start;
        int end;
        if (questionMarks > 0) {
            String base = hex.toString();
            start = Integer.parseInt(base + "0".repeat(questionMarks), 16);
            end = Integer.parseInt(base + "F".repeat(questionMarks), 16);
        } else {
            start = Integer.parseInt(hex.toString(), 16);
            if (at(pos) == '-' && isHexDigit(at(pos + 1))) {
                advance();
                StringBuilder hex2 = new StringBuilder();
                int length2 = 0;
                while (length2 < 6 && isHexDigit(at(pos))) {
                    hex2.append(at(pos));
                    advance();
                    length2++;
                }
                end = Integer.parseInt(hex2.toString(), 16);
            } else {
                end = start;
            }
        }
        String raw = input.substring(tokenStart, pos);
        tokens.add(
            new Token(
                TokenKind.unicodeRange,
                raw,
                0,
                null,
                false,
                false,
                '\0',
                false,
                start,
                end,
                tokenStart,
                pos,
                tokenLine,
                tokenColumn
            )
        );
    }
    // endregion
}
