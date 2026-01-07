# 01. 系统介绍

## 目录

- [什么是 CloudLib 响应式 UI](#什么是-cloudlib-响应式-ui)
- [为什么需要响应式 UI](#为什么需要响应式-ui)
- [设计灵感来源](#设计灵感来源)
- [核心设计原则](#核心设计原则)
- [与其他框架对比](#与其他框架对比)
- [架构总览](#架构总览)

---

## 什么是 CloudLib 响应式 UI

CloudLib 响应式 UI 是一个专为 Minecraft Mod 开发设计的**细粒度响应式 UI 框架**。它提供了一套声明式的 API，让开发者可以用类似现代前端框架（Vue、React、Compose）的方式来构建游戏内 GUI。

### 核心特性

1. **细粒度响应式** - 状态变化时，只有真正依赖该状态的组件才会更新
2. **声明式 UI** - 描述"UI 应该是什么样"，而不是"如何更新 UI"
3. **自动依赖追踪** - 框架自动知道哪些组件依赖哪些状态
4. **类型安全** - 完全基于 Java 泛型，编译时类型检查

### 一个简单的例子

```java
// 传统命令式方式
public class OldCounter {
    private int count = 0;
    private Label countLabel;
    
    public void increment() {
        count++;
        countLabel.setText("Count: " + count);  // 手动更新 UI
    }
}

// CloudLib 响应式方式
Component counter = Component.stateful(ctx -> {
    var count = ctx.signal(0);  // 响应式状态
    
    return Render.column(
        Render.text(() -> "Count: " + count.get()),  // 自动更新
        Render.button("+", () -> count.update(n -> n + 1))
    );
});
```

**区别在哪里？**

| 传统方式 | CloudLib 响应式 |
|---------|----------------|
| 手动调用 `setText()` 更新 | 声明 `() -> "Count: " + count.get()` |
| 忘记更新会导致 UI 不同步 | 自动保持同步，不可能忘记 |
| 状态散落在各处 | 状态集中在 `signal` 中 |
| 更新逻辑分散 | 更新逻辑由框架处理 |

---

## 为什么需要响应式 UI

### 问题：传统 GUI 开发的痛点

在传统的 Minecraft Mod GUI 开发中，我们经常遇到这些问题：

#### 1. 状态同步噩梦

```java
// 传统方式：容易出错
public class InventoryScreen {
    private int selectedSlot = 0;
    private Label slotLabel;
    private Button deleteButton;
    private Panel detailPanel;
    
    public void selectSlot(int slot) {
        selectedSlot = slot;
        
        // 必须记得更新所有相关 UI
        slotLabel.setText("Slot: " + slot);           // 容易忘记
        deleteButton.setEnabled(slot >= 0);           // 容易忘记
        detailPanel.setVisible(slot >= 0);            // 容易忘记
        detailPanel.refresh();                        // 容易忘记
        // 如果新增了依赖 selectedSlot 的 UI，这里还要加代码
    }
}
```

**问题**：
- 每次状态变化，必须手动更新所有相关 UI
- 新增 UI 元素时，必须修改所有状态更新的地方
- 容易遗漏，导致 UI 不同步

#### 2. 条件渲染复杂

```java
// 传统方式：混乱的条件逻辑
public void updateUI() {
    if (isLoggedIn) {
        loginButton.setVisible(false);
        logoutButton.setVisible(true);
        userPanel.setVisible(true);
        userName.setText(currentUser.getName());
        // ... 更多条件逻辑
    } else {
        loginButton.setVisible(true);
        logoutButton.setVisible(false);
        userPanel.setVisible(false);
        // ...
    }
}
```

#### 3. 列表更新困难

```java
// 传统方式：列表更新很痛苦
public void updateItemList(List<Item> newItems) {
    // 完全重建？性能差
    itemPanel.clear();
    for (Item item : newItems) {
        itemPanel.add(createItemWidget(item));
    }
    
    // 增量更新？代码复杂
    // 要处理：新增、删除、移动、更新...
}
```

### 解决方案：响应式 UI

CloudLib 响应式 UI 通过**声明式编程**和**自动依赖追踪**解决这些问题：

```java
// CloudLib 方式：简洁、安全、自动
Component inventory = Component.stateful(ctx -> {
    var selectedSlot = ctx.signal(-1);
    var items = ctx.signal(List.<Item>of());
    
    // 派生状态：自动计算，自动更新
    var hasSelection = ctx.computed(() -> selectedSlot.get() >= 0);
    var selectedItem = ctx.computed(() -> {
        int slot = selectedSlot.get();
        return slot >= 0 ? items.get().get(slot) : null;
    });
    
    return Column(() -> {
        // 条件渲染：声明式，自动切换
        Child(RenderNode.showWhen(hasSelection::get, 
            buildDetailPanel(selectedItem)));
        
        // 列表渲染：自动 diff，高效更新
        Child(RenderNode.forEach(
            items::get,
            Item::id,  // key for efficient updates
            (item, i) -> buildItemSlot(item, i, selectedSlot)
        ));
    });
});
```

**优势**：
- ✅ 状态变化自动触发 UI 更新
- ✅ 条件渲染声明式表达
- ✅ 列表自动 diff 更新
- ✅ 新增 UI 元素不需要修改状态更新代码

---

## 设计灵感来源

CloudLib 响应式 UI 综合了多个现代 UI 框架的优秀设计：

### Vue 3 Reactivity

**借鉴点**：Signal/Computed 响应式原语、自动依赖追踪

```javascript
// Vue 3
const count = ref(0)
const doubled = computed(() => count.value * 2)

// CloudLib
Signal<Integer> count = Signal.of(0);
Computed<Integer> doubled = Computed.of(() -> count.get() * 2);
```

**为什么借鉴 Vue 3？**
- Vue 3 的响应式系统是目前最优雅的实现之一
- 自动依赖追踪避免了手动声明依赖的繁琐
- Signal/Computed 模型简单直观

### React Hooks

**借鉴点**：Hook 式状态管理 API

```javascript
// React
function Counter() {
    const [count, setCount] = useState(0);
    useEffect(() => { /* side effect */ }, [count]);
    return <div>{count}</div>;
}

// CloudLib
Component.stateful(ctx -> {
    var count = ctx.signal(0);
    ctx.effect(() -> { /* side effect */ }, count.get());
    return Render.text(() -> String.valueOf(count.get()));
});
```

**为什么借鉴 React Hooks？**
- Hook 模式让函数式组件拥有状态
- API 简洁，学习曲线平缓
- 强制状态和 UI 放在一起，便于理解

### Jetpack Compose

**借鉴点**：声明式 DSL 语法

```kotlin
// Compose
Column {
    Text("Hello")
    Button(onClick = { count++ }) {
        Text("Click")
    }
}

// CloudLib
Column(() -> {
    Text("Hello");
    Button("Click", () -> count.update(n -> n + 1));
});
```

**为什么借鉴 Compose？**
- DSL 语法清晰表达 UI 结构
- 隐式作用域避免大量 `.child()` 调用
- Java 的 lambda 可以实现类似效果

### Flutter

**借鉴点**：Widget/Element/RenderObject 三层架构

```
Flutter:                    CloudLib:
Widget (immutable)    →    RenderNode (immutable)
Element (persistent)  →    Element (persistent)
RenderObject          →    (渲染层，待实现)
```

**为什么借鉴 Flutter？**
- 三层分离使得重建成本最小化
- Widget 可以频繁创建，Element 保持稳定
- 支持高效的 reconciliation

### SolidJS

**借鉴点**：细粒度响应式更新

```javascript
// SolidJS - 只有 count() 的地方会更新
const [count, setCount] = createSignal(0);
return <div>{count()}</div>;  // 不会重建整个组件
```

**为什么借鉴 SolidJS？**
- 真正的细粒度更新，不是虚拟 DOM diff
- 性能极佳，更新精确到依赖点
- CloudLib 的 `Render.text(() -> ...)` 实现了类似效果

---

## 核心设计原则

### 原则 1：声明式优于命令式

```java
// ❌ 命令式：描述"怎么做"
if (isVisible) {
    panel.show();
} else {
    panel.hide();
}

// ✅ 声明式：描述"是什么"
RenderNode.showWhen(() -> isVisible.get(), panel)
```

**为什么？**
- 声明式代码更易读，表达意图而非步骤
- 框架可以优化执行顺序和方式
- 减少状态不一致的可能性

### 原则 2：单向数据流

```
State (Signal) → Compute (Computed) → Render (Component) → Event → State
      ↑                                                            |
      └────────────────────────────────────────────────────────────┘
```

**为什么？**
- 数据流向清晰，易于追踪
- 避免循环依赖
- 便于调试

### 原则 3：最小化更新

```java
// 当 count 变化时：
// - text1 会更新 ✓
// - text2 不会更新 ✓ （它不依赖 count）

var count = Signal.of(0);
var name = Signal.of("Alice");

Column(() -> {
    Text(() -> "Count: " + count.get());   // 依赖 count
    Text(() -> "Name: " + name.get());      // 依赖 name
});
```

**为什么？**
- 避免不必要的重建
- 提升性能
- UI 响应更流畅

### 原则 4：组合优于继承

```java
// ❌ 继承：僵化，难以复用
class MyButton extends BaseButton { ... }

// ✅ 组合：灵活，易于复用
Component myButton = Component.stateful(ctx -> {
    return Render.button(label, onClick, style);
});
```

**为什么？**
- 组件可以任意组合
- 避免继承层次过深
- 逻辑可以通过 Hook 复用

---

## 与其他框架对比

### vs 传统 Minecraft GUI

| 特性 | 传统方式 | CloudLib |
|------|---------|----------|
| 状态管理 | 分散在各处 | 集中在 Signal |
| UI 更新 | 手动调用 | 自动响应 |
| 列表渲染 | 手动管理 | 自动 diff |
| 条件渲染 | if/else 控制可见性 | 声明式 |
| 代码组织 | 类继承 | 函数式组件 |

### vs React

| 特性 | React | CloudLib |
|------|-------|----------|
| 更新粒度 | 组件级（虚拟 DOM diff） | Signal 级（细粒度） |
| 依赖声明 | 手动（useEffect deps） | 自动追踪 |
| 语言 | JSX | Java DSL |
| 运行时 | 需要 React 运行时 | 纯 Java |

### vs Vue 3

| 特性 | Vue 3 | CloudLib |
|------|-------|----------|
| 响应式 | Proxy-based | 手动调用 get() |
| 模板 | SFC 模板语法 | Java DSL |
| 组件 | Options/Composition API | 函数式 |
| 平台 | Web | Minecraft |

---

## 架构总览

```
┌─────────────────────────────────────────────────────────────────────┐
│                         应用层 (Application)                         │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Screen / Container                                          │   │
│  │  - 持有 ElementTree                                          │   │
│  │  - 每帧调用 flushBuild()                                     │   │
│  └─────────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────────┤
│                       组件层 (Component Layer)                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │
│  │  Component   │  │  Component   │  │  Component   │              │
│  │  (stateful)  │  │  (stateless) │  │    (pure)    │              │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘              │
│         │                 │                 │                       │
│         └─────────────────┼─────────────────┘                       │
│                           ▼                                         │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  RenderNode (Widget Tree)                                    │   │
│  │  - 不可变描述                                                 │   │
│  │  - 每次 build 创建新的                                        │   │
│  └─────────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────────┤
│                       元素层 (Element Layer)                         │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  ElementTree                                                 │   │
│  │  └── Element (persistent)                                    │   │
│  │      ├── ComponentElement (持有状态)                          │   │
│  │      ├── GroupElement                                        │   │
│  │      ├── LeafElement                                         │   │
│  │      └── ...                                                 │   │
│  └─────────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────────┤
│                      响应式层 (Reactive Layer)                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │
│  │    Signal    │──▶│   Computed   │──▶│   Tracker    │              │
│  │   (state)    │  │   (derive)   │  │  (tracking)  │              │
│  └──────────────┘  └──────────────┘  └──────────────┘              │
└─────────────────────────────────────────────────────────────────────┘
```

### 层次职责

| 层 | 职责 | 关键类 |
|---|------|--------|
| 应用层 | 持有树、驱动更新 | Screen, ElementTree |
| 组件层 | 定义 UI 逻辑 | Component, ComponentContext |
| 元素层 | 管理实例、生命周期 | Element, BuildOwner |
| 响应式层 | 状态管理、依赖追踪 | Signal, Computed, Tracker |

---

## 下一步

现在你已经了解了系统的全貌，接下来建议阅读：

➡️ **[02-reactive-primitives.md](./02-reactive-primitives.md)** - 深入了解 Signal、Computed 响应式原语

