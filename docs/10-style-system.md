# 10. Style System - 样式系统设计

## 目录

- [概述](#概述)
- [为什么需要样式系统](#为什么需要样式系统)
- [Style 类设计](#style-类设计)
- [样式属性分类](#样式属性分类)
- [Builder 模式](#builder-模式)
- [样式组合与继承](#样式组合与继承)
- [响应式样式](#响应式样式)
- [主题系统](#主题系统)
- [完整案例](#完整案例)
- [常见问题与解答](#常见问题与解答)

---

## 概述

**Style** 是描述 UI 元素视觉外观的数据类。它采用 **不可变设计** 和 **Builder 模式**。

```
┌─────────────────────────────────────────────────────────┐
│                      Style 系统                         │
│                                                         │
│  设计原则：                                             │
│  • 不可变（Immutable）- 安全、可缓存                   │
│  • Builder 模式 - 流畅的构建体验                       │
│  • 组合优于继承 - 样式可以组合                         │
│  • 类型安全 - 编译时检查                               │
│                                                         │
│  Style.builder()                                        │
│      .padding(16)                                       │
│      .background(0xFFFFFF)                              │
│      .borderRadius(8)                                   │
│      .build()                                           │
└─────────────────────────────────────────────────────────┘
```

---

## 为什么需要样式系统

### 问题：散乱的样式代码

```java
// 没有样式系统：样式散落在各处
TextWidget text = new TextWidget("Hello");
text.setFontSize(14);
text.setColor(0x333333);
text.setPadding(8);

BoxWidget box = new BoxWidget();
box.setBackground(0xFFFFFF);
box.setBorderRadius(8);
box.setPadding(16);
box.addChild(text);
```

**问题：**
1. 样式和逻辑混在一起
2. 难以复用
3. 难以统一修改
4. 没有类型约束

### 解决方案：集中的样式系统

```java
// 样式系统：集中定义，统一管理
Style textStyle = Style.builder()
    .fontSize(14)
    .color(0x333333)
    .padding(8)
    .build();

Style cardStyle = Style.builder()
    .background(0xFFFFFF)
    .borderRadius(8)
    .padding(16)
    .build();

// 使用
Box(cardStyle, () -> {
    Text("Hello", textStyle);
});
```

---

## Style 类设计

### 不可变 Record

```java
public record Style(
    // 尺寸
    OptionalInt width,
    OptionalInt height,
    OptionalInt minWidth,
    OptionalInt maxWidth,
    OptionalInt minHeight,
    OptionalInt maxHeight,
    
    // 间距
    Insets padding,
    Insets margin,
    
    // 背景
    OptionalInt backgroundColor,
    @Nullable ResourceLocation backgroundImage,
    
    // 边框
    OptionalInt borderColor,
    OptionalInt borderWidth,
    OptionalInt borderRadius,
    
    // 文本
    OptionalInt textColor,
    OptionalInt fontSize,
    @Nullable String fontWeight,
    @Nullable TextAlign textAlign,
    
    // 布局
    @Nullable Direction flexDirection,
    @Nullable Align alignItems,
    @Nullable Justify justifyContent,
    OptionalDouble flexGrow,
    
    // 其他
    boolean visible,
    float opacity
) {
    public static final Style EMPTY = new Style(...);  // 所有默认值
}
```

### 为什么用 Record？

1. **不可变性**：Record 天然不可变
2. **简洁**：自动生成 equals/hashCode/toString
3. **安全**：可以安全地在多处使用同一个 Style

---

## 样式属性分类

### 尺寸属性

```java
Style.builder()
    // 固定尺寸
    .width(200)
    .height(100)
    
    // 约束尺寸
    .minWidth(50)
    .maxWidth(300)
    .minHeight(30)
    .maxHeight(150)
    
    .build()
```

### 间距属性

```java
Style.builder()
    // 内边距
    .padding(16)                    // 四边相同
    .paddingHorizontal(8)          // 左右
    .paddingVertical(4)            // 上下
    .paddingTop(10)                // 单独设置
    .paddingRight(10)
    .paddingBottom(10)
    .paddingLeft(10)
    
    // 外边距
    .margin(8)
    .marginHorizontal(4)
    .marginVertical(2)
    
    .build()
```

### 背景属性

```java
Style.builder()
    // 纯色背景
    .background(0x4488FF)          // RGB
    .background(0x804488FF)        // ARGB (带透明度)
    
    // 图片背景
    .backgroundImage(new ResourceLocation("mod", "textures/bg.png"))
    
    .build()
```

### 边框属性

```java
Style.builder()
    // 边框
    .borderColor(0x888888)
    .borderWidth(1)
    .borderRadius(8)               // 圆角
    
    // 单独设置圆角
    .borderTopLeftRadius(8)
    .borderTopRightRadius(8)
    .borderBottomLeftRadius(0)
    .borderBottomRightRadius(0)
    
    .build()
```

### 文本属性

```java
Style.builder()
    // 颜色和大小
    .color(0x333333)               // 文字颜色
    .fontSize(14)
    
    // 字体样式
    .fontWeight("bold")            // normal, bold
    .fontStyle("italic")           // normal, italic
    
    // 对齐
    .textAlign(TextAlign.CENTER)   // LEFT, CENTER, RIGHT
    
    // 装饰
    .textDecoration("underline")   // none, underline, line-through
    
    .build()
```

### 布局属性

```java
Style.builder()
    // Flex 方向
    .flexDirection(Direction.ROW)  // ROW, COLUMN
    
    // 对齐
    .alignItems(Align.CENTER)      // START, CENTER, END, STRETCH
    .justifyContent(Justify.SPACE_BETWEEN)  // START, END, CENTER, SPACE_BETWEEN, SPACE_AROUND
    
    // 弹性
    .flexGrow(1)                   // 占据剩余空间的比例
    .flexShrink(0)                 // 收缩比例
    
    .build()
```

### 可见性属性

```java
Style.builder()
    .visible(false)                // 是否可见
    .opacity(0.5f)                 // 透明度 0.0 ~ 1.0
    .build()
```

---

## Builder 模式

### StyleBuilder 类

```java
public class StyleBuilder {
    private OptionalInt width = OptionalInt.empty();
    private OptionalInt height = OptionalInt.empty();
    private Insets padding = Insets.ZERO;
    // ... 其他属性
    
    public StyleBuilder width(int width) {
        this.width = OptionalInt.of(width);
        return this;
    }
    
    public StyleBuilder padding(int all) {
        this.padding = new Insets(all, all, all, all);
        return this;
    }
    
    public StyleBuilder padding(int vertical, int horizontal) {
        this.padding = new Insets(vertical, horizontal, vertical, horizontal);
        return this;
    }
    
    public Style build() {
        return new Style(
            width, height, /* ... */
        );
    }
}
```

### 使用方式

```java
// 创建 Builder
StyleBuilder builder = Style.builder();

// 链式调用
Style style = builder
    .width(200)
    .height(100)
    .padding(16)
    .background(0xFFFFFF)
    .borderRadius(8)
    .build();

// 一行创建
Style style = Style.builder()
    .padding(16)
    .background(0xFFFFFF)
    .build();
```

---

## 样式组合与继承

### 样式合并

```java
// 基础样式
Style baseCard = Style.builder()
    .padding(16)
    .background(0xFFFFFF)
    .borderRadius(8)
    .build();

// 扩展样式：继承 baseCard 并覆盖/添加属性
Style highlightedCard = baseCard.toBuilder()  // 从现有样式创建 Builder
    .borderWidth(2)
    .borderColor(0x4488FF)
    .build();

// highlightedCard 包含 baseCard 的所有属性，加上边框
```

### 样式覆盖优先级

```java
Style a = Style.builder().padding(8).background(0xFF0000).build();
Style b = Style.builder().padding(16).borderRadius(4).build();

// 合并：b 的属性覆盖 a 的属性
Style merged = Style.merge(a, b);
// 结果：padding=16, background=0xFF0000, borderRadius=4
```

### 预定义样式

```java
public class Styles {
    // 卡片样式
    public static final Style CARD = Style.builder()
        .padding(16)
        .background(0xFFFFFF)
        .borderRadius(8)
        .shadow(4)
        .build();
    
    // 按钮样式
    public static final Style BUTTON_PRIMARY = Style.builder()
        .padding(12, 24)
        .background(0x4488FF)
        .color(0xFFFFFF)
        .borderRadius(4)
        .build();
    
    public static final Style BUTTON_SECONDARY = Style.builder()
        .padding(12, 24)
        .background(0xE0E0E0)
        .color(0x333333)
        .borderRadius(4)
        .build();
    
    // 文本样式
    public static final Style TITLE = Style.builder()
        .fontSize(24)
        .fontWeight("bold")
        .color(0x333333)
        .build();
    
    public static final Style BODY = Style.builder()
        .fontSize(14)
        .color(0x666666)
        .lineHeight(1.5)
        .build();
}

// 使用
Box(Styles.CARD, () -> {
    Text("Title", Styles.TITLE);
    Text("Body content...", Styles.BODY);
    Button("Submit", onClick, Styles.BUTTON_PRIMARY);
});
```

---

## 响应式样式

### 动态样式

样式可以根据状态动态变化：

```java
Component button = Component.stateful(ctx -> {
    var isHovered = ctx.signal(false);
    var isPressed = ctx.signal(false);
    
    // 动态样式
    Supplier<Style> buttonStyle = () -> Style.builder()
        .padding(12, 24)
        .background(
            isPressed.get() ? 0x2266DD :
            isHovered.get() ? 0x5599FF :
            0x4488FF
        )
        .color(0xFFFFFF)
        .borderRadius(4)
        .build();
    
    return Box(
        buttonStyle.get(),
        onHover(isHovered::set),
        onPress(isPressed::set),
        Text("Click me")
    );
});
```

### 条件样式

```java
// 根据条件选择样式
Style getButtonStyle(boolean isDisabled, boolean isActive) {
    if (isDisabled) {
        return Style.builder()
            .background(0xCCCCCC)
            .color(0x888888)
            .build();
    } else if (isActive) {
        return Style.builder()
            .background(0x4488FF)
            .color(0xFFFFFF)
            .build();
    } else {
        return Style.builder()
            .background(0xE0E0E0)
            .color(0x333333)
            .build();
    }
}

// 使用
Text("Button", () -> getButtonStyle(
    isDisabled.get(), 
    isActive.get()
));
```

---

## 主题系统

### Theme 定义

```java
public record Theme(
    // 颜色
    int primaryColor,
    int secondaryColor,
    int backgroundColor,
    int surfaceColor,
    int textColor,
    int textSecondaryColor,
    int errorColor,
    
    // 间距
    int spacingSmall,
    int spacingMedium,
    int spacingLarge,
    
    // 圆角
    int radiusSmall,
    int radiusMedium,
    int radiusLarge,
    
    // 字体
    int fontSizeSmall,
    int fontSizeMedium,
    int fontSizeLarge
) {
    // 预定义主题
    public static final Theme LIGHT = new Theme(
        0x4488FF,   // primary
        0x66BB6A,   // secondary
        0xF5F5F5,   // background
        0xFFFFFF,   // surface
        0x333333,   // text
        0x666666,   // textSecondary
        0xF44336,   // error
        4, 8, 16,   // spacing
        4, 8, 16,   // radius
        12, 14, 18  // fontSize
    );
    
    public static final Theme DARK = new Theme(
        0x64B5F6,   // primary
        0x81C784,   // secondary
        0x121212,   // background
        0x1E1E1E,   // surface
        0xFFFFFF,   // text
        0xB0B0B0,   // textSecondary
        0xEF5350,   // error
        4, 8, 16,   // spacing
        4, 8, 16,   // radius
        12, 14, 18  // fontSize
    );
}
```

### 主题样式生成

```java
public class ThemedStyles {
    private final Theme theme;
    
    public ThemedStyles(Theme theme) {
        this.theme = theme;
    }
    
    public Style card() {
        return Style.builder()
            .padding(theme.spacingMedium())
            .background(theme.surfaceColor())
            .borderRadius(theme.radiusMedium())
            .build();
    }
    
    public Style primaryButton() {
        return Style.builder()
            .padding(theme.spacingSmall(), theme.spacingMedium())
            .background(theme.primaryColor())
            .color(0xFFFFFF)
            .borderRadius(theme.radiusSmall())
            .build();
    }
    
    public Style title() {
        return Style.builder()
            .fontSize(theme.fontSizeLarge())
            .fontWeight("bold")
            .color(theme.textColor())
            .build();
    }
    
    public Style body() {
        return Style.builder()
            .fontSize(theme.fontSizeMedium())
            .color(theme.textSecondaryColor())
            .build();
    }
}
```

### 主题切换

```java
Component themedApp = Component.stateful(ctx -> {
    // 主题状态
    var theme = ctx.signal(Theme.LIGHT);
    var styles = ctx.computed(() -> new ThemedStyles(theme.get()));
    
    // 注入主题
    ctx.inject(ThemeSignal.class, theme);
    
    return Column(() -> {
        // 主题切换按钮
        Row(() -> {
            Button("Light", () -> theme.set(Theme.LIGHT));
            Button("Dark", () -> theme.set(Theme.DARK));
        });
        
        Spacer(16);
        
        // 使用主题样式
        Box(styles.get().card(), () -> {
            Text("Card Title", styles.get().title());
            Text("Card content...", styles.get().body());
            Button("Action", onClick, styles.get().primaryButton());
        });
    });
});
```

---

## 完整案例

### 案例：可复用的 UI 组件库

```java
public class UIKit {
    private final ThemedStyles styles;
    
    public UIKit(Signal<Theme> theme) {
        // 响应式样式
        this.styles = new ThemedStyles(theme);
    }
    
    // === 按钮组件 ===
    
    public Component primaryButton(String text, Runnable onClick) {
        return button(text, onClick, styles.primaryButton());
    }
    
    public Component secondaryButton(String text, Runnable onClick) {
        return button(text, onClick, styles.secondaryButton());
    }
    
    public Component dangerButton(String text, Runnable onClick) {
        return button(text, onClick, styles.dangerButton());
    }
    
    private Component button(String text, Runnable onClick, Style style) {
        return Component.stateful(ctx -> {
            var isHovered = ctx.signal(false);
            var isPressed = ctx.signal(false);
            
            // 动态样式：悬停和按下时变化
            var dynamicStyle = ctx.computed(() -> {
                if (isPressed.get()) {
                    return style.toBuilder().opacity(0.8f).build();
                } else if (isHovered.get()) {
                    return style.toBuilder().opacity(0.9f).build();
                }
                return style;
            });
            
            return Box(
                dynamicStyle.get(),
                onHover(isHovered::set),
                onPress(isPressed::set),
                onClick(onClick),
                Text(text, Style.builder().color(style.textColor()).build())
            );
        });
    }
    
    // === 卡片组件 ===
    
    public Component card(Component content) {
        return Component.stateless(ctx -> 
            Box(styles.card(), content)
        );
    }
    
    public Component card(String title, Component content) {
        return Component.stateless(ctx -> 
            Box(styles.card(), () -> {
                Text(title, styles.title());
                Spacer(8);
                Child(content);
            })
        );
    }
    
    // === 输入组件 ===
    
    public Component textInput(Signal<String> value, String placeholder) {
        return Component.stateful(ctx -> {
            var isFocused = ctx.signal(false);
            
            var inputStyle = ctx.computed(() -> Style.builder()
                .padding(8, 12)
                .background(0xFFFFFF)
                .borderWidth(1)
                .borderColor(isFocused.get() ? styles.theme().primaryColor() : 0xCCCCCC)
                .borderRadius(4)
                .build()
            );
            
            return Box(
                inputStyle.get(),
                TextInput(value, placeholder, onFocus(isFocused::set))
            );
        });
    }
    
    // === 列表组件 ===
    
    public <T> Component list(
        Signal<List<T>> items,
        Function<T, Object> keyExtractor,
        Function<T, Component> itemRenderer
    ) {
        return Component.stateless(ctx -> 
            Box(
                styles.list(),
                RenderNode.forEach(items::get, keyExtractor, itemRenderer)
            )
        );
    }
}

// 使用
Component app = Component.stateful(ctx -> {
    var theme = ctx.signal(Theme.LIGHT);
    var ui = new UIKit(theme);
    
    var username = ctx.signal("");
    var password = ctx.signal("");
    
    return Column(16, () -> {
        Child(ui.card("Login", () -> {
            Column(8, () -> {
                Child(ui.textInput(username, "Username"));
                Child(ui.textInput(password, "Password"));
                
                Spacer(8);
                
                Row(8, () -> {
                    Child(ui.primaryButton("Login", () -> login()));
                    Child(ui.secondaryButton("Cancel", () -> cancel()));
                });
            });
        }));
    });
});
```

---

## 常见问题与解答

### Q1: 为什么 Style 是不可变的？

**A**: 不可变带来多个好处：
- **安全共享**：多个组件可以安全使用同一个 Style
- **可缓存**：相同的 Style 可以被缓存
- **易于调试**：Style 不会被意外修改
- **响应式友好**：新 Style = 新对象 = 触发更新

### Q2: 动态样式会影响性能吗？

**A**: 有一定影响，但可以优化：

```java
// ❌ 每帧都创建新 Style
Box(
    Style.builder().background(color.get()).build(),  // 每次渲染都 new
    content
);

// ✅ 使用 Computed 缓存
var style = ctx.computed(() -> 
    Style.builder().background(color.get()).build()
);
// 只在 color 变化时创建新 Style

Box(style.get(), content);
```

### Q3: 如何复用样式但覆盖部分属性？

**A**: 使用 toBuilder()：

```java
Style base = Style.builder()
    .padding(16)
    .background(0xFFFFFF)
    .borderRadius(8)
    .build();

// 基于 base 创建新样式
Style variant = base.toBuilder()
    .background(0xF5F5F5)  // 覆盖背景
    .borderWidth(1)        // 添加边框
    .build();
```

### Q4: Style 和 CSS 有什么区别？

**A**:
| 特性 | Style | CSS |
|------|-------|-----|
| 类型 | 强类型 | 字符串 |
| 继承 | 显式合并 | 级联继承 |
| 选择器 | 无 | 有 |
| 动态性 | 需要代码 | 有限（CSS 变量） |
| 性能 | 编译时检查 | 运行时解析 |

---

## 下一步

现在你已经理解了样式系统，接下来学习条件渲染和列表渲染的详细用法：

➡️ **[11-conditional-list.md](./11-conditional-list.md)** - 条件渲染与列表渲染

