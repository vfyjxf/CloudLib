# 12. Best Practices - 最佳实践与常见陷阱

## 目录

- [概述](#概述)
- [状态管理最佳实践](#状态管理最佳实践)
- [组件设计最佳实践](#组件设计最佳实践)
- [性能最佳实践](#性能最佳实践)
- [代码组织最佳实践](#代码组织最佳实践)
- [常见陷阱](#常见陷阱)
- [调试技巧](#调试技巧)
- [检查清单](#检查清单)

---

## 概述

本章汇总了使用 CloudLib 响应式 UI 系统的最佳实践和需要避免的常见陷阱。

```
┌─────────────────────────────────────────────────────────┐
│                     最佳实践总览                        │
│                                                         │
│  ✅ DO                          ❌ DON'T                │
│  ────────────────────────────────────────────────────  │
│  • 最小化状态                   • 冗余状态              │
│  • 使用 Computed 派生          • 重复计算              │
│  • 正确使用 key                 • 用索引作为 key       │
│  • 细粒度组件                   • 巨型组件              │
│  • peek() 避免依赖             • 不必要的依赖          │
│  • 批量更新                     • 频繁单独更新          │
│  • 清理 effects                • 忘记 cleanup          │
│  • 类型安全的依赖注入          • 全局变量              │
└─────────────────────────────────────────────────────────┘
```

---

## 状态管理最佳实践

### ✅ DO: 最小化状态

只存储"源数据"，派生数据用 Computed。

```java
// ✅ 好：最小化状态
Component cart = Component.stateful(ctx -> {
    // 只存储原始数据
    var items = ctx.signal(List.<CartItem>of());
    
    // 派生计算
    var totalPrice = ctx.computed(() -> 
        items.get().stream()
            .mapToDouble(i -> i.price() * i.quantity())
            .sum()
    );
    
    var itemCount = ctx.computed(() -> 
        items.get().stream()
            .mapToInt(CartItem::quantity)
            .sum()
    );
    
    return Column(() -> {
        Text(() -> "Items: " + itemCount.get());
        Text(() -> "Total: $" + totalPrice.get());
    });
});

// ❌ 坏：冗余状态
Component cart = Component.stateful(ctx -> {
    var items = ctx.signal(List.<CartItem>of());
    var totalPrice = ctx.signal(0.0);    // 冗余！需要手动同步
    var itemCount = ctx.signal(0);        // 冗余！需要手动同步
    
    // 容易忘记更新，导致不同步
    // ...
});
```

### ✅ DO: 合理粒度的 Signal

```java
// ✅ 好：独立变化的数据分开
var firstName = ctx.signal("");
var lastName = ctx.signal("");
// firstName 变化不影响 lastName 的订阅者

// ❌ 坏：不相关的数据放一起
var user = ctx.signal(new User("", "", 0, ""));
// 任何属性变化都会触发所有订阅者

// ✅ 好（如果属性总是一起变化）：用对象
var position = ctx.signal(new Point(0, 0));
// x 和 y 总是一起更新
```

### ✅ DO: 使用 peek() 避免不必要的依赖

```java
Component logger = Component.stateful(ctx -> {
    var count = ctx.signal(0);
    
    // ✅ 好：peek() 不建立依赖
    ctx.onMount(() -> {
        System.out.println("Initial count: " + count.peek());
    });
    
    // ✅ 好：在 effect 中只追踪需要的依赖
    ctx.effect(() -> {
        int c = count.get();  // 追踪 count
        // 不需要追踪其他 signal 时使用 peek
        log("Count changed to " + c + ", user: " + user.peek().name());
        return null;
    });
    
    return Render.text(() -> count.get().toString());
});
```

### ✅ DO: 批量更新相关状态

```java
// ✅ 好：使用 batch 或单个 Signal
Component form = Component.stateful(ctx -> {
    // 方式 1：单个 Signal（如果总是一起更新）
    var formData = ctx.signal(new FormData("", "", 0));
    
    Runnable reset = () -> formData.set(new FormData("", "", 0));
    
    // 方式 2：batch 更新多个 Signal
    var name = ctx.signal("");
    var email = ctx.signal("");
    
    Runnable reset = () -> {
        // 批量更新，只触发一次重新渲染
        batch(() -> {
            name.set("");
            email.set("");
        });
    };
    
    return /* ... */;
});
```

---

## 组件设计最佳实践

### ✅ DO: 单一职责

```java
// ✅ 好：每个组件做一件事
Component userAvatar(Signal<User> user) { /* 只负责头像 */ }
Component userName(Signal<User> user) { /* 只负责名字 */ }
Component userBio(Signal<User> user) { /* 只负责简介 */ }

Component userCard(Signal<User> user) {
    return Component.stateless(ctx -> 
        Column(() -> {
            Child(userAvatar(user));
            Child(userName(user));
            Child(userBio(user));
        })
    );
}

// ❌ 坏：一个组件做太多事
Component userCard = Component.stateful(ctx -> {
    // 头像逻辑...
    // 名字逻辑...
    // 简介逻辑...
    // 编辑逻辑...
    // 关注逻辑...
    // 500 行代码...
});
```

### ✅ DO: 优先使用无状态组件

```java
// ✅ 好：能用 stateless 就用 stateless
Component listItem(Item item) {
    return Component.stateless(ctx -> 
        Row(() -> {
            Text(item.name());
            Text(item.price().toString());
        })
    );
}

// ❌ 不必要的 stateful
Component listItem(Item item) {
    return Component.stateful(ctx -> {  // 没用到任何状态！
        return Row(() -> {
            Text(item.name());
            Text(item.price().toString());
        });
    });
}
```

### ✅ DO: 合理的组件粒度

```java
// ✅ 好：有意义的组件边界
Component productPage = Component.stateless(ctx -> 
    Column(() -> {
        Child(productHeader);      // 独立组件
        Child(productGallery);     // 独立组件
        Child(productDetails);     // 独立组件
        Child(productReviews);     // 独立组件
    })
);

// ❌ 过细：每个文本都是组件
Component title(String text) {
    return Component.stateless(ctx -> Render.text(text));
}

// ❌ 过粗：整个页面一个组件
Component app = Component.stateful(ctx -> {
    // 1000 行代码...
});
```

### ✅ DO: 通过 props 传递数据，通过回调传递事件

```java
// ✅ 好：清晰的数据流
Component counter(Signal<Integer> count, Runnable onIncrement) {
    return Component.stateless(ctx -> 
        Row(() -> {
            Text(() -> "Count: " + count.get());
            Button("+", onIncrement);
        })
    );
}

// 使用
var count = ctx.signal(0);
Child(counter(count, () -> count.update(n -> n + 1)));
```

---

## 性能最佳实践

### ✅ DO: 使用正确的 key

```java
// ✅ 好：稳定、唯一的 key
RenderNode.forEach(
    items::get,
    Item::id,  // 数据库 ID
    item -> itemView(item)
);

// ❌ 坏：索引作为 key
int[] i = {0};
RenderNode.forEach(
    items::get,
    item -> i[0]++,
    item -> itemView(item)
);

// ❌ 坏：随机 key
RenderNode.forEach(
    items::get,
    item -> UUID.randomUUID(),
    item -> itemView(item)
);
```

### ✅ DO: 使用 Computed 缓存昂贵计算

```java
// ✅ 好：缓存过滤结果
var filteredItems = ctx.computed(() -> 
    items.get().stream()
        .filter(predicate)
        .sorted(comparator)
        .toList()
);

// 多次访问使用缓存
Text(() -> "Count: " + filteredItems.get().size());
RenderNode.forEach(filteredItems::get, Item::id, item -> itemView(item));

// ❌ 坏：每次访问都重新计算
Text(() -> "Count: " + items.get().stream().filter(predicate).count());
// 列表也重新过滤
RenderNode.forEach(
    () -> items.get().stream().filter(predicate).toList(),
    Item::id,
    item -> itemView(item)
);
```

### ✅ DO: 细粒度的响应式绑定

```java
// ✅ 好：只有动态部分是响应式的
Column(() -> {
    Text("Static Title");                     // 静态
    Text(() -> "Count: " + count.get());      // 只有这个更新
    Text("Static Footer");                    // 静态
});

// ❌ 坏：整个组件重建
Component.stateful(ctx -> {
    var count = ctx.signal(0);
    
    // count 变化导致整个 render 重新执行
    String title = "Title";
    String countText = "Count: " + count.get();
    String footer = "Footer";
    
    return Column(() -> {
        Text(title);
        Text(countText);
        Text(footer);
    });
});
```

### ✅ DO: 大列表使用虚拟化

```java
// ✅ 好：只渲染可见项
Component virtualList = Component.stateful(ctx -> {
    var items = ctx.signal(List.<Item>of());  // 10000 项
    var scrollOffset = ctx.signal(0);
    
    var visibleItems = ctx.computed(() -> {
        int startIndex = scrollOffset.get() / ITEM_HEIGHT;
        int endIndex = startIndex + VISIBLE_COUNT;
        return items.get().subList(
            Math.max(0, startIndex),
            Math.min(items.get().size(), endIndex)
        );
    });
    
    return ScrollView(
        onScroll(scrollOffset::set),
        RenderNode.forEach(visibleItems::get, Item::id, item -> itemView(item))
    );
});
```

---

## 代码组织最佳实践

### ✅ DO: 分离关注点

```
src/
├── components/           # UI 组件
│   ├── common/          # 通用组件
│   │   ├── Button.java
│   │   ├── Input.java
│   │   └── Card.java
│   └── features/        # 功能组件
│       ├── user/
│       └── product/
├── state/               # 状态管理
│   ├── signals/         # Signal 定义
│   └── computed/        # Computed 定义
├── services/            # 业务逻辑
├── styles/              # 样式定义
│   ├── Theme.java
│   └── Styles.java
└── utils/               # 工具函数
```

### ✅ DO: 使用工厂方法创建组件

```java
// ✅ 好：工厂方法，清晰的参数
public class Buttons {
    public static Component primary(String text, Runnable onClick) {
        return Component.stateful(ctx -> {
            var isHovered = ctx.signal(false);
            return Box(
                primaryStyle(isHovered.get()),
                onHover(isHovered::set),
                onClick(onClick),
                Text(text)
            );
        });
    }
    
    public static Component secondary(String text, Runnable onClick) { /* ... */ }
    public static Component danger(String text, Runnable onClick) { /* ... */ }
}

// 使用
Child(Buttons.primary("Submit", this::handleSubmit));
```

### ✅ DO: 集中管理样式

```java
// styles/Styles.java
public class Styles {
    public static final Style CARD = Style.builder()
        .padding(16)
        .background(0xFFFFFF)
        .borderRadius(8)
        .build();
    
    public static final Style TITLE = Style.builder()
        .fontSize(18)
        .fontWeight("bold")
        .build();
}

// 使用
Box(Styles.CARD, () -> {
    Text("Title", Styles.TITLE);
});
```

---

## 常见陷阱

### ❌ 陷阱 1：在条件或循环中使用 Hook

```java
// ❌ 错误：条件中的 Hook
Component bad = Component.stateful(ctx -> {
    if (someCondition) {
        var count = ctx.signal(0);  // Hook 顺序会变化！
    }
    return Render.empty();
});

// ✅ 正确：Hook 总在顶层
Component good = Component.stateful(ctx -> {
    var count = ctx.signal(0);  // 总是第一个 Hook
    
    if (someCondition) {
        // 使用 count
    }
    return Render.empty();
});
```

### ❌ 陷阱 2：Effect 中忘记 cleanup

```java
// ❌ 错误：忘记清理订阅
ctx.effect(() -> {
    var subscription = eventBus.subscribe(handler);
    return null;  // 泄漏！
});

// ✅ 正确：返回 cleanup
ctx.effect(() -> {
    var subscription = eventBus.subscribe(handler);
    return () -> subscription.unsubscribe();
});
```

### ❌ 陷阱 3：Effect 中的无限循环

```java
// ❌ 错误：读取后设置同一个 Signal
ctx.effect(() -> {
    var value = count.get();
    count.set(value + 1);  // 无限循环！
    return null;
});

// ✅ 正确：使用条件防止循环
ctx.effect(() -> {
    var value = count.get();
    if (value < 10) {
        count.set(value + 1);
    }
    return null;
});
```

### ❌ 陷阱 4：在回调中使用过时闭包

```java
// ❌ 可能有问题：闭包捕获的是当时的值
Component counter = Component.stateful(ctx -> {
    var count = ctx.signal(0);
    
    Runnable logCount = () -> {
        // 总是打印最新值，因为 Signal 是引用
        System.out.println(count.get());
    };
    
    // 但如果是普通变量：
    int snapshot = count.get();  // 捕获当前值
    Runnable logSnapshot = () -> {
        System.out.println(snapshot);  // 总是打印 0！
    };
    
    return Button("Log", logCount);  // ✅ 用 Signal
});
```

### ❌ 陷阱 5：直接修改 Signal 中的对象

```java
// ❌ 错误：直接修改
var items = ctx.signal(new ArrayList<Item>());
items.get().add(newItem);  // 不会触发更新！

// ✅ 正确：创建新对象
items.update(list -> {
    var newList = new ArrayList<>(list);
    newList.add(newItem);
    return newList;
});
```

### ❌ 陷阱 6：在 stateless 组件中使用状态 Hook

```java
// ❌ 技术上可行但语义错误
Component display = Component.stateless(ctx -> {
    var count = ctx.signal(0);  // 每次渲染都新建！
    return Render.text(() -> count.get().toString());
});

// ✅ 正确：使用 stateful
Component display = Component.stateful(ctx -> {
    var count = ctx.signal(0);
    return Render.text(() -> count.get().toString());
});
```

---

## 调试技巧

### 技巧 1：追踪状态变化

```java
// 添加日志
var count = ctx.signal(0);
count.subscribe(newValue -> {
    System.out.println("[DEBUG] count changed to: " + newValue);
    new Throwable("Stack trace").printStackTrace();  // 查看调用栈
});
```

### 技巧 2：追踪依赖关系

```java
// 打印 Computed 的依赖
var computed = ctx.computed(() -> {
    System.out.println("[DEBUG] Computing...");
    int a = signalA.get();
    int b = signalB.get();
    return a + b;
});
```

### 技巧 3：追踪组件生命周期

```java
Component debugged = Component.stateful(ctx -> {
    var id = UUID.randomUUID().toString().substring(0, 8);
    
    ctx.onMount(() -> {
        System.out.println("[" + id + "] Mounted");
    });
    
    ctx.onUnmount(() -> {
        System.out.println("[" + id + "] Unmounted");
    });
    
    ctx.effect(() -> {
        System.out.println("[" + id + "] Effect running");
        return () -> System.out.println("[" + id + "] Effect cleanup");
    });
    
    return actualContent;
});
```

### 技巧 4：检查 Element 树

```java
// 遍历并打印 Element 树
void debugElementTree(Element element, int depth) {
    String indent = "  ".repeat(depth);
    System.out.println(indent + element.getClass().getSimpleName());
    
    if (element instanceof ContainerElement container) {
        for (Element child : container.getChildren()) {
            debugElementTree(child, depth + 1);
        }
    } else if (element instanceof ComponentElement comp) {
        debugElementTree(comp.getChild(), depth + 1);
    }
}
```

---

## 检查清单

### 新组件检查清单

- [ ] 是否需要 stateful？没有状态就用 stateless
- [ ] Signal 粒度是否合适？
- [ ] 派生数据是否用了 Computed？
- [ ] Effect 是否返回了 cleanup？
- [ ] 列表是否使用了正确的 key？
- [ ] 是否避免了条件/循环中的 Hook？

### 性能检查清单

- [ ] 大列表是否考虑虚拟化？
- [ ] 昂贵计算是否用 Computed 缓存？
- [ ] 是否避免了不必要的依赖？（使用 peek）
- [ ] 是否批量更新了相关状态？
- [ ] 组件粒度是否合理？

### 发布前检查清单

- [ ] 是否移除了调试日志？
- [ ] Effect 是否都有 cleanup？
- [ ] 是否处理了边界情况（空列表、null 值）？
- [ ] 是否处理了加载和错误状态？

---

## 总结

CloudLib 响应式 UI 系统的核心理念：

1. **声明式**：描述"是什么"，不是"怎么做"
2. **响应式**：状态变化自动更新 UI
3. **细粒度**：只更新需要更新的部分
4. **组合式**：小组件组合成大组件

遵循这些最佳实践，你将能构建高性能、可维护的 UI。

---

## 附录

- **[Appendix A: API Reference](./appendix-a-api-reference.md)** - API 速查手册
- **[Appendix B: Examples](./appendix-b-examples.md)** - 更多示例代码

