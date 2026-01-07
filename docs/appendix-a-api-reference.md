# Appendix A: API Reference - API 速查手册

## 目录

- [Signal API](#signal-api)
- [Computed API](#computed-api)
- [Tracker API](#tracker-api)
- [Component API](#component-api)
- [ComponentContext API](#componentcontext-api)
- [RenderNode API](#rendernode-api)
- [Render DSL API](#render-dsl-api)
- [Style API](#style-api)
- [Element API](#element-api)

---

## Signal API

### 创建 Signal

```java
// 基本创建
Signal<T> signal = Signal.create(initialValue);

// 通过 ComponentContext 创建
Signal<T> signal = ctx.signal(initialValue);
```

### Signal 方法

| 方法 | 签名 | 说明 |
|------|------|------|
| `get()` | `T get()` | 获取值，建立追踪依赖 |
| `peek()` | `T peek()` | 获取值，不建立依赖 |
| `set(T)` | `void set(T value)` | 设置新值 |
| `update(Function)` | `void update(UnaryOperator<T> fn)` | 基于当前值更新 |
| `subscribe(Consumer)` | `void subscribe(Consumer<T> listener)` | 订阅变化 |

### 使用示例

```java
Signal<Integer> count = Signal.create(0);

// 读取（追踪依赖）
int current = count.get();

// 读取（不追踪）
int snapshot = count.peek();

// 设置
count.set(10);

// 更新
count.update(n -> n + 1);

// 订阅
count.subscribe(newValue -> System.out.println("Changed: " + newValue));
```

---

## Computed API

### 创建 Computed

```java
// 基本创建
Computed<T> computed = Computed.create(() -> expression);

// 通过 ComponentContext 创建
Computed<T> computed = ctx.computed(() -> expression);
```

### Computed 方法

| 方法 | 签名 | 说明 |
|------|------|------|
| `get()` | `T get()` | 获取值，自动追踪依赖 |
| `peek()` | `T peek()` | 获取值，不建立依赖 |

### 使用示例

```java
Signal<Integer> a = Signal.create(1);
Signal<Integer> b = Signal.create(2);

Computed<Integer> sum = Computed.create(() -> a.get() + b.get());
// sum.get() == 3

a.set(5);
// sum.get() == 7 (自动重新计算)
```

---

## Tracker API

### 静态方法

| 方法 | 签名 | 说明 |
|------|------|------|
| `track(Trackable)` | `static void track(Trackable t)` | 追踪依赖 |
| `trigger(Trackable)` | `static void trigger(Trackable t)` | 触发更新 |
| `effect(Runnable)` | `static Effect effect(Runnable fn)` | 创建 Effect |
| `effect(Supplier)` | `static Effect effect(Supplier<Runnable> fn)` | 创建带清理的 Effect |
| `batch(Runnable)` | `static void batch(Runnable fn)` | 批量更新 |
| `untrack(Supplier)` | `static T untrack(Supplier<T> fn)` | 不追踪执行 |
| `isTracking()` | `static boolean isTracking()` | 是否在追踪中 |

### Effect 方法

| 方法 | 签名 | 说明 |
|------|------|------|
| `dispose()` | `void dispose()` | 停止 Effect |
| `run()` | `void run()` | 手动运行 Effect |

### 使用示例

```java
// 创建 Effect
Effect effect = Tracker.effect(() -> {
    System.out.println("Value: " + signal.get());
    return () -> System.out.println("Cleanup");  // cleanup
});

// 批量更新
Tracker.batch(() -> {
    signal1.set(1);
    signal2.set(2);
    // 只触发一次更新
});

// 不追踪读取
int value = Tracker.untrack(() -> signal.get());

// 停止 Effect
effect.dispose();
```

---

## Component API

### 创建组件

```java
// 无状态组件
Component stateless = Component.stateless(ctx -> renderNode);

// 有状态组件
Component stateful = Component.stateful(ctx -> renderNode);

// 函数式组件（使用 functional）
Component functional = Component.functional(() -> renderNode);
```

### Component 接口

| 方法 | 签名 | 说明 |
|------|------|------|
| `render(ComponentContext)` | `RenderNode render(ComponentContext ctx)` | 渲染组件 |
| `isStateful()` | `default boolean isStateful()` | 是否有状态 |

### 组件类型

```java
// StatelessComponent - 无状态
// StatefulComponent - 有状态
// FunctionalComponent - 函数式（简化版）
```

---

## ComponentContext API

### 状态管理

| 方法 | 签名 | 说明 |
|------|------|------|
| `signal(T)` | `<T> Signal<T> signal(T initial)` | 创建 Signal |
| `computed(Supplier)` | `<T> Computed<T> computed(Supplier<T> fn)` | 创建 Computed |
| `effect(Supplier)` | `void effect(Supplier<Runnable> fn)` | 创建 Effect |

### 生命周期

| 方法 | 签名 | 说明 |
|------|------|------|
| `onMount(Runnable)` | `void onMount(Runnable callback)` | 挂载时执行 |
| `onUnmount(Runnable)` | `void onUnmount(Runnable callback)` | 卸载时执行 |

### 依赖注入

| 方法 | 签名 | 说明 |
|------|------|------|
| `provide(Key, T)` | `<T> void provide(Key<T> key, T value)` | 提供值 |
| `inject(Key)` | `<T> T inject(Key<T> key)` | 注入值 |
| `inject(Key, T)` | `<T> T inject(Key<T> key, T defaultValue)` | 注入值带默认 |

### 组件引用

| 方法 | 签名 | 说明 |
|------|------|------|
| `ref()` | `<T> ComponentRef<T> ref()` | 创建引用 |
| `ref(T)` | `<T> ComponentRef<T> ref(T initial)` | 创建带初始值引用 |

### 使用示例

```java
Component example = Component.stateful(ctx -> {
    // 状态
    var count = ctx.signal(0);
    var doubled = ctx.computed(() -> count.get() * 2);
    
    // Effect
    ctx.effect(() -> {
        System.out.println("Count: " + count.get());
        return () -> System.out.println("Cleanup");
    });
    
    // 生命周期
    ctx.onMount(() -> System.out.println("Mounted"));
    ctx.onUnmount(() -> System.out.println("Unmounted"));
    
    // 依赖注入
    ctx.provide(ThemeKey.INSTANCE, darkTheme);
    var theme = ctx.inject(ThemeKey.INSTANCE, defaultTheme);
    
    // Ref
    var inputRef = ctx.<InputElement>ref();
    
    return renderNode;
});
```

---

## RenderNode API

### 节点类型

| 类型 | 说明 | 创建方式 |
|------|------|----------|
| `RenderNode.Empty` | 空节点 | `RenderNode.empty()` |
| `RenderNode.Single` | 单组件 | `RenderNode.of(component)` |
| `RenderNode.Multi` | 多子节点 | `RenderNode.multi(nodes)` |
| `RenderNode.Conditional` | 条件节点 | `RenderNode.conditional(...)` |
| `RenderNode.Dynamic` | 动态节点 | `RenderNode.dynamic(supplier)` |
| `RenderNode.ForEach` | 列表节点 | `RenderNode.forEach(...)` |
| `RenderNode.Slot` | 插槽节点 | `RenderNode.slot(children)` |

### 静态工厂方法

```java
// 空节点
RenderNode.empty()

// 单组件
RenderNode.of(component)

// 多节点
RenderNode.multi(node1, node2, node3)
RenderNode.multi(List.of(node1, node2))

// 条件节点
RenderNode.conditional(condition, trueNode, falseNode)
RenderNode.showWhen(condition, node)

// 动态节点
RenderNode.dynamic(() -> computeNode())

// 列表节点
RenderNode.forEach(listSupplier, keyExtractor, itemRenderer)

// 插槽
RenderNode.slot(childrenNode)
```

### 组合节点

```java
RenderNode result = RenderNode.multi(
    RenderNode.of(header),
    RenderNode.showWhen(() -> showContent.get(), content),
    RenderNode.forEach(items::get, Item::id, item -> itemView(item)),
    RenderNode.of(footer)
);
```

---

## Render DSL API

### 布局容器

```java
// 垂直布局
Column(style, () -> { /* children */ })
Column(() -> { /* children */ })

// 水平布局
Row(style, () -> { /* children */ })
Row(() -> { /* children */ })

// 盒子容器
Box(style, () -> { /* children */ })
Box(() -> { /* children */ })
```

### 基础组件

```java
// 静态文本
Text(String text)
Text(String text, Style style)

// 动态文本
Text(Supplier<String> text)
Text(Supplier<String> text, Style style)

// 空节点
Empty()
```

### 子组件

```java
// 添加组件
Child(Component component)
Child(Component component, Object key)

// 添加 RenderNode
Child(RenderNode node)

// 添加多个
Children(RenderNode node)
Children(Component... components)
```

### 条件渲染

```java
// 简单条件
ShowWhen(Supplier<Boolean> condition, RenderNode node)

// if-else
Conditional(Supplier<Boolean> condition, RenderNode trueNode, RenderNode falseNode)

// 多分支
When(() -> value.get())
    .is(1, Render.text("One"))
    .is(2, Render.text("Two"))
    .otherwise(Render.text("Other"))
```

### 列表渲染

```java
ForEach(Supplier<List<T>> list, Function<T, Object> keyFn, Function<T, Component> renderer)
```

---

## Style API

### 创建 Style

```java
// 使用 Builder
Style style = Style.builder()
    .width(100)
    .height(50)
    .padding(8)
    .margin(4)
    .background(0xFF0000)
    .build();

// 空样式
Style empty = Style.empty();
```

### Style.Builder 方法

| 方法 | 参数 | 说明 |
|------|------|------|
| `width(int)` | 宽度像素 | 设置宽度 |
| `height(int)` | 高度像素 | 设置高度 |
| `minWidth(int)` | 最小宽度 | 设置最小宽度 |
| `minHeight(int)` | 最小高度 | 设置最小高度 |
| `maxWidth(int)` | 最大宽度 | 设置最大宽度 |
| `maxHeight(int)` | 最大高度 | 设置最大高度 |
| `padding(int)` | 内边距 | 四边内边距 |
| `padding(int, int, int, int)` | 上右下左 | 分别设置 |
| `margin(int)` | 外边距 | 四边外边距 |
| `margin(int, int, int, int)` | 上右下左 | 分别设置 |
| `background(int)` | ARGB 颜色 | 背景色 |
| `borderColor(int)` | ARGB 颜色 | 边框色 |
| `borderWidth(int)` | 边框宽度 | 边框粗细 |
| `borderRadius(int)` | 圆角半径 | 圆角 |
| `fontSize(int)` | 字号 | 字体大小 |
| `fontColor(int)` | ARGB 颜色 | 字体颜色 |

### Style Record 属性

```java
public record Style(
    int width,
    int height,
    int minWidth,
    int minHeight,
    int maxWidth,
    int maxHeight,
    int paddingTop,
    int paddingRight,
    int paddingBottom,
    int paddingLeft,
    int marginTop,
    int marginRight,
    int marginBottom,
    int marginLeft,
    int background,
    int borderColor,
    int borderWidth,
    int borderRadius,
    int fontSize,
    int fontColor
) { }
```

---

## Element API

### Element 接口

| 方法 | 签名 | 说明 |
|------|------|------|
| `getComponent()` | `Component getComponent()` | 获取组件 |
| `mount(Element)` | `void mount(Element parent)` | 挂载到父元素 |
| `unmount()` | `void unmount()` | 卸载 |
| `update(Component)` | `void update(Component newComponent)` | 更新组件 |
| `rebuild()` | `void rebuild()` | 重建子树 |
| `getKey()` | `Object getKey()` | 获取 key |

### Element 类型

| 类型 | 说明 |
|------|------|
| `ComponentElement` | 组件元素 |
| `TextElement` | 文本元素 |
| `ContainerElement` | 容器元素 |
| `ConditionalElement` | 条件元素 |
| `ForEachElement` | 列表元素 |

### ElementTree

```java
// 创建
ElementTree tree = new ElementTree();

// 设置根节点
tree.setRoot(rootComponent);

// 调度更新
tree.scheduleUpdate();

// 获取根元素
Element root = tree.getRoot();
```

---

## 快速参考表

### 创建状态

```java
// Signal
Signal<T> s = ctx.signal(initialValue);

// Computed
Computed<T> c = ctx.computed(() -> expression);

// Effect
ctx.effect(() -> { /* side effect */ return cleanup; });
```

### 读写状态

```java
// 读取（追踪）
T value = signal.get();

// 读取（不追踪）
T value = signal.peek();

// 写入
signal.set(newValue);
signal.update(old -> newValue);
```

### 渲染

```java
// 布局
Column(() -> { });
Row(() -> { });
Box(() -> { });

// 文本
Text("static");
Text(() -> "dynamic");

// 子组件
Child(component);

// 条件
ShowWhen(condition, node);

// 列表
ForEach(list, keyFn, renderer);
```

### 样式

```java
Style.builder()
    .width(100)
    .height(50)
    .padding(8)
    .background(0xFF0000)
    .build();
```

---

## 参见

- **[02. Reactive Primitives](./02-reactive-primitives.md)** - 响应式原语详解
- **[05. ComponentContext](./05-component-context.md)** - Hook API 详解
- **[10. Style System](./10-style-system.md)** - 样式系统详解

