# CloudLib Theme System

> Status: design — decisions confirmed, parser port in progress
> Goal: resource-pack-loadable, cascade-capable styling for every CloudLib widget
> (screens, overlays, in-world panels) plus a stock OreUI-style pixel theme.

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

```
assets/<namespace>/ui/themes/<path>.css      → theme id <namespace>:<path>
assets/cloudlib/ui/themes/standard.css       → cloudlib:standard
assets/cloudlib/ui/themes/hacker.css         → cloudlib:hacker
```

Layering uses **`@import`** — the web-standard composition mechanism:

```css
@import "cloudlib:standard";   /* pulled in at this position in the cascade */
```

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
3. **Order** — sheets apply in *cascade order*: the active theme list
   (`cloudlib.ui_themes` config, ordered, default `["cloudlib:standard"]`)
   flattened with each file's `@import`s inlined at their position.
   Later sheets win ties.
4. **Inline `useStyle`** — treated as the style attribute: wins over all
   normal declarations, loses to `!important` theme rules. (Web-faithful.)
5. **Inheritance** — text-ish properties (`color`, `font`) inherit down the
   widget tree; layout/box properties do not. Matches CSS.
6. **Fallback** — `var(--x, fallback)` supported; unresolved vars make the
   declaration invalid at computed-value time (spec behavior: property is
   unset/inherited, never a parse error).

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
| `sound-*`, `cursor`, `--custom`, anything unknown | `VisualContext.setProperty(name, raw)` |

Inheritance: only `color`, `font-family`, `font-weight`, `font-style`,
`text-decoration`, `text-align`, `direction` inherit (web's inherited set,
minus things taffy doesn't have).

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
theme reload via `Scene.liveScenes` → `ThemeManager.refreshTree`):
`theme cascade (specificity→order, !important first) → inherited props →
code useStyle replay`. Layout props mark the taffy node dirty; visual props
repaint next frame. `useStyle` calls are recorded in `widget.codeStyles` so a
theme refresh never loses code-applied styles — the inline-style rule.

## 5. Runtime (landed)

- `ThemeLoader` (internal): `SimplePreparableReloadListener` on
  `RegisterClientReloadListenersEvent` — reads `assets/*/ui/themes/**.css`,
  parses once, flattens `@import` chains (cycle-safe), registers into
  `ThemeManager`.
- `ThemeManager` (api): `id → Theme` registry + activation **stack** —
  `activate/deactivate/active/activeStack/onChange/refreshTree`.
  `cloudlib:standard` auto-activates when present; packs override by id.
- `ThemeEngine.resolve(theme, widget)` → `UIStyle`: `Cascade` (match →
  specificity/order → var() → inherit) → `PropertyParsers` → `StyleProperty`s.
- `Scene.liveScenes()` tracks mounted scenes; `ThemeManager.onChange` in
  `CloudLibClient` re-resolves every live tree on reload — F3+T style hot
  swap. Parse errors log `file:line:col` and never kill the game.

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

- `TextureFactory` registry: `ThemeBootstrap.registerTextureType(name, fn)` —
  custom `VisualTexture` types become themeable values.
- Unknown/custom properties flow into `VisualContext.properties` — widgets
  read theme-driven data (`--scanline-speed: 12`) CloudLib never heard of.
- Custom pseudo-classes register matcher predicates (feature-owned state
  vocabularies like `:engaged`).

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
