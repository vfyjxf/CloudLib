/* Ported from katana-parser (MIT, (c) 2015 Hackers and Painters) — see LICENSE-katana.txt */
package dev.vfyjxf.cloudlib.api.css;

import dev.vfyjxf.cloudlib.api.css.CssTokenizer.Token;
import dev.vfyjxf.cloudlib.api.css.CssTokenizer.TokenKind;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Recursive-descent implementation of the CSS Syntax Level 3 grammar over the token
 * stream produced by {@link CssTokenizer} — the Java counterpart of katana's
 * {@code parser.c} / generated {@code katana.tab.c} semantic actions.
 * <p>
 * Everything here is error-tolerant per spec: malformed declarations skip to the
 * next {@code ;} or {@code }, malformed rules to the matching close brace, and
 * unclosed constructs close at EOF. Errors are collected, never thrown.
 */
public final class CssParser {

    private CssParser() {}

    /** Parses a whole stylesheet. */
    public static Stylesheet parse(String input) {
        return parse(input, new ArrayList<>());
    }

    /** Parses a whole stylesheet, collecting recoverable errors. */
    public static Stylesheet parse(String input, List<CssError> errors) {
        List<Token> tokens = CssTokenizer.tokenize(input, errors);
        return new Parser(input, tokens, errors).stylesheet();
    }

    /** Parses a standalone component-value list (a declaration value). */
    public static List<ComponentValue> parseValueList(String input) {
        List<CssError> errors = new ArrayList<>();
        List<Token> tokens = CssTokenizer.tokenize(input, errors);
        Parser p = new Parser(input, tokens, errors);
        List<ComponentValue> values = p.componentValuesUntil(TokenKind.eof);
        return List.copyOf(values);
    }

    /** Parses a standalone selector list; empty when nothing parses. */
    public static List<ComplexSelector> parseSelectorList(String input) {
        List<CssError> errors = new ArrayList<>();
        List<Token> tokens = CssTokenizer.tokenize(input, errors);
        return new SelectorParser(input, tokens, errors).selectorList();
    }

    /** Single-colon pseudo-elements per spec — these count as elements even with `:`. */
    private static final Set<String> legacyPseudoElements = Set.of("before", "after", "first-line", "first-letter");

    // region at-rule block kinds
    /** At-rules whose {@code {…}} holds a nested rule list. */
    private static final Set<String> ruleBlocks = Set
            .of("media", "supports", "layer", "scope", "container", "document", "starting-style");
    /** At-rules whose {@code {…}} holds a declaration list. */
    private static final Set<String> declarationBlocks = Set.of(
        "font-face",
        "page",
        "viewport",
        "counter-style",
        "property",
        "font-feature-values",
        "position-try",
        "font-palette-values"
    );
    /** At-rules whose {@code {…}} keeps raw component values (keyframe selectors are not declarations). */
    private static final Set<String> rawBlocks = Set.of("keyframes", "-webkit-keyframes", "-moz-keyframes");

    /** Pseudo-classes/elements whose argument is a (possibly relative) selector list. */
    private static final Set<String> selectorArgs = Set
            .of("not", "is", "where", "has", "host", "host-context", "slotted", "current", "past", "future");
    /** Functional pseudos taking an An+B expression. */
    private static final Set<String> anPlusBArgs = Set
            .of("nth-child", "nth-last-child", "nth-of-type", "nth-last-of-type", "nth-col", "nth-last-col");
    /** Functional pseudos taking a comma-separated identifier/word list. */
    private static final Set<String> identArgs = Set.of("lang", "dir", "part", "state", "highlight");

    /** The text a token of a kind that carries one — its decoded value or its raw numeric text. */
    private static String text(Token t) {
        return Objects.requireNonNull(t.string());
    }

    /** The unit of a dimension token. */
    private static String unit(Token t) {
        return Objects.requireNonNull(t.unit());
    }

    private static final class Parser {
        /** Source text — selector sub-parsers slice it for An+B reconstruction. */
        private final String input;

        private final List<Token> tokens;
        private final List<CssError> errors;
        private int pos;

        Parser(String input, List<Token> tokens, List<CssError> errors) {
            this.input = input;
            this.tokens = tokens;
            this.errors = errors;
        }

        // endregion

        // region cursor
        /** Token slices (raw pseudo args) carry no eof token — synthesize one past the end. */
        private Token eofToken() {
            if (tokens.isEmpty()) return Token.bare(TokenKind.eof, 0, 0, 1, 1);
            Token last = tokens.get(tokens.size() - 1);
            return Token.bare(TokenKind.eof, last.end(), last.end(), last.line(), last.column());
        }

        private Token peek() {
            return pos < tokens.size() ? tokens.get(pos) : eofToken();
        }

        private Token peekAt(int ahead) {
            int i = pos + ahead;
            return i < tokens.size() ? tokens.get(i) : eofToken();
        }

        private Token take() {
            return pos < tokens.size() ? tokens.get(pos++) : eofToken();
        }

        private boolean at(TokenKind kind) {
            return peek().kind() == kind;
        }

        private boolean atEnd() {
            return at(TokenKind.eof);
        }

        private void errorAt(Token token, String message) {
            errors.add(new CssError(token.line(), token.column(), message));
        }

        // endregion

        // region stylesheet & rules
        /** Spec "consume a list of rules" — stops at {@code }/EOF. */
        Stylesheet stylesheet() {
            List<Rule> rules = rulesUntil(TokenKind.eof, true);
            return new Stylesheet(rules);
        }

        private List<Rule> rulesUntil(TokenKind close, boolean topLevel) {
            List<Rule> rules = new ArrayList<>();
            while (true) {
                Token t = peek();
                switch (t.kind()) {
                    case eof, rightCurly -> {
                        if (t.kind() == close || close == TokenKind.eof) return rules;
                        // a stray } closes our block whether or not it was asked for
                        return rules;
                    }
                    case whitespace, semicolon -> take();
                    case cdo, cdc -> {
                        // only valid between rules at the top level
                        if (topLevel) take();
                        else {
                            take();
                            errorAt(t, "unexpected " + t.kind());
                        }
                    }
                    case atKeyword -> {
                        Rule rule = atRule();
                        if (rule != null) rules.add(rule);
                    }
                    default -> {
                        Rule rule = qualifiedRule();
                        if (rule != null) rules.add(rule);
                    }
                }
            }
        }

        /** Spec "consume an at-rule" — cursor is on the {@code @keyword} token. */
        private @Nullable Rule atRule() {
            Token at = take();
            String name = at.string() != null ? at.string().toLowerCase(Locale.ROOT) : "";
            List<ComponentValue> prelude = new ArrayList<>();
            while (true) {
                Token t = peek();
                if (atEnd()) {
                    return new AtRule(name, prelude, null);
                }
                if (t.kind() == TokenKind.semicolon) {
                    take();
                    return new AtRule(name, prelude, null);
                }
                if (t.kind() == TokenKind.rightCurly) {
                    // } ends the enclosing block — leave it for the caller
                    return new AtRule(name, prelude, null);
                }
                if (t.kind() == TokenKind.leftCurly) {
                    take();
                    return new AtRule(name, prelude, atBlock(name));
                }
                prelude.add(componentValue());
            }
        }

        /** Parses the body of an already-consumed {@code {}-block of an at-rule. */
        private AtRule.Block atBlock(String name) {
            if (ruleBlocks.contains(name)) {
                List<Rule> rules = rulesUntil(TokenKind.rightCurly, false);
                expectClose();
                return new AtRule.Block.Rules(rules);
            }
            if (declarationBlocks.contains(name)) {
                List<Declaration> decls = declarationList();
                expectClose();
                return new AtRule.Block.Declarations(decls);
            }
            // raw/unknown: keep component values (keyframes selectors, foreign at-rules)
            List<ComponentValue> values = componentValuesUntil(TokenKind.rightCurly);
            expectClose();
            return new AtRule.Block.Values(values);
        }

        /** Consumes the closing {@code }} if present; EOF silently closes per spec. */
        private void expectClose() {
            if (at(TokenKind.rightCurly)) take();
        }

        /** Spec "consume a qualified rule" — prelude until {@code {}, then a declaration list. */
        private @Nullable Rule qualifiedRule() {
            Token start = peek();
            List<Token> prelude = new ArrayList<>();
            // dump every raw token until '{' — selectors contain no legal '{', and nested
            // blocks in the prelude belong to the selector grammar (attr brackets, pseudo args)
            while (true) {
                Token t = peek();
                if (atEnd() || t.kind() == TokenKind.rightCurly) {
                    errorAt(start, "qualified rule without block");
                    return null;
                }
                if (t.kind() == TokenKind.leftCurly) {
                    take();
                    break;
                }
                prelude.add(take());
            }
            List<ComplexSelector> selectors = new SelectorParser(input, prelude, errors).selectorList();
            List<Declaration> declarations = declarationList();
            expectClose();
            if (selectors.isEmpty()) {
                // selector invalid — the rule is dropped per spec, error already recorded
                return null;
            }
            return new StyleRule(selectors, declarations);
        }

        // endregion

        // region declarations
        /** Spec "consume a list of declarations" — stops before {@code }/EOF (not consumed). */
        private List<Declaration> declarationList() {
            List<Declaration> out = new ArrayList<>();
            while (true) {
                Token t = peek();
                switch (t.kind()) {
                    case eof, rightCurly -> {
                        return out;
                    }
                    case whitespace, semicolon -> take();
                    case atKeyword -> {
                        errorAt(t, "at-rule inside declaration list");
                        atRule(); // consume it fully, discard
                    }
                    default -> {
                        Declaration decl = declaration();
                        if (decl != null) out.add(decl);
                    }
                }
            }
        }

        /**
         * Spec "consume a declaration" — {@code ident : value [!important] ;}.
         * Custom properties ({@code --*}) keep their raw component values.
         */
        private @Nullable Declaration declaration() {
            Token name = peek();
            if (name.kind() != TokenKind.ident) {
                errorAt(name, "expected a property name");
                skipDeclarationRemnants();
                return null;
            }
            take();
            String property = text(name).toLowerCase(Locale.ROOT);
            skipWhitespace();
            if (!at(TokenKind.colon)) {
                errorAt(peek(), "expected ':' after property '" + property + "'");
                skipDeclarationRemnants();
                return null;
            }
            take();
            boolean custom = property.startsWith("--");
            List<ComponentValue> value = new ArrayList<>();
            while (true) {
                Token t = peek();
                if (t.kind() == TokenKind.semicolon || t.kind() == TokenKind.rightCurly || t.kind() == TokenKind.eof)
                    break;
                // bad-string/bad-url break the declaration per spec
                if (t.kind() == TokenKind.badString || t.kind() == TokenKind.badUrl) {
                    errorAt(t, "bad token in declaration value");
                    skipDeclarationRemnants();
                    return null;
                }
                value.add(componentValue());
            }
            if (at(TokenKind.semicolon)) take();
            // spec tail check: "! ws* important" — whitespace around/between is legal
            boolean important = false;
            int i = value.size() - 1;
            while (i >= 0 && value.get(i) == ComponentValue.Whitespace.instance) i--;
            if (i >= 0 && value.get(i) instanceof ComponentValue.Ident iv && iv.value().equalsIgnoreCase("important")) {
                int j = i - 1;
                while (j >= 0 && value.get(j) == ComponentValue.Whitespace.instance) j--;
                if (j >= 0 && value.get(j) instanceof ComponentValue.Delim d && d.value() == '!') {
                    important = true;
                    value = new ArrayList<>(value.subList(0, j));
                }
            }
            if (!custom) {
                // canonical decls: trim surrounding whitespace
                while (!value.isEmpty() && value.get(0) == ComponentValue.Whitespace.instance) value.remove(0);
                while (!value.isEmpty() && value.get(value.size() - 1) == ComponentValue.Whitespace.instance) {
                    value.remove(value.size() - 1);
                }
            }
            if (value.isEmpty() && !custom) {
                errorAt(name, "missing value for '" + property + "'");
                return null;
            }
            return new Declaration(property, value, important);
        }

        /** Spec "consume the remnants of a bad declaration" — to {@code ;} or {@code }. */
        private void skipDeclarationRemnants() {
            while (true) {
                Token t = peek();
                if (t.kind() == TokenKind.eof || t.kind() == TokenKind.rightCurly) return;
                if (t.kind() == TokenKind.semicolon) {
                    take();
                    return;
                }
                consumeComponentToken();
            }
        }

        private void skipWhitespace() {
            while (at(TokenKind.whitespace)) take();
        }

        // endregion

        // region component values
        /** Component values until {@code close} (not consumed) or EOF. */
        List<ComponentValue> componentValuesUntil(TokenKind close) {
            List<ComponentValue> out = new ArrayList<>();
            while (!atEnd() && peek().kind() != close) {
                out.add(componentValue());
            }
            return out;
        }

        /** Spec "consume a component value". */
        private ComponentValue componentValue() {
            Token t = peek();
            switch (t.kind()) {
                case function -> {
                    take();
                    List<ComponentValue> args = componentValuesUntil(TokenKind.rightParen);
                    if (at(TokenKind.rightParen)) take();
                    return new ComponentValue.Function(text(t), args);
                }
                case leftParen -> {
                    take();
                    List<ComponentValue> values = componentValuesUntil(TokenKind.rightParen);
                    if (at(TokenKind.rightParen)) take();
                    return new ComponentValue.Block(ComponentValue.BlockKind.paren, values);
                }
                case leftSquare -> {
                    take();
                    List<ComponentValue> values = componentValuesUntil(TokenKind.rightSquare);
                    if (at(TokenKind.rightSquare)) take();
                    return new ComponentValue.Block(ComponentValue.BlockKind.square, values);
                }
                case leftCurly -> {
                    take();
                    List<ComponentValue> values = componentValuesUntil(TokenKind.rightCurly);
                    if (at(TokenKind.rightCurly)) take();
                    return new ComponentValue.Block(ComponentValue.BlockKind.curly, values);
                }
                default -> {
                    consumeComponentToken();
                    return tokenToValue(t);
                }
            }
        }

        /** Consumes a token treating block openers as nested component-value spans. */
        private void consumeComponentToken() {
            Token t = peek();
            switch (t.kind()) {
                case function -> {
                    take();
                    componentValuesUntil(TokenKind.rightParen);
                    if (at(TokenKind.rightParen)) take();
                }
                case leftParen -> {
                    take();
                    componentValuesUntil(TokenKind.rightParen);
                    if (at(TokenKind.rightParen)) take();
                }
                case leftSquare -> {
                    take();
                    componentValuesUntil(TokenKind.rightSquare);
                    if (at(TokenKind.rightSquare)) take();
                }
                case leftCurly -> {
                    take();
                    componentValuesUntil(TokenKind.rightCurly);
                    if (at(TokenKind.rightCurly)) take();
                }
                default -> take();
            }
        }

        private ComponentValue tokenToValue(Token t) {
            return switch (t.kind()) {
                case ident -> new ComponentValue.Ident(text(t));
                case atKeyword -> new ComponentValue.AtKeyword(text(t));
                case string, badString -> new ComponentValue.StringValue(text(t));
                case url, badUrl -> new ComponentValue.UrlValue(text(t));
                case hash -> new ComponentValue.HashValue(text(t), t.idFlag());
                case number -> new ComponentValue.NumericValue(
                    t.number(),
                    "",
                    ComponentValue.NumericKind.number,
                    t.integer(),
                    text(t)
                );
                case percentage -> new ComponentValue.NumericValue(
                    t.number(),
                    "%",
                    ComponentValue.NumericKind.percentage,
                    t.integer(),
                    text(t)
                );
                case dimension -> new ComponentValue.NumericValue(
                    t.number(),
                    unit(t),
                    ComponentValue.NumericKind.dimension,
                    t.integer(),
                    text(t)
                );
                case unicodeRange -> new ComponentValue.UnicodeRange(t.rangeStart(), t.rangeEnd());
                case whitespace -> ComponentValue.Whitespace.instance;
                case cdo -> ComponentValue.Cdo.instance;
                case cdc -> ComponentValue.Cdc.instance;
                case colon -> new ComponentValue.Delim(':');
                case semicolon -> new ComponentValue.Delim(';');
                case comma -> new ComponentValue.Delim(',');
                case delim -> new ComponentValue.Delim(t.delim());
                default -> new ComponentValue.Delim((char) 0xFFFD);
            };
        }
    }

    // endregion

    // region selectors
    /**
     * Parses a selector list from a token slice — Selectors Level 3 grammar with the
     * Level 4 extras the AST already models ({@code :has} relative selectors,
     * {@code of}-clauses, {@code ||} columns).
     */
    private static final class SelectorParser {
        private final String input;
        private final List<Token> tokens;
        private final List<CssError> errors;
        private int pos;

        SelectorParser(String input, List<Token> tokens, List<CssError> errors) {
            this.input = input;
            this.tokens = tokens;
            this.errors = errors;
        }

        private Token eofToken() {
            if (tokens.isEmpty()) return Token.bare(TokenKind.eof, 0, 0, 1, 1);
            Token last = tokens.get(tokens.size() - 1);
            return Token.bare(TokenKind.eof, last.end(), last.end(), last.line(), last.column());
        }

        private Token peek() {
            return pos < tokens.size() ? tokens.get(pos) : eofToken();
        }

        private Token take() {
            return pos < tokens.size() ? tokens.get(pos++) : eofToken();
        }

        private boolean eof() {
            return peek().kind() == TokenKind.eof;
        }

        private boolean at(TokenKind kind) {
            return !eof() && peek().kind() == kind;
        }

        private void skipWs() {
            while (at(TokenKind.whitespace)) take();
        }

        private void error(String message) {
            Token t = peek();
            errors.add(new CssError(t.line(), t.column(), message));
        }

        // endregion

        // region list & complex
        List<ComplexSelector> selectorList() {
            List<ComplexSelector> out = new ArrayList<>();
            while (true) {
                skipWs();
                if (eof()) break;
                ComplexSelector s = complexSelector();
                if (s != null) out.add(s);
                skipWs();
                if (at(TokenKind.comma)) {
                    take();
                    continue;
                }
                if (!eof()) {
                    error("unexpected token in selector list");
                    // recover to the next comma
                    while (!eof() && !at(TokenKind.comma)) take();
                    if (at(TokenKind.comma)) take();
                }
            }
            return out;
        }

        /** A complex selector — compounds joined by combinators. */
        private @Nullable ComplexSelector complexSelector() {
            List<CompoundSelector> compounds = new ArrayList<>();
            List<Combinator> combinators = new ArrayList<>();
            CompoundSelector first = compoundSelector();
            if (first == null) return null;
            compounds.add(first);
            while (true) {
                // whitespace between compounds = descendant, unless a real combinator follows
                boolean hadWs = at(TokenKind.whitespace);
                skipWs();
                if (eof() || at(TokenKind.comma) || at(TokenKind.rightParen)) break;
                Combinator comb;
                Token t = peek();
                if (t.kind() == TokenKind.delim) {
                    switch (t.delim()) {
                        case '>' -> {
                            take();
                            comb = Combinator.child;
                        }
                        case '+' -> {
                            take();
                            comb = Combinator.nextSibling;
                        }
                        case '~' -> {
                            take();
                            comb = Combinator.subsequentSibling;
                        }
                        case '|' -> {
                            // "||" column combinator — a lone '|' belongs to a namespace prefix
                            if (peekAt1().kind() == TokenKind.delim && peekAt1().delim() == '|') {
                                take();
                                take();
                                comb = Combinator.column;
                            } else {
                                comb = hadWs ? Combinator.descendant : null;
                            }
                        }
                        default -> comb = null;
                    }
                } else {
                    comb = hadWs ? Combinator.descendant : null;
                }
                if (comb == null) {
                    if (hadWs) {
                        comb = Combinator.descendant;
                    } else {
                        error("expected a combinator");
                        return null;
                    }
                }
                skipWs();
                CompoundSelector next = compoundSelector();
                if (next == null) {
                    error("expected a selector after combinator");
                    return null;
                }
                combinators.add(comb);
                compounds.add(next);
            }
            return new ComplexSelector(compounds, combinators);
        }

        // endregion

        // region compound
        private @Nullable CompoundSelector compoundSelector() {
            String tag = null;
            String namespace = null;
            String id = null;
            List<String> classes = new ArrayList<>();
            List<AttributeSelector> attrs = new ArrayList<>();
            List<PseudoClass> pseudos = new ArrayList<>();
            Specificity spec = Specificity.zero;
            boolean any = false;

            // optional type/universal part, possibly namespaced: ns|tag *|tag |tag ns|* * |*
            Token t = peek();
            if (t.kind() == TokenKind.ident || isDelim(t, '*') || isDelim(t, '|')) {
                boolean sawNamespace = false;
                if (isDelim(t, '|')) {
                    // |tag — explicitly no namespace
                    take();
                    namespace = "";
                    sawNamespace = true;
                } else {
                    // lookahead: ident/* followed by '|' (not '|=') → namespace prefix
                    int save = pos;
                    take();
                    skipWsNs();
                    if (isDelim(peek(), '|') && !isDelim(peekAt1(), '=') && !isDelim(peekAt1(), '|')) {
                        take(); // the |
                        namespace = t.kind() == TokenKind.ident ? t.string() : "*";
                        sawNamespace = true;
                    } else {
                        pos = save;
                    }
                }
                if (sawNamespace) {
                    Token tagTok = peek();
                    if (tagTok.kind() == TokenKind.ident) {
                        tag = tagTok.string();
                        take();
                    } else if (isDelim(tagTok, '*')) {
                        take();
                    } else {
                        error("expected a type after namespace prefix");
                        return null;
                    }
                    any = true;
                    spec = spec.plus(tag != null ? new Specificity(0, 0, 1) : Specificity.zero);
                } else if (t.kind() == TokenKind.ident) {
                    tag = take().string();
                    any = true;
                    spec = spec.plus(new Specificity(0, 0, 1));
                } else if (isDelim(t, '*')) {
                    take();
                    any = true; // universal — no specificity
                }
            }

            while (true) {
                t = peek();
                if (t.kind() == TokenKind.hash && t.idFlag()) {
                    id = take().string();
                    spec = spec.plus(new Specificity(1, 0, 0));
                    any = true;
                } else if (isDelim(t, '.')) {
                    take();
                    Token name = peek();
                    if (name.kind() != TokenKind.ident) {
                        error("expected class name after '.'");
                        return null;
                    }
                    classes.add(name.string());
                    take();
                    spec = spec.plus(new Specificity(0, 1, 0));
                    any = true;
                } else if (t.kind() == TokenKind.leftSquare) {
                    AttributeSelector a = attributeSelector();
                    if (a == null) return null;
                    attrs.add(a);
                    spec = spec.plus(new Specificity(0, 1, 0));
                    any = true;
                } else if (t.kind() == TokenKind.colon) {
                    take();
                    boolean element = at(TokenKind.colon);
                    if (element) take();
                    Token nameTok = peek();
                    if (nameTok.kind() != TokenKind.ident && nameTok.kind() != TokenKind.function) {
                        error("expected a pseudo name after ':'");
                        return null;
                    }
                    String name = text(nameTok).toLowerCase(Locale.ROOT);
                    take();
                    // :before/:after/:first-line/:first-letter are elements even single-colon
                    if (legacyPseudoElements.contains(name)) element = true;
                    PseudoArgs args = PseudoArgs.None.instance;
                    Specificity add = element ? new Specificity(0, 0, 1) : new Specificity(0, 1, 0);
                    if (nameTok.kind() == TokenKind.function) {
                        // arg tokens until ')'
                        List<Token> argTokens = argTokens();
                        ParsedArgs parsed = pseudoArgs(name, element, argTokens);
                        args = parsed.args;
                        add = parsed.spec(element);
                    }
                    pseudos.add(new PseudoClass(name, element, args));
                    spec = spec.plus(add);
                    any = true;
                } else {
                    break;
                }
            }
            if (!any) return null;
            return CompoundSelector.of(tag, namespace, id, classes, attrs, pseudos, spec);
        }

        private void skipWsNs() {
            // namespace separator may be surrounded by whitespace: ns | tag
            skipWs();
        }

        private Token peekAt1() {
            int i = pos + 1;
            return i < tokens.size() ? tokens.get(i) : eofToken();
        }

        private boolean isDelim(Token t, char c) {
            return t.kind() == TokenKind.delim && t.delim() == c;
        }

        // endregion

        // region attribute selector
        private @Nullable AttributeSelector attributeSelector() {
            take(); // [
            skipWs();
            String ns = null;
            Token t = peek();
            if (isDelim(t, '|')) {
                take();
                ns = "";
            } else if (t.kind() == TokenKind.ident || isDelim(t, '*')) {
                int save = pos;
                String candidate = t.kind() == TokenKind.ident ? take().string() : "*";
                skipWs();
                if (isDelim(peek(), '|') && !isDelim(peekAt1(), '=')) {
                    take();
                    ns = candidate;
                } else {
                    pos = save;
                }
            }
            Token nameTok = peek();
            if (nameTok.kind() != TokenKind.ident) {
                error("expected attribute name");
                recoverToSquare();
                return null;
            }
            String name = text(nameTok);
            take();
            skipWs();
            if (at(TokenKind.rightSquare)) {
                take();
                return new AttributeSelector(ns, name, null, null, null);
            }
            AttributeSelector.Operator op = attrOperator();
            if (op == null) {
                error("expected an attribute operator");
                recoverToSquare();
                return null;
            }
            skipWs();
            Token valTok = peek();
            String value;
            if (valTok.kind() == TokenKind.ident || valTok.kind() == TokenKind.string) {
                value = text(valTok);
                take();
            } else {
                error("expected an attribute value");
                recoverToSquare();
                return null;
            }
            skipWs();
            AttributeSelector.MatchFlag flag = null;
            if (peek().kind() == TokenKind.ident) {
                String f = text(peek());
                if (f.equalsIgnoreCase("i")) flag = AttributeSelector.MatchFlag.insensitive;
                else if (f.equalsIgnoreCase("s")) flag = AttributeSelector.MatchFlag.sensitive;
                else {
                    error("unknown attribute flag '" + f + "'");
                    recoverToSquare();
                    return null;
                }
                take();
                skipWs();
            }
            if (!at(TokenKind.rightSquare)) {
                error("expected ']'");
                recoverToSquare();
                return null;
            }
            take();
            return new AttributeSelector(ns, name, op, value, flag);
        }

        private AttributeSelector.@Nullable Operator attrOperator() {
            Token t = peek();
            if (isDelim(t, '=')) {
                take();
                return AttributeSelector.Operator.exact;
            }
            if (t.kind() == TokenKind.delim && peekAt1().kind() == TokenKind.delim && peekAt1().delim() == '=') {
                AttributeSelector.Operator op = switch (t.delim()) {
                    case '~' -> AttributeSelector.Operator.includes;
                    case '|' -> AttributeSelector.Operator.dashMatch;
                    case '^' -> AttributeSelector.Operator.prefixMatch;
                    case '$' -> AttributeSelector.Operator.suffixMatch;
                    case '*' -> AttributeSelector.Operator.substringMatch;
                    default -> null;
                };
                if (op != null) {
                    take();
                    take();
                    return op;
                }
            }
            return null;
        }

        private void recoverToSquare() {
            while (!eof() && !at(TokenKind.rightSquare)) take();
            if (at(TokenKind.rightSquare)) take();
        }

        // endregion

        // region functional pseudos
        /** Consumes the argument token run of an already-taken {@code function} token. */
        private List<Token> argTokens() {
            List<Token> out = new ArrayList<>();
            int depth = 0;
            while (!eof()) {
                Token t = peek();
                if (t.kind() == TokenKind.rightParen && depth == 0) {
                    take();
                    return out;
                }
                if (t.kind() == TokenKind.function
                        || t.kind() == TokenKind.leftParen
                        || t.kind() == TokenKind.leftSquare
                        || t.kind() == TokenKind.leftCurly) {
                    depth++;
                } else
                    if ((t.kind() == TokenKind.rightParen && depth > 0)
                            || t.kind() == TokenKind.rightSquare
                            || t.kind() == TokenKind.rightCurly) {
                                depth--;
                            }
                out.add(take());
            }
            error("unclosed function arguments");
            return out;
        }

        private record ParsedArgs(PseudoArgs args, Specificity flat) {
            Specificity spec(boolean element) {
                // element pseudos always add to c; classes to b — args may override via their
                // own specificity for not/is/has
                return flat;
            }
        }

        private ParsedArgs pseudoArgs(String name, boolean element, List<Token> argTokens) {
            String lname = name.toLowerCase(Locale.ROOT);
            if (selectorArgs.contains(lname)) {
                List<RelativeSelector> sels = relativeSelectorList(
                    argTokens,
                    lname.equals("has") || lname.equals("host-context") || lname.equals("slotted")
                );
                Specificity max = Specificity.zero;
                for (RelativeSelector r : sels) {
                    max = max.max(r.selector().specificity());
                }
                Specificity add = lname.equals("where") ? Specificity.zero : (element ? new Specificity(0, 0, 1) : max);
                return new ParsedArgs(new PseudoArgs.SelectorList(sels), add);
            }
            if (anPlusBArgs.contains(lname)) {
                return new ParsedArgs(
                    parseAnPlusB(argTokens),
                    element ? new Specificity(0, 0, 1) : new Specificity(0, 1, 0)
                );
            }
            if (identArgs.contains(lname)) {
                List<String> words = new ArrayList<>();
                for (Token t : argTokens) {
                    if (t.kind() == TokenKind.ident || t.kind() == TokenKind.string) {
                        words.add(t.string());
                    }
                }
                return new ParsedArgs(
                    new PseudoArgs.Idents(words),
                    element ? new Specificity(0, 0, 1) : new Specificity(0, 1, 0)
                );
            }
            // raw component values
            Parser vp = new Parser(input, argTokens, errors);
            List<ComponentValue> values = vp.componentValuesUntil(TokenKind.eof);
            return new ParsedArgs(
                new PseudoArgs.Raw(values),
                element ? new Specificity(0, 0, 1) : new Specificity(0, 1, 0)
            );
        }

        /**
         * Parses a (possibly relative) selector list from function-argument tokens.
         * {@code relativeAllowed} permits a leading combinator ({@code :has(> a)}).
         */
        private List<RelativeSelector> relativeSelectorList(List<Token> argTokens, boolean relativeAllowed) {
            List<RelativeSelector> out = new ArrayList<>();
            SelectorParser sub = new SelectorParser(input, argTokens, errors);
            while (true) {
                sub.skipWs();
                if (sub.eof()) break;
                Combinator leading = null;
                Token t = sub.peek();
                if (t.kind() == TokenKind.delim) {
                    switch (t.delim()) {
                        case '>' -> leading = Combinator.child;
                        case '+' -> leading = Combinator.nextSibling;
                        case '~' -> leading = Combinator.subsequentSibling;
                        case '|' -> {
                            if (sub.peekAt1().kind() == TokenKind.delim && sub.peekAt1().delim() == '|')
                                leading = Combinator.column;
                        }
                        default -> {}
                    }
                    if (leading != null) {
                        if (!relativeAllowed) {
                            sub.error("leading combinator not allowed here");
                            return out;
                        }
                        sub.take();
                        if (leading == Combinator.column) sub.take();
                        sub.skipWs();
                    }
                }
                ComplexSelector sel = sub.complexSelector();
                if (sel != null) out.add(new RelativeSelector(leading, sel));
                sub.skipWs();
                if (sub.at(TokenKind.comma)) {
                    sub.take();
                } else {
                    break;
                }
            }
            return out;
        }

        // endregion

        // region An+B
        /**
         * Parses the An+B microsyntax plus an optional {@code of <selector-list>}
         * (Selectors L4 {@code :nth-child}).
         */
        private PseudoArgs parseAnPlusB(List<Token> argTokens) {
            // split at a top-level ident "of"
            int ofIndex = -1;
            for (int i = 0; i < argTokens.size(); i++) {
                Token t = argTokens.get(i);
                if (t.kind() == TokenKind.ident && "of".equalsIgnoreCase(t.string())) {
                    ofIndex = i;
                    break;
                }
            }
            List<ComplexSelector> ofSelectors = List.of();
            List<Token> anb = argTokens;
            if (ofIndex >= 0) {
                anb = argTokens.subList(0, ofIndex);
                List<Token> rest = argTokens.subList(ofIndex + 1, argTokens.size());
                ofSelectors = new SelectorParser(input, rest, errors).selectorList();
            }
            int[] ab = anbValue(anb);
            return new PseudoArgs.AnPlusB(ab[0], ab[1], ofSelectors);
        }

        /**
         * Evaluates the An+B token run. Accepts the spec forms:
         * {@code odd even ±N ±N?n±B ±n±B n±B} with whitespace permitted around
         * the second sign. Unparseable input yields 0/0 with an error recorded.
         */
        private int[] anbValue(List<Token> toks) {
            // rebuild the condensed source text — token offsets are relative to
            // the original input, so substring over it
            StringBuilder sb = new StringBuilder();
            for (Token t : toks) {
                if (t.kind() == TokenKind.whitespace) sb.append(' ');
                else sb.append(input, t.start(), t.end());
            }
            String s = sb.toString().strip().toLowerCase(Locale.ROOT);
            if (s.isEmpty()) {
                error("empty :nth-* argument");
                return new int[]{0, 0};
            }
            if (s.equals("odd")) return new int[]{2, 1};
            if (s.equals("even")) return new int[]{2, 0};
            int[] r = anbChars(s);
            if (r == null) {
                error("invalid An+B expression '" + s + "'");
                return new int[]{0, 0};
            }
            return r;
        }

        private static int @Nullable [] anbChars(String s) {
            int i = 0;
            int signA = 1;
            if (i < s.length() && (s.charAt(i) == '+' || s.charAt(i) == '-')) {
                if (s.charAt(i) == '-') signA = -1;
                i++;
            }
            int dStart = i;
            while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
            String aDigits = s.substring(dStart, i);
            if (i < s.length() && s.charAt(i) == 'n') {
                i++;
                int a = aDigits.isEmpty() ? signA : signA * Integer.parseInt(aDigits);
                // optional ±B — whitespace allowed around the sign per spec
                while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
                if (i >= s.length()) return new int[]{a, 0};
                int signB = 1;
                if (s.charAt(i) == '+' || s.charAt(i) == '-') {
                    if (s.charAt(i) == '-') signB = -1;
                    i++;
                    while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
                } else {
                    return null;
                }
                int bStart = i;
                while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
                if (bStart == i) return null;
                int b = signB * Integer.parseInt(s.substring(bStart, i));
                return i == s.length() ? new int[]{a, b} : null;
            }
            // plain integer B (a = 0); a leading sign belongs to B
            if (!aDigits.isEmpty() && i == s.length()) {
                return new int[]{0, signA * Integer.parseInt(aDigits)};
            }
            return null;
        }
    }
    // endregion
}
