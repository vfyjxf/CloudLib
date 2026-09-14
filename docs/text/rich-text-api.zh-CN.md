# 富文本 API

> [English](rich-text-api.md)

CloudLib 的富文本系统是一套与原版兼容的文档模型，自带排版引擎，并接入 taffy 的文本测量。它在原版 `Component` 之上提供：混合样式文本、可携带富参数的本地化、内联图片/物品/方块/实体、可交互（可点击/可悬停）片段，以及内嵌的活 widget。

包结构：

- `dev.vfyjxf.cloudlib.api.text` — 文档模型（`RichText`、`RichNode` 树、动作、样式）
- `dev.vfyjxf.cloudlib.api.text.layout` — 排版引擎（`RichTextLayouter`、对接 taffy 的 `RichTextMeasure`）
- `dev.vfyjxf.cloudlib.api.text.render` — 渲染 SPI（`RichTextRenderer`、`RenderOptions`、`CustomRenderer`）
- `dev.vfyjxf.cloudlib.text` — 基于原版的实现与 `RichTextManager`
- `dev.vfyjxf.cloudlib.ui.widget.RichTextWidget` — widget

## 编写文档

文档是不可变的 `RichNode` 树，用流式 builder 构建。样式方法移动一支"笔"，作用于之后添加的所有内容；节点本身不持有样式——builder 会把它们包进 `StyledNode`。

```java
RichText text = RichText.builder()
        .text("合成材料 ").color(0xFFAA00)
        .item(new ItemStack(Items.DIAMOND_PICKAXE), 16, true)   // 带数量栏
        .text(" 和 ")
        .block(Blocks.BEACON.defaultBlockState(), 18)
        .newline()
        .bold().text("第二行").clearStyle()
        .build();
```

笔的操作：`color(int|TextColor|ChatFormatting)`、`bold()`、`italic()`、
`underlined()`、`strikethrough()`、`obfuscated()`、`font(ResourceLocation)`、
`shadow(boolean)`、`highlight(int argb)`（背景色块）、`verticalAlign(...)`（内联对象的垂直对齐）、
`padding(Insets)`（给随后的对象加留白）、`clearStyle()`，以及用于局部样式的
`pushStyle()`/`popStyle()`。

原版 component 可以原样嵌入，保留其样式与事件：

```java
RichText.builder()
        .component(Component.translatable("item.minecraft.diamond_sword").withStyle(ChatFormatting.AQUA))
        .build();
```

纯文本文档可以用 `toComponent()` 退化回原版 component（嵌入了非文本节点时抛
`IllegalStateException`；用 `isTextual()` 判断）。

## 本地化

`TranslatableNode` 在排版时按当前语言解析 `%s` / `%n$s` / `%%`。参数可以是普通值、原版 `Component`，**也可以是富节点**，因此图片和物品可以被插入到译文占位符的位置：

```java
RichText.builder()
        .translatable("mymod.recipe.hint", new ImageNode(texture, 9, 9), 42)
        .build();
```

缺失的 key 渲染为 key 本身，未满足的占位符按字面量输出——与原版一致的回退行为。

## 内联对象

| 节点 | 内容 |
|---|---|
| `ImageNode` | 任意 `VisualTexture`，按声明尺寸 |
| `ItemNode` | `ItemStack`，可选数量/耐久角标 |
| `BlockNode` | `BlockState`，GUI 等距模型 |
| `EntityNode` | 由 `Supplier` 提供的实体，物品栏式渲染；`followMouse` 让生物跟随鼠标 |
| `WidgetNode` | 内嵌的活 widget（见下文） |
| `CustomRenderNode` | 由 `CustomRenderer` 回调绘制的盒子 |
| `SpacerNode` | 水平间隔 |
| `BreakNode` / `newline()` | 显式换行 |

实体走物品栏渲染管线（全亮光照），并裁剪到预留盒内。supplier 在渲染时调用；请基于当前客户端 level 创建实体。

## 交互

给最近添加的节点附加动作：

```java
RichText.builder()
        .text("[+1]").color(ChatFormatting.GREEN)
        .onClickLast(ClickAction.run(() -> counter++))
        .onHoverLast(HoverAction.text(Component.literal("增加计数器")))
        .build();
```

- `ClickAction`：`run`/`of`（带鼠标上下文的回调）、`openUrl`（原版确认屏）、
  `copyToClipboard`、`vanilla(ClickEvent)`——vanilla 事件按屏幕语义处理
  （`OPEN_URL`、`COPY_TO_CLIPBOARD`、`RUN_COMMAND`）。
- `HoverAction`：`text(Component)`、`tooltip(Tooltip)`、`of`（回调）、
  `vanilla(HoverEvent)`（`SHOW_TEXT` / `SHOW_ITEM` / `SHOW_ENTITY` 会转成 tooltip）。
  嵌在 component 样式里的 vanilla 事件同样生效。

## 排版与 taffy 集成

`RichTextLayouter` 按 CSS 内联布局分三个阶段——展平（样式继承、译文拼接）、贪心断行（超长词按 codepoint 回退断词，行尾空白折叠）、装配（逐片段 `VerticalAlign`、行高取最高对象、水平 `TextAlignment`）。结果是 `LaidOutText`：若干行 `TextFragment`，带精确包围盒、来源节点和解析后的动作，可直接用于渲染与命中测试（`fragmentAt` / `interactiveFragmentAt`）。

`RichTextMeasure` 是 taffy 桥（`MeasureFunc`），语义遵循 CSS：

- 已知宽度 → 恰好按该宽度换行；
- `DEFINITE` → 换行后回报最宽行（收缩适应）；
- `MAX_CONTENT` → 最长自然行；`MIN_CONTENT` → 最长不可断单元；
- 已知高度优先于排版高度。

`layoutAt(width)` 同时是渲染期入口，按宽度缓存。

## RichTextWidget

```java
var widget = RichTextWidget.of(richText)
        .setColor(0xFFFFFFFF)   // 无样式 run 的默认颜色
        .setShadow(false);      // 默认阴影
widget.useStyle(UIStyle.of(widthOf(220), textAlign(TextAlign.CENTER)));
```

widget 通过 taffy 自动测量（被施加宽度时换行），跟随 `text-align` 样式属性（或 `setAlignment(...)` 覆盖），并把点击/悬停分发到片段动作。`WidgetNode` 的子 widget 会成为绝对定位的子节点，每次布局都跟踪其片段盒。

`TextWidget` 与 `LabelWidget` 保持原有 API，但内部同样走这条管线——注意它们现在在布局施加更窄宽度时会换行。

## Tooltip

`Tooltip.add(RichText)`（或 `TooltipEntry.richText`）可以把富文本嵌入任何 tooltip。纯文本文档退化为原版 component 行；富内容通过 `RichTextTooltipComponent` 渲染（客户端工厂由 `CloudLibClient` 注册）。

## 扩展

- `GlyphMeasurer` / `TranslationResolver` 把引擎与原版字体、语言解耦（单元测试跑在假测量器上；`RichTextManager` 接入原版实现）。
- `RichTextRenderer` 可整体替换；`CustomRenderNode` 用于一次性的内联绘制。

## 限制

- 不支持超出原版字体逐段能力的词级 bidi/RTL 塑形（与原版 tooltip 一致）。
- 当前语言按排版时捕获；切换语言或资源包后调用 `RichTextManager.invalidate()` 并重建 measure（widget 在挂载时重建 measure，重开界面即生效）。
- 对已挂载的 `TextWidget`/`LabelWidget`/`RichTextWidget` 重设文本会自动重新测量；修改 `text-align` 样式会通过样式变更监听器标脏。
