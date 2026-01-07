# 02. 响应式原语（Reactive Primitives）

## 目录

- [概述](#概述)
- [Signal - 响应式状态](#signal---响应式状态)
- [Computed - 派生计算](#computed---派生计算)
- [Effect - 副作用](#effect---副作用)
- [响应式原语的协作](#响应式原语的协作)
- [常见问题与解答](#常见问题与解答)

---

## 概述

响应式原语是整个系统的基础。它们是最小的响应式单元，所有复杂的响应式行为都由它们组合而成。

### 三种原语

| 原语 | 职责 | 类比 |
|------|------|------|
| **Signal** | 持有可变状态 | 变量 |
| **Computed** | 从其他状态派生值 | Excel 公式单元格 |
| **Effect** | 执行副作用 | 事件监听器 |

### 设计理念

这套设计来源于 **"响应式编程"** 范式，核心思想是：

> **当数据变化时，所有依赖该数据的计算和副作用自动执行。**

就像 Excel 表格：
- A1 单元格是 Signal（输入值）
- B1 = A1 * 2 是 Computed（派生公式）
- 当你修改 A1，B1 自动更新

---

## Signal - 响应式状态

### 什么是 Signal？

Signal 是一个**可观察的值容器**。它：
- 持有一个值
- 当值变化时通知所有订阅者
- 支持依赖追踪（被读取时记录依赖）

### 为什么需要 Signal？

普通变量的问题：

```java
int count = 0;
// ... 某处修改了 count
count = 5;
// 问题：没有人知道 count 变了！UI 不会更新
```

Signal 的解决方案：

```java
Signal<Integer> count = Signal.of(0);
// ... 某处修改 count
count.set(5);
// Signal 会通知所有订阅者，UI 自动更新
```

### 基础用法

#### 创建 Signal

```java
// 创建带初始值的 Signal
Signal<Integer> count = Signal.of(0);
Signal<String> name = Signal.of("Alice");
Signal<List<Item>> items = Signal.of(new ArrayList<>());

// 创建空 Signal（初始值为 null）
Signal<User> currentUser = Signal.empty();
```

**为什么用静态工厂而不是构造函数？**
- 更清晰的语义：`Signal.of(0)` 比 `new Signal<>(0)` 更易读
- 未来可以返回特化实现（如 `IntSignal`）
- 可以实现缓存/享元模式

#### 读取值

```java
Signal<Integer> count = Signal.of(0);

// get() - 读取并追踪依赖
int value = count.get();
// 如果在 Tracker 作用域内，当前作用域会被记录为依赖者

// peek() - 只读取，不追踪
int value = count.peek();
// 即使在 Tracker 作用域内，也不会记录依赖
```

**什么时候用 `get()` vs `peek()`？**

| 场景 | 使用 | 原因 |
|------|------|------|
| UI 显示 | `get()` | 需要在值变化时更新 UI |
| 计算派生值 | `get()` | 需要在依赖变化时重算 |
| 日志输出 | `peek()` | 只是读取，不想触发重建 |
| 调试 | `peek()` | 避免影响正常行为 |
| 条件判断（不影响 UI） | `peek()` | 如果条件结果不影响渲染 |

**案例：日志不应该建立依赖**

```java
Component debugPanel = Component.stateful(ctx -> {
    var count = ctx.signal(0);
    
    ctx.effect(() -> {
        // ❌ 错误：使用 get() 会导致每次 count 变化都重新执行 effect
        System.out.println("Current count: " + count.get());
        return null;
    });
    
    ctx.effect(() -> {
        // ✅ 正确：peek() 只是读取当前值，不建立依赖
        // 这个 effect 只在挂载时执行一次
        System.out.println("Initial count: " + count.peek());
        return null;
    });
    
    return Render.text(() -> "Count: " + count.get());
});
```

#### 修改值

```java
Signal<Integer> count = Signal.of(0);

// set() - 直接设置新值
count.set(5);
// 返回旧值
int oldValue = count.set(10);  // oldValue = 5

// update() - 基于当前值更新
count.update(n -> n + 1);  // count 变成 11
// 返回新值
int newValue = count.update(n -> n * 2);  // newValue = 22

// 只有值真正变化时才通知
count.set(22);  // 值没变，不会通知订阅者
```

**set() vs update() 的选择：**

```java
// ✅ 用 set()：当你知道确切的新值
count.set(0);  // 重置
name.set(newName);  // 从外部获取的值

// ✅ 用 update()：当新值依赖当前值
count.update(n -> n + 1);  // 递增
items.update(list -> {
    var newList = new ArrayList<>(list);
    newList.add(newItem);
    return newList;
});
```

#### 订阅变化

```java
Signal<Integer> count = Signal.of(0);

// 订阅变化
ReactiveState.Subscription sub = count.subscribe(newValue -> {
    System.out.println("Count changed to: " + newValue);
});

count.set(5);  // 输出: Count changed to: 5

// 取消订阅
sub.unsubscribe();

count.set(10);  // 不再输出

// 订阅时也获取旧值
count.subscribeWithOld((oldValue, newValue) -> {
    System.out.println("Changed from " + oldValue + " to " + newValue);
});
```

### 完整案例：购物车计数器

```java
public class ShoppingCart {
    // 状态
    private final Signal<List<CartItem>> items = Signal.of(new ArrayList<>());
    
    // 添加商品
    public void addItem(Product product) {
        items.update(list -> {
            var newList = new ArrayList<>(list);
            
            // 查找是否已存在
            for (int i = 0; i < newList.size(); i++) {
                if (newList.get(i).product().id() == product.id()) {
                    // 增加数量
                    var old = newList.get(i);
                    newList.set(i, new CartItem(product, old.quantity() + 1));
                    return newList;
                }
            }
            
            // 新商品
            newList.add(new CartItem(product, 1));
            return newList;
        });
    }
    
    // 移除商品
    public void removeItem(int productId) {
        items.update(list -> list.stream()
            .filter(item -> item.product().id() != productId)
            .toList());
    }
    
    // 获取状态（供 UI 使用）
    public Signal<List<CartItem>> getItems() {
        return items;
    }
    
    record CartItem(Product product, int quantity) {}
}
```

---

## Computed - 派生计算

### 什么是 Computed？

Computed 是一个**自动计算的派生值**。它：
- 根据其他响应式状态计算值
- 自动追踪依赖
- 缓存结果，只在依赖变化时重算
- 本身也是响应式的（可以被其他 Computed 依赖）

### 为什么需要 Computed？

问题：派生值的同步

```java
int price = 100;
int quantity = 2;
int total = price * quantity;  // 200

quantity = 3;
// 问题：total 还是 200！必须手动重算
total = price * quantity;  // 300
```

Computed 的解决方案：

```java
Signal<Integer> price = Signal.of(100);
Signal<Integer> quantity = Signal.of(2);
Computed<Integer> total = Computed.of(() -> price.get() * quantity.get());

total.get();  // 200

quantity.set(3);
total.get();  // 300 - 自动重算！
```

### 基础用法

#### 创建 Computed

```java
Signal<Integer> count = Signal.of(5);

// 从单个 Signal 派生
Computed<String> countText = Computed.of(() -> "Count: " + count.get());

// 从多个 Signal 派生
Signal<Integer> price = Signal.of(100);
Signal<Integer> quantity = Signal.of(2);
Computed<Integer> total = Computed.of(() -> price.get() * quantity.get());

// 从其他 Computed 派生
Computed<String> totalText = Computed.of(() -> "Total: $" + total.get());
```

#### 读取值

```java
Computed<Integer> doubled = Computed.of(() -> count.get() * 2);

// get() - 获取值（懒计算 + 追踪）
int value = doubled.get();

// peek() - 获取值（懒计算，不追踪）
int value = doubled.peek();
```

**关键特性：懒计算 + 缓存**

```java
int[] computeCount = {0};

Computed<Integer> expensive = Computed.of(() -> {
    computeCount[0]++;
    return someExpensiveCalculation();
});

// 还没计算（懒）
assertEquals(0, computeCount[0]);

// 第一次访问，计算
expensive.get();
assertEquals(1, computeCount[0]);

// 第二次访问，使用缓存
expensive.get();
assertEquals(1, computeCount[0]);  // 没有重算！

// 依赖变化，下次访问时重算
count.set(10);
expensive.get();
assertEquals(2, computeCount[0]);  // 重算了
```

### 依赖追踪原理

Computed 如何知道它依赖哪些 Signal？

```java
Signal<Integer> a = Signal.of(1);
Signal<Integer> b = Signal.of(2);
Signal<Integer> c = Signal.of(3);

Computed<Integer> result = Computed.of(() -> {
    // 执行这个函数时，框架在"监听"
    int x = a.get();  // a 被记录为依赖
    int y = b.get();  // b 被记录为依赖
    // c 没有被调用，所以不是依赖
    return x + y;
});

// result 依赖 a 和 b，不依赖 c
a.set(10);  // result 失效，下次 get() 会重算
c.set(30);  // result 不受影响，c 不是依赖
```

**条件依赖的情况：**

```java
Signal<Boolean> useA = Signal.of(true);
Signal<Integer> a = Signal.of(1);
Signal<Integer> b = Signal.of(2);

Computed<Integer> result = Computed.of(() -> {
    if (useA.get()) {
        return a.get();  // 依赖 useA 和 a
    } else {
        return b.get();  // 依赖 useA 和 b
    }
});

// 当 useA = true 时，依赖是 {useA, a}
// 当 useA = false 时，依赖是 {useA, b}
// 依赖会在每次计算时重新确定！
```

### Computed 链

Computed 可以形成链式依赖：

```java
Signal<Integer> base = Signal.of(1);

Computed<Integer> doubled = Computed.of(() -> base.get() * 2);
Computed<Integer> quadrupled = Computed.of(() -> doubled.get() * 2);
Computed<Integer> plusTen = Computed.of(() -> quadrupled.get() + 10);

plusTen.get();  // (1 * 2 * 2) + 10 = 14

base.set(5);
plusTen.get();  // (5 * 2 * 2) + 10 = 30
```

**失效传播：**
```
base.set(5)
    ↓
doubled 失效
    ↓
quadrupled 失效
    ↓
plusTen 失效
    ↓
下次 get() 时重算
```

### 完整案例：购物车统计

```java
public class CartViewModel {
    private final Signal<List<CartItem>> items;
    
    // 派生：总数量
    private final Computed<Integer> totalQuantity;
    
    // 派生：总价
    private final Computed<Double> totalPrice;
    
    // 派生：是否为空
    private final Computed<Boolean> isEmpty;
    
    // 派生：折扣后价格
    private final Computed<Double> discountedPrice;
    
    public CartViewModel(Signal<List<CartItem>> items) {
        this.items = items;
        
        this.totalQuantity = Computed.of(() -> 
            items.get().stream()
                .mapToInt(CartItem::quantity)
                .sum()
        );
        
        this.totalPrice = Computed.of(() ->
            items.get().stream()
                .mapToDouble(item -> item.product().price() * item.quantity())
                .sum()
        );
        
        this.isEmpty = Computed.of(() -> items.get().isEmpty());
        
        // 链式：discountedPrice 依赖 totalPrice
        this.discountedPrice = Computed.of(() -> {
            double total = totalPrice.get();
            // 满 100 打 9 折
            return total >= 100 ? total * 0.9 : total;
        });
    }
    
    // 暴露给 UI
    public Computed<Integer> getTotalQuantity() { return totalQuantity; }
    public Computed<Double> getTotalPrice() { return totalPrice; }
    public Computed<Boolean> getIsEmpty() { return isEmpty; }
    public Computed<Double> getDiscountedPrice() { return discountedPrice; }
}
```

UI 使用：

```java
Component cartSummary = Component.stateless(ctx -> {
    var vm = ctx.provide(CartViewModel.class);
    
    return Column(() -> {
        // 这些 Text 只在各自的依赖变化时更新
        Text(() -> "Items: " + vm.getTotalQuantity().get());
        Text(() -> "Subtotal: $" + vm.getTotalPrice().get());
        Text(() -> "After discount: $" + vm.getDiscountedPrice().get());
        
        // 条件渲染
        Child(RenderNode.showWhen(
            () -> !vm.getIsEmpty().get(),
            Render.button("Checkout", () -> checkout())
        ));
    });
});
```

---

## Effect - 副作用

### 什么是 Effect？

Effect 是在响应式系统中**执行副作用**的机制。副作用包括：
- 打印日志
- 网络请求
- 本地存储
- DOM 操作（在我们的场景中是游戏状态更新）
- 订阅外部事件

### 为什么需要 Effect？

Computed 是**纯函数**，不应该有副作用：

```java
// ❌ 错误：Computed 不应该有副作用
Computed<Integer> bad = Computed.of(() -> {
    System.out.println("Computing...");  // 副作用！
    saveToFile(count.get());             // 副作用！
    return count.get() * 2;
});
```

Effect 专门处理副作用：

```java
// ✅ 正确：Effect 处理副作用
ctx.effect(() -> {
    int value = count.get();
    System.out.println("Count changed: " + value);
    saveToFile(value);
    return null;  // 或返回 cleanup 函数
});
```

### 在组件中使用 Effect

Effect 通常在组件的 `ComponentContext` 中使用：

```java
Component.stateful(ctx -> {
    var count = ctx.signal(0);
    
    // 基础 effect：每次 count 变化时执行
    ctx.effect(() -> {
        System.out.println("Count: " + count.get());
    });
    
    // 带 cleanup 的 effect
    ctx.effect(() -> {
        var subscription = eventBus.subscribe(e -> handleEvent(e));
        return () -> subscription.unsubscribe();  // cleanup
    });
    
    // 带依赖的 effect
    ctx.effect(() -> {
        System.out.println("Count: " + count.get());
        return null;
    }, count.get());  // 只在 count.get() 的值变化时执行
    
    return Render.text(() -> "Count: " + count.get());
});
```

### Effect 的执行时机

```
组件挂载 → effect 首次执行 → 返回 cleanup
           ↓
依赖变化 → cleanup 执行 → effect 重新执行 → 返回新 cleanup
           ↓
组件卸载 → cleanup 执行
```

### 完整案例：自动保存

```java
Component editor = Component.stateful(ctx -> {
    var content = ctx.signal("");
    var saveStatus = ctx.signal("saved");
    
    // 自动保存 effect
    ctx.effect(() -> {
        String text = content.get();
        
        // 防抖：500ms 后保存
        var timer = scheduleDelayed(500, () -> {
            saveToServer(text);
            saveStatus.set("saved");
        });
        
        saveStatus.set("saving...");
        
        // cleanup：取消之前的定时器
        return () -> timer.cancel();
    });
    
    return Column(() -> {
        // 编辑器
        TextInput(content);
        
        // 状态显示
        Text(() -> "Status: " + saveStatus.get());
    });
});
```

---

## 响应式原语的协作

### 典型模式

```
┌──────────────────────────────────────────────────────────┐
│                      User Action                         │
│                          │                               │
│                          ▼                               │
│  ┌─────────────────────────────────────────────────┐    │
│  │              Signal (source of truth)            │    │
│  │  count.update(n -> n + 1)                        │    │
│  └────────────────────┬────────────────────────────┘    │
│                       │                                  │
│          ┌────────────┼────────────┐                    │
│          ▼            ▼            ▼                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐              │
│  │ Computed │  │ Computed │  │  Effect  │              │
│  │ doubled  │  │ isEven   │  │  log()   │              │
│  └────┬─────┘  └────┬─────┘  └──────────┘              │
│       │             │                                   │
│       └──────┬──────┘                                   │
│              ▼                                          │
│  ┌─────────────────────────────────────────────────┐   │
│  │              UI (auto-update)                    │   │
│  │  Text(() -> "Count: " + count.get())            │   │
│  │  Text(() -> "Doubled: " + doubled.get())        │   │
│  └─────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────┘
```

### 案例：完整的计数器

```java
Component counter = Component.stateful(ctx -> {
    // === Signal: 源状态 ===
    var count = ctx.signal(0);
    var step = ctx.signal(1);
    
    // === Computed: 派生状态 ===
    var doubled = ctx.computed(() -> count.get() * 2);
    var canDecrement = ctx.computed(() -> count.get() > 0);
    var summary = ctx.computed(() -> 
        String.format("Count: %d (x2=%d), Step: %d", 
            count.get(), doubled.get(), step.get()));
    
    // === Effect: 副作用 ===
    ctx.effect(() -> {
        System.out.println("Count changed to: " + count.get());
        return null;
    });
    
    // === UI: 响应式渲染 ===
    return Column(() -> {
        // 动态文本
        Text(summary::get);
        
        Spacer(8);
        
        // 按钮
        Row(() -> {
            Button("-", () -> count.update(n -> n - step.get()));
            Button("Reset", () -> count.set(0));
            Button("+", () -> count.update(n -> n + step.get()));
        });
        
        Spacer(8);
        
        // 步长调整
        Row(() -> {
            Text("Step: ");
            Button("1", () -> step.set(1));
            Button("5", () -> step.set(5));
            Button("10", () -> step.set(10));
        });
    });
});
```

---

## 常见问题与解答

### Q1: Signal 是线程安全的吗？

**A**: 当前实现使用 `CopyOnWriteArrayList` 存储订阅者，读取是线程安全的。但写入（set/update）应该在主线程进行。如果需要跨线程更新，应该使用线程安全的调度机制。

### Q2: Computed 会自动更新吗？

**A**: Computed 是**懒计算**的。当依赖变化时，它只是被标记为"失效"，不会立即重算。只有下次调用 `get()` 时才会重算。这是一种优化，避免不必要的计算。

### Q3: 为什么 effect 需要返回 cleanup？

**A**: cleanup 用于释放资源，比如取消订阅、清除定时器等。如果不返回 cleanup：
- 订阅会泄漏
- 定时器会继续执行
- 可能导致内存泄漏或错误行为

### Q4: 什么时候用 Signal，什么时候用 Computed？

**A**: 
- **Signal**: 当值是"源头"，由用户输入或外部事件产生
- **Computed**: 当值可以从其他值计算出来

```java
// Signal: 用户输入
var username = ctx.signal("");
var password = ctx.signal("");

// Computed: 派生值
var isValid = ctx.computed(() -> 
    !username.get().isEmpty() && password.get().length() >= 6);
```

### Q5: Computed 的依赖是动态的吗？

**A**: 是的！每次 Computed 重算时，依赖会重新确定：

```java
var mode = ctx.signal("A");
var a = ctx.signal(1);
var b = ctx.signal(2);

var result = ctx.computed(() -> {
    if (mode.get().equals("A")) {
        return a.get();  // 依赖: mode, a
    } else {
        return b.get();  // 依赖: mode, b
    }
});
```

---

## 下一步

现在你已经理解了响应式原语，接下来学习它们如何工作：

➡️ **[03-tracker.md](./03-tracker.md)** - 依赖追踪系统的原理

