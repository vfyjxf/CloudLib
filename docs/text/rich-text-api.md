# Rich Text API

> [中文文档](rich-text-api.zh-CN.md)

CloudLib's rich text system is a vanilla-friendly document model with its own layout
engine, plugged into taffy text measurement. It goes beyond vanilla `Component`:
mixed styled runs, localization with rich arguments, inline images/items/blocks/
entities, interactive (clickable/hoverable) fragments, and embedded live widgets.

Packages:

- `dev.vfyjxf.cloudlib.api.text` — document model (`RichText`, `RichNode` tree, actions, styles)
- `dev.vfyjxf.cloudlib.api.text.layout` — layout engine (`RichTextLayouter`, `RichTextMeasure` for taffy)
- `dev.vfyjxf.cloudlib.api.text.render` — rendering SPI (`RichTextRenderer`, `RenderOptions`, `CustomRenderer`)
- `dev.vfyjxf.cloudlib.text` — vanilla-backed implementations and `RichTextManager`
- `dev.vfyjxf.cloudlib.ui.widget.RichTextWidget` — the widget

## Building documents

A document is an immutable tree of `RichNode`s built with the fluent builder. Style
methods move a "pen" that applies to all subsequently added content; nodes never
own style directly — the builder wraps them in a `StyledNode`.

```java
RichText text = RichText.builder()
        .text("Crafts with ").color(0xFFAA00)
        .item(new ItemStack(Items.DIAMOND_PICKAXE), 16, true)   // count bar included
        .text(" and ")
        .block(Blocks.BEACON.defaultBlockState(), 18)
        .newline()
        .bold().text("Second line").clearStyle()
        .build();
```

Pen operations: `color(int|TextColor|ChatFormatting)`, `bold()`, `italic()`,
`underlined()`, `strikethrough()`, `obfuscated()`, `font(ResourceLocation)`,
`shadow(boolean)`, `highlight(int argb)` (background quad), `verticalAlign(...)` for
inline objects, `padding(Insets)` (extra space around the next objects),
`clearStyle()`, and `pushStyle()`/`popStyle()` for scoped styling.

Vanilla components embed as-is and keep their styles and events:

```java
RichText.builder()
        .component(Component.translatable("item.minecraft.diamond_sword").withStyle(ChatFormatting.AQUA))
        .build();
```

A purely textual document degrades back to vanilla with `toComponent()` (throws
`IllegalStateException` when it embeds non-textual nodes; check `isTextual()`).

## Localization

`TranslatableNode` resolves `%s` / `%n$s` / `%%` against the active language at
layout time. Arguments can be plain values, vanilla `Component`s, **or rich nodes**,
so images and items can be spliced into a translated sentence at the placeholder
position:

```java
RichText.builder()
        .translatable("mymod.recipe.hint", new ImageNode(texture, 9, 9), 42)
        .build();
```

Missing keys render the key itself, and unsatisfied placeholders render literally —
the same fallbacks as vanilla.

## Inline objects

| Node | Content |
|---|---|
| `ImageNode` | any `VisualTexture` at a declared size |
| `ItemNode` | `ItemStack`, optional count/durability decorations |
| `BlockNode` | `BlockState` as a GUI-isometric model |
| `EntityNode` | an entity from a `Supplier`, inventory-style; `followMouse` makes living entities track the cursor |
| `WidgetNode` | an embedded live widget (see below) |
| `CustomRenderNode` | a box painted by a `CustomRenderer` callback |
| `SpacerNode` | horizontal gap |
| `BreakNode` / `newline()` | explicit line break |

Entities are rendered through the inventory pipeline with full-bright lighting and
are clipped to their reserved box. The supplier is invoked at render time; create
the entity against the current client level.

## Interaction

Attach actions to the most recently added node:

```java
RichText.builder()
        .text("[+1]").color(ChatFormatting.GREEN)
        .onClickLast(ClickAction.run(() -> counter++))
        .onHoverLast(HoverAction.text(Component.literal("Increments the counter")))
        .build();
```

- `ClickAction`: `run`/`of` (callback with mouse context), `openUrl` (vanilla
  confirmation screen), `copyToClipboard`, `vanilla(ClickEvent)` — vanilla events
  follow screen semantics (`OPEN_URL`, `COPY_TO_CLIPBOARD`, `RUN_COMMAND`).
- `HoverAction`: `text(Component)`, `tooltip(Tooltip)`, `of` (callback),
  `vanilla(HoverEvent)` (`SHOW_TEXT` / `SHOW_ITEM` / `SHOW_ENTITY` are converted to
  tooltips). Vanilla events embedded in component styles are honored the same way.

## Layout and taffy integration

`RichTextLayouter` performs CSS-like inline layout in three phases — flatten (style
inheritance, translation splicing), greedy line breaking (per-codepoint fallback for
overlong words, trailing whitespace collapses at line ends), and assembly
(per-fragment `VerticalAlign`, line height from the tallest object, horizontal
`TextAlignment`). Results are `LaidOutText` — lines of `TextFragment`s with exact
boxes, source nodes and resolved actions, ready for rendering and hit-testing
(`fragmentAt` / `interactiveFragmentAt`).

`RichTextMeasure` is the taffy bridge (a `MeasureFunc`), with CSS sizing semantics:

- known width → wrap at exactly that width;
- `DEFINITE` → wrap, then report the widest resulting line (shrink-to-fit);
- `MAX_CONTENT` → longest natural line; `MIN_CONTENT` → longest unbreakable unit;
- known height always wins.

`layoutAt(width)` doubles as the render-time accessor and is cached per width.

## RichTextWidget

```java
var widget = RichTextWidget.of(richText)
        .setColor(0xFFFFFFFF)   // default color for unstyled runs
        .setShadow(false);      // default shadow
widget.useStyle(UIStyle.of(widthOf(220), textAlign(TextAlign.CENTER)));
```

The widget auto-measures through taffy (wrap when a width is imposed), follows the
`text-align` style property (or `setAlignment(...)`), and dispatches clicks/hover to
fragment actions. `WidgetNode` children become absolute-positioned child widgets
that track their fragment's box every layout pass.

`TextWidget` and `LabelWidget` keep their existing API but are backed by the same
pipeline — note they now wrap when the layout imposes a narrower width.

## Tooltips

`Tooltip.add(RichText)` (or `TooltipEntry.richText`) embeds rich text into any
tooltip. Purely textual documents degrade to a vanilla component line; rich content
renders through `RichTextTooltipComponent` (client factory registered by
`CloudLibClient`).

## Extending

- `GlyphMeasurer` / `TranslationResolver` decouple the engine from the vanilla font
  and language (the unit tests run on a fake measurer; `RichTextManager` wires the
  vanilla ones).
- `RichTextRenderer` can be replaced wholesale; `CustomRenderNode` covers one-off
  inline drawing.

## Limitations

- Word-level bidi/RTL shaping is not supported beyond what the vanilla font does per
  glyph run (same as vanilla tooltips).
- The active language is captured per layout; after a locale or resource-pack change,
  call `RichTextManager.invalidate()` and re-create measures (widgets recreate their
  measure on mount, so reopened screens pick up changes automatically).
- Text set on an already-mounted `TextWidget`/`LabelWidget`/`RichTextWidget`
  re-measures automatically; changing the `text-align` style marks the node dirty
  through the style change listener.
