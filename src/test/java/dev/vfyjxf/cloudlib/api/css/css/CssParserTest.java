package dev.vfyjxf.cloudlib.api.css;

import dev.vfyjxf.cloudlib.api.css.ComponentValue.Function;
import dev.vfyjxf.cloudlib.api.css.ComponentValue.HashValue;
import dev.vfyjxf.cloudlib.api.css.ComponentValue.Ident;
import dev.vfyjxf.cloudlib.api.css.ComponentValue.NumericValue;
import dev.vfyjxf.cloudlib.api.css.ComponentValue.StringValue;
import dev.vfyjxf.cloudlib.api.css.ComponentValue.UrlValue;
import dev.vfyjxf.cloudlib.api.css.ComponentValue.Whitespace;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CssParserTest {

    /** JUnit 5.7 lacks assertInstanceOf — same contract: fail on type mismatch, else cast. */
    @SuppressWarnings("unchecked")
    private static <T> T cast(Class<T> type, Object value) {
        assertNotNull(value, "expected " + type.getSimpleName() + ", got null");
        assertTrue(
            type.isInstance(value),
            "expected " + type.getSimpleName() + " but got " + value.getClass().getSimpleName()
        );
        return (T) value;
    }

    private static Stylesheet sheet(String css) {
        return CssParser.parse(css);
    }

    private static Stylesheet sheet(String css, List<CssError> errors) {
        return CssParser.parse(css, errors);
    }

    private static StyleRule onlyRule(Stylesheet sheet) {
        assertEquals(1, sheet.rules().size());
        return cast(StyleRule.class, sheet.rules().get(0));
    }

    private static ComplexSelector onlySelector(StyleRule rule) {
        assertEquals(1, rule.selectors().size());
        return rule.selectors().get(0);
    }

    // ------------------------------------------------------------------ basics

    @Test
    void emptyInputParsesToEmptySheet() {
        assertTrue(sheet("").rules().isEmpty());
        assertTrue(sheet("  \n\t ").rules().isEmpty());
    }

    @Test
    void simpleRuleParses() {
        StyleRule rule = onlyRule(sheet("a { color: red }"));
        CompoundSelector sel = onlySelector(rule).last();
        assertEquals("a", sel.tag());
        assertEquals(1, rule.declarations().size());
        Declaration decl = rule.declarations().get(0);
        assertEquals("color", decl.property());
        assertFalse(decl.important());
        cast(Ident.class, decl.value().get(0));
        assertEquals("red", ((Ident) decl.value().get(0)).value());
    }

    @Test
    void propertyNamesAreLowercased() {
        StyleRule rule = onlyRule(sheet("a { COLOR: red; Padding-Top: 1px }"));
        assertEquals("color", rule.declarations().get(0).property());
        assertEquals("padding-top", rule.declarations().get(1).property());
    }

    @Test
    void commentsAreStripped() {
        StyleRule rule = onlyRule(sheet("/* head */ a /* mid */ { /* in */ color: red /* tail */ }"));
        assertEquals("a", onlySelector(rule).last().tag());
        assertEquals(1, rule.declarations().size());
    }

    @Test
    void unclosedCommentEndsInput() {
        List<CssError> errors = new ArrayList<>();
        Stylesheet s = sheet("a { color: red } /* never closed", errors);
        assertEquals(1, s.rules().size());
        assertFalse(errors.isEmpty());
    }

    @Test
    void cdoCdcAreSkippedAtTopLevel() {
        Stylesheet s = sheet("<!-- a { color: red } -->");
        assertEquals(1, s.rules().size());
    }

    // ------------------------------------------------------------------ declarations

    @Test
    void importantIsParsed() {
        StyleRule rule = onlyRule(sheet("a { color: red !important }"));
        Declaration d = rule.declarations().get(0);
        assertTrue(d.important());
        assertEquals(1, d.value().size());
    }

    @Test
    void importantWithWhitespaceParses() {
        StyleRule rule = onlyRule(sheet("a { color: red ! important }"));
        assertTrue(rule.declarations().get(0).important());
    }

    @Test
    void bangNonImportantIsNotMarked() {
        StyleRule rule = onlyRule(sheet("a { color: red !foo }"));
        assertFalse(rule.declarations().get(0).important());
    }

    @Test
    void customPropertyKeepsRawValue() {
        StyleRule rule = onlyRule(sheet(":root { --accent: #35D6D0; --pad: 4px 8px }"));
        assertEquals(2, rule.declarations().size());
        Declaration d = rule.declarations().get(0);
        assertEquals("--accent", d.property());
        // custom props keep raw tokens — the value is "ws #35D6D0"
        HashValue hash = cast(
            HashValue.class,
            d.value().stream().filter(v -> v != Whitespace.instance).findFirst().orElseThrow()
        );
        assertEquals("35D6D0", hash.value());
    }

    @Test
    void varFunctionParsesAsFunctionValue() {
        StyleRule rule = onlyRule(sheet("a { background: var(--x, #fff) }"));
        Function fn = cast(Function.class, rule.declarations().get(0).value().get(0));
        assertEquals("var", fn.name());
        cast(Ident.class, fn.args().get(0));
    }

    @Test
    void numericValueKinds() {
        StyleRule rule = onlyRule(sheet("a { width: 4px; gap: 50%; opacity: .5; z-index: -2 }"));
        var decls = rule.declarations();
        NumericValue px = (NumericValue) decls.get(0).value().get(0);
        assertEquals("px", px.unit());
        assertEquals(ComponentValue.NumericKind.dimension, px.kind());
        assertEquals(4.0, px.value());
        NumericValue pct = (NumericValue) decls.get(1).value().get(0);
        assertEquals("%", pct.unit());
        NumericValue half = (NumericValue) decls.get(2).value().get(0);
        assertEquals(0.5, half.value());
        assertFalse(half.integer());
        NumericValue neg = (NumericValue) decls.get(3).value().get(0);
        assertEquals(-2.0, neg.value());
        assertTrue(neg.integer());
    }

    @Test
    void stringsAndUrlsAndEscapes() {
        StyleRule rule = onlyRule(
            sheet("a { content: \"it\\'s\"; font: 'x'; background: url(img.png); alt: url(\"q.png\") }")
        );
        var decls = rule.declarations();
        assertEquals("it's", ((StringValue) decls.get(0).value().get(0)).value());
        assertEquals("x", ((StringValue) decls.get(1).value().get(0)).value());
        assertEquals("img.png", ((UrlValue) decls.get(2).value().get(0)).value());
        // quoted url(…) is a function token, not a url-token
        Function u = cast(Function.class, decls.get(3).value().get(0));
        assertEquals("url", u.name());
    }

    @Test
    void edgeShorthandKeepsFourValues() {
        StyleRule rule = onlyRule(sheet("a { padding: 1px 2px 3px 4px }"));
        List<ComponentValue> v = rule.declarations().get(0).value().stream().filter(c -> c != Whitespace.instance)
                .toList();
        assertEquals(4, v.size());
    }

    // ------------------------------------------------------------------ selectors

    @Test
    void commaSelectorList() {
        StyleRule rule = onlyRule(sheet("a, .b, #c { color: red }"));
        assertEquals(3, rule.selectors().size());
    }

    @Test
    void descendantCombinator() {
        ComplexSelector s = onlySelector(onlyRule(sheet("panel button { x: y }")));
        assertEquals(2, s.compounds().size());
        assertEquals(Combinator.descendant, s.combinators().get(0));
    }

    @Test
    void childCombinator() {
        ComplexSelector s = onlySelector(onlyRule(sheet("panel > button { x: y }")));
        assertEquals(Combinator.child, s.combinators().get(0));
    }

    @Test
    void siblingCombinators() {
        assertEquals(Combinator.nextSibling, onlySelector(onlyRule(sheet("a + b { x: y }"))).combinators().get(0));
        assertEquals(
            Combinator.subsequentSibling,
            onlySelector(onlyRule(sheet("a ~ b { x: y }"))).combinators().get(0)
        );
    }

    @Test
    void idClassTagCompound() {
        CompoundSelector s = onlySelector(onlyRule(sheet("button.primary#go { x: y }"))).last();
        assertEquals("button", s.tag());
        assertEquals("go", s.id());
        assertEquals(List.of("primary"), s.classes());
        assertEquals(new Specificity(1, 1, 1), s.specificity());
    }

    @Test
    void universalMatchesWithoutSpecificity() {
        CompoundSelector s = onlySelector(onlyRule(sheet("* { x: y }"))).last();
        assertNull(s.tag());
        assertEquals(Specificity.zero, s.specificity());
    }

    @Test
    void namespacePrefixParses() {
        CompoundSelector s = onlySelector(onlyRule(sheet("ns|button { x: y }"))).last();
        assertEquals("ns", s.namespace());
        assertEquals("button", s.tag());
    }

    @Test
    void emptyNamespaceParses() {
        CompoundSelector s = onlySelector(onlyRule(sheet("|button { x: y }"))).last();
        assertEquals("", s.namespace());
        assertEquals("button", s.tag());
    }

    @Test
    void universalNamespaceParses() {
        CompoundSelector s = onlySelector(onlyRule(sheet("*|button { x: y }"))).last();
        assertEquals("*", s.namespace());
    }

    // ------------------------------------------------------------------ attribute selectors

    @Test
    void attributePresence() {
        CompoundSelector s = onlySelector(onlyRule(sheet("a[kind] { x: y }"))).last();
        assertEquals(1, s.attributes().size());
        assertEquals("kind", s.attributes().get(0).name());
        assertNull(s.attributes().get(0).operator());
    }

    @Test
    void attributeOperators() {
        Object[][] cases = {{"=", AttributeSelector.Operator.exact}, {"~=", AttributeSelector.Operator.includes},
                {"|=", AttributeSelector.Operator.dashMatch}, {"^=", AttributeSelector.Operator.prefixMatch},
                {"$=", AttributeSelector.Operator.suffixMatch}, {"*=", AttributeSelector.Operator.substringMatch},};
        for (Object[] c : cases) {
            CompoundSelector s = onlySelector(onlyRule(sheet("a[k" + c[0] + "\"v\"] { x: y }"))).last();
            assertEquals(c[1], s.attributes().get(0).operator(), (String) c[0]);
            assertEquals("v", s.attributes().get(0).value());
        }
    }

    @Test
    void attributeFlags() {
        CompoundSelector i = onlySelector(onlyRule(sheet("a[k=\"v\" i] { x: y }"))).last();
        assertEquals(AttributeSelector.MatchFlag.insensitive, i.attributes().get(0).flag());
        CompoundSelector s = onlySelector(onlyRule(sheet("a[k=\"v\" s] { x: y }"))).last();
        assertEquals(AttributeSelector.MatchFlag.sensitive, s.attributes().get(0).flag());
    }

    @Test
    void namespacedAttribute() {
        CompoundSelector s = onlySelector(onlyRule(sheet("[ns|k=v] { x: y }"))).last();
        assertEquals("ns", s.attributes().get(0).namespace());
    }

    // ------------------------------------------------------------------ pseudo-classes

    @Test
    void simplePseudoClass() {
        CompoundSelector s = onlySelector(onlyRule(sheet("a:hover { x: y }"))).last();
        PseudoClass p = s.pseudos().get(0);
        assertEquals("hover", p.name());
        assertFalse(p.element());
        cast(PseudoArgs.None.class, p.args());
    }

    @Test
    void pseudoElementIsElement() {
        CompoundSelector s = onlySelector(onlyRule(sheet("a::part(x) { x: y }"))).last();
        PseudoClass p = s.pseudos().get(0);
        assertTrue(p.element());
        assertEquals("part", p.name());
    }

    @Test
    void legacySingleColonElement() {
        CompoundSelector s = onlySelector(onlyRule(sheet("a:before { x: y }"))).last();
        assertTrue(s.pseudos().get(0).element());
        assertEquals(new Specificity(0, 0, 2), s.specificity()); // type + element
    }

    @Test
    void notTakesMaxArgSpecificity() {
        CompoundSelector s = onlySelector(onlyRule(sheet("a:not(#x.b) { x: y }"))).last();
        PseudoClass p = s.pseudos().get(0);
        PseudoArgs.SelectorList args = cast(PseudoArgs.SelectorList.class, p.args());
        assertEquals(new Specificity(1, 1, 0), args.selectors().get(0).selector().specificity());
        // a + max(#x.b = 1,1,0)
        assertEquals(new Specificity(1, 1, 1), s.specificity());
    }

    @Test
    void whereAddsNoSpecificity() {
        CompoundSelector s = onlySelector(onlyRule(sheet("a:where(#x) { x: y }"))).last();
        assertEquals(new Specificity(0, 0, 1), s.specificity());
    }

    @Test
    void hasAllowsLeadingCombinator() {
        CompoundSelector s = onlySelector(onlyRule(sheet("a:has(> b) { x: y }"))).last();
        PseudoArgs.SelectorList args = cast(PseudoArgs.SelectorList.class, s.pseudos().get(0).args());
        assertEquals(Combinator.child, args.selectors().get(0).combinator());
        assertEquals("b", args.selectors().get(0).selector().last().tag());
    }

    @Test
    void isWithSelectorList() {
        CompoundSelector s = onlySelector(onlyRule(sheet("a:is(.x, #y) { x: y }"))).last();
        PseudoArgs.SelectorList args = cast(PseudoArgs.SelectorList.class, s.pseudos().get(0).args());
        assertEquals(2, args.selectors().size());
    }

    // ------------------------------------------------------------------ An+B

    @Test
    void nthOddEven() {
        PseudoClass odd = onlySelector(onlyRule(sheet("a:nth-child(odd) { x: y }"))).last().pseudos().get(0);
        PseudoArgs.AnPlusB ab = cast(PseudoArgs.AnPlusB.class, odd.args());
        assertEquals(2, ab.a());
        assertEquals(1, ab.b());
        PseudoClass even = onlySelector(onlyRule(sheet("a:nth-child(even) { x: y }"))).last().pseudos().get(0);
        PseudoArgs.AnPlusB ev = (PseudoArgs.AnPlusB) even.args();
        assertEquals(2, ev.a());
        assertEquals(0, ev.b());
    }

    @Test
    void nthForms() {
        Object[][] cases = {{"2n+1", 2, 1}, {"-n+3", -1, 3}, {"3n", 3, 0}, {"n", 1, 0}, {"-n", -1, 0},
                {"-2n-1", -2, -1}, {"4", 0, 4}, {"-5", 0, -5}, {"+2n", 2, 0}, {"3n - 1", 3, -1}, {"n+ 2", 1, 2},
                {"-n- 3", -1, -3},};
        for (Object[] c : cases) {
            PseudoArgs.AnPlusB ab = (PseudoArgs.AnPlusB) onlySelector(
                onlyRule(sheet("a:nth-child(" + c[0] + ") { x: y }"))
            ).last().pseudos().get(0).args();
            assertEquals(c[1], ab.a(), (String) c[0]);
            assertEquals(c[2], ab.b(), (String) c[0]);
        }
    }

    @Test
    void nthWithOfClause() {
        PseudoArgs.AnPlusB ab = (PseudoArgs.AnPlusB) onlySelector(onlyRule(sheet("a:nth-child(2n of .x) { x: y }")))
                .last().pseudos().get(0).args();
        assertEquals(2, ab.a());
        assertEquals(1, ab.of().size());
        assertEquals("x", ab.of().get(0).last().classes().get(0));
    }

    @Test
    void langTakesIdentList() {
        PseudoArgs.Idents args = (PseudoArgs.Idents) onlySelector(onlyRule(sheet("a:lang(en, fr) { x: y }"))).last()
                .pseudos().get(0).args();
        assertEquals(List.of("en", "fr"), args.values());
    }

    @Test
    void unknownFunctionalPseudoKeepsRawArgs() {
        PseudoArgs.Raw args = (PseudoArgs.Raw) onlySelector(onlyRule(sheet("a:mypseudo(1px solid) { x: y }"))).last()
                .pseudos().get(0).args();
        assertFalse(args.values().isEmpty());
    }

    // ------------------------------------------------------------------ specificity

    @Test
    void specificityTable() {
        Object[][] cases = {{"*", 0, 0, 0}, {"a", 0, 0, 1}, {".c", 0, 1, 0}, {"#i", 1, 0, 0}, {"a.b:c", 0, 2, 1},
                {"a::c", 0, 0, 2}, {"a#i.b[k]:hover", 1, 3, 1}, {"panel > button.x", 0, 1, 2}, {"::part(t)", 0, 0, 1},};
        for (Object[] c : cases) {
            ComplexSelector s = onlySelector(onlyRule(sheet(c[0] + " { x: y }")));
            assertEquals(new Specificity((int) c[1], (int) c[2], (int) c[3]), s.specificity(), (String) c[0]);
        }
    }

    // ------------------------------------------------------------------ at-rules

    @Test
    void importHasNoBlock() {
        Stylesheet s = sheet("@import \"cloudlib:standard\"; a { color: red }");
        AtRule at = cast(AtRule.class, s.rules().get(0));
        assertEquals("import", at.name());
        assertNull(at.block());
        assertEquals(2, s.rules().size());
    }

    @Test
    void mediaHoldsNestedRules() {
        Stylesheet s = sheet("@media screen and (min-width: 10px) { a { color: red } }");
        AtRule at = cast(AtRule.class, s.rules().get(0));
        AtRule.Block.Rules block = cast(AtRule.Block.Rules.class, at.block());
        assertEquals(1, block.rules().size());
        StyleRule inner = cast(StyleRule.class, block.rules().get(0));
        assertEquals("a", inner.selectors().get(0).last().tag());
    }

    @Test
    void fontFaceHoldsDeclarations() {
        Stylesheet s = sheet("@font-face { font-family: \"x\"; src: url(y) }");
        AtRule at = cast(AtRule.class, s.rules().get(0));
        AtRule.Block.Declarations block = cast(AtRule.Block.Declarations.class, at.block());
        assertEquals(2, block.declarations().size());
    }

    @Test
    void unknownAtRuleKeepsRawValues() {
        Stylesheet s = sheet("@custom-thing foo { a: b }");
        AtRule at = cast(AtRule.class, s.rules().get(0));
        assertEquals("custom-thing", at.name());
        cast(AtRule.Block.Values.class, at.block());
    }

    @Test
    void atRuleNamesAreCaseInsensitive() {
        AtRule at = cast(AtRule.class, sheet("@MEDIA x { a { c: d } }").rules().get(0));
        assertEquals("media", at.name());
        cast(AtRule.Block.Rules.class, at.block());
    }

    // ------------------------------------------------------------------ error recovery

    @Test
    void missingCloseBraceStillParses() {
        List<CssError> errors = new ArrayList<>();
        Stylesheet s = sheet("a { color: red", errors);
        assertEquals(1, s.rules().size());
        assertEquals("color", ((StyleRule) s.rules().get(0)).declarations().get(0).property());
    }

    @Test
    void straySemicolonBetweenRulesIsSkipped() {
        Stylesheet s = sheet("a { x: y } ;;; b { c: d }");
        assertEquals(2, s.rules().size());
    }

    @Test
    void unclosedStringRecovers() {
        List<CssError> errors = new ArrayList<>();
        Stylesheet s = sheet("a { content: \"oops\n } b { x: y }", errors);
        // the bad string breaks the decl; following rule still parses
        assertFalse(errors.isEmpty());
        assertEquals(2, s.rules().size());
    }

    @Test
    void garbageBetweenRulesIsTolerated() {
        List<CssError> errors = new ArrayList<>();
        // "%%% b" merges into one invalid selector — the rule is dropped per spec,
        // the well-formed rules around it survive
        Stylesheet s = sheet("a { x: y } %%% b { c: d }", errors);
        assertFalse(errors.isEmpty());
        assertEquals(1, s.rules().size());
        assertEquals("a", ((StyleRule) s.rules().get(0)).selectors().get(0).last().tag());
    }

    @Test
    void badDeclarationSkipsToNext() {
        Stylesheet s = sheet("a { !!bad; color: red }");
        StyleRule rule = cast(StyleRule.class, s.rules().get(0));
        assertEquals(1, rule.declarations().size());
        assertEquals("color", rule.declarations().get(0).property());
    }

    @Test
    void invalidSelectorDropsRule() {
        Stylesheet s = sheet(".. { x: y } b { c: d }");
        assertEquals(1, s.rules().size());
        assertEquals("b", ((StyleRule) s.rules().get(0)).selectors().get(0).last().tag());
    }

    @Test
    void unclosedParenInValueTolerated() {
        Stylesheet s = sheet("a { background: nine-slice(\"t\", 3; color: red }");
        StyleRule rule = cast(StyleRule.class, s.rules().get(0));
        assertFalse(rule.declarations().isEmpty());
    }

    @Test
    void eofMidRuleClosesImplicitly() {
        Stylesheet s = sheet("a { color: red; padding");
        StyleRule rule = cast(StyleRule.class, s.rules().get(0));
        assertEquals(1, rule.declarations().size());
    }

    // ------------------------------------------------------------------ entry points & fixture

    @Test
    void parseValueListEntry() {
        List<ComponentValue> v = CssParser.parseValueList("nine-slice(\"x\", 3)");
        Function f = cast(Function.class, v.get(0));
        assertEquals("nine-slice", f.name());
    }

    @Test
    void parseSelectorListEntry() {
        List<ComplexSelector> sels = CssParser.parseSelectorList("panel > button.x:hover, *");
        assertEquals(2, sels.size());
        assertEquals(Combinator.child, sels.get(0).combinators().get(0));
    }

    @Test
    void realisticThemeFixture() {
        Stylesheet s = sheet("""
                /* a stock theme exercising the full surface */
                :root { --accent: #35D6D0; --frame: nine-slice("cloudlib:gui/background/dark", 3); }
                panel { padding: 4px 6px; background: var(--frame); color: var(--accent, #fff); }
                panel:focused > button.primary[icon] { border-width: 1px 2px; }
                item-slot:nth-child(2n+1):not(.empty) { background: tiled("t", 16px 16px); }
                @import "cloudlib:standard";
                @media ui { scrollbar::part(thumb) { opacity: .9 } }
                """);
        List<CssError> errors = new ArrayList<>();
        Stylesheet s2 = CssParser.parse("""
                panel { color: red }
                """, errors);
        assertTrue(errors.isEmpty());
        assertEquals(1, s2.rules().size());
        // :root + panel + panel:focused>button + item-slot + @import + @media
        assertEquals(6, s.rules().size());
    }
}
