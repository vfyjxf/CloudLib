# 03. Tracker - 依赖追踪系统

## 目录

- [概述](#概述)
- [为什么需要统一的 Tracker](#为什么需要统一的-tracker)
- [Tracker 的工作原理](#tracker-的工作原理)
- [API 详解](#api-详解)
- [实现细节](#实现细节)
- [使用场景](#使用场景)
- [常见问题与解答](#常见问题与解答)

---

## 概述

**Tracker** 是响应式系统的核心引擎。它负责：
1. 追踪依赖关系
2. 管理订阅者
3. 在值变化时通知依赖者

### 核心概念

```
┌─────────────────────────────────────────────────────────┐
│                     Tracker                             │
│                                                         │
│   ┌─────────────┐    track()    ┌─────────────┐        │
│   │   Signal    │──────────────▶│  Computed   │        │
│   │   (source)  │               │ (subscriber)│        │
│   └─────────────┘               └─────────────┘        │
│         │                              │               │
│         │ notify()                     │ invalidate() │
│         ▼                              ▼               │
│   ┌─────────────────────────────────────────────────┐  │
│   │              Dependency Graph                    │  │
│   │                                                  │  │
│   │   Signal A ──┬──▶ Computed X ──▶ Computed Z     │  │
│   │              └──▶ Computed Y                     │  │
│   │   Signal B ──────▶ Computed Y                    │  │
│   └─────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

---

## 为什么需要统一的 Tracker

### 设计演进

**最初的设计（分散的依赖追踪）：**

```java
// 每个 Signal 自己管理订阅者
public class Signal<T> {
    private final List<Runnable> subscribers = new ArrayList<>();
    private static Signal<?> currentlyTracking;
    
    public T get() {
        if (currentlyTracking != null) {
            // ???  Signal 怎么知道谁在追踪？
        }
        return value;
    }
}

// 每个 Computed 自己追踪依赖
public class Computed<T> {
    private final List<Signal<?>> dependencies = new ArrayList<>();
    
    public T get() {
        // 需要设置全局变量来追踪
        // 问题：多个 Computed 嵌套怎么办？
    }
}
```

**问题：**
1. 全局状态分散，难以管理
2. 嵌套追踪难以实现
3. 循环依赖检测困难
4. 批量更新难以协调

**统一的 Tracker 设计：**

```java
// 所有依赖追踪逻辑集中在一处
public final class Tracker {
    // 唯一的追踪入口
    public static <T> T track(ReactiveState<?> subscriber, Supplier<T> fn) {
        // 集中管理追踪上下文
    }
    
    // 唯一的通知入口
    public static void notify(ReactiveState<?> source) {
        // 集中管理通知逻辑
    }
}
```

**优势：**
1. **单一职责**：Tracker 专注于依赖追踪
2. **清晰的上下文**：使用栈管理嵌套追踪
3. **全局可见性**：可以检测循环依赖
4. **批量优化**：可以延迟/合并通知

---

## Tracker 的工作原理

### 追踪流程

当 Computed 计算其值时：

```java
Computed<Integer> sum = Computed.of(() -> a.get() + b.get());
```

内部流程：

```
1. sum.get() 被调用
   
2. sum 调用 Tracker.track(sum, supplier)
   ┌─────────────────────────────────┐
   │ Tracker.track(sum, () -> ...)  │
   │   ├─ 将 sum 压入追踪栈         │
   │   ├─ 清空 sum 的旧依赖         │
   │   ├─ 执行 supplier             │
   │   │    └─ a.get() 被调用       │
   │   │         └─ a 调用 Tracker.track(a) │
   │   │              └─ 检测到栈顶是 sum   │
   │   │              └─ 将 sum 添加为 a 的订阅者 │
   │   │    └─ b.get() 被调用       │
   │   │         └─ ... 同上        │
   │   └─ 将 sum 弹出追踪栈         │
   └─────────────────────────────────┘

3. 结果：
   - a.subscribers = [sum]
   - b.subscribers = [sum]
   - sum.dependencies = [a, b]
```

### 通知流程

当 Signal 值变化时：

```java
a.set(10);
```

内部流程：

```
1. a.set(10) 被调用
   
2. a 调用 Tracker.notify(a)
   ┌─────────────────────────────────┐
   │ Tracker.notify(a)              │
   │   ├─ 获取 a 的所有订阅者        │
   │   │    └─ subscribers = [sum]  │
   │   ├─ 对每个订阅者:             │
   │   │    └─ sum.invalidate()     │
   │   │         └─ 标记 sum 为失效  │
   │   └─ 结束                       │
   └─────────────────────────────────┘

3. 下次 sum.get() 时：
   - 检测到已失效
   - 重新执行计算
   - 重新追踪依赖
```

### 嵌套追踪

当 Computed 依赖另一个 Computed 时：

```java
Signal<Integer> x = Signal.of(1);
Computed<Integer> doubled = Computed.of(() -> x.get() * 2);
Computed<Integer> quadrupled = Computed.of(() -> doubled.get() * 2);
```

追踪栈变化：

```
quadrupled.get()
│
├─ Tracker.track(quadrupled, ...)
│  └─ 栈: [quadrupled]
│
│  doubled.get()
│  │
│  ├─ Tracker.track(doubled, ...)
│  │  └─ 栈: [quadrupled, doubled]
│  │
│  │  x.get()
│  │  │
│  │  └─ Tracker.track(x)
│  │     └─ 栈顶是 doubled
│  │     └─ x -> doubled 依赖建立
│  │
│  └─ 弹出 doubled
│     └─ 栈: [quadrupled]
│
│  └─ doubled -> quadrupled 依赖建立
│
└─ 弹出 quadrupled
   └─ 栈: []

结果：
  x.subscribers = [doubled]
  doubled.subscribers = [quadrupled]
```

---

## API 详解

### track() - 建立追踪上下文

```java
public static <T> T track(ReactiveState<?> subscriber, Supplier<T> fn)
```

**参数：**
- `subscriber`: 当前正在计算的响应式状态（通常是 Computed）
- `fn`: 计算函数

**返回：**
- 计算函数的返回值

**行为：**
1. 将 subscriber 压入追踪栈
2. 清空 subscriber 的旧依赖
3. 执行 fn
4. 将 subscriber 弹出追踪栈
5. 返回 fn 的结果

**使用示例：**

```java
public class Computed<T> implements ReactiveState<T> {
    @Override
    public T get() {
        if (dirty) {
            cachedValue = Tracker.track(this, supplier);
            dirty = false;
        }
        return cachedValue;
    }
}
```

### trackAccess() - 记录读取

```java
public static void trackAccess(ReactiveState<?> source)
```

**参数：**
- `source`: 被读取的响应式状态

**行为：**
1. 检查追踪栈是否非空
2. 如果非空，将栈顶添加为 source 的订阅者

**使用示例：**

```java
public class Signal<T> implements ReactiveState<T> {
    @Override
    public T get() {
        Tracker.trackAccess(this);  // 记录这次读取
        return value;
    }
}
```

### notify() - 通知变化

```java
public static void notify(ReactiveState<?> source)
```

**参数：**
- `source`: 值已变化的响应式状态

**行为：**
1. 获取 source 的所有订阅者
2. 对每个订阅者调用 invalidate()

**使用示例：**

```java
public class Signal<T> implements ReactiveState<T> {
    public void set(T newValue) {
        if (!Objects.equals(value, newValue)) {
            value = newValue;
            Tracker.notify(this);  // 通知订阅者
        }
    }
}
```

### isTracking() - 检查追踪状态

```java
public static boolean isTracking()
```

**返回：**
- 如果当前在追踪上下文中，返回 true

**使用场景：**

```java
// 条件追踪：只在追踪上下文中记录依赖
public T get() {
    if (Tracker.isTracking()) {
        Tracker.trackAccess(this);
    }
    return value;
}
```

### getCurrentSubscriber() - 获取当前订阅者

```java
public static @Nullable ReactiveState<?> getCurrentSubscriber()
```

**返回：**
- 当前追踪栈的栈顶，如果栈空则返回 null

---

## 实现细节

### 追踪栈的实现

```java
public final class Tracker {
    // 使用 ThreadLocal 支持多线程
    private static final ThreadLocal<Deque<ReactiveState<?>>> trackingStack = 
        ThreadLocal.withInitial(ArrayDeque::new);
    
    public static <T> T track(ReactiveState<?> subscriber, Supplier<T> fn) {
        Deque<ReactiveState<?>> stack = trackingStack.get();
        
        // 压栈
        stack.push(subscriber);
        
        // 清空旧依赖
        subscriber.clearDependencies();
        
        try {
            return fn.get();
        } finally {
            // 弹栈（即使发生异常）
            stack.pop();
        }
    }
    
    public static void trackAccess(ReactiveState<?> source) {
        Deque<ReactiveState<?>> stack = trackingStack.get();
        
        if (!stack.isEmpty()) {
            ReactiveState<?> subscriber = stack.peek();
            source.addSubscriber(subscriber);
            subscriber.addDependency(source);
        }
    }
}
```

### 为什么使用 ThreadLocal？

虽然 UI 通常在单线程中运行，但 ThreadLocal 提供了：
1. **线程安全**：不同线程有独立的追踪栈
2. **隔离性**：一个线程的追踪不影响其他线程
3. **测试友好**：测试可以在独立线程中运行

### 依赖关系的存储

```java
public interface ReactiveState<T> {
    // 依赖存储（双向引用）
    Set<ReactiveState<?>> getDependencies();
    Set<ReactiveState<?>> getSubscribers();
    
    void addDependency(ReactiveState<?> dependency);
    void addSubscriber(ReactiveState<?> subscriber);
    
    void clearDependencies();
    void removeSubscriber(ReactiveState<?> subscriber);
}
```

**为什么需要双向引用？**

```
Signal A ──▶ Computed B
       │
       └─ A.subscribers 包含 B（正向：A 知道谁依赖它）
          B.dependencies 包含 A（反向：B 知道它依赖谁）
```

正向引用：用于通知（A 变了，通知 B）
反向引用：用于清理（B 重算时，清理旧依赖）

---

## 使用场景

### 场景 1：手动追踪

虽然通常框架会自动追踪，但有时需要手动控制：

```java
// 创建自定义的响应式计算
public <T> T computeWithTracking(Supplier<T> fn) {
    var result = new Object() { T value; };
    
    // 创建一个临时的追踪目标
    var tracker = new SimpleReactiveState();
    
    result.value = Tracker.track(tracker, fn);
    
    // 现在 tracker.dependencies 包含了所有被读取的响应式状态
    return result.value;
}
```

### 场景 2：调试依赖关系

```java
// 打印依赖图
public static void debugDependencies(ReactiveState<?> state) {
    System.out.println("Dependencies of " + state + ":");
    for (var dep : state.getDependencies()) {
        System.out.println("  ← " + dep);
    }
    
    System.out.println("Subscribers of " + state + ":");
    for (var sub : state.getSubscribers()) {
        System.out.println("  → " + sub);
    }
}
```

### 场景 3：批量更新

```java
// 批量更新多个 Signal，只触发一次通知
public static void batch(Runnable fn) {
    // 暂停通知
    Tracker.beginBatch();
    try {
        fn.run();
    } finally {
        // 恢复并触发所有积累的通知
        Tracker.endBatch();
    }
}

// 使用
batch(() -> {
    a.set(1);  // 不立即通知
    b.set(2);  // 不立即通知
    c.set(3);  // 不立即通知
});
// 批量结束，一次性通知所有依赖者
```

---

## 常见问题与解答

### Q1: 循环依赖怎么处理？

**A**: Tracker 可以检测循环依赖：

```java
public static <T> T track(ReactiveState<?> subscriber, Supplier<T> fn) {
    Deque<ReactiveState<?>> stack = trackingStack.get();
    
    // 检测循环
    if (stack.contains(subscriber)) {
        throw new CircularDependencyException(
            "Circular dependency detected: " + subscriber);
    }
    
    stack.push(subscriber);
    // ...
}
```

### Q2: 为什么每次重算都要清空依赖？

**A**: 因为依赖是动态的！

```java
var mode = ctx.signal("A");
var a = ctx.signal(1);
var b = ctx.signal(2);

var result = ctx.computed(() -> {
    if (mode.get().equals("A")) {
        return a.get();  // 这次依赖 {mode, a}
    } else {
        return b.get();  // 下次可能依赖 {mode, b}
    }
});
```

如果不清空旧依赖，当 mode 从 "A" 变成 "B"：
- 旧依赖：{mode, a}
- 新依赖：{mode, b}
- 结果：{mode, a, b}（错误！a 不应该再是依赖）

### Q3: 追踪有性能开销吗？

**A**: 有，但很小：
- 栈操作：O(1)
- 依赖存储：O(1) 添加，O(n) 清空
- 在实际应用中，这些开销相比渲染开销可以忽略

如果需要优化，可以：
1. 使用 `peek()` 避免不必要的追踪
2. 在 Computed 外部缓存稳定值
3. 减少嵌套 Computed 的深度

### Q4: peek() 是如何绕过追踪的？

**A**: peek() 直接访问值，不调用 trackAccess()：

```java
public class Signal<T> {
    public T get() {
        Tracker.trackAccess(this);  // 建立依赖
        return value;
    }
    
    public T peek() {
        return value;  // 不建立依赖
    }
}
```

---

## 下一步

现在你已经理解了依赖追踪的原理，接下来学习组件如何使用这些响应式能力：

➡️ **[04-component.md](./04-component.md)** - 组件系统设计

