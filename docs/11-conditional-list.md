# 11. Conditional & List Rendering - 条件渲染与列表渲染

## 目录

- [概述](#概述)
- [条件渲染](#条件渲染)
- [列表渲染](#列表渲染)
- [Key 的重要性](#key-的重要性)
- [性能优化](#性能优化)
- [常见模式](#常见模式)
- [完整案例](#完整案例)
- [常见问题与解答](#常见问题与解答)

---

## 概述

条件渲染和列表渲染是动态 UI 的两大基石：

```
┌─────────────────────────────────────────────────────────┐
│              条件渲染 vs 列表渲染                        │
│                                                         │
│  条件渲染：根据条件显示/隐藏内容                        │
│  ┌─────────────────────────────────────┐               │
│  │ if (isLoggedIn)                      │               │
│  │   show(Dashboard)                    │               │
│  │ else                                 │               │
│  │   show(LoginForm)                    │               │
│  └─────────────────────────────────────┘               │
│                                                         │
│  列表渲染：根据数据生成多个元素                         │
│  ┌─────────────────────────────────────┐               │
│  │ for (item in items)                  │               │
│  │   show(ItemCard(item))               │               │
│  └─────────────────────────────────────┘               │
└─────────────────────────────────────────────────────────┘
```

---

## 条件渲染

### showWhen - 条件显示

最简单的条件渲染：满足条件时显示，否则不显示。

```java
// 基础用法
RenderNode.showWhen(
    () -> isLoggedIn.get(),
    logoutButton
);

// 等价于
RenderNode.conditional(
    () -> isLoggedIn.get(),
    logoutButton,
    Render.empty()
);
```

**使用场景：**
- 可选内容
- 权限控制
- 加载状态

```java
Column(() -> {
    // 标题总是显示
    Text("My Profile");
    
    // 管理员才能看到的按钮
    RenderNode.showWhen(
        () -> user.get().isAdmin(),
        adminPanel
    );
    
    // 加载完成后显示内容
    RenderNode.showWhen(
        () -> !isLoading.get() && data.get() != null,
        dataView
    );
});
```

### conditional - 二选一

根据条件选择两个分支之一。

```java
// 基础用法
RenderNode.conditional(
    () -> isLoading.get(),
    loadingSpinner,    // 条件为 true
    contentView        // 条件为 false
);
```

**使用场景：**
- 两种状态切换
- A/B 视图

```java
// 登录状态切换
RenderNode.conditional(
    () -> isLoggedIn.get(),
    userDashboard,
    loginForm
);

// 编辑/查看模式
RenderNode.conditional(
    () -> isEditing.get(),
    editForm(data),
    readOnlyView(data)
);
```

### 嵌套条件

处理多个条件时可以嵌套：

```java
// 多状态处理
RenderNode.conditional(
    () -> status.get() == Status.LOADING,
    loadingView,
    RenderNode.conditional(
        () -> status.get() == Status.ERROR,
        errorView,
        RenderNode.conditional(
            () -> status.get() == Status.EMPTY,
            emptyView,
            contentView
        )
    )
);
```

**更好的方式：使用 switch 辅助方法**

```java
// 自定义 switch 辅助
static RenderNode renderSwitch(
    Supplier<Object> condition,
    Map<Object, RenderNode> cases,
    RenderNode defaultCase
) {
    return new SwitchNode(condition, cases, defaultCase);
}

// 使用
renderSwitch(
    () -> status.get(),
    Map.of(
        Status.LOADING, loadingView,
        Status.ERROR, errorView,
        Status.EMPTY, emptyView
    ),
    contentView  // default
);
```

### 条件渲染的生命周期

```
条件: true → false → true

true:
├── 创建 whenTrue 的 Element
└── 挂载 whenTrue

false:
├── 卸载 whenTrue（执行 cleanup）
├── 创建 whenFalse 的 Element
└── 挂载 whenFalse

true:
├── 卸载 whenFalse（执行 cleanup）
├── 创建新的 whenTrue Element（之前的已销毁）
└── 挂载 whenTrue

注意：每次切换，组件的状态都会重置！
```

---

## 列表渲染

### forEach - 带 Key 的列表

最常用的列表渲染方式，每个项有唯一标识。

```java
// 基础用法
RenderNode.forEach(
    () -> items.get(),           // 数据源 Supplier
    Item::id,                     // Key 提取器
    item -> ItemCard(item)        // 项渲染器
);
```

**参数说明：**
- `itemsSupplier`: 响应式数据源
- `keyExtractor`: 从 item 提取唯一标识
- `itemRenderer`: 将 item 转换为 Component

```java
// 完整示例
Component todoList = Component.stateful(ctx -> {
    var todos = ctx.signal(List.of(
        new Todo(1, "Learn Java", false),
        new Todo(2, "Build UI", false),
        new Todo(3, "Write docs", true)
    ));
    
    return Column(() -> {
        RenderNode.forEach(
            todos::get,
            Todo::id,
            todo -> todoItem(todo, id -> toggleTodo(todos, id))
        );
    });
});

static Component todoItem(Todo todo, Consumer<Integer> onToggle) {
    return Component.stateless(ctx -> 
        Row(() -> {
            Checkbox(todo.completed(), () -> onToggle.accept(todo.id()));
            Text(todo.text());
        })
    );
}
```

### indexed - 带索引的列表

当不需要 key（或 key 就是索引）时使用。

```java
// 基础用法
RenderNode.indexed(
    () -> tabs.get(),
    (tab, index) -> tabButton(tab, index)
);
```

**适用场景：**
- 顺序固定的列表
- Tab 列表
- 固定数量的项

```java
// Tab 示例
Component tabBar = Component.stateful(ctx -> {
    var activeIndex = ctx.signal(0);
    var tabs = List.of("Home", "Profile", "Settings");
    
    return Row(() -> {
        RenderNode.indexed(
            () -> tabs,
            (tab, index) -> tabButton(
                tab, 
                index,
                () -> activeIndex.get() == index,
                () -> activeIndex.set(index)
            )
        );
    });
});
```

### 嵌套列表

列表可以嵌套：

```java
// 分组列表
Component groupedList = Component.stateless(ctx -> 
    Column(() -> {
        RenderNode.forEach(
            groups::get,
            Group::id,
            group -> Column(() -> {
                // 组标题
                Text(group.name(), titleStyle);
                
                // 组内项目
                RenderNode.forEach(
                    () -> group.items(),
                    Item::id,
                    item -> itemView(item)
                );
            })
        );
    })
);
```

---

## Key 的重要性

### 为什么需要 Key？

Key 用于在列表更新时识别哪些项是"相同的"，从而：
1. 复用 Element，保持状态
2. 减少不必要的重建
3. 正确处理动画

### Key 的工作原理

```
旧列表: [A(key=1), B(key=2), C(key=3)]
新列表: [B(key=2), C(key=3), D(key=4)]

无 Key（按位置匹配）:
  位置 0: A → B (更新 A 的 Element 为 B 的内容)
  位置 1: B → C (更新 B 的 Element 为 C 的内容)
  位置 2: C → D (更新 C 的 Element 为 D 的内容)
  结果: 3 次更新，状态全部错乱

有 Key（按 Key 匹配）:
  key=1: A → 无 (销毁 A 的 Element)
  key=2: B → B (保持，位置可能变化)
  key=3: C → C (保持，位置可能变化)
  key=4: 无 → D (创建 D 的 Element)
  结果: 1 次销毁 + 1 次创建，B 和 C 的状态保持
```

### 好的 Key

```java
// ✅ 好：稳定且唯一的 ID
RenderNode.forEach(
    items::get,
    Item::id,        // 数据库 ID
    item -> itemView(item)
);

// ✅ 好：组合键
RenderNode.forEach(
    items::get,
    item -> item.type() + "-" + item.id(),
    item -> itemView(item)
);

// ✅ 好：自然唯一的属性
RenderNode.forEach(
    users::get,
    User::email,     // 邮箱是唯一的
    user -> userCard(user)
);
```

### 坏的 Key

```java
// ❌ 坏：索引（列表重排时无效）
int[] i = {0};
RenderNode.forEach(
    items::get,
    item -> i[0]++,
    item -> itemView(item)
);

// ❌ 坏：随机值（每次渲染都不同）
RenderNode.forEach(
    items::get,
    item -> Math.random(),
    item -> itemView(item)
);

// ❌ 坏：非唯一值
RenderNode.forEach(
    items::get,
    Item::name,      // 名字可能重复！
    item -> itemView(item)
);
```

### Key 影响的场景

```java
// 场景：可编辑的待办事项列表
Component editableTodoItem = Component.stateful(ctx -> {
    var isEditing = ctx.signal(false);     // 本地状态
    var editText = ctx.signal("");         // 本地状态
    
    // 如果没有正确的 key，列表重排时：
    // - isEditing 和 editText 会错乱
    // - 用户正在编辑的内容会"跳"到其他项
    
    return Row(() -> {
        RenderNode.conditional(
            isEditing::get,
            TextInput(editText),
            Text(todo.text())
        );
    });
});
```

---

## 性能优化

### 优化 1：使用 Computed 过滤列表

```java
Component filteredList = Component.stateful(ctx -> {
    var items = ctx.signal(List.<Item>of());
    var searchQuery = ctx.signal("");
    
    // ✅ 好：使用 Computed 缓存过滤结果
    var filteredItems = ctx.computed(() -> {
        String query = searchQuery.get().toLowerCase();
        if (query.isEmpty()) {
            return items.get();
        }
        return items.get().stream()
            .filter(item -> item.name().toLowerCase().contains(query))
            .toList();
    });
    
    return Column(() -> {
        TextInput(searchQuery, "Search...");
        
        // filteredItems 只在 items 或 searchQuery 变化时重算
        RenderNode.forEach(
            filteredItems::get,
            Item::id,
            item -> itemView(item)
        );
    });
});
```

### 优化 2：虚拟列表

对于大量数据，只渲染可见部分：

```java
Component virtualList = Component.stateful(ctx -> {
    var items = ctx.signal(List.<Item>of());  // 可能有 10000 个
    var scrollOffset = ctx.signal(0);
    var containerHeight = ctx.signal(500);
    var itemHeight = 50;
    
    // 计算可见范围
    var visibleRange = ctx.computed(() -> {
        int start = scrollOffset.get() / itemHeight;
        int count = containerHeight.get() / itemHeight + 2;  // +2 缓冲
        return new int[]{start, Math.min(start + count, items.get().size())};
    });
    
    // 只渲染可见项
    var visibleItems = ctx.computed(() -> {
        int[] range = visibleRange.get();
        return items.get().subList(range[0], range[1]);
    });
    
    return ScrollView(
        onScroll(scrollOffset::set),
        Box(
            Style.builder()
                .height(items.get().size() * itemHeight)  // 总高度
                .build(),
            Column(
                Style.builder()
                    .paddingTop(visibleRange.get()[0] * itemHeight)  // 偏移
                    .build(),
                () -> {
                    RenderNode.forEach(
                        visibleItems::get,
                        Item::id,
                        item -> itemView(item)
                    );
                }
            )
        )
    );
});
```

### 优化 3：懒加载

```java
Component lazyList = Component.stateful(ctx -> {
    var items = ctx.signal(List.<Item>of());
    var hasMore = ctx.signal(true);
    var isLoadingMore = ctx.signal(false);
    
    Runnable loadMore = () -> {
        if (isLoadingMore.peek() || !hasMore.peek()) return;
        
        isLoadingMore.set(true);
        fetchMoreItems(items.peek().size())
            .thenAccept(newItems -> {
                if (newItems.isEmpty()) {
                    hasMore.set(false);
                } else {
                    items.update(list -> {
                        var combined = new ArrayList<>(list);
                        combined.addAll(newItems);
                        return combined;
                    });
                }
                isLoadingMore.set(false);
            });
    };
    
    return ScrollView(
        onScrollToBottom(loadMore),  // 滚动到底部时加载更多
        Column(() -> {
            RenderNode.forEach(
                items::get,
                Item::id,
                item -> itemView(item)
            );
            
            // 加载中指示器
            RenderNode.showWhen(isLoadingMore::get, loadingSpinner);
            
            // 没有更多
            RenderNode.showWhen(
                () -> !hasMore.get() && !items.get().isEmpty(),
                Text("No more items")
            );
        })
    );
});
```

---

## 常见模式

### 模式 1：空状态处理

```java
Component listWithEmpty = Component.stateless(ctx -> {
    var items = ctx.provide(ItemsSignal.class);
    
    return RenderNode.conditional(
        () -> items.get().isEmpty(),
        // 空状态
        Column(() -> {
            Image(emptyIcon);
            Text("No items yet");
            Button("Add first item", onAdd);
        }),
        // 有数据
        RenderNode.forEach(
            items::get,
            Item::id,
            item -> itemView(item)
        )
    );
});
```

### 模式 2：加载/错误/数据三态

```java
enum DataState { LOADING, ERROR, SUCCESS }

Component dataView = Component.stateful(ctx -> {
    var state = ctx.signal(DataState.LOADING);
    var data = ctx.signal(List.<Item>of());
    var error = ctx.signal("");
    
    // 加载数据
    ctx.effect(() -> {
        state.set(DataState.LOADING);
        fetchData()
            .thenAccept(result -> {
                data.set(result);
                state.set(DataState.SUCCESS);
            })
            .exceptionally(e -> {
                error.set(e.getMessage());
                state.set(DataState.ERROR);
                return null;
            });
        return null;
    });
    
    return RenderNode.conditional(
        () -> state.get() == DataState.LOADING,
        loadingView,
        RenderNode.conditional(
            () -> state.get() == DataState.ERROR,
            errorView(error, () -> {/* retry */}),
            contentView(data)
        )
    );
});
```

### 模式 3：选中项列表

```java
Component selectableList = Component.stateful(ctx -> {
    var items = ctx.signal(List.<Item>of());
    var selectedIds = ctx.signal(Set.<Integer>of());
    
    Consumer<Integer> toggle = id -> {
        selectedIds.update(set -> {
            var newSet = new HashSet<>(set);
            if (newSet.contains(id)) {
                newSet.remove(id);
            } else {
                newSet.add(id);
            }
            return newSet;
        });
    };
    
    var selectedCount = ctx.computed(() -> selectedIds.get().size());
    
    return Column(() -> {
        // 选中计数
        Text(() -> "Selected: " + selectedCount.get());
        
        // 列表
        RenderNode.forEach(
            items::get,
            Item::id,
            item -> selectableItem(
                item,
                () -> selectedIds.get().contains(item.id()),
                () -> toggle.accept(item.id())
            )
        );
    });
});

static Component selectableItem(
    Item item, 
    Supplier<Boolean> isSelected,
    Runnable onToggle
) {
    return Component.stateless(ctx -> 
        Row(
            Style.builder()
                .background(isSelected.get() ? 0xE3F2FD : 0xFFFFFF)
                .build(),
            onClick(onToggle),
            () -> {
                Checkbox(isSelected.get(), onToggle);
                Text(item.name());
            }
        )
    );
}
```

### 模式 4：可拖拽排序

```java
Component sortableList = Component.stateful(ctx -> {
    var items = ctx.signal(List.<Item>of());
    var draggedId = ctx.signal((Integer) null);
    var dropTargetId = ctx.signal((Integer) null);
    
    BiConsumer<Integer, Integer> reorder = (fromId, toId) -> {
        items.update(list -> {
            var newList = new ArrayList<>(list);
            int fromIndex = -1, toIndex = -1;
            for (int i = 0; i < newList.size(); i++) {
                if (newList.get(i).id() == fromId) fromIndex = i;
                if (newList.get(i).id() == toId) toIndex = i;
            }
            if (fromIndex >= 0 && toIndex >= 0) {
                var item = newList.remove(fromIndex);
                newList.add(toIndex, item);
            }
            return newList;
        });
    };
    
    return Column(() -> {
        RenderNode.forEach(
            items::get,
            Item::id,
            item -> draggableItem(
                item,
                () -> draggedId.get() == item.id(),
                () -> dropTargetId.get() == item.id(),
                // 拖拽回调
                () -> draggedId.set(item.id()),
                () -> {
                    if (draggedId.peek() != null && draggedId.peek() != item.id()) {
                        dropTargetId.set(item.id());
                    }
                },
                () -> {
                    if (draggedId.peek() != null && dropTargetId.peek() != null) {
                        reorder.accept(draggedId.peek(), dropTargetId.peek());
                    }
                    draggedId.set(null);
                    dropTargetId.set(null);
                }
            )
        );
    });
});
```

---

## 完整案例

### 案例：带筛选和排序的列表

```java
record Product(int id, String name, double price, String category) {}

enum SortBy { NAME, PRICE_ASC, PRICE_DESC }

Component productList = Component.stateful(ctx -> {
    // 数据
    var products = ctx.signal(List.<Product>of());
    
    // 筛选条件
    var searchQuery = ctx.signal("");
    var selectedCategory = ctx.signal("All");
    var sortBy = ctx.signal(SortBy.NAME);
    
    // 可用分类（从数据派生）
    var categories = ctx.computed(() -> {
        var cats = new ArrayList<String>();
        cats.add("All");
        products.get().stream()
            .map(Product::category)
            .distinct()
            .sorted()
            .forEach(cats::add);
        return cats;
    });
    
    // 筛选后的产品
    var filteredProducts = ctx.computed(() -> {
        String query = searchQuery.get().toLowerCase();
        String category = selectedCategory.get();
        
        return products.get().stream()
            .filter(p -> category.equals("All") || p.category().equals(category))
            .filter(p -> query.isEmpty() || p.name().toLowerCase().contains(query))
            .toList();
    });
    
    // 排序后的产品
    var sortedProducts = ctx.computed(() -> {
        var list = new ArrayList<>(filteredProducts.get());
        switch (sortBy.get()) {
            case NAME -> list.sort(Comparator.comparing(Product::name));
            case PRICE_ASC -> list.sort(Comparator.comparing(Product::price));
            case PRICE_DESC -> list.sort(Comparator.comparing(Product::price).reversed());
        }
        return list;
    });
    
    // 结果计数
    var resultCount = ctx.computed(() -> sortedProducts.get().size());
    
    return Column(16, () -> {
        // 搜索栏
        Row(8, () -> {
            TextInput(searchQuery, "Search products...");
        });
        
        // 筛选栏
        Row(8, () -> {
            Text("Category: ");
            RenderNode.forEach(
                categories::get,
                cat -> cat,
                cat -> chipButton(
                    cat,
                    () -> selectedCategory.get().equals(cat),
                    () -> selectedCategory.set(cat)
                )
            );
        });
        
        // 排序栏
        Row(8, () -> {
            Text("Sort by: ");
            Button("Name", () -> sortBy.set(SortBy.NAME),
                buttonStyle(() -> sortBy.get() == SortBy.NAME));
            Button("Price ↑", () -> sortBy.set(SortBy.PRICE_ASC),
                buttonStyle(() -> sortBy.get() == SortBy.PRICE_ASC));
            Button("Price ↓", () -> sortBy.set(SortBy.PRICE_DESC),
                buttonStyle(() -> sortBy.get() == SortBy.PRICE_DESC));
        });
        
        // 结果信息
        Text(() -> resultCount.get() + " products found");
        
        // 产品列表
        RenderNode.conditional(
            () -> sortedProducts.get().isEmpty(),
            // 空结果
            Column(() -> {
                Text("No products match your criteria");
                Button("Clear filters", () -> {
                    searchQuery.set("");
                    selectedCategory.set("All");
                });
            }),
            // 产品网格
            Grid(3, () -> {
                RenderNode.forEach(
                    sortedProducts::get,
                    Product::id,
                    product -> productCard(product)
                );
            })
        );
    });
});

// 产品卡片
static Component productCard(Product product) {
    return Component.stateless(ctx -> 
        Box(cardStyle, () -> {
            Image(productImage(product.id()));
            Text(product.name(), nameStyle);
            Text("$" + product.price(), priceStyle);
            Text(product.category(), categoryStyle);
            Button("Add to cart", () -> addToCart(product.id()));
        })
    );
}

// 筛选按钮
static Component chipButton(
    String text,
    Supplier<Boolean> isActive,
    Runnable onClick
) {
    return Component.stateless(ctx -> 
        Box(
            Style.builder()
                .padding(4, 12)
                .background(isActive.get() ? 0x4488FF : 0xE0E0E0)
                .color(isActive.get() ? 0xFFFFFF : 0x333333)
                .borderRadius(16)
                .build(),
            onClick(onClick),
            Text(text)
        )
    );
}
```

---

## 常见问题与解答

### Q1: 条件切换时如何保持状态？

**A**: 默认情况下，条件切换会销毁非活跃分支的状态。如果需要保持状态：

```java
// 方式 1：使用 showWhen 隐藏而不是销毁
Column(() -> {
    Box(Style.builder().visible(tab == 0).build(), content0);
    Box(Style.builder().visible(tab == 1).build(), content1);
});

// 方式 2：将状态提升到父组件
Component parent = Component.stateful(ctx -> {
    var sharedState = ctx.signal(...);  // 状态在这里
    
    return RenderNode.conditional(
        condition,
        childA(sharedState),  // 传入共享状态
        childB(sharedState)
    );
});
```

### Q2: forEach 的 key 可以是复合的吗？

**A**: 可以，但必须正确实现：

```java
// ✅ 好：字符串拼接
RenderNode.forEach(
    items::get,
    item -> item.type() + "-" + item.id(),
    item -> itemView(item)
);

// ✅ 好：使用 record 作为 key
record ItemKey(String type, int id) {}

RenderNode.forEach(
    items::get,
    item -> new ItemKey(item.type(), item.id()),
    item -> itemView(item)
);
```

### Q3: 嵌套 forEach 的性能如何？

**A**: 取决于数据规模。对于大量数据：

```java
// ❌ 可能有性能问题：10 组 × 100 项 = 1000 个组件
RenderNode.forEach(groups::get, Group::id, group ->
    RenderNode.forEach(() -> group.items(), Item::id, item ->
        itemView(item)
    )
);

// ✅ 考虑扁平化 + 虚拟列表
var flattenedItems = ctx.computed(() -> 
    groups.get().stream()
        .flatMap(g -> g.items().stream()
            .map(item -> new GroupedItem(g, item)))
        .toList()
);
```

### Q4: 列表项中的事件处理器如何优化？

**A**: 避免在 forEach 中创建闭包：

```java
// ❌ 每次渲染都创建新的 lambda
RenderNode.forEach(
    items::get,
    Item::id,
    item -> itemView(item, () -> handleClick(item.id()))  // 新的 lambda
);

// ✅ 使用 Consumer 或 event bus
Consumer<Integer> handleClick = id -> { /* ... */ };

RenderNode.forEach(
    items::get,
    Item::id,
    item -> itemView(item, handleClick)  // 复用同一个 Consumer
);
```

---

## 下一步

现在你已经掌握了条件渲染和列表渲染，接下来学习最佳实践：

➡️ **[12-best-practices.md](./12-best-practices.md)** - 最佳实践与常见陷阱

