# 07. RenderNode - 虚拟节点类型系统

## 目录

- [概述](#概述)
- [为什么需要 RenderNode](#为什么需要-rendernode)
- [RenderNode 类型层次](#rendernode-类型层次)
- [基础节点类型](#基础节点类型)
- [容器节点类型](#容器节点类型)
- [特殊节点类型](#特殊节点类型)
- [节点创建与遍历](#节点创建与遍历)
- [与 Element 的关系](#与-element-的关系)
- [常见问题与解答](#常见问题与解答)

---

## 概述

**RenderNode** 是 UI 结构的描述对象，类似于 React 的 Virtual DOM 或 Flutter 的 Widget。

```
┌─────────────────────────────────────────────────────────┐
│                     RenderNode                          │
│                                                         │
│  "我是 UI 的蓝图，描述你想要什么样的界面"               │
│                                                         │
│  特点：                                                 │
│  • 不可变（Immutable）                                 │
│  • 轻量级                                              │
│  • 可以快速创建和比较                                  │
│  • 与实际 UI 元素分离                                  │
└─────────────────────────────────────────────────────────┘
```

### 核心概念

| 概念 | 类比 | 说明 |
|------|------|------|
| **RenderNode** | 建筑蓝图 | 描述结构，不是实际建筑 |
| **Element** | 实际建筑 | 根据蓝图建造的真实 UI |
| **Component** | 建筑设计师 | 产出蓝图的函数 |

---

## 为什么需要 RenderNode

### 问题：直接操作 UI 元素

```java
// 每次状态变化都直接操作 UI
void updateUI(List<Item> items) {
    container.clear();
    for (Item item : items) {
        Widget widget = new ItemWidget(item);
        container.add(widget);  // 创建大量新对象
    }
}
```

**问题：**
1. 每次更新都创建新对象，效率低
2. 丢失状态（输入焦点、滚动位置等）
3. 难以做差异更新

### 解决方案：虚拟节点 + 差异对比

```java
// 1. 创建 RenderNode（轻量描述）
RenderNode newTree = Column(() -> {
    for (Item item : items) {
        Child(itemWidget(item));
    }
});

// 2. 对比旧树和新树
Diff diff = compare(oldTree, newTree);

// 3. 只更新变化的部分
apply(diff);  // 复用未变化的 Element
```

**优势：**
1. RenderNode 创建很快（只是数据结构）
2. 差异对比后只更新变化的部分
3. 复用 Element，保持状态

---

## RenderNode 类型层次

```
RenderNode (sealed interface)
│
├── EmptyNode              - 空节点
│
├── LeafNode (sealed)      - 叶子节点（无子节点）
│   ├── TextNode           - 文本
│   ├── ImageNode          - 图片
│   └── CustomNode         - 自定义渲染
│
├── ContainerNode (sealed) - 容器节点（有子节点）
│   ├── ColumnNode         - 垂直布局
│   ├── RowNode            - 水平布局
│   ├── BoxNode            - 通用容器
│   ├── ScrollNode         - 滚动容器
│   └── StackNode          - 层叠布局
│
├── ComponentRef           - 组件引用
│
└── StructuralNode (sealed) - 结构控制节点
    ├── ConditionalNode    - 条件渲染
    ├── ForEachNode        - 列表渲染
    └── ProviderNode       - 上下文提供
```

### 为什么用 Sealed Interface？

```java
public sealed interface RenderNode permits 
    EmptyNode, LeafNode, ContainerNode, ComponentRef, StructuralNode {
}
```

**优势：**
1. **穷尽性检查**：switch 必须处理所有情况
2. **类型安全**：不能随意添加新实现
3. **优化友好**：编译器知道所有可能类型

```java
// 编译器确保处理所有类型
String describe(RenderNode node) {
    return switch (node) {
        case EmptyNode e -> "empty";
        case LeafNode l -> "leaf";
        case ContainerNode c -> "container";
        case ComponentRef r -> "component";
        case StructuralNode s -> "structural";
        // 不需要 default，编译器知道已经穷尽
    };
}
```

---

## 基础节点类型

### EmptyNode - 空节点

```java
public record EmptyNode() implements RenderNode {
    public static final EmptyNode INSTANCE = new EmptyNode();
}
```

**用途：**
- 占位符
- 条件渲染的空分支
- 组件不需要渲染内容时

```java
// 使用示例
Component maybeContent = Component.stateless(ctx -> {
    if (shouldShow()) {
        return Render.text("Content");
    }
    return Render.empty();  // 返回 EmptyNode
});
```

### TextNode - 文本节点

```java
public record TextNode(
    Supplier<String> textSupplier,
    Style style
) implements LeafNode {
    
    // 静态文本便捷构造
    public static TextNode of(String text) {
        return new TextNode(() -> text, Style.EMPTY);
    }
    
    // 动态文本
    public static TextNode dynamic(Supplier<String> supplier) {
        return new TextNode(supplier, Style.EMPTY);
    }
}
```

**特点：**
- `textSupplier` 支持响应式文本
- `style` 控制文本样式

```java
// 静态文本
TextNode.of("Hello")

// 动态文本
TextNode.dynamic(() -> "Count: " + count.get())

// 带样式
new TextNode(
    () -> "Styled",
    Style.builder().color(0xFF0000).fontSize(16).build()
)
```

### ImageNode - 图片节点

```java
public record ImageNode(
    Supplier<ResourceLocation> textureSupplier,
    int width,
    int height,
    Style style
) implements LeafNode {
}
```

**用途：**
- 静态图标
- 动态头像
- 背景图片

```java
// 静态图片
new ImageNode(
    () -> new ResourceLocation("mod", "textures/icon.png"),
    32, 32,
    Style.EMPTY
)

// 动态图片
new ImageNode(
    () -> user.get().avatar(),
    64, 64,
    Style.EMPTY
)
```

### ButtonNode - 按钮节点

```java
public record ButtonNode(
    Supplier<String> labelSupplier,
    Runnable onClick,
    Style style
) implements LeafNode {
}
```

---

## 容器节点类型

### ColumnNode - 垂直布局

```java
public record ColumnNode(
    List<RenderNode> children,
    int spacing,
    Style style
) implements ContainerNode {
}
```

**行为：**
- 子元素从上到下排列
- spacing 控制子元素间距
- 默认占据父容器宽度

```java
// 创建
new ColumnNode(
    List.of(
        TextNode.of("A"),
        TextNode.of("B"),
        TextNode.of("C")
    ),
    8,  // 8px 间距
    Style.EMPTY
)

// 渲染结果
// ┌──────────┐
// │    A     │
// │    B     │
// │    C     │
// └──────────┘
```

### RowNode - 水平布局

```java
public record RowNode(
    List<RenderNode> children,
    int spacing,
    Style style
) implements ContainerNode {
}
```

**行为：**
- 子元素从左到右排列
- spacing 控制子元素间距
- 默认占据父容器高度

```java
// 创建
new RowNode(
    List.of(
        TextNode.of("Left"),
        TextNode.of("Center"),
        TextNode.of("Right")
    ),
    4,
    Style.EMPTY
)

// 渲染结果
// ┌──────────────────────┐
// │ Left  Center  Right  │
// └──────────────────────┘
```

### BoxNode - 通用容器

```java
public record BoxNode(
    RenderNode child,
    Style style
) implements ContainerNode {
    
    @Override
    public List<RenderNode> children() {
        return List.of(child);
    }
}
```

**用途：**
- 应用样式（边距、背景等）
- 约束尺寸
- 单子容器

```java
// 卡片容器
new BoxNode(
    content,
    Style.builder()
        .padding(16)
        .background(0xFFFFFF)
        .borderRadius(8)
        .shadow(4)
        .build()
)
```

### ScrollNode - 滚动容器

```java
public record ScrollNode(
    RenderNode child,
    ScrollDirection direction,
    Style style
) implements ContainerNode {
}

public enum ScrollDirection {
    VERTICAL,
    HORIZONTAL,
    BOTH
}
```

### StackNode - 层叠布局

```java
public record StackNode(
    List<RenderNode> children,
    Style style
) implements ContainerNode {
}
```

**行为：**
- 子元素重叠在同一位置
- 后面的子元素覆盖前面的

```java
// 创建
new StackNode(
    List.of(
        backgroundImage,
        overlayGradient,
        foregroundText
    ),
    Style.EMPTY
)

// 渲染结果（从下到上）
// ┌──────────────────┐
// │   background     │ ← 底层
// │   +overlay       │ ← 中层
// │   +text          │ ← 顶层
// └──────────────────┘
```

---

## 特殊节点类型

### ComponentRef - 组件引用

```java
public record ComponentRef(
    Component component,
    @Nullable String key
) implements RenderNode {
}
```

**关键属性：**
- `component`: 要渲染的组件
- `key`: 用于列表渲染的标识（可选）

```java
// 无 key
new ComponentRef(myComponent, null)

// 有 key（用于列表）
new ComponentRef(itemComponent, "item-" + item.id())
```

**Key 的作用：**

```
列表 [A, B, C] → [B, C, D]

无 key:
  位置 0: A → B (销毁 A，创建 B)
  位置 1: B → C (销毁 B，创建 C)
  位置 2: C → D (销毁 C，创建 D)
  → 3 次销毁 + 3 次创建

有 key:
  key=A: 存在 → 不存在 (销毁 A)
  key=B: 位置 0 → 位置 0 (复用)
  key=C: 位置 1 → 位置 1 (复用)
  key=D: 不存在 → 存在 (创建 D)
  → 1 次销毁 + 1 次创建
```

### ConditionalNode - 条件渲染

```java
public record ConditionalNode(
    Supplier<Boolean> condition,
    RenderNode whenTrue,
    RenderNode whenFalse
) implements StructuralNode {
}
```

**行为：**
- 根据 condition 选择渲染哪个分支
- condition 变化时切换分支
- 不活跃的分支不会创建 Element

```java
// 创建
new ConditionalNode(
    () -> isLoggedIn.get(),
    userDashboard,
    loginForm
)

// 等价于
RenderNode.conditional(
    () -> isLoggedIn.get(),
    userDashboard,
    loginForm
)
```

### ForEachNode - 列表渲染

```java
public record ForEachNode<T>(
    Supplier<List<T>> itemsSupplier,
    Function<T, Object> keyExtractor,
    Function<T, RenderNode> itemRenderer
) implements StructuralNode {
}
```

**行为：**
- 为每个 item 创建一个 RenderNode
- 使用 keyExtractor 为 item 生成唯一标识
- items 变化时，根据 key 差异更新

```java
// 创建
new ForEachNode<>(
    () -> items.get(),
    Item::id,
    item -> new ComponentRef(itemCard(item), String.valueOf(item.id()))
)

// 等价于
RenderNode.forEach(
    items::get,
    Item::id,
    item -> itemCard(item)
)
```

### ProviderNode - 上下文提供

```java
public record ProviderNode<T>(
    Class<T> type,
    T value,
    RenderNode child
) implements StructuralNode {
}
```

**用途：**
- 向子树提供依赖
- 实现跨层级数据传递

```java
// 提供主题
new ProviderNode<>(
    ThemeContext.class,
    darkTheme,
    mainContent
)

// 子组件可以通过 ctx.provide(ThemeContext.class) 获取
```

---

## 节点创建与遍历

### RenderNode 静态工厂

```java
public interface RenderNode {
    // 空节点
    static RenderNode empty() {
        return EmptyNode.INSTANCE;
    }
    
    // 条件渲染
    static RenderNode conditional(
        Supplier<Boolean> condition,
        RenderNode whenTrue,
        RenderNode whenFalse
    ) {
        return new ConditionalNode(condition, whenTrue, whenFalse);
    }
    
    // 显示/隐藏
    static RenderNode showWhen(
        Supplier<Boolean> condition,
        RenderNode content
    ) {
        return conditional(condition, content, empty());
    }
    
    // 列表渲染
    static <T> RenderNode forEach(
        Supplier<List<T>> items,
        Function<T, Object> keyExtractor,
        Function<T, Component> itemRenderer
    ) {
        return new ForEachNode<>(items, keyExtractor, 
            item -> new ComponentRef(itemRenderer.apply(item), 
                String.valueOf(keyExtractor.apply(item))));
    }
}
```

### 遍历 RenderNode 树

```java
// 深度优先遍历
void traverse(RenderNode node, Consumer<RenderNode> visitor) {
    visitor.accept(node);
    
    switch (node) {
        case ContainerNode container -> {
            for (RenderNode child : container.children()) {
                traverse(child, visitor);
            }
        }
        case ConditionalNode cond -> {
            // 只遍历活跃分支
            if (cond.condition().get()) {
                traverse(cond.whenTrue(), visitor);
            } else {
                traverse(cond.whenFalse(), visitor);
            }
        }
        case ForEachNode<?> forEach -> {
            var items = forEach.itemsSupplier().get();
            for (var item : items) {
                traverse(forEach.itemRenderer().apply(item), visitor);
            }
        }
        default -> { /* 叶子节点，无需继续 */ }
    }
}
```

### 统计节点数量

```java
int countNodes(RenderNode node) {
    return switch (node) {
        case EmptyNode e -> 0;
        case LeafNode l -> 1;
        case ContainerNode c -> 
            1 + c.children().stream()
                .mapToInt(this::countNodes)
                .sum();
        case ComponentRef r -> 1;  // 不展开组件
        case StructuralNode s -> 1;
    };
}
```

---

## 与 Element 的关系

### RenderNode vs Element

| 特性 | RenderNode | Element |
|------|------------|---------|
| 本质 | 描述/蓝图 | 实例/实体 |
| 可变性 | 不可变 | 可变 |
| 生命周期 | 短暂（每帧重建） | 持久（跨帧存在） |
| 状态 | 无状态 | 有状态 |
| 创建成本 | 低（数据结构） | 高（UI 对象） |

### 转换流程

```
Component.render(ctx)
        │
        ▼
    RenderNode (描述)
        │
        ▼
    ElementTree.reconcile(oldElement, newNode)
        │
        ├─ 相同类型 → 更新 Element
        │
        └─ 不同类型 → 销毁旧 Element，创建新 Element
        │
        ▼
    Element (实例)
```

### 示例：从 RenderNode 到 Element

```java
// RenderNode
ColumnNode renderNode = new ColumnNode(
    List.of(
        TextNode.of("Hello"),
        TextNode.of("World")
    ),
    8,
    Style.EMPTY
);

// 转换为 Element
Element element = createElement(renderNode);
// element 结构:
// ColumnElement
// ├── TextElement("Hello")
// └── TextElement("World")

// 更新时
ColumnNode newRenderNode = new ColumnNode(
    List.of(
        TextNode.of("Hello"),    // 相同，复用 TextElement
        TextNode.of("Universe")  // 不同，更新 TextElement
    ),
    8,
    Style.EMPTY
);

reconcile(element, newRenderNode);
// 结果: 只有第二个 TextElement 被更新
```

---

## 常见问题与解答

### Q1: RenderNode 为什么要不可变？

**A**: 不可变带来多个好处：

1. **安全比较**：可以安全地比较新旧树
2. **缓存**：相同的 RenderNode 可以缓存
3. **并发安全**：多线程访问无风险
4. **简单性**：不需要担心状态变化

### Q2: 为什么 TextNode 用 Supplier 而不是 String？

**A**: 支持响应式更新：

```java
// 如果用 String
TextNode.of("Count: " + count.get())  // 创建时就固定了

// 用 Supplier
TextNode.dynamic(() -> "Count: " + count.get())  // 每次渲染都重新求值
```

### Q3: ComponentRef 的 key 什么时候需要？

**A**: 在列表中使用组件时需要 key：

```java
// ❌ 没有 key：列表重排时会销毁重建
forEach(items, item -> new ComponentRef(itemCard(item), null))

// ✅ 有 key：列表重排时可以复用
forEach(items, item -> new ComponentRef(itemCard(item), item.id()))
```

### Q4: StructuralNode 和 ContainerNode 有什么区别？

**A**:
- **ContainerNode**: 布局容器，决定子元素如何排列
- **StructuralNode**: 逻辑控制，决定渲染哪些子元素

```java
// ContainerNode: 所有子元素都渲染
ColumnNode column = new ColumnNode(List.of(a, b, c), ...);
// 渲染: a, b, c

// StructuralNode: 根据逻辑选择
ConditionalNode cond = new ConditionalNode(condition, a, b);
// 渲染: a 或 b（取决于 condition）
```

---

## 下一步

现在你已经理解了 RenderNode 类型系统，接下来学习 Element 树的实现：

➡️ **[08-element-tree.md](./08-element-tree.md)** - Element 树与协调算法

