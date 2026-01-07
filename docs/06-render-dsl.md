# 06. Render DSL - 渲染领域特定语言

## 目录

- [概述](#概述)
- [为什么需要 DSL](#为什么需要-dsl)
- [DSL 设计原则](#dsl-设计原则)
- [Render 类详解](#render-类详解)
- [布局 DSL](#布局-dsl)
- [条件与列表 DSL](#条件与列表-dsl)
- [样式 DSL](#样式-dsl)
- [DSL 实现原理](#dsl-实现原理)
- [完整案例](#完整案例)
- [常见问题与解答](#常见问题与解答)

---

## 概述

**Render DSL** 是一套用于声明式描述 UI 结构的 API。它让 Java 代码看起来像是在描述 UI，而不是在构建对象。

### 对比：传统方式 vs DSL

```java
// 传统方式：创建对象、设置属性
VBox vbox = new VBox();
vbox.setPadding(new Insets(16));
Label label = new Label("Hello");
label.setStyle("-fx-font-size: 18");
Button button = new Button("Click");
button.setOnAction(e -> handleClick());
vbox.getChildren().addAll(label, button);
panel.add(vbox);

// DSL 方式：声明式描述
Column(() -> {
    Text("Hello", Style.builder().fontSize(18).build());
    Button("Click", () -> handleClick());
});
```

DSL 更像在**描述**你想要什么，而不是**命令**如何构建。

---

## 为什么需要 DSL

### 问题 1：嵌套地狱

```java
// 没有 DSL：深度嵌套的构造调用
new VBox(
    new HBox(
        new VBox(
            new Label("Title"),
            new Label("Subtitle")
        ),
        new Button("Action")
    ),
    new ScrollPane(
        new VBox(
            // ... 更多嵌套
        )
    )
);
```

### 问题 2：可读性差

```java
// 很难看出 UI 结构
ContainerWidget container = new ContainerWidget();
TextWidget title = new TextWidget("Title");
title.setStyle(titleStyle);
container.addChild(title);
TextWidget subtitle = new TextWidget("Subtitle");
subtitle.setStyle(subtitleStyle);
container.addChild(subtitle);
```

### 解决方案：DSL

```java
// DSL：结构一目了然
Column(() -> {
    Text("Title", titleStyle);
    Text("Subtitle", subtitleStyle);
});

// 嵌套也很清晰
Column(() -> {
    Row(() -> {
        Column(() -> {
            Text("Title");
            Text("Subtitle");
        });
        Button("Action", onClick);
    });
    ScrollView(() -> {
        // ... 内容
    });
});
```

---

## DSL 设计原则

### 原则 1：声明式

```java
// ❌ 命令式：描述过程
Widget widget = new Widget();
widget.setText("Hello");
widget.setColor(RED);
parent.add(widget);

// ✅ 声明式：描述结果
Text("Hello", Style.builder().color(RED).build());
```

### 原则 2：组合式

```java
// 小的 DSL 可以组合成大的
Column(() -> {
    Header();      // 可复用的 Header DSL
    Content();     // 可复用的 Content DSL
    Footer();      // 可复用的 Footer DSL
});
```

### 原则 3：响应式友好

```java
// DSL 自然支持动态内容
Text(() -> "Count: " + count.get());  // 响应式文本
RenderNode.showWhen(
    () -> isVisible.get(),            // 响应式条件
    content
);
```

### 原则 4：类型安全

```java
// 编译时检查
Column(() -> {
    Text("Hello");           // ✅ 编译通过
    Button("Click", onClick); // ✅ 编译通过
    // Text(123);             // ❌ 编译错误：类型不匹配
});
```

---

## Render 类详解

`Render` 类是 DSL 的入口点，提供创建各种 RenderNode 的静态方法。

### 文本节点

```java
// 静态文本
Render.text("Hello, World!")

// 动态文本（响应式）
Render.text(() -> "Count: " + count.get())

// 带样式的文本
Render.text("Styled", Style.builder().color(0xFF0000).fontSize(16).build())

// 动态文本 + 样式
Render.text(
    () -> user.get().name(),
    Style.builder().fontWeight("bold").build()
)
```

### 按钮节点

```java
// 基础按钮
Render.button("Click Me", () -> handleClick())

// 动态文本按钮
Render.button(() -> isLoading.get() ? "Loading..." : "Submit", onSubmit)

// 带样式的按钮
Render.button(
    "Danger", 
    onDelete,
    Style.builder().background(0xFF0000).color(0xFFFFFF).build()
)
```

### 图片节点

```java
// 静态图片
Render.image(new ResourceLocation("modid", "textures/icon.png"))

// 动态图片
Render.image(() -> user.get().avatarTexture())

// 带尺寸
Render.image(texture, 64, 64)
```

### 空节点

```java
// 占位符，不渲染任何内容
Render.empty()

// 常用于条件渲染的 else 分支
RenderNode.conditional(
    condition,
    content,
    Render.empty()  // 条件不满足时什么都不显示
)
```

### 组件引用

```java
// 引用其他组件
Render.component(headerComponent)

// 带 key 的组件引用（用于列表）
Render.component(itemComponent, "item-" + item.id())
```

---

## 布局 DSL

布局 DSL 用于组织子元素的排列方式。

### Column - 垂直布局

```java
// 基础垂直布局
Column(() -> {
    Text("Line 1");
    Text("Line 2");
    Text("Line 3");
});

// 带间距
Column(8, () -> {  // 8px 间距
    Text("Line 1");
    Text("Line 2");
});

// 带样式
Column(
    Style.builder().padding(16).background(0xF5F5F5).build(),
    () -> {
        Text("Styled Column");
    }
);
```

### Row - 水平布局

```java
// 基础水平布局
Row(() -> {
    Text("Left");
    Text("Center");
    Text("Right");
});

// 带间距
Row(4, () -> {
    Icon(iconA);
    Icon(iconB);
    Icon(iconC);
});

// 嵌套布局
Row(() -> {
    Column(() -> {
        Text("Title");
        Text("Subtitle");
    });
    Spacer();
    Button("Action", onClick);
});
```

### Box - 通用容器

```java
// 带样式的容器
Box(
    Style.builder()
        .padding(16)
        .margin(8)
        .background(0xFFFFFF)
        .borderRadius(8)
        .shadow(4)
        .build(),
    () -> {
        Text("Content in a box");
    }
);

// 固定尺寸
Box(
    Style.builder().width(200).height(100).build(),
    content
);
```

### Spacer - 间隔

```java
// 固定间隔
Row(() -> {
    Text("Left");
    Spacer(16);  // 16px 间隔
    Text("Right");
});

// 弹性间隔（占据剩余空间）
Row(() -> {
    Text("Left");
    Expanded();  // 占据剩余空间
    Text("Right");
});
```

### ScrollView - 滚动容器

```java
// 垂直滚动
ScrollView(() -> {
    Column(() -> {
        // 很多内容...
        for (int i = 0; i < 100; i++) {
            Text("Item " + i);
        }
    });
});

// 水平滚动
ScrollView(ScrollDirection.HORIZONTAL, () -> {
    Row(() -> {
        // 很多内容...
    });
});
```

---

## 条件与列表 DSL

### 条件渲染

```java
// showWhen - 条件显示
RenderNode.showWhen(
    () -> isLoggedIn.get(),
    logoutButton
);

// conditional - if-else
RenderNode.conditional(
    () -> isLoading.get(),
    Render.text("Loading..."),    // true 分支
    actualContent                  // false 分支
);

// 嵌套条件
RenderNode.conditional(
    () -> status.get() == Status.LOADING,
    loadingView,
    RenderNode.conditional(
        () -> status.get() == Status.ERROR,
        errorView,
        successView
    )
);
```

### 列表渲染

```java
// forEach - 带 key 的列表
RenderNode.forEach(
    () -> items.get(),           // 数据源
    Item::id,                     // key 提取器
    item -> itemCard(item)        // 项目渲染器
);

// indexed - 带索引的列表（无 key）
RenderNode.indexed(
    () -> tabs.get(),
    (tab, index) -> tabButton(tab, index)
);
```

### 带 Key 列表的重要性

```java
// ❌ 没有 key：重新排序时会重建所有元素
Column(() -> {
    for (var item : items.get()) {
        Child(itemCard(item));  // 每个都是新的
    }
});

// ✅ 有 key：重新排序时复用元素
RenderNode.forEach(
    items::get,
    Item::id,  // key
    item -> itemCard(item)
);
```

---

## 样式 DSL

样式通过 `Style.builder()` 创建：

### 尺寸

```java
Style.builder()
    .width(100)
    .height(50)
    .minWidth(80)
    .maxWidth(200)
    .build()
```

### 间距

```java
Style.builder()
    .padding(16)                    // 四边相同
    .paddingHorizontal(8)          // 左右
    .paddingVertical(4)            // 上下
    .margin(8)
    .marginTop(16)
    .build()
```

### 背景和边框

```java
Style.builder()
    .background(0x4488FF)           // 背景色
    .borderColor(0x888888)          // 边框色
    .borderWidth(1)                 // 边框宽度
    .borderRadius(8)                // 圆角
    .build()
```

### 文本样式

```java
Style.builder()
    .color(0x333333)                // 文字颜色
    .fontSize(14)                   // 字体大小
    .fontWeight("bold")             // 粗细
    .textAlign(TextAlign.CENTER)    // 对齐
    .build()
```

### 布局

```java
Style.builder()
    .alignItems(Align.CENTER)       // 交叉轴对齐
    .justifyContent(Justify.SPACE_BETWEEN)  // 主轴对齐
    .flexDirection(Direction.ROW)   // 方向
    .flexGrow(1)                    // 弹性增长
    .build()
```

### 样式组合

```java
// 预定义样式
Style cardStyle = Style.builder()
    .padding(16)
    .background(0xFFFFFF)
    .borderRadius(8)
    .shadow(4)
    .build();

Style titleStyle = Style.builder()
    .fontSize(18)
    .fontWeight("bold")
    .color(0x333333)
    .build();

// 使用
Box(cardStyle, () -> {
    Text("Title", titleStyle);
    Text("Content");
});
```

---

## DSL 实现原理

### 上下文栈

DSL 使用上下文栈来追踪当前正在构建的容器：

```java
// 伪代码
class RenderContext {
    static Stack<ContainerNode> containerStack = new Stack<>();
    
    static void Column(Runnable children) {
        var column = new ColumnNode();
        
        // 将 column 添加到当前容器
        if (!containerStack.isEmpty()) {
            containerStack.peek().addChild(column);
        }
        
        // column 成为新的当前容器
        containerStack.push(column);
        
        try {
            children.run();  // 执行子元素构建
        } finally {
            containerStack.pop();  // 恢复
        }
    }
    
    static void Text(String text) {
        var textNode = new TextNode(text);
        containerStack.peek().addChild(textNode);
    }
}
```

### 执行流程

```
Column(() -> {           // 1. 创建 ColumnNode，压栈
    Text("A");           // 2. 创建 TextNode("A")，添加到栈顶（Column）
    Row(() -> {          // 3. 创建 RowNode，添加到 Column，压栈
        Text("B");       // 4. 创建 TextNode("B")，添加到 Row
        Text("C");       // 5. 创建 TextNode("C")，添加到 Row
    });                  // 6. Row 弹栈
    Text("D");           // 7. 创建 TextNode("D")，添加到 Column
});                      // 8. Column 弹栈

结果：
Column
├── Text("A")
├── Row
│   ├── Text("B")
│   └── Text("C")
└── Text("D")
```

### 响应式 DSL

对于响应式内容，DSL 使用 Supplier 延迟计算：

```java
// Text 的响应式版本
static RenderNode text(Supplier<String> textSupplier) {
    return new DynamicTextNode(textSupplier);
}

// DynamicTextNode 在渲染时调用 supplier
class DynamicTextNode {
    private Supplier<String> textSupplier;
    
    String getText() {
        return Tracker.track(this, textSupplier);
        // 追踪依赖，当依赖变化时，这个节点会更新
    }
}
```

---

## 完整案例

### 案例 1：用户卡片

```java
Component userCard(Signal<User> user) {
    return Component.stateless(ctx -> 
        Box(
            Style.builder()
                .padding(16)
                .background(0xFFFFFF)
                .borderRadius(8)
                .shadow(2)
                .build(),
            Column(8, () -> {
                // 头部
                Row(12, () -> {
                    // 头像
                    Box(
                        Style.builder()
                            .width(48)
                            .height(48)
                            .borderRadius(24)
                            .background(0xE0E0E0)
                            .build(),
                        Render.image(() -> user.get().avatar())
                    );
                    
                    // 信息
                    Column(4, () -> {
                        Text(
                            () -> user.get().name(),
                            Style.builder()
                                .fontSize(16)
                                .fontWeight("bold")
                                .build()
                        );
                        Text(
                            () -> user.get().email(),
                            Style.builder()
                                .fontSize(12)
                                .color(0x666666)
                                .build()
                        );
                    });
                });
                
                // 分隔线
                Box(Style.builder()
                    .height(1)
                    .background(0xE0E0E0)
                    .build());
                
                // 操作按钮
                Row(8, () -> {
                    Button("Message", () -> openChat(user.peek()));
                    Button("Profile", () -> viewProfile(user.peek()));
                });
            })
        )
    );
}
```

### 案例 2：导航菜单

```java
Component navMenu(Signal<String> currentPage, List<NavItem> items) {
    return Component.stateless(ctx -> 
        Column(() -> {
            // Logo
            Box(
                Style.builder().padding(16).build(),
                Render.image(logoTexture, 120, 40)
            );
            
            // 菜单项
            RenderNode.indexed(
                () -> items,
                (item, i) -> navMenuItem(item, currentPage)
            );
            
            // 底部间隔
            Expanded();
            
            // 用户信息
            Box(
                Style.builder()
                    .padding(12)
                    .borderTop(1, 0xE0E0E0)
                    .build(),
                userMiniCard(ctx.provide(UserSignal.class))
            );
        })
    );
}

Component navMenuItem(NavItem item, Signal<String> currentPage) {
    return Component.stateless(ctx -> {
        var isActive = () -> currentPage.get().equals(item.id());
        
        return Box(
            Style.builder()
                .padding(12)
                .background(isActive.get() ? 0x4488FF20 : 0x00000000)
                .borderRadius(4)
                .cursor("pointer")
                .build(),
            Row(8, () -> {
                Render.image(item.icon(), 20, 20);
                Text(
                    item.label(),
                    Style.builder()
                        .color(isActive.get() ? 0x4488FF : 0x333333)
                        .fontWeight(isActive.get() ? "bold" : "normal")
                        .build()
                );
            }),
            onClick(() -> currentPage.set(item.id()))
        );
    });
}

record NavItem(String id, String label, ResourceLocation icon) {}
```

### 案例 3：数据表格

```java
Component dataTable(
    Signal<List<DataRow>> rows,
    List<Column> columns
) {
    return Component.stateless(ctx -> 
        Column(() -> {
            // 表头
            Row(
                Style.builder()
                    .background(0xF5F5F5)
                    .borderBottom(1, 0xE0E0E0)
                    .build(),
                () -> {
                    for (var col : columns) {
                        Box(
                            Style.builder()
                                .width(col.width())
                                .padding(8)
                                .build(),
                            Text(
                                col.header(),
                                Style.builder()
                                    .fontWeight("bold")
                                    .build()
                            )
                        );
                    }
                }
            );
            
            // 数据行
            ScrollView(() -> {
                RenderNode.forEach(
                    rows::get,
                    DataRow::id,
                    row -> tableRow(row, columns)
                );
            });
        })
    );
}

Component tableRow(DataRow row, List<Column> columns) {
    return Component.stateful(ctx -> {
        var isHovered = ctx.signal(false);
        
        return Row(
            Style.builder()
                .background(isHovered.get() ? 0xF0F0F0 : 0xFFFFFF)
                .borderBottom(1, 0xE8E8E8)
                .build(),
            onHover(h -> isHovered.set(h)),
            () -> {
                for (var col : columns) {
                    Box(
                        Style.builder()
                            .width(col.width())
                            .padding(8)
                            .build(),
                        Text(row.getValue(col.key()))
                    );
                }
            }
        );
    });
}
```

---

## 常见问题与解答

### Q1: DSL 和直接创建 RenderNode 有什么区别？

**A**: 功能上等价，DSL 更易读：

```java
// 直接创建（更底层）
new ColumnNode(List.of(
    new TextNode("A"),
    new TextNode("B")
));

// DSL（更易读）
Column(() -> {
    Text("A");
    Text("B");
});
```

### Q2: 可以在 DSL 中使用循环吗？

**A**: 可以，但要注意响应式更新：

```java
// ✅ 静态循环（数量固定）
Column(() -> {
    for (int i = 0; i < 5; i++) {
        Text("Item " + i);
    }
});

// ⚠️ 动态循环（使用 forEach 更好）
Column(() -> {
    for (var item : items.get()) {  // items 变化时需要重建整个 Column
        Child(itemCard(item));
    }
});

// ✅ 响应式列表
RenderNode.forEach(
    items::get,      // 响应式数据源
    Item::id,        // key
    item -> itemCard(item)
);
```

### Q3: DSL 中如何处理空值？

**A**: 使用条件渲染或默认值：

```java
// 方式 1：条件渲染
Column(() -> {
    RenderNode.showWhen(
        () -> user.get() != null,
        userCard(user)
    );
});

// 方式 2：默认值
Text(() -> {
    var u = user.get();
    return u != null ? u.name() : "Anonymous";
});
```

### Q4: 样式可以动态变化吗？

**A**: 可以，通过响应式 Style：

```java
// 动态样式
Box(
    Style.builder()
        .background(isActive.get() ? 0x4488FF : 0xCCCCCC)
        .build(),
    content
);

// 或者使用条件
RenderNode.conditional(
    isActive::get,
    Box(activeStyle, content),
    Box(inactiveStyle, content)
);
```

---

## 下一步

现在你已经掌握了 Render DSL，接下来了解 RenderNode 的内部类型：

➡️ **[07-render-node.md](./07-render-node.md)** - RenderNode 类型系统

