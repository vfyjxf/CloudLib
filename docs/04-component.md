# 04. Component - 组件系统

## 目录

- [概述](#概述)
- [为什么需要组件](#为什么需要组件)
- [组件类型](#组件类型)
- [Component 接口设计](#component-接口设计)
- [无状态组件](#无状态组件)
- [有状态组件](#有状态组件)
- [组件组合](#组件组合)
- [完整案例](#完整案例)
- [常见问题与解答](#常见问题与解答)

---

## 概述

**Component** 是 UI 的构建单元。它封装了：
- 渲染逻辑（如何显示）
- 状态管理（显示什么）
- 生命周期（何时创建/更新/销毁）

### 核心特点

```
┌─────────────────────────────────────────────────────────┐
│                     Component                           │
│                                                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │              render() → RenderNode              │   │
│  │                                                  │   │
│  │  "我告诉你想要什么，你来负责如何实现"            │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
│  特点：                                                 │
│  • 声明式：描述"是什么"，不是"怎么做"                  │
│  • 组合式：大组件由小组件组合而成                      │
│  • 可复用：同一组件可在多处使用                        │
│  • 响应式：状态变化自动触发更新                        │
└─────────────────────────────────────────────────────────┘
```

---

## 为什么需要组件

### 问题：直接操作 UI 元素

```java
// 命令式：告诉每一步怎么做
void updateCounter(int count) {
    textWidget.setText("Count: " + count);
    if (count > 10) {
        warningWidget.setVisible(true);
        warningWidget.setText("Count is high!");
    } else {
        warningWidget.setVisible(false);
    }
    if (count == 0) {
        resetButton.setEnabled(false);
    } else {
        resetButton.setEnabled(true);
    }
}
```

**问题：**
1. 状态和 UI 分离，容易不同步
2. 条件逻辑复杂，难以维护
3. 难以复用
4. 难以测试

### 解决方案：组件化

```java
// 声明式：描述你想要什么
Component counter = Component.stateful(ctx -> {
    var count = ctx.signal(0);
    
    return Column(() -> {
        Text(() -> "Count: " + count.get());
        
        RenderNode.showWhen(
            () -> count.get() > 10,
            Render.text(() -> "Count is high!")
        );
        
        RenderNode.showWhen(
            () -> count.get() > 0,
            Render.button("Reset", () -> count.set(0))
        );
    });
});
```

**优势：**
1. 状态和 UI 统一管理
2. 条件逻辑清晰明了
3. 可以轻松复用
4. 容易测试

---

## 组件类型

CloudLib 提供两种组件类型：

| 类型 | 状态 | 用途 | 创建方式 |
|------|------|------|----------|
| **无状态** | 无自己的状态 | 纯展示 | `Component.stateless()` |
| **有状态** | 有自己的状态 | 交互、数据管理 | `Component.stateful()` |

### 选择指南

```
┌─────────────────────────────────────────────────────────┐
│ 这个组件需要管理状态吗？                                │
│                                                         │
│         ┌─────┐                    ┌─────┐             │
│         │ 否  │                    │ 是  │             │
│         └──┬──┘                    └──┬──┘             │
│            │                          │                 │
│            ▼                          ▼                 │
│   ┌────────────────┐        ┌────────────────┐         │
│   │ 无状态组件     │        │ 有状态组件     │         │
│   │                │        │                │         │
│   │ • 纯展示       │        │ • 管理 Signal  │         │
│   │ • 从外部获取数据│        │ • 有 Effect    │         │
│   │ • 更轻量       │        │ • 有生命周期   │         │
│   └────────────────┘        └────────────────┘         │
└─────────────────────────────────────────────────────────┘
```

---

## Component 接口设计

### 接口定义

```java
@FunctionalInterface
public interface Component {
    /**
     * 渲染组件，返回描述 UI 结构的 RenderNode
     */
    RenderNode render(ComponentContext ctx);
}
```

### 为什么是函数式接口？

1. **简洁**：可以用 lambda 创建组件
2. **灵活**：可以是方法引用
3. **组合**：函数易于组合

```java
// Lambda
Component hello = ctx -> Render.text("Hello");

// 方法引用
Component counter = CounterComponent::render;

// 类实现
class MyComponent implements Component {
    @Override
    public RenderNode render(ComponentContext ctx) {
        return Render.text("Custom");
    }
}
```

### 工厂方法

```java
public interface Component {
    // 创建无状态组件
    static Component stateless(Component component) {
        return component;  // 直接返回，无包装
    }
    
    // 创建有状态组件
    static Component stateful(Component component) {
        return new StatefulComponent(component);
    }
}
```

**为什么用工厂方法而不是子类？**

```java
// ❌ 继承方式：需要创建类
class MyStatefulComponent extends StatefulComponent {
    @Override
    public RenderNode render(ComponentContext ctx) { ... }
}

// ✅ 工厂方式：直接用 lambda
Component myComponent = Component.stateful(ctx -> ...);
```

优势：
- 更简洁
- 可以用 lambda
- 不暴露实现细节

---

## 无状态组件

### 定义

无状态组件：
- 没有自己管理的状态
- 每次渲染都完全由输入决定
- 相同输入总是产生相同输出

### 创建方式

```java
// 方式 1：使用 stateless() 工厂方法
Component greeting = Component.stateless(ctx -> 
    Render.text("Hello, World!")
);

// 方式 2：直接实现接口（lambda）
Component greeting = ctx -> Render.text("Hello, World!");

// 方式 3：方法引用
Component greeting = GreetingView::render;

static RenderNode render(ComponentContext ctx) {
    return Render.text("Hello, World!");
}
```

### 从外部接收数据

无状态组件通过闭包或 Context API 获取数据：

```java
// 方式 1：闭包
public static Component userCard(Signal<User> user) {
    return Component.stateless(ctx -> 
        Column(() -> {
            Text(() -> user.get().name());
            Text(() -> user.get().email());
        })
    );
}

// 使用
var currentUser = Signal.of(new User("Alice", "alice@example.com"));
Child(userCard(currentUser));

// 方式 2：Context API
Component userCard = Component.stateless(ctx -> {
    var user = ctx.provide(UserContext.class);
    return Column(() -> {
        Text(() -> user.name().get());
        Text(() -> user.email().get());
    });
});
```

### 典型用途

```java
// 1. 纯展示组件
Component avatar = Component.stateless(ctx -> {
    var user = ctx.provide(UserContext.class);
    return Render.image(user.avatarUrl().get());
});

// 2. 布局组件
Component card(Component content) {
    return Component.stateless(ctx -> 
        Render.box(
            Style.builder()
                .padding(16)
                .background(0xFFFFFF)
                .borderRadius(8)
                .build(),
            content
        )
    );
}

// 3. 高阶组件
Component withTheme(Component inner) {
    return Component.stateless(ctx -> {
        var theme = ctx.provide(ThemeContext.class);
        // 注入主题后渲染内部组件
        return inner.render(ctx);
    });
}
```

---

## 有状态组件

### 定义

有状态组件：
- 管理自己的内部状态
- 可以有副作用
- 有完整的生命周期

### 创建方式

```java
Component counter = Component.stateful(ctx -> {
    // 声明状态
    var count = ctx.signal(0);
    
    // 声明副作用
    ctx.effect(() -> {
        System.out.println("Count: " + count.get());
        return null;
    });
    
    // 返回 UI
    return Column(() -> {
        Text(() -> "Count: " + count.get());
        Button("+", () -> count.update(n -> n + 1));
    });
});
```

### 状态是如何保持的？

关键点：**组件的状态存储在 Element 中，而不是 Component 中**

```
Component（蓝图）           Element（实例）
┌──────────────────┐       ┌──────────────────┐
│ render = ctx ->  │       │ component = ...  │
│   signal(0);     │ ───▶  │ state = {        │
│   text(count)    │       │   count: Signal  │
│                  │       │ }                │
└──────────────────┘       └──────────────────┘
        │                           │
        │                           │
        ▼                           ▼
  多个 Element                每个 Element 有
  可以共享同一个               自己独立的状态
  Component
```

### 状态 Hook

有状态组件通过 `ComponentContext` 访问状态 Hook：

```java
Component form = Component.stateful(ctx -> {
    // signal：可变状态
    var name = ctx.signal("");
    var age = ctx.signal(0);
    
    // computed：派生状态
    var isValid = ctx.computed(() -> 
        !name.get().isEmpty() && age.get() > 0
    );
    
    // effect：副作用
    ctx.effect(() -> {
        if (isValid.get()) {
            enableSubmitButton();
        }
        return null;
    });
    
    return Column(() -> {
        TextInput(name);
        NumberInput(age);
        
        RenderNode.showWhen(
            isValid::get,
            Render.button("Submit", () -> submit())
        );
    });
});
```

### 生命周期

```
┌─────────────────────────────────────────────────────────┐
│                    组件生命周期                          │
│                                                         │
│  挂载 ─────────────────────────────────────────▶ 卸载   │
│    │                                              │     │
│    ▼                                              │     │
│  render()                                         │     │
│    │                                              │     │
│    ├─ signal() ──▶ 创建状态                       │     │
│    ├─ computed() ──▶ 创建计算                     │     │
│    ├─ effect() ──▶ 注册副作用                     │     │
│    │                                              │     │
│    ▼                                              │     │
│  effect 执行                                      │     │
│    │                                              │     │
│    │ ◀──── 状态变化 ◀──── 用户交互               │     │
│    │                                              │     │
│    ▼                                              │     │
│  组件更新（重新渲染受影响部分）                    │     │
│    │                                              │     │
│    │                                              │     │
│    └─────────────────────────────────────────────┘     │
│                                                         │
│  卸载时：                                               │
│    • 执行所有 effect 的 cleanup                        │
│    • 清理订阅关系                                      │
│    • 释放资源                                          │
└─────────────────────────────────────────────────────────┘
```

---

## 组件组合

### 嵌套组件

```java
// 父组件
Component app = Component.stateful(ctx -> {
    var user = ctx.signal(new User("Alice"));
    
    return Column(() -> {
        // 嵌套子组件
        Child(header);
        Child(userProfile(user));
        Child(footer);
    });
});

// 子组件
Component header = Component.stateless(ctx -> 
    Render.text("My App")
);

Component userProfile(Signal<User> user) {
    return Component.stateless(ctx -> 
        Column(() -> {
            Text(() -> user.get().name());
            Child(avatar(user));
        })
    );
}
```

### 组件工厂

组件可以是返回 Component 的函数：

```java
// 带参数的组件
public static Component button(String text, Runnable onClick) {
    return Component.stateless(ctx -> 
        Render.button(text, onClick)
    );
}

// 使用
Child(button("Click me", () -> handleClick()));
```

### 高阶组件

接收 Component 并返回新 Component 的函数：

```java
// 添加 loading 状态的高阶组件
public static Component withLoading(
    Computed<Boolean> isLoading, 
    Component content
) {
    return Component.stateless(ctx -> {
        return RenderNode.conditional(
            isLoading::get,
            Render.text("Loading..."),
            content
        );
    });
}

// 使用
var isLoading = ctx.signal(true);
Child(withLoading(isLoading, actualContent));
```

### 组件插槽

```java
// 布局组件，接收多个插槽
public static Component pageLayout(
    Component header,
    Component content,
    Component footer
) {
    return Component.stateless(ctx -> 
        Column(() -> {
            // Header 区域
            Box(Style.builder().height(60).build(), () -> {
                Child(header);
            });
            
            // Content 区域（填充剩余空间）
            Expanded(() -> {
                Child(content);
            });
            
            // Footer 区域
            Box(Style.builder().height(40).build(), () -> {
                Child(footer);
            });
        })
    );
}

// 使用
Component app = pageLayout(
    Header::new,
    MainContent::new,
    Footer::new
);
```

---

## 完整案例

### 案例 1：待办事项组件

```java
// 数据模型
record TodoItem(int id, String text, boolean completed) {}

// 待办事项列表组件
Component todoList = Component.stateful(ctx -> {
    // 状态
    var items = ctx.signal(new ArrayList<TodoItem>());
    var newItemText = ctx.signal("");
    var nextId = ctx.signal(1);
    
    // 派生状态
    var completedCount = ctx.computed(() -> 
        (int) items.get().stream().filter(TodoItem::completed).count()
    );
    var totalCount = ctx.computed(() -> items.get().size());
    
    // 添加事项
    Runnable addItem = () -> {
        String text = newItemText.peek();
        if (!text.isEmpty()) {
            int id = nextId.peek();
            nextId.update(n -> n + 1);
            items.update(list -> {
                var newList = new ArrayList<>(list);
                newList.add(new TodoItem(id, text, false));
                return newList;
            });
            newItemText.set("");
        }
    };
    
    // 切换完成状态
    Consumer<Integer> toggleItem = (id) -> {
        items.update(list -> list.stream()
            .map(item -> item.id() == id 
                ? new TodoItem(id, item.text(), !item.completed())
                : item)
            .toList());
    };
    
    // 删除事项
    Consumer<Integer> deleteItem = (id) -> {
        items.update(list -> list.stream()
            .filter(item -> item.id() != id)
            .toList());
    };
    
    // UI
    return Column(() -> {
        // 标题和统计
        Text(() -> String.format("Todo (%d/%d completed)", 
            completedCount.get(), totalCount.get()));
        
        Spacer(8);
        
        // 输入区域
        Row(() -> {
            TextInput(newItemText);
            Button("Add", addItem);
        });
        
        Spacer(8);
        
        // 列表
        RenderNode.forEach(
            items::get,
            TodoItem::id,
            item -> todoItemView(item, toggleItem, deleteItem)
        );
    });
});

// 单个待办事项视图
static Component todoItemView(
    TodoItem item, 
    Consumer<Integer> onToggle,
    Consumer<Integer> onDelete
) {
    return Component.stateless(ctx -> 
        Row(() -> {
            Checkbox(item.completed(), () -> onToggle.accept(item.id()));
            Text(item.text());
            Expanded(() -> {});  // 占位
            Button("×", () -> onDelete.accept(item.id()));
        })
    );
}
```

### 案例 2：选项卡组件

```java
// 选项卡组件
public static Component tabs(List<Tab> tabList) {
    return Component.stateful(ctx -> {
        var activeIndex = ctx.signal(0);
        
        return Column(() -> {
            // Tab 头部
            Row(() -> {
                for (int i = 0; i < tabList.size(); i++) {
                    final int index = i;
                    Tab tab = tabList.get(i);
                    
                    Button(
                        tab.title(),
                        () -> activeIndex.set(index),
                        Style.builder()
                            .background(activeIndex.peek() == index 
                                ? 0x4488FF : 0xCCCCCC)
                            .build()
                    );
                }
            });
            
            // Tab 内容
            RenderNode.indexed(
                () -> tabList,
                (tab, i) -> RenderNode.showWhen(
                    () -> activeIndex.get() == i,
                    tab.content()
                )
            );
        });
    });
}

record Tab(String title, Component content) {}

// 使用
Child(tabs(List.of(
    new Tab("Home", homeContent),
    new Tab("Profile", profileContent),
    new Tab("Settings", settingsContent)
)));
```

---

## 常见问题与解答

### Q1: 无状态组件可以使用 signal 吗？

**A**: 技术上可以，但违反了设计意图：

```java
// ❌ 技术上可行，但不推荐
Component bad = Component.stateless(ctx -> {
    var count = ctx.signal(0);  // 每次渲染都创建新 Signal！
    return Render.text(() -> "Count: " + count.get());
});

// ✅ 应该用 stateful
Component good = Component.stateful(ctx -> {
    var count = ctx.signal(0);  // 状态被保持
    return Render.text(() -> "Count: " + count.get());
});
```

### Q2: 组件什么时候会重新渲染？

**A**: 只有在以下情况：
1. 组件内的 Signal 值变化
2. 父组件重新渲染且决定重建此组件
3. 条件渲染的条件变化

**注意**：使用细粒度响应式时，很多情况不需要重新渲染整个组件：

```java
Component.stateful(ctx -> {
    var count = ctx.signal(0);
    
    return Column(() -> {
        Text(() -> "Count: " + count.get());  // 只有这个 Text 会更新
        Text("Static text");  // 不会更新
        Button("+", () -> count.update(n -> n + 1));  // 不会更新
    });
});
```

### Q3: 如何在组件间共享状态？

**A**: 有多种方式：

```java
// 方式 1：提升状态到共同父组件
Component parent = Component.stateful(ctx -> {
    var sharedState = ctx.signal(0);
    
    return Column(() -> {
        Child(childA(sharedState));
        Child(childB(sharedState));
    });
});

// 方式 2：使用 Context API
Component app = Component.stateful(ctx -> {
    var sharedState = ctx.signal(0);
    ctx.inject(SharedState.class, sharedState);
    
    return Column(() -> {
        Child(childA);  // 通过 ctx.provide() 获取
        Child(childB);
    });
});

// 方式 3：全局状态管理
class AppState {
    static final Signal<Integer> count = Signal.of(0);
}
```

### Q4: Component 和 RenderNode 的关系是什么？

**A**: 
- **Component**: 可复用的 UI 构建块，封装逻辑
- **RenderNode**: UI 结构的描述，Component 的输出

```
Component.render(ctx) → RenderNode
                            │
                            ▼
                        Element（真实 UI）
```

---

## 下一步

现在你已经理解了组件系统，接下来学习 ComponentContext 提供的 Hook API：

➡️ **[05-component-context.md](./05-component-context.md)** - 组件上下文与 Hook

