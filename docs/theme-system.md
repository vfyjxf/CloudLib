# CloudLib Theme System

> Status: landed — descriptor-driven themes inside the style layer
> Goal: resource-pack-loadable, cascade-capable styling for every CloudLib widget
> (screens, overlays, in-world panels) plus a stock OreUI-style pixel theme.
>
> Themes are part of the **style layer** (`api/ui/style`, `internal/ui/style`) —
> there is no separate theme package. The style loader reads `theme.json`
> descriptors + stylesheets from resource packs into the `Themes` registry.

## 1. What exists today

- **Layout styles** — `UIStyle` / `UIStyles.*` → taffy (`padding`, `margin`, `gap`,
  `flex*`, `size`, `inset`, `display`, ~40 properties).
- **Visual styles** — `VisualProperty` → `VisualContext` (`background`, `icon`,
  `borderWidth/Color`, `shadow*`, `text*`, `zIndex`, arbitrary `setProperty`).
- **Textures** — `VisualTexture` family: `NineSliceTexture` (incl. vanilla
  gui-sprite-atlas mode), `BorderTexture`, `TiledTexture`, `SpriteTexture`,
  `GradientTexture`, `ColorTexture`, `ProgressTexture`, `RoundedRectTexture`,
  `ShadowTexture`, animations/tweens.
- **Widget state** — `hovered`, `active`, `interactive`, scene focus.
- **Consumption** — `useStyle(...)` applies properties; `renderInternal` reads
  `visualContext.background()/icon()`.

The missing layer: a *descriptor format + selector/cascade engine + theme
registry* that resolves those styles from resource packs instead of code.

## 2. Format decision — real CSS

**Confirmed: actual CSS files, parsed by a vendored port of Katana.**

- Katana (pure C99, MIT, WebKit-derived CSS Syntax algorithm) is being ported
  to Java as `dev.vfyjxf.cloudlib.internal.css`. MIT attribution ships in-tree.
- Fidelity target: CSS Syntax Module Level 3 — full tokenizer, qualified rules,
  at-rules with nested blocks, declaration parsing incl. `!important`, custom
  properties (`--*` raw tokens), selector AST with specificity, spec-compliant
  error recovery. Non-standard extensions stay possible because we own the
  value/pseudo vocabularies.
- Why not JSON: rejected — user directive. The format must be authorable CSS so
  pack makers use the language they already know, and so unknown/nonstandard
  constructs degrade gracefully instead of failing a codec.

### File location & identity

A theme is a **directory** under `ui/themes/` described by `theme.json`:

```
assets/<namespace>/ui/themes/<id>/theme.json   → theme id <namespace>:<id>
assets/<namespace>/ui/themes/<id>/*.css        → its stylesheets
assets/cloudlib/ui/themes/standard/theme.json  → cloudlib:standard
```

```json
{
  "name": "Standard",                          // display name → Theme.meta()
  "description": "The stock pixel theme",
  "css": ["base.css", "widgets.css"],          // composing files, in order;
                                               // default: every direct-child *.css by name
  "extends": ["cloudlib:core"],                // theme dependencies — their rules
                                               // cascade before this theme's
  "default": true                              // join the packs' recommended active stack
}
```

Layering uses **`extends`** — a theme-level dependency, resolved at load:
the parent's rules are spliced in before the child's (source order), so the
child wins equal-specificity ties. Cycles are cut with a warning. There is no
css-level `@import` — the descriptor owns composition.

A bare `*.css` outside every descriptor directory still loads as an implicit
single-file theme (`ns:path` minus the extension) — useful for quick packs.

## 3. Theme anatomy

```css
/* assets/cloudlib/ui/themes/standard.css */

:root {
    --accent: #35D6D0;
    --panel-bg: #D80A0E12;
    --text: #E8F4F8;
    --cell: 18px;
    --panel-frame: nine-slice("cloudlib:gui/panel/dark", 3);
}

panel {
    padding: 4px 6px;
    background: var(--panel-frame);
    color: var(--text);
}

panel:focused {
    background: nine-slice("cloudlib:gui/panel/light", 3);
}

button.primary:pressed {
    background: var(--button-down, nine-slice("cloudlib:gui/button/pressed", 2));
    color: var(--accent);
    sound-click: "minecraft:ui.button.click";
}

panel > item-slot { background: nine-slice("cloudlib:gui/slot/base", 2); }

#close, .danger { color: #FF5252; }
```

### 3.1 Cascade — web standard

1. **Origin**: every loaded theme sheet is author-origin; `!important` wins
   over normal declarations (kept — it is part of the standard cascade).
2. **Specificity** `(a,b,c)` — ids > classes+attributes+pseudo-classes >
   types, per spec.
3. **Order** — sheets apply in *cascade order*: the active stack
   (`cloudlib-themes.toml` `ui_themes` config → descriptor `"default": true`
   themes → `cloudlib:standard` fallback) flattened with each theme's
   `extends` chain inlined first. Later sheets win ties.
4. **Inline `useStyle`/`setVar`** — treated as the style attribute: wins over
   all normal declarations (inline `--*` bindings top the node's var
   environment too), loses to `!important` theme rules. (Web-faithful.)
5. **Inheritance** — text-ish properties (`color`, `font`) inherit down the
   widget tree; layout/box properties do not; **custom properties (`--*`)
   inherit unconditionally**. Matches CSS.
6. **Fallback** — `var(--x, fallback)` supported; unresolved vars and var
   cycles make the declaration invalid at computed-value time (the property
   stays unset; the cyclic `--*` itself emits nothing).

### 3.2 Selectors

Full CSS3 selector grammar (parser gives the complete AST); the *matcher*
supports:

| Form | Meaning |
|---|---|
| `panel` | widget theme tag (registered or kebab-cased type name) |
| `.primary` | `widget.themeClasses()` |
| `#close` | `widget.styleId()` |
| `*` | universal |
| `[key]`, `[key=value]`, `[key~=w]`, `[key|=v]`, `^= $= *=` (+ `i`/`s` flags) | widget `themeAttribute(name)` map |
| `:hover` `:focus` `:active` `:disabled` `:enabled` `:checked` `:selected` | builtin state set |
| `:not(...)`, `:is(...)`, `:where(...)` | selector-list pseudos (`:where` = zero specificity) |
| `:root` | the scene root widget |
| `A B`, `A > B`, `A + B`, `A ~ B` | combinators over the widget parent chain |
| `::part(name)` | exposes a widget's named inner part (e.g. `panel::part(title)`) |

CloudLib extension pseudo-classes (non-standard, matcher-level):
`:engaged`, `:open`, `:focused` (alias of `:focus` kept for theme authors),
`:pointed` (in-world pointer). Unknown pseudo-classes never match —
spec-consistent forward compatibility.

### 3.3 Values — taffy-aligned, not full web CSS

The **parser** accepts complete CSS Syntax L3; the **theme layer** validates
against taffy-java's actual capability set. Unsupported units/properties log a
warning and drop the declaration (never crash).

- **lengths** — taffy's real vocabulary: `<number>px`, `<n>%`, `calc(...)`,
  `auto`, `min-content`, `max-content`, `fit-content`, `stretch`. (`em`, `rem`,
  `vw/vh` and other web units are **unsupported** — rejected with a log line.
  Taffy has no rem/viewport concept.)
- **colors** — `#RGB #RGBA #RRGGBB #RRGGBBAA`, `rgb()/rgba()`, `hsl()/hsla()`,
  named CSS colors, `transparent`, `currentColor`.
- **edge shorthands** — `padding`/`margin`/`border-width`/`gap` take 1-4
  values, CSS ordering (top right bottom left).
- **keywords** per taffy enums:

| property | accepted values (taffy enum) |
|---|---|
| `display` | `block flex grid none` (no inline/table) |
| `position` | `relative absolute` only (no fixed/sticky) |
| `overflow` / `-x/-y` | `visible clip hidden scroll` |
| `box-sizing` | `border-box content-box` |
| `flex-direction` | `row column row-reverse column-reverse` |
| `flex-wrap` | `nowrap wrap wrap-reverse` |
| `align/justify/align-self/align-content` | `auto start end flex-start flex-end center baseline stretch` + `space-between/around/evenly` (justify) |
| `direction` | `inherit ltr rtl` |
| `text-align` | `auto start end left right center justify justify-all` |
| `grid-auto-flow` | `row column row-dense column-dense` |
| `grid-*` placements | `auto`, `<line>`, `span <n>`, named lines/spans |
| `grid-template-*` | track lists: `fr`, `px`, `%`, `minmax()`, `repeat(<n>|auto-fill|auto-fit, …)`, named lines `[name]` |
| `aspect-ratio` | `<n>` or `<n>/<n>` |

- **CloudLib texture functions** (non-standard value functions — the reason we
  own the value pipeline):

```css
background: nine-slice("cloudlib:gui/panel/dark", 3);
background: nine-slice("cloudlib:gui/panel/dark", 3 5 3 5);   /* t r b l */
background: sprite("cloudlib:gui/button/base");               /* gui-sprite atlas */
background: tiled("cloudlib:gui/checker", 16px 16px);
background: linear-gradient(#0000, #000C);                    /* vertical default */
background: color(#D80A0E12);
background: border-texture("cloudlib:gui/frame/dark", 3, #0008);
```

  Function name → `TextureFactory` registry entry — mods register factories
  for custom `VisualTexture` types.

- **strings** — `"..."`/`'...'` for sounds, fonts.
- **fonts** — `font: "ns:id"` / `font-family: ...` (vanilla font registry ids).
- **sounds** — `sound-click`, `sound-hover`, … → `VisualContext` custom props.

### 3.4 Property mapping — `UIStyles` vocabulary is the contract

The themable set = exactly what `StyleProperty`/`VisualProperty` can apply.
CSS names map 1:1 to the existing property classes; names taffy lacks
(`float`, `vertical-align`, `line-height`, `font-size` …) are rejected with a
warning.

| CSS property | target |
|---|---|
| `display`, `position`, `inset`/`top/right/bottom/left`, `overflow`, `box-sizing`, `direction`, `aspect-ratio`, `scrollbar-width` | taffy layout style — **themable** (theme can hide chrome via `display:none`), but values restricted to the enum sets above |
| `padding*`, `margin*`, `gap`/`row-gap`/`column-gap`, `width`/`height`/`min-*`/`max-*`, `flex`/`flex-*`, `align-*`, `justify-*`, `grid-*`, `text-align` | taffy layout style |
| `background`, `background-color`, `background-image`, `icon`, `border-width`, `border-color`, `box-shadow`, `opacity`, `z-index` | `VisualContext` |
| `color`, `font`/`font-family`, `font-weight`, `font-style`, `text-decoration` | `VisualContext` text props |
| `--*` custom properties | `UIStyle.vars()` → `StyleContext.vars()` — raw token streams, `var()`-resolved; read via `StyleVar<T>` |
| anything else unknown | warned + dropped (closed vocabulary — unknown non-`--` names are never keys) |

Inheritance: only `color`, `font-family`, `font-weight`, `font-style`,
`text-decoration`, `text-align`, `direction` inherit (web's inherited set,
minus things taffy doesn't have) — plus every `--*`, which always inherits.

## 4. Widget model (landed)

`Widget implements Themeable` — the selector surface is:

```java
widget.styleId("close")            // #id
widget.addStyleClass("primary")    // .class
widget.styleAttr("kind", "close")  // [attr=]
widget.addStyleState("engaged")    // :custom-state
```

`themeTag()` defaults to the kebab-cased class name (`LabelWidget` →
`label`); `themeParent/themeSiblings/themeChildren` walk the widget tree;
`themePart()`/`::part(name)` address named parts of compound widgets.

Builtin pseudo mapping: `:hover`/`:hovered`←`hovered`, `:disabled`/`!active`,
`:enabled`/`active`, `:focused`←focus chain, `:interactive`/`:non-interactive`,
plus widget-pushed states (`:pressed`, `:checked`, `:selected`, `:engaged`…).

Effective style resolution (per widget, recomputed on mount / state flip /
theme reload via the `StyleEvents.themeReload` subscription every mounted
`Scene` holds):
`theme cascade (specificity→order, !important first) → inherited props →
code useStyle/setVar replay`. Layout props mark the taffy node dirty; visual
props repaint next frame. `useStyle` calls and inline vars are recorded in
`widget.codeStyles`/`codeVars` so a theme refresh never loses code-applied
styles — the inline-style rule.

## 5. Runtime (landed)

- `StyleLoader` (internal/ui/style): `SimplePreparableReloadListener` on
  `RegisterClientReloadListenersEvent` — scans `assets/*/ui/themes/`, reads
  every `theme.json` descriptor + its css files, resolves `extends` chains
  (cycle-safe), parses each stylesheet, registers into `Themes`, then picks
  the active stack: `ui_themes` config → descriptor `default` themes →
  `cloudlib:standard` fallback.
- `Themes` (api/ui/style): `id → Theme` registry + the active-id **stack** —
  `register/unregister/setActive/active/activeIds/refreshTree/reload`.
  `Themes.reload()` re-runs the loader inline on the client thread.
- `ThemeEngine` + `Cascade` (style layer): `Cascade` matches rules, orders
  the cascade, substitutes `var()`, merges inline `codeVars` on top, and
  inherits `--*` unconditionally; `ThemeEngine` parses winning declarations
  through `BuiltinKeys` and emits `UIStyle` = values + `vars`.
- `StyleEvents.themeReload` — the CloudLib event fired whenever the effective
  theme may have changed (loader apply, `Themes.setActive`, `unregister`).
  Each mounted `Scene` subscribes on mount and unsubscribes on destroy;
  the listener calls `refreshTheme()` — re-resolving the tree while
  honoring per-scene theme overrides.
- **Dev loop**: `/cloudlib reload` (client command) reloads themes by hand;
  `/cloudlib themes` lists the registry + active stack. `StyleWatcher`
  (dev only, `ui_theme_watch` config) watches exploded mod roots, classpath
  resource dirs and directory resource packs for `*.css`/`theme.json`
  changes, debounces ~300ms, and reloads on the client thread.
- Parse errors log `theme:file:line:col` and never kill the game.

## 6. Stock themes (CalculatorCirrus port)

From `directed-graph-calculator`, generic OreUI-style assets →
`assets/cloudlib/textures/gui/`:

| source | target | use |
|---|---|---|
| `background/{flat,dark,inset,frame}.png` (18²) | `gui/panel/{flat,dark,inset,frame}` | `nine-slice(...,3)` |
| `background/outlined/{flat,inset}.png` | `gui/panel/outlined_*` | alt panels |
| `background/border/{dark,light}.png` | `gui/frame/{dark,light}` | framed panels |
| `background/slot/{base,dark}.png` | `gui/slot/{base,dark}` | item slots |
| `background/scoll.png` | `gui/scroll/track` | scrollbar |
| `background/paged/{arrow,button}.png` | `gui/pager/*` | pagers |
| `background/sign/{amount_sign,bar}.png` | `gui/sign/*` | info bars |
| `toggle/button/{up,hover,down}.png` | `gui/button/{base,hover,pressed}` | buttons |
| `toggle/switch/**` | `gui/switch/*` | switches |
| `toggle/triple/**` | `gui/toggle_triple/*` | tri-state |
| `tab/{hover,select,unselect}.png` | `gui/tab/*` | tabs |
| `icon/arrow_*`, `icon/middle/*`, `icon/small/*` | `gui/icon/*` | generic icons |
| `icon/calc/**` (calculator/db/sort/tree…) | — skip — | mod-specific |

- `cloudlib:standard` — OreUI pixel theme on the ported assets.
- `cloudlib:hacker` — `HackerTheme` constants re-expressed in CSS; the
  in-world chrome tags (`inworld-panel`, `hint-chip`, `leader-line`,
  `presence-pip`) become the dogfooding surface. `HackerTheme` constants stay
  as deprecated delegates during migration.

## 7. Extensibility

- **`--*` custom properties are the extension mechanism.** Themes declare them
  like any other property; they cascade, inherit unconditionally, and keep
  their raw token streams. Java reads them through `StyleVar<T>` lenses —
  a mod-side constant declaring name + parser + optional writer/fallback,
  no registration:

  ```java
  public static final StyleVar<Integer> accent = StyleVar.color("--accent");
  public static final StyleVar<Float>  speed   = StyleVar.number("--speed").orElse(1f);

  Float speed = widget.style().var(MyVars.speed);
  widget.setVar(MyVars.accent, 0xFFFF6FA5);   // writer → tokens, inline-level priority
  widget.setVar("--scanline", "12px");        // raw string write works too
  ```

- The builtin property vocabulary is **closed** — `StyleKey` is not publicly
  constructible; `Styles.*`/`BuiltinKeys` are the only keys. Mods needing new
  css-visible data use `--*` + `StyleVar`, exactly like the web.
- Texture value functions (`nine-slice`, `sprite`, `tiled`, `color`,
  `linear-gradient`, `border-texture`…) are a fixed builtin table in
  `CssTextures`.

## 8. Quality bar (parser port acceptance)

- Full CSS Syntax L3 tokenizer (all token kinds: at-keyword, hash, strings
  with escapes, url/bad-url, numbers/dimensions/percentages, CDO/CDC,
  unicode-range, comments, EOF-in-string tolerance).
- Qualified rules + at-rules incl. nested rule blocks (`@media` keeps rules).
- Declarations: `!important`, `--*` custom props (raw tokens preserved),
  spec error recovery (skip-to-`;`/`}`).
- Selector AST: compounds/complexes, all 4 combinators, attribute ops +
  `i`/`s` flags, pseudo-classes/elements with argument lists, `:not`/`:is`/
  `:where`/`:nth-*` argument groups, specificity (a,b,c).
- Test suite: tokenizer + parser + selector + recovery + a real-world
  fixture sheet; every test green; spotless-clean; MIT headers on all files.

## 9. Implementation phases

1. ✅ **Parser** — Katana→Java port (`internal.css`), 56 tests.
2. ✅ **Theme model** — `Theme`/`Cascade`/`SelectorMatcher`/`PropertyParsers`/
   `ThemeValues` + `ThemeManager`/`ThemeEngine`/`ThemeLoader`, 26 cascade tests.
3. ✅ **Widget model** — `Widget implements Themeable`, tag/class/id/attr/state
   setters, code-style provenance replay, `::part` hook, state-refresh wiring
   (hover/focus/active).
4. ✅ **Stock assets** — 104 Cirrus textures in, `standard.css` + `hacker.css`
   dogfood-tested (every tag resolves ≥1 property).
5. 🔲 **Runtime dogfood** — `HackerTheme` constants deprecated in favor of
   `hacker.css`; in-world chrome widgets tagged (`inworld-panel` etc).
