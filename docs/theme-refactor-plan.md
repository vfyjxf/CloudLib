# Theme/Style Refactor Plan — closed keys, real custom properties, theme.json

> Status: executed. All steps landed; Nimbus migrated.
> Replaces: `StyleRegistry` + `registerStyleKeys` + css-only `--*` handling + flat
> `**.css` scan + `@import` + `themes.json` manifest.

## 1. Goals

1. **Delete the registration system.** `StyleRegistry`, `CloudLibPlugin.registerStyleKeys`,
   the bootstrap loop — all gone. The style vocabulary is closed.
2. **Seal `StyleKey`.** Builtin properties remain `StyleKey<T>` (they deliver typed
   values into taffy/visual), but construction is internal-only.
3. **Custom properties done the CSS way.** `--*` are ordinary declarations: same
   cascade, unconditional inheritance, raw token values, `var()` substituted at
   computed-value time. They land on the resolved style and on `StyleContext`,
   inspectable and settable from Java.
4. **Java type safety without registration.** `StyleVar<T>` — a mod-owned typed
   lens over a `--name`: parser (tokens→T), writer (T→css text), fallback.
   No global registry; whoever holds the constant uses it.
5. **Per-theme `theme.json` descriptor.** A theme is a directory:
   `assets/<ns>/ui/themes/<id>/theme.json` + css files. The descriptor declares
   file order (`css`), theme dependencies (`extends`), metadata, and whether the
   theme is a recommended default (`default`). Replaces the flat scan, `@import`,
   and the `themes.json` default-stack manifest.
6. **Dev reload.** `Themes.reload()` API, a dev-only file watcher over theme
   directories, and the `/cloudlib reload` client command (the user chose the
   command over wiring `Alt+R`).

## 2. Layer structure

No `theme` package — theming is a capability of the style layer. The JSON
descriptor keeps the `theme` name (`theme.json`, `ui/themes/` paths, theme ids).

```
api/css            pure CSS: tokenizer, parser, selectors, at-rules,
                   + Tokens (component-value list wrapper: of(String), serialize)
api/ui/style/key   closed vocabulary — StyleKey (package-private ctor),
                   BuiltinKeys (the only construction site + fixed lookup tables),
                   StyleValue/StyleValues/StyleEntry/Shorthand/StyleParser/
                   StyleApply/StyleScope/StyleParseContext (unchanged SPI),
                   + VarBinding (StyleEntry for --* writes)
api/ui/style       Styles (constant facade → BuiltinKeys), StyleVar<T>,
                   UIStyle (+ vars map), StyleContext (+ vars map),
                   UIStyles (+ var() factories),
                   Theme (+ metadata), Themes (+ reload, fires themeReload event),
                   ThemeEngine ← moved in
internal/ui/style  CssEnums/CssValues/CssTextures/StyleApplies (unchanged)
                   + Cascade (+ inline var env), SelectorMatcher,
                   StyleLoader (descriptor-driven), StyleWatcher (dev),
                   StyleConfig ← moved in, renamed
```

## 3. Custom properties model

- css `--x: <tokens>` competes in the same winners map, inherits unconditionally.
- `var(--x, fb)` resolves at computed-value time against the node's own resolved
  `--*` table (incl. inherited), then `theme.rootVars()`, then fallback.
  Unresolved → declaration dropped (current/katana semantics, kept).
- **Inline vars**: `widget.setVar(...)` bindings merge into the node's resolved
  `--*` table at top priority (style-attribute semantics) and feed `var()`.
  `setVar` marks the widget style-dirty so dependent declarations re-resolve.
- Resolved `--*` bindings land in `UIStyle.vars` → `StyleContext.vars`.
- `StyleVar<T>`:

```java
public final class StyleVar<T> {
    String name;                                        // "--nimbus-bg"
    StyleParser<T> parser;                              // tokens -> T
    Function<T, String> writer;                         // T -> css text (nullable = read-only)
    @Nullable T fallback;

    static <T> StyleVar<T> of(String name, StyleParser<T> parser);
    static <T> StyleVar<T> of(String name, StyleParser<T> parser,
                              Function<T, String> writer, @Nullable T fallback);
    // codec factories backed by the builtin value grammar:
    static StyleVar<Integer> color(String name);
    static StyleVar<Float>   number(String name);
    static StyleVar<Integer> integer(String name);
    static StyleVar<Boolean> bool(String name);
    static StyleVar<String>  ident(String name);
    static StyleVar<Tokens>  tokens(String name);
    static StyleVar<VisualTexture> texture(String name);   // surface slot: <texture> | <color>
}
```

- Reads: `style().var(key)`, `uistyle.var(key)`, `varRaw(name)` → `Tokens`.
- Writes: `widget.setVar(key, v)`, `widget.setVar("--x", "4px")`,
  `useStyle(var(key, v))` / `var("--x", "4px")` via `VarBinding`.
- Java-written `var()` references (`setVar("--a", "var(--b)")`) substitute at
  apply time against the context's own vars.

## 4. Sealing mechanics

- `StyleKey` record → `final class`, **package-private constructor** in
  `api.ui.style.key`. Only code inside that package can construct keys.
- `BuiltinKeys` (public, `key` package) holds every builtin constant plus the
  fixed `byId`/`shorthand`/alias tables — today's `Styles` body moves here.
- `api.ui.style.Styles` stays as the public facade: `public static final`
  constant aliases + `byId`/`init` delegates — zero call-site churn.
- `StyleScope` unchanged (`custom` scope stays for builtin context-stored keys
  like `accent`, `scrollbar-style`).

## 5. theme.json descriptor

```
assets/<ns>/ui/themes/<id>/theme.json
assets/<ns>/ui/themes/<id>/*.css
```

```json
{
  "name": "Standard",
  "description": "stock pixel theme",
  "css": ["base.css", "widgets.css"],
  "extends": ["cloudlib:core"],
  "default": true
}
```

- `css` — files composing the theme, in order. Default: every `*.css` in the
  directory, filename-sorted.
- `extends` — author-time composition: dependency themes' rules are spliced in
  before this theme's (cycle → warn + cut). Replaces cross-theme `@import`.
- `default` — collected across packs as the recommended activation stack;
  `ui_themes` config still wins; `cloudlib:standard` is the final fallback.
- Theme id = descriptor directory path under `ui/themes/` (`ns:path`), so
  `cloudlib:standard` keeps working.
- Backward compat: a bare `ui/themes/x.css` with no descriptor loads as an
  implicit single-file theme `ns:x`.
- `Theme` gains `meta` (name/description); `compose` keeps the top layer's meta.

## 6. Reload & watch

- `StyleLoader` (renamed from `ThemeLoader`, `internal/ui/style`) — descriptor
  driven; `reload(ResourceManager)` runs prepare + apply inline (render thread;
  IO is a handful of small files).
- `Themes.reload()` — client facade using the current resource manager.
- **Reload event**: `StyleLoader`/activation changes fire a global cloudlib
  event `StyleEvents.themeReload` (`api.ui.style`) instead of the old
  `Themes.onChange` wiring; `Scene` registers itself on mount and decides how to
  react — `Themes.refreshTree(root)` (style re-resolve) today, rebuild-capable
  later. `Themes.onChange` is removed; listeners subscribe to the event.
- `StyleWatcher` (new, `internal/ui/style`):
  - Enabled when `!FMLEnvironment.production` and `ui_theme_watch` config
    (new, default `!production`).
  - Daemon thread + `WatchService`; watch roots = every mod file's
    `assets/*/ui/themes` (`IModFile.findResource`, jar entries naturally skip)
    plus directory packs under `resourcepacks/`; watches the `ui/themes` roots
    recursively so new theme dirs are discovered.
  - `*.css`/`theme.json` create/modify/delete → debounce ~300 ms →
    `Minecraft.execute(Themes::reload)`; OVERFLOW → reload anyway.
  - Watch set rebuilt after each reload.
- `/cloudlib reload` + `/cloudlib themes` client commands (registered via
  `RegisterClientCommandsEvent`) — the manual reload entry; `KeyMappings.
  refreshUI` stays unused.

## 7. Migration — NimbusProjection

- css: `nimbus-*` properties → `--*` custom properties.
- `TraceKeys`/`InworldKeys` → `StyleVar` constants; `registerAll` deleted.
- `NimbusPlugin.registerStyleKeys` override removed.
- `TraceStyle`/`InworldStyle`: `style.get(key)` → `style.var(key)`.

## 8. Execution order

0. Fold theme into style: `api/ui/theme/*` → `api/ui/style/`,
   `internal/ui/theme/*` → `internal/ui/style/`; rename `ThemeLoader` →
   `StyleLoader`, `ThemeConfig` → `StyleConfig`. `theme.json`/`ui/themes`/
   `Theme`/`Themes` names stay. Fix imports.
1. `api/css/Tokens` (+ `ComponentValue` serializer for inspector/writer).
2. Seal `StyleKey`; `BuiltinKeys` (constants + tables); `Styles` facade;
   delete `StyleRegistry` + `registerStyleKeys`.
3. `StyleVar`, `VarBinding`, `StyleEntry` plumbing.
4. `UIStyle.vars`, `StyleContext.vars`/`var`/`setVar`, `Widget.codeVars`/`setVar`.
5. `Cascade` inline env; `ThemeEngine` emits vars.
6. `StyleLoader` descriptor rewrite; `Theme.meta`; `Themes.reload` +
   `StyleEvents.themeReload`; `Scene` subscribes on mount.
7. `StyleWatcher`, `ui_theme_watch`, keybind wiring.
8. Inspector `vars:` section.
9. Migrate bundled `standard.css` to the directory layout.
10. Tests: update registry tests; add vars/descriptor coverage.
11. Nimbus migration.

## 9. Verification status

- `./gradlew test` — all green (style + cascade + vars tests).
- `./gradlew spotlessCheck` — clean; `noFullyQualifiedNames` lint added to
  `cloudlang-plugin`'s `CloudStylePlugin` (report-only, collision-aware,
  `// fqn-ok` suppression) and the whole codebase passes it. Generated
  `CloudLang.java` conforms after the writer was fixed to emit `{}`.
- File watcher — verified end-to-end in a real client run:
  `Theme file watcher started` on boot, `Loaded 1 theme(s)` on resource
  load, and editing `build/resources/main/.../standard/base.css` fired a
  second `Loaded` line ~1s later on the render thread (watch → 300ms
  debounce → `Minecraft.execute(Themes::reload)`). A broken-css edit
  produced `base.css:91:1 unexpected token in selector list`, proving the
  file is genuinely re-parsed.
- `/cloudlib reload` — registered via `RegisterClientCommandsEvent` without
  error; it calls the same `Themes.reload()` the watcher verified.
- `runGameTestServer` is a no-op — the codebase has no `@GameTest` classes
  and the style system is client-only.
