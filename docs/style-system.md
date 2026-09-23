# CloudLib Style System — def-first redesign spec

Status: DRAFT (design review pending)

Replaces: ad-hoc `StyleType`/`StyleProperty`/`UIStyles` trio + bolted-on theme layer.
Mirrors: `EventDefinition` (def objects) + `CloudLang` (generated entry class) +
existing theme cascade (CSS stays, Java gets parity).

## 0. Why refactor

Today three disconnected vocabularies describe the same thing:

| surface | vocabulary | problem |
|---|---|---|
| Java DSL | 40 `XxxProperty` record classes + 244 `UIStyles` factories | value = behaviorless record; parse/apply/inherit metadata lives nowhere |
| CSS | `PropertyParsers` — a second, parallel `String → StyleProperty` table | duplicates the DSL by hand; drifts out of sync |
| Theme | `UIStyle` = `List<StyleProperty>` | untyped bag; widget integration bolted on via `themeStyle` field + scattered `refreshTheme()` |

Target: **one definition object per style property** — a `StyleDef<T>` — that
carries everything: css name, value type, parse, apply, inheritance, default.
Java DSL and CSS are just two front-ends writing into the same
`Map<StyleDef, value>` layer.

## 1. Core contract — DECIDED (record, not interface)

```java
// api/ui/style/key/StyleKey.java — the typeclass as a data record
public record StyleKey<T>(
        String id,                     // "padding" — css name, kebab-case
        Class<T> type,                 // EdgeRect — the Java value type
        StyleScope scope,              // layout | visual | custom — which context it feeds
        boolean inherited,             // participates in parent→child inheritance
        @Nullable T initial,           // initial value (null = no initial write)
        StyleParser<T> parser,         // CSS front-end: tokens → T (null = invalid)
        StyleApply<T> applier,         // T → StyleContext effect
        Function<T, String> formatter  // inspector display
) {
    public StyleValue<T> of(T value) {
        return new StyleValue<>(this, value);
    }
}

// the (key, value) pair — replaces StyleProperty
public record StyleValue<T>(StyleKey<T> key, T value) {
    public void apply(StyleContext ctx) { key.applier().apply(ctx, value); }
}
```

`StyleType` is deleted; inspector metadata (displayName/category/formatter)
folds into `StyleKey`. The 40 `XxxProperty` classes are deleted; their apply
logic moves into each key's `applier`. `UIStyles` factories are rewritten as
`Styles.padding.of(…)` delegates.

## 2. Vocabulary — DECIDED (closed; no registration)

The builtin property vocabulary is a **closed set**: `StyleKey` is a
`final class` with a package-private constructor — `BuiltinKeys` (in the same
`key` package) is the only construction site. `Styles` is the public facade
delegating to it; every `Styles.paddingLeft`-style constant is a fixed
builtin. There is no `StyleRegistry` and no `registerStyleKeys` plugin hook —
`new StyleKey<>(…)` does not compile outside `BuiltinKeys`.

```java
// api/ui/style/key/BuiltinKeys.java — the only place keys exist
public class BuiltinKeys {
    public static final StyleKey<LengthPercentage> paddingLeft =
            key("padding-left", LengthPercentage.class,
                StyleScope.layout, CssValues::lengthPercentage,
                StyleApplies::paddingLeft, StyleKey::formatLp, Edge::left);

    // fixed lookup tables — name → key, shorthand → expander, alias → name
    public static @Nullable StyleKey<?> byId(String cssName) { … }
    public static @Nullable Shorthand shorthand(String name) { … }
    public static Collection<StyleKey<?>> all() { … }
}
```

External extensibility is the css-native mechanism: **`--*` custom
properties** + `StyleVar<T>` (see §4d). A mod wanting new css-visible data
declares `--my-thing` in a theme and holds a `StyleVar` lens — it never
registers a key.

`Styles.init()` is still a no-op forcing class init so the whole index exists
before any CSS parses.

## 3. Two front-ends, one layer — DECIDED

Widget-level Java DSL = generated-shape factory functions (current `UIStyles`
call habits), each delegating to the def:

```java
// C shape — main API
var card = UIStyle.of(
    padding(4),                       // → Styles.padding.of(EdgeRect.all(4))
    display(TaffyDisplay.flex),
    gap(6),
    background(NineSliceTexture.of(loc, 3)));
widget.useStyle(card);

// low-level typed setter stays available
widget.set(Styles.padding, EdgeRect.all(4));
```

`UIStyles.padding(4)` etc are hand-written thin factories — one per key,
living next to `Styles` so the pair stays in sync. No selector/rules DSL on
the Java side: stylesheet authoring is CSS-only; Java only produces per-widget
`UIStyle` objects.

CSS: `padding: 4px` → registry lookup `Styles.byId("padding")` →
`key.parser()` → `StyleValue`. `PropertyParsers` table dies — the CSS
property vocabulary IS the `Styles` registry now.

## 4. Widget integration — DECIDED (WidgetPath-consistent, zero grafted types)

The grafted interface is gone entirely — no `Themeable`/`Selectable`/
`SelectorMeta`. The matcher works on **`Widget` directly**, navigating the real
widget tree; ancestry uses the same idiom as `WidgetPath` (parent chain,
root→leaf order).

```java
// internal — matcher signature, no new api types:
SelectorMatcher.matches(ComplexSelector sel, Widget leaf, MatchContext ctx)

// inside matchAt:
//   descendant  → walk leaf.parent() chain up
//   child       → leaf.parent()
//   sibling     → leaf.parent().children()  (CompositeWidget)
//   :has()      → subtree scan via composite.children()
```

`Widget` keeps only element metadata — the fields it already owns, no extra
interface:

```java
widget.styleId("close");            // #id       — String
widget.addStyleClass("primary");    // .class    — Set<String>
widget.styleAttr("kind", "close");  // [attr=]   — Map<String,String>
widget.addStyleState("engaged");    // :state    — Set<String>
widget.stylePart("thumb");          // ::part()  — String
widget.styleTag();                  // "button"  — kebab class name, overridable
```

`MatchContext` memoizes siblings/states/child-snapshots per `Widget` identity
for the resolution pass — same perf semantics, zero api surface.

Test fixtures: `TestPanel extends CompositeWidget<Widget>` +
`TestLeaf extends Widget` build real trees with the public child API — no
mock interfaces.

Ordered segments + dirty-mark flush stay as decided:

- `useStyle`/`set` write the `codeStyles` segment — applied last, wins.
- Theme resolution rewrites the `themeStyle` segment only.
- Widget factory defaults write `defaultStyle` — applied first, so theme
  can override component defaults.
- State flips → `widget.markStyleDirty()` → `scene.styleDirty(widget)` →
  per-tick flush re-resolves dirty widgets, posts `onThemeUpdate`.

## 4b. Open details — DECIDED

1. **`StyleKey.of(v)`** — def→value method name.
2. **`StyleValue<T>(key, value)`** — the pair record.
3. **Shorthands = CSS-side expansion** — the `Styles` registry is
   **longhand-only** (`paddingLeft`, `paddingRight`, `marginTop`, … each a
   `StyleKey<LengthPercentage>`-shaped key). `EdgeRect` is not a key value
   type — it's a Java-side convenience only. Shorthands (`padding`, `margin`,
   `inset`, `border-width`, `gap`, `overflow`, `flex`, `grid-column`…) live in
   a separate shorthand table:
   - CSS: `padding: 2px 4px` expands into 4 longhand declarations *at
     declaration-collection time*, preserving rule specificity/order — the
     cascade's winners map only ever sees longhand ids, so
     `padding: 2px; padding-left: 4px` resolves correctly by source order.
   - Java: `padding(4)` returns a `StyleValues` group of four
     `StyleValue`s; `UIStyle.of`/`useStyle` flatten groups uniformly.
   - `StyleContext` holds per-edge values; the taffy `Rect` is assembled at
     apply time (longhand applier patches one slot of the current rect).
4. **`StyleParser`/`StyleApply`** — api-level functional interfaces used by
   the builtin key definitions; there is no external registration (§2).
5. **"Layers" = three ordered `UIStyle` fields, not a layer system** —
   `defaultStyle` (widget factory defaults) → `themeStyle` (theme cascade
   result) → `codeStyles` (useStyle/set). Rebuild = `style.reset()` then
   apply each in order; same key → later writer wins. No enum, no registry,
   no priority sort — order IS the mechanism. Theme refresh only
   re-resolves the `themeStyle` segment and replays.
6. **Conflict granularity**: per-key — different keys coexist across
   segments; same key → later segment wins. `!important` lives strictly
   inside the theme cascade (theme rule vs theme rule), never escalates
   into `codeStyles`.

## 4c. CSS loading & ordering — DECIDED (descriptor-driven)

**Entry point: resource packs only.** A theme is a directory —
`assets/<ns>/ui/themes/<id>/theme.json` + its css files; the descriptor
(`name`, `description`, `css` order, `extends` dependencies, `default` flag)
is parsed by `StyleLoader`. Bare `*.css` files outside descriptor dirs load
as implicit single-file themes. No config-dir themes; programmatic themes can
still `Themes.register` directly.

**Discovery ≠ activation.** Every discovered theme registers into `Themes` by
id. What actually applies:

```java
// activation stack, in order:
//   1. user config selection   (cloudlib-themes.toml ui_themes = [...]) — wins
//   2. descriptor defaults     (theme.json "default": true, pack order)
//   3. cloudlib:standard       — the conventional base / final fallback
// stack order = cascade order inside the theme segment; later wins
```

**`extends` composes themes** — dependency rules splice in before the
dependent's own (source order); cycles cut with a warning. There is no
`@import`.

**Scene may self-decide.** The global stack is a *recommended default*,
not a mandate: `scene.setTheme(theme)` / per-widget overrides may bypass
or replace it. The cascade resolves against the scene's effective stack.

**Ordering rules**: within the theme segment, stack order = source order
(later wins). `!important` stays inside the cascade (theme rule vs theme
rule), never escalates into `codeStyles` — inline applies last, wins.

**Reload**: `StyleLoader` is a client reload listener; `Themes.reload()` is
the manual entry (`/cloudlib reload`, `StyleWatcher` file watching in dev).
Every change fires `StyleEvents.themeReload`; mounted scenes subscribe and
re-resolve their trees.

**Out of v1**: `@layer`, config-dir themes, `registerThemes` plugin hook,
hot per-scene theme switching UI.

## 4d. Custom properties (`--*`) — DECIDED (css-native, no registration)

Custom properties are real properties, not a parallel api:

- **Cascade** — `--*` declarations compete in the same winners map
  (specificity → order → `!important`), inherit unconditionally, and keep
  their **raw token stream** (`Tokens`) as the value. `var(--x, fb)`
  substitutes at computed-value time against the node's own resolved `--*`
  table; cycles and unresolvable references poison the declaration.
- **Storage** — resolved `--*` bindings ride on `UIStyle.vars()`
  (`Map<String, Tokens>`) and land in `StyleContext.vars()`.
- **Java lens** — `StyleVar<T>` is a mod-held constant: name + parser +
  optional writer + optional fallback. No registration; whoever holds the
  lens can read/write. Builtin codecs cover the common shapes:
  `StyleVar.color/number/integer/bool/ident/tokens/texture` — `texture` is
  the surface slot (`<texture>`, a bare `<color>` promoted to
  `ColorTexture`), for chrome slots that hold a background or a border
  rather than an ink.
- **Reads** — `style().var(MyVars.accent)` (typed, falls back),
  `style().varRaw("--accent")` (resolved tokens).
- **Writes** — `widget.setVar(var, v)` / `setVar("--x", "4px")` are
  **inline** bindings (`codeVars`): they top the node's var environment —
  the style-attribute semantic — and `markStyleDirty` re-resolves theme
  declarations referencing them. `UIStyles.var("--x", "4px")` /
  `var(myVar, v)` produce `VarBinding` entries usable in `UIStyle.of(...)`.

```java
public static final StyleVar<Integer> accent = StyleVar.color("--accent");
public static final StyleVar<Float>   pad    = StyleVar.number("--pad").orElse(4f);

widget.setVar(MyVars.accent, 0xFFFF6FA5);
Integer a = widget.style().var(MyVars.accent);
```

## 5. Migration path — DECIDED (full cut, no compat shim)

1. `StyleKey`/`StyleValue`/`StyleScope`/`ParseContext`/`StyleParser`/`StyleApply`
   land in `api.ui.style.key`; `StyleKey` sealed — `BuiltinKeys` is the only
   construction site; `Styles` is the public facade. `Styles.init()` runs at
   common setup.
2. Handlers ported per value shape (edge-box, dimension, enum, color, texture,
   grid-track, …). `PropertyParsers` deleted — CSS name resolution becomes
   `BuiltinKeys.byId` → `key.parser()`; shorthands via `BuiltinKeys.shorthand`.
3. `UIStyle` becomes `Map<StyleKey<?>, StyleValue<?>>`-backed (ordered);
   `StyleValue` replaces `StyleProperty` in all signatures; the 40
   `XxxProperty` classes deleted; `StyleType` deleted.
4. `UIStyles` factories rewritten as thin `key.of(...)` wrappers — same
   call sites keep compiling where signatures still fit.
5. `Widget`: `Themeable` interface deleted — matcher works on `Widget` +
   real `parent()`/`children()` directly; layered style lands;
   `codeStyles`/`themeStyle`/`refreshTheme` removed; dirty-mark +
   `onThemeUpdate` flush.
6. `SelectorMatcher`/`Cascade` signatures switch `Themeable` → `Widget`
   (match context memoizes per-Widget identity); theme tests rebuild fixtures
   as real `Widget`/`CompositeWidget` subclasses.
7. ThemeEngine output type changes from `UIStyle` assembly to the same
   `StyleValue` map the DSL writes — one representation end to end.

No deprecation window: prerelease line, single clean cut.

## 5b. Eager vs lazy CSS values — DECIDED

CSS declarations parse to `Styled` **eagerly at theme load** where possible;
declarations containing `var()` stay token-deferred (substitute at resolve
time, then parse). Cascade output is a `Map<StyleDef, Styled>` — no re-parse
per node. This also kills `PropertyParsers.Context` per-node parse overhead
seen in the profile.

## 6. Non-goals (unchanged)

- Selector engine, cascade, var() — untouched; only the *output type* of
  resolution changes (`Map<StyleDef,?>` → `UIStyle`).
- Theme file format, reload pipeline, `Scene.liveScenes` refresh.
- Taffy mapping semantics.

## 7. Quality bar

- Every def round-trips: `parse(format(v))` ≡ v for canonical forms.
- CSS parity test: for each registered def id, a fixture declaration parses
  to the same `Styled` the DSL factory produces.
- Inspector: every def has `format` + scope; `Styles.all()` drives the style
  inspector table without hardcoded rows.
- `LayeredStyle`: theme layer re-resolution never disturbs the inline layer;
  dirty-mark batching verified by accessor-call counts (same deterministic
  technique as ThemePerfTest).
