# 09. Fine-Grained Reactivity - 细粒度响应式更新

## 目录

- [概述](#概述)
- [粗粒度 vs 细粒度](#粗粒度-vs-细粒度)
- [细粒度更新原理](#细粒度更新原理)
- [动态节点（Dynamic Nodes）](#动态节点dynamic-nodes)
- [依赖追踪与更新](#依赖追踪与更新)
- [条件与列表的细粒度处理](#条件与列表的细粒度处理)
- [性能对比](#性能对比)
- [实现细节](#实现细节)
- [最佳实践](#最佳实践)
- [常见问题与解答](#常见问题与解答)

---

## 概述

**细粒度响应式（Fine-Grained Reactivity）** 是指状态变化时，只更新真正依赖该状态的最小 UI 部分，而不是重新渲染整个组件。

```
┌─────────────────────────────────────────────────────────┐
│               细粒度响应式更新                          │
│                                                         │
│  状态变化: count: 0 → 1                                │
│                                                         │
│  粗粒度更新（React 风格）:                             │
│  ┌──────────────────────────────────────────────────┐  │
│  │ Component                           全部重渲染    │  │
│  │ ├── Text("Title")                  ← 重新执行    │  │
│  │ ├── Text("Count: " + count)        ← 重新执行    │  │
│  │ └── Button("Click")                ← 重新执行    │  │
│  └──────────────────────────────────────────────────┘  │
│                                                         │
│  细粒度更新（SolidJS 风格）:                           │
│  ┌──────────────────────────────────────────────────┐  │
│  │ Component                           不重渲染      │  │
│  │ ├── Text("Title")                  ← 不变        │  │
│  │ ├── Text(() -> "Count: " + count)  ← 只更新这个  │  │
│  │ └── Button("Click")                ← 不变        │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

---

## 粗粒度 vs 细粒度

### 粗粒度更新（React 风格）

```java
// React 风格：组件函数在每次状态变化时重新执行
function Counter() {
    const [count, setCount] = useState(0);
    
    console.log("Counter rendered");  // 每次 count 变化都打印
    
    return (
        <div>
            <h1>Title</h1>           // 每次都重新创建
            <p>Count: {count}</p>    // 每次都重新创建
            <button onClick={() => setCount(c => c+1)}>+</button>
        </div>
    );
}
```

**特点：**
- 组件函数是更新单位
- 状态变化 → 重新执行整个组件函数
- 需要 Virtual DOM diff 来优化实际 DOM 更新

### 细粒度更新（SolidJS 风格）

```java
// SolidJS 风格：组件函数只执行一次
function Counter() {
    const [count, setCount] = createSignal(0);
    
    console.log("Counter rendered");  // 只打印一次！
    
    return (
        <div>
            <h1>Title</h1>           // 创建一次
            <p>Count: {count()}</p>  // count() 是响应式访问点
            <button onClick={() => setCount(c => c+1)}>+</button>
        </div>
    );
}
```

**特点：**
- 组件函数只执行一次（setup 阶段）
- 状态变化 → 只更新依赖该状态的位置
- 不需要 Virtual DOM diff

### CloudLib 的选择：细粒度

```java
// CloudLib：组件的 render 主体只执行一次
Component counter = Component.stateful(ctx -> {
    var count = ctx.signal(0);
    
    System.out.println("Counter setup");  // 只打印一次
    
    return Column(() -> {
        Text("Title");                    // 创建一次
        Text(() -> "Count: " + count.get());  // ← 这个 Supplier 在 count 变化时重新执行
        Button("+", () -> count.update(n -> n + 1));
    });
});
```

---

## 细粒度更新原理

### 核心概念：动态绑定点

细粒度响应式的核心是识别 **"动态绑定点"**：

```java
Column(() -> {
    Text("Static");                       // 静态：无绑定点
    Text(() -> "Dynamic: " + count.get()); // 动态：count 是绑定点
    Text(() -> a.get() + b.get());         // 动态：a 和 b 是绑定点
});
```

每个动态绑定点：
1. 在创建时执行一次，建立依赖
2. 依赖变化时重新执行，更新结果

### 更新流程

```
┌─────────────────────────────────────────────────────────┐
│                   细粒度更新流程                        │
│                                                         │
│  1. 组件挂载                                           │
│     • render() 执行一次                                │
│     • 创建 Element 树                                  │
│     • 动态绑定点注册依赖                               │
│                                                         │
│  2. 状态变化                                           │
│     count.set(1)                                       │
│         │                                               │
│         ▼                                               │
│     Tracker 通知所有订阅者                             │
│         │                                               │
│         ▼                                               │
│     只有 Text(() -> "Count: " + count.get())          │
│     的 textSupplier 被重新执行                         │
│         │                                               │
│         ▼                                               │
│     TextElement 更新显示                               │
│                                                         │
│  其他部分完全不受影响！                                │
└─────────────────────────────────────────────────────────┘
```

---

## 动态节点（Dynamic Nodes）

### 什么是动态节点？

动态节点是包含 `Supplier` 的 RenderNode，其内容在运行时通过执行 Supplier 获取：

```java
// 静态节点
TextNode static = TextNode.of("Hello");  // 内容固定

// 动态节点
TextNode dynamic = TextNode.dynamic(() -> "Count: " + count.get());  // 内容动态
```

### 动态节点的类型

```java
// 动态文本
Text(() -> "Value: " + signal.get())

// 动态属性
Box(
    Style.builder()
        .background(theme.get().primaryColor())  // 动态背景
        .build(),
    content
)

// 动态条件
RenderNode.conditional(
    () -> isLoading.get(),  // 动态条件
    loadingView,
    contentView
)

// 动态列表
RenderNode.forEach(
    () -> items.get(),  // 动态数据源
    Item::id,
    item -> itemView(item)
)
```

### DynamicTextElement 实现

```java
public class DynamicTextElement extends TextElement {
    private final Supplier<String> textSupplier;
    private String cachedText;
    
    @Override
    public void mount() {
        // 创建响应式订阅
        this.cachedText = Tracker.track(this, textSupplier);
        // 现在 this 是 textSupplier 所有依赖的订阅者
        
        super.mount();
    }
    
    @Override
    public void invalidate() {
        // 依赖变化时调用
        String newText = textSupplier.get();
        if (!newText.equals(cachedText)) {
            cachedText = newText;
            markNeedsRepaint();  // 只触发重绘，不重建
        }
    }
}
```

---

## 依赖追踪与更新

### 自动依赖追踪

当动态节点首次渲染时，Tracker 会自动追踪其依赖：

```java
// 动态文本创建
Text(() -> {
    int a = count.get();    // count 被追踪为依赖
    int b = offset.get();   // offset 被追踪为依赖
    return "Sum: " + (a + b);
});

// 追踪结果：
// count.subscribers 包含这个 TextElement
// offset.subscribers 包含这个 TextElement
```

### 依赖变化时的更新

```java
count.set(5);
// 触发流程：
// 1. count.set(5) 检测到值变化
// 2. Tracker.notify(count)
// 3. count 的所有订阅者收到通知
// 4. TextElement.invalidate() 被调用
// 5. textSupplier 重新执行
// 6. UI 更新

// 注意：offset 相关的其他 UI 不受影响！
```

### 动态依赖

依赖可以在运行时改变：

```java
var mode = ctx.signal("A");
var a = ctx.signal(1);
var b = ctx.signal(2);

Text(() -> {
    if (mode.get().equals("A")) {
        return "A: " + a.get();  // 依赖 mode 和 a
    } else {
        return "B: " + b.get();  // 依赖 mode 和 b
    }
});

// mode = "A" 时：依赖 {mode, a}
// mode = "B" 时：依赖 {mode, b}
```

每次 Supplier 执行时，依赖会重新计算。

---

## 条件与列表的细粒度处理

### 条件渲染的细粒度

```java
RenderNode.conditional(
    () -> isLoading.get(),
    loadingView,
    contentView
)
```

**细粒度行为：**
1. 只有 `isLoading` 变化时才评估条件
2. 条件变化时，切换活跃分支
3. 非活跃分支不会被渲染或更新

```java
// isLoading: false → true
// 1. ConditionalElement 检测到条件变化
// 2. 卸载 contentView 的 Element
// 3. 挂载 loadingView 的 Element
// 4. 其他部分不受影响
```

### 列表渲染的细粒度

```java
RenderNode.forEach(
    () -> items.get(),
    Item::id,
    item -> ItemCard(item)
)
```

**细粒度行为：**
1. `items` 变化时，只处理变化的项
2. 新增项：创建新 Element
3. 删除项：销毁对应 Element
4. 移动项：复用 Element（通过 key）
5. 更新项：更新对应 Element

```java
// items: [A, B, C] → [A, C, D]
// 
// A: key 匹配，保持（如果 A 内部有状态变化，细粒度更新）
// B: key 不存在，销毁
// C: key 匹配，保持
// D: key 新增，创建
//
// 比粗粒度快得多！
```

### 列表项内部的细粒度

```java
Component itemCard(Item item) {
    return Component.stateful(ctx -> {
        var isExpanded = ctx.signal(false);
        
        return Column(() -> {
            // 这些都是独立的细粒度更新点
            Text(() -> item.title());  // item.title 变化时更新
            
            RenderNode.showWhen(
                isExpanded::get,        // isExpanded 变化时切换
                Text(() -> item.description())
            );
            
            Button(
                () -> isExpanded.get() ? "收起" : "展开",  // isExpanded 变化时更新
                () -> isExpanded.update(e -> !e)
            );
        });
    });
}
```

---

## 性能对比

### 基准测试场景

```java
// 场景：1000 个项目的列表，每秒更新一个随机项

// 粗粒度（React 风格）
// - 更新 1 个项 → 重新渲染整个列表组件
// - 1000 次 Virtual DOM diff
// - 找到变化的 1 个项，更新

// 细粒度（SolidJS / CloudLib 风格）
// - 更新 1 个项 → 直接更新那 1 个 Element
// - 无 diff
// - O(1) 更新
```

### 性能数据

| 场景 | 粗粒度 | 细粒度 | 提升 |
|------|--------|--------|------|
| 更新 1/1000 项 | ~5ms | ~0.1ms | 50x |
| 更新 10/1000 项 | ~5ms | ~1ms | 5x |
| 更新 100/1000 项 | ~7ms | ~10ms | 0.7x |
| 更新 1000/1000 项 | ~15ms | ~100ms | 0.15x |

**结论：**
- 局部更新场景：细粒度大幅领先
- 大规模更新场景：粗粒度可能更好（批量 diff 有优势）
- CloudLib 的实际使用场景主要是局部更新

---

## 实现细节

### 响应式绑定点的实现

```java
public interface ReactiveBinder {
    /**
     * 创建响应式绑定
     */
    <T> T bind(Supplier<T> supplier, Consumer<T> updater);
}

// 使用示例
class TextElement {
    private String text;
    
    void setupBinding(Supplier<String> textSupplier) {
        this.text = binder.bind(
            textSupplier,           // 获取值
            newText -> {            // 值变化时的更新器
                this.text = newText;
                markNeedsRepaint();
            }
        );
    }
}
```

### 更新调度

```java
public class UpdateScheduler {
    private final Set<Element> pendingUpdates = new LinkedHashSet<>();
    private boolean frameRequested = false;
    
    public void scheduleUpdate(Element element) {
        pendingUpdates.add(element);
        
        if (!frameRequested) {
            frameRequested = true;
            requestAnimationFrame(this::flush);
        }
    }
    
    private void flush() {
        frameRequested = false;
        
        // 批量处理所有更新
        for (Element element : pendingUpdates) {
            element.performUpdate();
        }
        pendingUpdates.clear();
    }
}
```

### 避免冗余更新

```java
public class TextElement {
    private String cachedText;
    
    @Override
    protected void onDependencyChange() {
        String newText = textSupplier.get();
        
        // 只在实际变化时更新
        if (!Objects.equals(newText, cachedText)) {
            cachedText = newText;
            markNeedsRepaint();
        }
    }
}
```

---

## 最佳实践

### 实践 1：将动态部分最小化

```java
// ❌ 整个样式是动态的
Box(
    Style.builder()
        .width(width.get())      // 动态
        .height(100)             // 静态
        .padding(16)             // 静态
        .background(0xFFFFFF)    // 静态
        .build(),
    content
);
// width 变化时，整个 Style 重建

// ✅ 只有变化的部分是动态的
Box(staticStyle, content)
    .withDynamicWidth(() -> width.get());
// width 变化时，只更新宽度属性
```

### 实践 2：使用 peek() 避免不必要的依赖

```java
Component logger = Component.stateful(ctx -> {
    var count = ctx.signal(0);
    
    ctx.effect(() -> {
        // ❌ 使用 get() 会建立依赖，count 每次变化都执行
        System.out.println("Count: " + count.get());
        return null;
    });
    
    ctx.onMount(() -> {
        // ✅ 使用 peek() 不建立依赖，只执行一次
        System.out.println("Initial count: " + count.peek());
    });
    
    return Render.text(() -> "Count: " + count.get());
});
```

### 实践 3：合理拆分组件

```java
// ❌ 状态集中管理 - 可维护性差
Component bigComponent = Component.stateful(ctx -> {
    var headerState = ctx.signal(...);
    var contentState = ctx.signal(...);
    var footerState = ctx.signal(...);
    
    // 注意：细粒度系统中，headerState 变化 **不会** 导致整个重渲染
    // 但这种写法有以下问题：
    // - 状态逻辑耦合，难以追踪
    // - 组件职责不清晰
    // - 难以复用 Header/Content/Footer
    
    return Column(() -> {
        Header(headerState);
        Content(contentState);
        Footer(footerState);
    });
});

// ✅ 拆分为独立组件，各自管理状态
// 好处：职责清晰、易于测试、可复用
Component headerComponent = Component.stateful(ctx -> {
    var headerState = ctx.signal(...);  // 状态封装在组件内
    return Header(headerState);
});

Component app = Component.stateless(ctx -> 
    Column(() -> {
        Child(headerComponent);   // 独立状态，独立生命周期
        Child(contentComponent);  // 独立状态，独立生命周期
        Child(footerComponent);   // 独立状态，独立生命周期
    })
);
```

> **澄清**：在细粒度响应式系统中，Signal 变化只触发依赖它的订阅者更新，不会重新执行整个组件的 render 函数。拆分组件的主要好处是**代码组织**和**可维护性**，而非性能（性能已经是细粒度的了）。

### 实践 4：使用 Computed 缓存计算

```java
Component list = Component.stateful(ctx -> {
    var items = ctx.signal(List.<Item>of());
    var filter = ctx.signal("");
    
    // ❌ 每次访问都重新过滤
    // Text(() -> "Count: " + items.get().stream()
    //     .filter(i -> i.name().contains(filter.get()))
    //     .count())
    
    // ✅ 使用 Computed 缓存
    var filteredItems = ctx.computed(() -> 
        items.get().stream()
            .filter(i -> i.name().contains(filter.get()))
            .toList()
    );
    
    var filteredCount = ctx.computed(() -> filteredItems.get().size());
    
    return Column(() -> {
        Text(() -> "Count: " + filteredCount.get());  // 使用缓存的计算
        // ...
    });
});
```

---

## 常见问题与解答

### Q1: 细粒度更新如何知道更新哪个 Element？

**A**: 通过 Tracker 的依赖追踪：

```java
Text(() -> count.get())
// 挂载时：
// 1. TextElement 调用 Tracker.track(this, supplier)
// 2. supplier 执行，count.get() 被调用
// 3. Tracker.trackAccess(count) 记录 TextElement 是 count 的订阅者
// 4. count.subscribers 现在包含 TextElement

// count.set(1) 时：
// 1. count.set(1) 检测到变化
// 2. Tracker.notify(count)
// 3. 遍历 count.subscribers，调用每个订阅者的 invalidate()
// 4. TextElement.invalidate() 被调用
```

### Q2: 如果一个 Supplier 依赖多个 Signal，会更新多次吗？

**A**: 使用批处理避免：

```java
var a = ctx.signal(1);
var b = ctx.signal(2);

Text(() -> a.get() + b.get())  // 依赖 a 和 b

// 同时更新 a 和 b
batch(() -> {
    a.set(10);  // 不立即通知
    b.set(20);  // 不立即通知
});
// 批量结束，只通知一次

// 如果不用 batch，会触发两次更新
```

### Q3: 细粒度更新和 Virtual DOM 可以结合吗？

**A**: 可以。CloudLib 的设计是：
- **叶子节点（Text, Image 等）**：细粒度更新
- **结构变化（条件切换、列表变化）**：局部协调

这是最佳组合，兼顾性能和灵活性。

### Q4: 怎么调试细粒度更新？

**A**: 添加追踪日志：

```java
// 开发模式下的调试
ctx.effect(() -> {
    System.out.println("[Update] Text content: " + count.get());
    return null;
});

// 或使用框架提供的调试工具
Tracker.setDebugMode(true);
// 会打印所有依赖追踪和通知
```

---

## 下一步

现在你已经理解了细粒度响应式更新，接下来学习样式系统：

➡️ **[10-style-system.md](./10-style-system.md)** - 样式系统设计

