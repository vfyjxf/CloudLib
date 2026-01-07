# CloudLib 响应式 UI 系统设计文档

## 概述

CloudLib 的响应式 UI 系统是一个**细粒度响应式**框架，灵感来源于多个现代前端框架：

| 设计灵感 | 来源 |
|---------|------|
| **Signal/Computed** | Vue 3 Reactivity、SolidJS |
| **Hooks 风格 API** | React Hooks |
| **声明式 DSL** | Jetpack Compose |
| **Element/Widget 分离** | Flutter |
| **依赖追踪** | Vue 3、MobX |

### 核心优势

- **细粒度更新**：只有读取了变化 Signal 的组件才会重建
- **自动依赖追踪**：无需手动声明依赖，框架自动追踪
- **声明式 API**：类似 Compose 的 DSL 语法
- **状态持久化**：Hook 式 API 保证状态跨重建保持

---

## 核心概念

### 1. Signal - 响应式状态

Signal 是最基本的响应式单元，持有可变值并在变化时通知订阅者。

```java
// 创建
Signal<Integer> count = Signal.of(0);

// 读取（触发依赖追踪）
int value = count.get();

// 读取（不触发追踪）
int value = count.peek();

// 设置
count.set(5);

// 更新
count.update(n -> n + 1);

// 订阅
count.subscribe(newValue -> System.out.println("Changed: " + newValue));
```

### 2. Computed - 派生计算

Computed 从其他响应式状态派生值，自动追踪依赖并缓存结果。

```java
Signal<Integer> count = Signal.of(5);
Signal<Integer> multiplier = Signal.of(2);

// 自动追踪 count 和 multiplier
Computed<Integer> result = Computed.of(() -> count.get() * multiplier.get());

result.get(); // 10

count.set(10);
result.get(); // 20 (自动重新计算)
```

### 3. Tracker - 依赖追踪

Tracker 是统一的依赖追踪系统，Signal.get() 内部会调用它。

```java
// 手动追踪
try (var scope = Tracker.start()) {
    signal1.get();
    signal2.get();
    
    // 获取追踪到的依赖
    Set<ReactiveState<?>> deps = scope.captured();
}

// 自定义追踪回调
try (var scope = Tracker.start(state -> handleDependency(state))) {
    // ...
}
```

### 4. Component - UI 组件

Component 是可复用的 UI 构建块，有三种类型：

```java
// Pure - 静态内容
Component header = Component.pure(Render.text("Hello"));

// Stateless - 无状态，依赖外部数据
Component greeting = Component.stateless(ctx -> {
    String name = userName.get(); // 读取外部 Signal
    return Render.text("Hello, " + name);
});

// Stateful - 有状态，使用 Hooks
Component counter = Component.stateful(ctx -> {
    // Hooks - 必须保持调用顺序一致
    var count = ctx.signal(0);           // 创建本地状态
    var doubled = ctx.computed(() -> count.get() * 2);
    
    ctx.effect(() -> {                   // 副作用
        System.out.println("Count: " + count.get());
        return () -> { /* cleanup */ };
    });
    
    return Render.column(
        Render.text(() -> "Count: " + count.get()),
        Render.button("+", () -> count.update(n -> n + 1))
    );
});
```

### 5. ComponentContext - Hook API

在 Stateful 组件中可用的 Hook：

| Hook | 用途 |
|------|------|
| `ctx.signal(initialValue)` | 创建本地响应式状态 |
| `ctx.computed(fn)` | 创建派生计算 |
| `ctx.memo(fn, deps)` | 带依赖的值缓存 |
| `ctx.effect(fn)` | 副作用（返回 cleanup） |
| `ctx.effect(fn, deps)` | 带依赖的副作用 |
| `ctx.onMount(fn)` | 挂载回调 |
| `ctx.onUnmount(fn)` | 卸载回调 |
| `ctx.ref(name)` | 可变引用 |

---

## 声明式 UI DSL

### Compose 风格（推荐）

```java
import static dev.vfyjxf.cloudlib.api.ui.reactive.Render.*;

// 使用隐式作用域
RenderNode ui = Column(() -> {
    Text("Header");
    Spacer(8);
    
    Row(() -> {
        Button("A", () -> handleA());
        Button("B", () -> handleB());
    });
    
    // 响应式文本
    Text(() -> "Count: " + count.get());
    
    // 嵌入组件
    Embed(myComponent);
});
```

### Builder 风格

```java
RenderNode ui = Render.column(col -> {
    col.text("Header");
    col.spacing(8);
    col.row(row -> {
        row.button("A", () -> handleA());
        row.button("B", () -> handleB());
    });
});
```

### Varargs 风格

```java
RenderNode ui = Render.column(
    Render.text("A"),
    Render.text("B"),
    Render.button("Click", onClick)
);
```

---

## 条件渲染与列表

### 条件渲染

```java
// if-else
RenderNode.when(
    () -> isLoggedIn.get(),
    Render.text("Welcome!"),
    Render.text("Please login")
)

// show if
RenderNode.showWhen(
    () -> showDetails.get(),
    detailsComponent
)
```

### 列表渲染

```java
// 基本列表
RenderNode.forEach(
    () -> items.get(),
    (item, index) -> Render.text(item.name())
)

// 带 Key 的列表（推荐，支持重排序优化）
RenderNode.forEach(
    () -> items.get(),
    item -> item.id(),  // key extractor
    (item, index) -> RenderNode.component(
        item.id(),      // component key
        itemComponent(item)
    )
)
```

---

## 样式系统

### Property 风格

```java
import static dev.vfyjxf.cloudlib.api.ui.reactive.Style.*;

Style style = Style.of(
    padding(8, 16),
    background(0x4488FF),
    color(0xFFFFFF),
    rounded(4)
);

Render.text("Styled", style);
```

### Builder 风格

```java
Style style = Style.builder()
    .padding(8, 16)
    .background(0x4488FF)
    .color(0xFFFFFF)
    .rounded(4)
    .bold()
    .build();
```

### 样式组合

```java
Style base = Style.of(padding(8), rounded(4));
Style hover = base.with(background(0x5599FF));
```

---

## ElementTree - 树管理

ElementTree 管理 UI 元素树的生命周期和更新调度。

```java
// 创建树
ElementTree tree = new ElementTree();

// 挂载根节点
tree.attachRoot(buildUI());

// 每帧刷新（处理状态变更）
void render() {
    tree.flushBuild();  // 批量处理所有脏节点
    // ... 实际渲染
}

// 卸载
tree.detach();
```

---

## Key 的使用

### 什么时候需要 Key？

| 场景 | 需要 Key? |
|------|-----------|
| 静态固定布局 | ❌ |
| 动态列表 | ✅ |
| 可重排序列表 | ✅✅ |
| 条件切换同类型组件 | ⚠️ 看情况 |

### 示例

```java
// ❌ 不需要 - 固定位置
column(
    RenderNode.component(header),
    RenderNode.component(content),
    RenderNode.component(footer)
)

// ✅ 需要 - 动态列表
RenderNode.forEach(
    items::get,
    item -> item.id(),  // key
    (item, i) -> RenderNode.component(item.id(), itemComp)
)
```

---

## 架构图

```
┌─────────────────────────────────────────────────────────┐
│                    Application                          │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌──────────┐     ┌──────────┐     ┌──────────┐        │
│  │  Signal  │────▶│ Computed │────▶│   UI     │        │
│  │  (State) │     │ (Derive) │     │(Render)  │        │
│  └──────────┘     └──────────┘     └──────────┘        │
│       │                │                │               │
│       └────────────────┼────────────────┘               │
│                        │                                │
│                   ┌────▼────┐                          │
│                   │ Tracker │  (Dependency Tracking)   │
│                   └────┬────┘                          │
│                        │                                │
├────────────────────────┼────────────────────────────────┤
│                        │                                │
│  ┌─────────────────────▼─────────────────────────┐     │
│  │              ElementTree                       │     │
│  │  ┌─────────────────────────────────────────┐  │     │
│  │  │ ComponentElement (stateful)              │  │     │
│  │  │   ├── signals[]                         │  │     │
│  │  │   ├── computeds[]                       │  │     │
│  │  │   ├── effects[]                         │  │     │
│  │  │   └── trackedDependencies               │  │     │
│  │  ├─────────────────────────────────────────┤  │     │
│  │  │ GroupElement                             │  │     │
│  │  │ LeafElement                              │  │     │
│  │  │ ConditionalElement                       │  │     │
│  │  │ ForEachElement                           │  │     │
│  │  └─────────────────────────────────────────┘  │     │
│  └───────────────────────────────────────────────┘     │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## 完整示例

```java
public class TodoApp {
    
    // 全局状态
    private final Signal<List<Todo>> todos = Signal.of(new ArrayList<>());
    private final Signal<String> filter = Signal.of("all");
    
    public RenderNode build() {
        return Column(() -> {
            // 标题
            Text("Todo App", Style.builder().fontSize(24).bold().build());
            Spacer(16);
            
            // 添加按钮
            Button("+ Add", () -> addTodo(), Style.of(padding(8, 16)));
            Spacer(8);
            
            // 过滤器
            Child(buildFilterBar());
            Spacer(8);
            
            // Todo 列表
            Child(buildTodoList());
            
            // 统计
            Child(buildStats());
        });
    }
    
    private RenderNode buildFilterBar() {
        return RenderNode.component("filter", Component.stateful(ctx -> {
            return Row(() -> {
                for (String f : List.of("all", "active", "completed")) {
                    boolean active = filter.get().equals(f);
                    Button(f, () -> filter.set(f), 
                        Style.builder()
                            .background(active ? 0x4488FF : 0x666666)
                            .color(0xFFFFFF)
                            .padding(4, 8)
                            .margin(0, 4)
                            .build());
                }
            });
        }));
    }
    
    private RenderNode buildTodoList() {
        return RenderNode.component("list", Component.stateful(ctx -> {
            // 过滤后的列表（computed）
            var filtered = ctx.computed(() -> {
                String f = filter.get();
                return todos.get().stream()
                    .filter(t -> switch (f) {
                        case "active" -> !t.done();
                        case "completed" -> t.done();
                        default -> true;
                    })
                    .toList();
            });
            
            return RenderNode.forEach(
                filtered::get,
                Todo::id,
                (todo, i) -> buildTodoItem(todo)
            );
        }));
    }
    
    private RenderNode buildTodoItem(Todo todo) {
        return RenderNode.component(todo.id(), Component.stateful(ctx -> {
            return Row(() -> {
                Button(todo.done() ? "☑" : "☐", 
                    () -> toggleTodo(todo.id()));
                Spacer(8);
                Text(todo.text(), Style.builder()
                    .strikethrough(todo.done())
                    .color(todo.done() ? 0x888888 : 0x333333)
                    .build());
            });
        }));
    }
    
    private RenderNode buildStats() {
        return RenderNode.component("stats", Component.stateless(ctx -> {
            int total = todos.get().size();
            int done = (int) todos.get().stream().filter(Todo::done).count();
            return Text(() -> String.format("Done: %d / %d", done, total),
                Style.of(color(0x666666)));
        }));
    }
    
    record Todo(int id, String text, boolean done) {}
}
```

---

## 最佳实践

### ✅ DO

1. **保持 Hook 调用顺序一致**
   ```java
   // ✅ Good
   var a = ctx.signal(0);
   var b = ctx.signal("");
   ```

2. **使用 Computed 避免重复计算**
   ```java
   var filtered = ctx.computed(() -> items.get().stream()...);
   ```

3. **动态列表使用 Key**
   ```java
   RenderNode.forEach(items::get, Item::id, ...)
   ```

4. **Effect 返回 cleanup**
   ```java
   ctx.effect(() -> {
       var sub = source.subscribe(...);
       return () -> sub.unsubscribe();
   });
   ```

### ❌ DON'T

1. **条件性调用 Hook**
   ```java
   // ❌ Bad - 破坏 Hook 顺序
   if (condition) {
       var x = ctx.signal(0);
   }
   ```

2. **在渲染中修改状态**
   ```java
   // ❌ Bad - 导致无限循环
   Component.stateful(ctx -> {
       count.set(count.get() + 1);  // 💥
       return ...;
   });
   ```

3. **忘记依赖追踪**
   ```java
   // ❌ Bad - peek() 不追踪
   Text(() -> "Count: " + count.peek());  // 不会更新！
   ```

---

## API 快速参考

### Signal

| 方法 | 描述 |
|------|------|
| `Signal.of(value)` | 创建 |
| `get()` | 读取（追踪） |
| `peek()` | 读取（不追踪） |
| `set(value)` | 设置 |
| `update(fn)` | 函数更新 |
| `subscribe(fn)` | 订阅变化 |

### Component

| 方法 | 描述 |
|------|------|
| `Component.pure(node)` | 静态组件 |
| `Component.stateless(fn)` | 无状态组件 |
| `Component.stateful(fn)` | 有状态组件 |

### Render DSL

| 方法 | 描述 |
|------|------|
| `Column(block)` | 垂直布局 |
| `Row(block)` | 水平布局 |
| `Stack(block)` | 层叠布局 |
| `Text(string)` | 静态文本 |
| `Text(supplier)` | 响应式文本 |
| `Button(label, onClick)` | 按钮 |
| `Spacer(size)` | 间距 |
| `Child(node)` | 嵌入节点 |
| `Embed(component)` | 嵌入组件 |

### RenderNode

| 方法 | 描述 |
|------|------|
| `component(comp)` | 组件引用 |
| `component(key, comp)` | 带 Key 的组件 |
| `when(cond, then, else)` | 条件渲染 |
| `showWhen(cond, node)` | 条件显示 |
| `forEach(items, render)` | 列表渲染 |
| `forEach(items, key, render)` | 带 Key 列表 |
| `dynamic(supplier)` | 动态节点 |

---

## 版本信息

- **框架版本**: CloudLib Reactive UI 1.0
- **设计灵感**: Vue 3, React, Jetpack Compose, Flutter, SolidJS
- **目标平台**: Minecraft NeoForge

