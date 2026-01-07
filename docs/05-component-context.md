# 05. ComponentContext - 组件上下文与 Hook

## 目录

- [概述](#概述)
- [为什么需要 ComponentContext](#为什么需要-componentcontext)
- [Hook API 详解](#hook-api-详解)
- [生命周期管理](#生命周期管理)
- [依赖注入](#依赖注入)
- [Hook 规则](#hook-规则)
- [高级用法](#高级用法)
- [完整案例](#完整案例)
- [常见问题与解答](#常见问题与解答)

---

## 概述

**ComponentContext** 是组件与框架交互的桥梁。它提供：
- 状态管理 Hook（signal, computed）
- 副作用 Hook（effect）
- 生命周期 Hook（onMount, onUnmount）
- 依赖注入（provide, inject）

### 设计理念

```
┌─────────────────────────────────────────────────────────┐
│                    ComponentContext                     │
│                                                         │
│  "我是你与框架之间的桥梁"                               │
│                                                         │
│  ┌─────────────────┐  ┌─────────────────┐              │
│  │   状态 Hook     │  │   副作用 Hook   │              │
│  │                 │  │                 │              │
│  │  • signal()     │  │  • effect()     │              │
│  │  • computed()   │  │  • onMount()    │              │
│  │                 │  │  • onUnmount()  │              │
│  └─────────────────┘  └─────────────────┘              │
│                                                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │              依赖注入                            │   │
│  │                                                  │   │
│  │  • provide()  - 获取祖先提供的依赖              │   │
│  │  • inject()   - 向后代提供依赖                  │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

---

## 为什么需要 ComponentContext

### 问题：状态在哪里存储？

```java
// 问题：这个 Signal 存储在哪里？
Component counter = ctx -> {
    Signal<Integer> count = Signal.of(0);  // ← 每次渲染都创建新的！
    return Render.text(() -> "Count: " + count.get());
};
```

每次渲染都创建新 Signal，状态无法保持。

### 解决方案：通过 Context 管理状态

```java
// 解决：Context 帮助管理状态的生命周期
Component counter = Component.stateful(ctx -> {
    // ctx.signal() 会：
    // 1. 首次渲染：创建新 Signal，存储在 Element 中
    // 2. 后续渲染：返回之前存储的 Signal
    Signal<Integer> count = ctx.signal(0);
    return Render.text(() -> "Count: " + count.get());
});
```

### ComponentContext 的职责

```
┌─────────────────────────────────────────────────────────┐
│                 ComponentContext 职责                   │
│                                                         │
│  1. 状态管理                                            │
│     • 创建和存储响应式状态                             │
│     • 确保状态在渲染间保持                             │
│                                                         │
│  2. 生命周期                                            │
│     • 追踪组件的挂载/卸载                              │
│     • 在适当时机执行 effect                            │
│     • 清理资源                                         │
│                                                         │
│  3. 依赖注入                                            │
│     • 连接组件树中的依赖提供者和消费者                 │
│     • 支持跨组件的状态共享                             │
│                                                         │
│  4. Hook 调度                                           │
│     • 确保 Hook 按正确顺序调用                         │
│     • 维护 Hook 的索引                                 │
└─────────────────────────────────────────────────────────┘
```

---

## Hook API 详解

### signal() - 创建响应式状态

```java
<T> Signal<T> signal(T initialValue)
```

创建一个与组件生命周期绑定的 Signal。

**使用示例：**

```java
Component form = Component.stateful(ctx -> {
    // 基础用法
    var name = ctx.signal("");
    var age = ctx.signal(0);
    var isActive = ctx.signal(true);
    
    // 复杂类型
    var items = ctx.signal(new ArrayList<String>());
    var user = ctx.signal(new User("Anonymous"));
    
    return Column(() -> {
        TextInput(name);
        NumberInput(age);
        Toggle(isActive);
    });
});
```

**关键特性：**

1. **首次渲染**：创建新 Signal，存储在 ComponentElement 中
2. **后续渲染**：返回已存储的 Signal（通过索引定位）
3. **卸载时**：自动清理

### computed() - 创建派生计算

```java
<T> Computed<T> computed(Supplier<T> computation)
```

创建一个与组件生命周期绑定的 Computed。

**使用示例：**

```java
Component priceCalculator = Component.stateful(ctx -> {
    var price = ctx.signal(100.0);
    var quantity = ctx.signal(1);
    var discount = ctx.signal(0.0);
    
    // 派生计算
    var subtotal = ctx.computed(() -> 
        price.get() * quantity.get()
    );
    
    var total = ctx.computed(() -> 
        subtotal.get() * (1 - discount.get())
    );
    
    var isExpensive = ctx.computed(() -> 
        total.get() > 1000
    );
    
    return Column(() -> {
        Text(() -> "Subtotal: $" + subtotal.get());
        Text(() -> "Total: $" + total.get());
        
        RenderNode.showWhen(
            isExpensive::get,
            Render.text("⚠️ High value order!")
        );
    });
});
```

### effect() - 执行副作用

```java
void effect(Supplier<Runnable> effectFn)
void effect(Supplier<Runnable> effectFn, Object... deps)
```

注册一个副作用，会在挂载时执行，依赖变化时重新执行。

**基础用法：**

```java
Component logger = Component.stateful(ctx -> {
    var count = ctx.signal(0);
    
    // 自动追踪依赖的 effect
    ctx.effect(() -> {
        System.out.println("Count changed to: " + count.get());
        return null;  // 无需清理
    });
    
    return Render.button("+", () -> count.update(n -> n + 1));
});
```

**带清理的 effect：**

```java
Component subscriber = Component.stateful(ctx -> {
    var channel = ctx.signal("news");
    
    ctx.effect(() -> {
        String ch = channel.get();
        var subscription = eventBus.subscribe(ch, this::handleEvent);
        
        // 返回清理函数
        return () -> {
            subscription.unsubscribe();
            System.out.println("Unsubscribed from: " + ch);
        };
    });
    
    return Render.text(() -> "Listening to: " + channel.get());
});
```

**带显式依赖的 effect：**

```java
Component fetcher = Component.stateful(ctx -> {
    var userId = ctx.signal(1);
    var userData = ctx.signal(null);
    
    // 只在 userId 变化时执行（而不是任何依赖）
    ctx.effect(() -> {
        int id = userId.peek();  // 使用 peek 避免自动追踪
        fetchUserData(id).thenAccept(userData::set);
        return null;
    }, userId.get());  // 显式依赖
    
    return RenderNode.showWhen(
        () -> userData.get() != null,
        userCard(userData)
    );
});
```

### onMount() - 挂载回调

```java
void onMount(Runnable callback)
```

注册一个只在组件首次挂载时执行的回调。

```java
Component analytics = Component.stateful(ctx -> {
    ctx.onMount(() -> {
        trackPageView("home");
        System.out.println("Component mounted!");
    });
    
    return Render.text("Welcome!");
});
```

### onUnmount() - 卸载回调

```java
void onUnmount(Runnable callback)
```

注册一个在组件卸载时执行的回调。

```java
Component timer = Component.stateful(ctx -> {
    var seconds = ctx.signal(0);
    var timerId = new Object() { int value; };
    
    ctx.onMount(() -> {
        timerId.value = setInterval(() -> 
            seconds.update(n -> n + 1), 1000);
    });
    
    ctx.onUnmount(() -> {
        clearInterval(timerId.value);
        System.out.println("Timer stopped!");
    });
    
    return Render.text(() -> "Seconds: " + seconds.get());
});
```

---

## 生命周期管理

### 生命周期时序图

```
┌─────────────────────────────────────────────────────────┐
│                    组件生命周期                          │
│                                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │ 挂载阶段                                          │  │
│  │                                                   │  │
│  │  1. 创建 ComponentElement                         │  │
│  │  2. 调用 component.render(ctx)                   │  │
│  │     • signal() 创建状态                          │  │
│  │     • computed() 创建计算                        │  │
│  │     • effect() 注册副作用                        │  │
│  │  3. 执行 onMount 回调                            │  │
│  │  4. 执行所有 effect                              │  │
│  └──────────────────────────────────────────────────┘  │
│                         │                               │
│                         ▼                               │
│  ┌──────────────────────────────────────────────────┐  │
│  │ 更新阶段                                          │  │
│  │                                                   │  │
│  │  状态变化                                         │  │
│  │     │                                             │  │
│  │     ▼                                             │  │
│  │  依赖该状态的 UI 部分更新                        │  │
│  │     │                                             │  │
│  │     ▼                                             │  │
│  │  依赖该状态的 effect 重新执行                    │  │
│  │     • 先执行旧的 cleanup                         │  │
│  │     • 再执行新的 effect                          │  │
│  └──────────────────────────────────────────────────┘  │
│                         │                               │
│                         ▼                               │
│  ┌──────────────────────────────────────────────────┐  │
│  │ 卸载阶段                                          │  │
│  │                                                   │  │
│  │  1. 执行所有 effect 的 cleanup                   │  │
│  │  2. 执行 onUnmount 回调                          │  │
│  │  3. 清理订阅关系                                 │  │
│  │  4. 释放 Element                                  │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### Effect 执行时机详解

```java
Component demo = Component.stateful(ctx -> {
    var a = ctx.signal(1);
    var b = ctx.signal(2);
    
    // Effect 1: 依赖 a
    ctx.effect(() -> {
        System.out.println("[Effect 1] a = " + a.get());
        return () -> System.out.println("[Cleanup 1]");
    });
    
    // Effect 2: 依赖 b
    ctx.effect(() -> {
        System.out.println("[Effect 2] b = " + b.get());
        return () -> System.out.println("[Cleanup 2]");
    });
    
    // Effect 3: 依赖 a 和 b
    ctx.effect(() -> {
        System.out.println("[Effect 3] a + b = " + (a.get() + b.get()));
        return () -> System.out.println("[Cleanup 3]");
    });
    
    return Column(() -> {
        Button("a++", () -> a.update(n -> n + 1));
        Button("b++", () -> b.update(n -> n + 1));
    });
});
```

执行序列：

```
=== 挂载 ===
[Effect 1] a = 1
[Effect 2] b = 2
[Effect 3] a + b = 3

=== 点击 "a++" ===
[Cleanup 1]
[Cleanup 3]
[Effect 1] a = 2
[Effect 3] a + b = 4

=== 点击 "b++" ===
[Cleanup 2]
[Cleanup 3]
[Effect 2] b = 3
[Effect 3] a + b = 5

=== 卸载 ===
[Cleanup 1]
[Cleanup 2]
[Cleanup 3]
```

---

## 依赖注入

### provide() - 获取祖先提供的依赖

```java
<T> T provide(Class<T> type)
```

从组件树中获取祖先组件注入的依赖。

```java
// 消费者组件
Component userDisplay = Component.stateless(ctx -> {
    // 从祖先获取 UserService
    var userService = ctx.provide(UserService.class);
    var currentUser = userService.getCurrentUser();
    
    return Render.text(() -> "Hello, " + currentUser.get().name());
});
```

### inject() - 向后代提供依赖

```java
<T> void inject(Class<T> type, T value)
```

向后代组件提供依赖。

```java
// 提供者组件
Component app = Component.stateful(ctx -> {
    // 创建服务
    var userService = new UserServiceImpl();
    var themeService = new ThemeServiceImpl();
    
    // 注入到组件树
    ctx.inject(UserService.class, userService);
    ctx.inject(ThemeService.class, themeService);
    
    // 所有后代组件都可以通过 provide() 获取这些服务
    return Column(() -> {
        Child(header);       // 可以获取 UserService, ThemeService
        Child(mainContent);  // 可以获取 UserService, ThemeService
        Child(footer);       // 可以获取 UserService, ThemeService
    });
});
```

### 完整的依赖注入示例

```java
// === 定义服务接口 ===
interface AuthService {
    Signal<User> getCurrentUser();
    void login(String username, String password);
    void logout();
}

// === 实现服务 ===
class AuthServiceImpl implements AuthService {
    private final Signal<User> currentUser = Signal.of(null);
    
    @Override
    public Signal<User> getCurrentUser() {
        return currentUser;
    }
    
    @Override
    public void login(String username, String password) {
        // 实际登录逻辑
        currentUser.set(new User(username));
    }
    
    @Override
    public void logout() {
        currentUser.set(null);
    }
}

// === 根组件注入服务 ===
Component app = Component.stateful(ctx -> {
    var authService = new AuthServiceImpl();
    ctx.inject(AuthService.class, authService);
    
    return RenderNode.conditional(
        () -> authService.getCurrentUser().get() != null,
        mainScreen,
        loginScreen
    );
});

// === 子组件消费服务 ===
Component header = Component.stateless(ctx -> {
    var auth = ctx.provide(AuthService.class);
    var user = auth.getCurrentUser();
    
    return Row(() -> {
        Text(() -> "Welcome, " + 
            (user.get() != null ? user.get().name() : "Guest"));
        
        RenderNode.showWhen(
            () -> user.get() != null,
            Render.button("Logout", auth::logout)
        );
    });
});

Component loginScreen = Component.stateful(ctx -> {
    var auth = ctx.provide(AuthService.class);
    var username = ctx.signal("");
    var password = ctx.signal("");
    
    return Column(() -> {
        TextInput(username, "Username");
        PasswordInput(password, "Password");
        Button("Login", () -> 
            auth.login(username.peek(), password.peek()));
    });
});
```

---

## Hook 规则

### 规则 1：只在组件顶层调用 Hook

```java
// ✅ 正确：顶层调用
Component good = Component.stateful(ctx -> {
    var count = ctx.signal(0);
    var name = ctx.signal("");
    
    ctx.effect(() -> {
        System.out.println(count.get());
        return null;
    });
    
    return Render.text("OK");
});

// ❌ 错误：条件内调用
Component bad = Component.stateful(ctx -> {
    if (someCondition) {
        var count = ctx.signal(0);  // 可能导致 Hook 顺序不一致！
    }
    return Render.text("BAD");
});

// ❌ 错误：循环内调用
Component bad2 = Component.stateful(ctx -> {
    for (int i = 0; i < 5; i++) {
        ctx.signal(i);  // Hook 数量不确定！
    }
    return Render.text("BAD");
});
```

**为什么？**
Hook 通过调用顺序（索引）来匹配状态。如果顺序变化，状态会错配：

```
第一次渲染:
  Hook[0] = signal(0)  → count
  Hook[1] = signal("") → name

条件变化后:
  // 跳过了第一个 signal
  Hook[0] = signal("") → 错误地匹配到 count 的位置！
```

### 规则 2：只在 stateful 组件中使用状态 Hook

```java
// ✅ 正确：stateful 组件
Component counter = Component.stateful(ctx -> {
    var count = ctx.signal(0);  // OK
    return Render.text(() -> count.get().toString());
});

// ⚠️ 警告：stateless 组件中使用（技术上可行但不推荐）
Component display = Component.stateless(ctx -> {
    var count = ctx.signal(0);  // 每次渲染都创建新的！
    return Render.text(() -> count.get().toString());
});
```

### 规则 3：不要在 render 返回后调用 Hook

```java
// ❌ 错误
Component bad = Component.stateful(ctx -> {
    var count = ctx.signal(0);
    
    var node = Render.text(() -> count.get().toString());
    
    ctx.effect(() -> {  // 在返回前调用是 OK 的
        System.out.println(count.get());
        return null;
    });
    
    return node;
});

// 注意：实际上上面的例子是 OK 的，因为 effect 在 return 前调用
// 问题是不要在异步回调中调用 Hook
Component bad = Component.stateful(ctx -> {
    var count = ctx.signal(0);
    
    fetchData().then(data -> {
        var newSignal = ctx.signal(data);  // ❌ 异步回调中调用 Hook！
    });
    
    return Render.text("Loading...");
});
```

---

## 高级用法

### 自定义 Hook

可以将相关的 Hook 组合成自定义 Hook：

```java
// 自定义 Hook: useTimer
class TimerHook {
    final Signal<Integer> seconds;
    final Computed<String> formatted;
    
    TimerHook(Signal<Integer> seconds, Computed<String> formatted) {
        this.seconds = seconds;
        this.formatted = formatted;
    }
}

static TimerHook useTimer(ComponentContext ctx) {
    var seconds = ctx.signal(0);
    var formatted = ctx.computed(() -> 
        String.format("%02d:%02d", seconds.get() / 60, seconds.get() % 60));
    
    ctx.onMount(() -> {
        var timer = setInterval(() -> seconds.update(n -> n + 1), 1000);
        ctx.onUnmount(() -> clearInterval(timer));
    });
    
    return new TimerHook(seconds, formatted);
}

// 使用自定义 Hook
Component timerDisplay = Component.stateful(ctx -> {
    var timer = useTimer(ctx);
    
    return Column(() -> {
        Text(() -> "Elapsed: " + timer.formatted.get());
        Button("Reset", () -> timer.seconds.set(0));
    });
});
```

### 异步数据获取

```java
// 自定义 Hook: useFetch
record FetchState<T>(
    boolean isLoading,
    T data,
    String error
) {}

static <T> Signal<FetchState<T>> useFetch(
    ComponentContext ctx, 
    Supplier<CompletableFuture<T>> fetcher
) {
    var state = ctx.signal(new FetchState<T>(true, null, null));
    
    ctx.effect(() -> {
        state.set(new FetchState<>(true, null, null));
        
        fetcher.get()
            .thenAccept(data -> 
                state.set(new FetchState<>(false, data, null)))
            .exceptionally(e -> {
                state.set(new FetchState<>(false, null, e.getMessage()));
                return null;
            });
        
        return null;
    });
    
    return state;
}

// 使用
Component userProfile = Component.stateful(ctx -> {
    var userId = ctx.signal(1);
    var userState = useFetch(ctx, () -> fetchUser(userId.peek()));
    
    return RenderNode.conditional(
        () -> userState.get().isLoading(),
        Render.text("Loading..."),
        RenderNode.conditional(
            () -> userState.get().error() != null,
            Render.text(() -> "Error: " + userState.get().error()),
            userCard(() -> userState.get().data())
        )
    );
});
```

### 表单管理

```java
// 自定义 Hook: useForm
record FormField<T>(Signal<T> value, Computed<String> error) {}

static FormField<String> useTextField(
    ComponentContext ctx, 
    String initial,
    Function<String, String> validator
) {
    var value = ctx.signal(initial);
    var error = ctx.computed(() -> validator.apply(value.get()));
    return new FormField<>(value, error);
}

// 使用
Component registrationForm = Component.stateful(ctx -> {
    var email = useTextField(ctx, "", v -> 
        v.isEmpty() ? "Required" : 
        !v.contains("@") ? "Invalid email" : "");
    
    var password = useTextField(ctx, "", v ->
        v.isEmpty() ? "Required" :
        v.length() < 8 ? "At least 8 characters" : "");
    
    var isValid = ctx.computed(() -> 
        email.error().get().isEmpty() && 
        password.error().get().isEmpty());
    
    return Column(() -> {
        TextInput(email.value(), "Email");
        Text(() -> email.error().get());
        
        PasswordInput(password.value(), "Password");
        Text(() -> password.error().get());
        
        Button("Register", 
            () -> submitForm(email.value().peek(), password.value().peek()),
            Style.builder().enabled(isValid.get()).build());
    });
});
```

---

## 完整案例

### 案例：实时搜索组件

```java
Component searchableList = Component.stateful(ctx -> {
    // 状态
    var query = ctx.signal("");
    var items = ctx.signal(List.<Item>of());
    var isLoading = ctx.signal(false);
    var error = ctx.signal("");
    
    // 派生状态
    var hasResults = ctx.computed(() -> !items.get().isEmpty());
    var hasError = ctx.computed(() -> !error.get().isEmpty());
    
    // 防抖搜索 Effect
    ctx.effect(() -> {
        String q = query.get();
        
        if (q.isEmpty()) {
            items.set(List.of());
            return null;
        }
        
        isLoading.set(true);
        error.set("");
        
        // 防抖定时器
        var timer = setTimeout(() -> {
            searchAPI(q)
                .thenAccept(results -> {
                    items.set(results);
                    isLoading.set(false);
                })
                .exceptionally(e -> {
                    error.set(e.getMessage());
                    isLoading.set(false);
                    return null;
                });
        }, 300);
        
        // Cleanup: 取消之前的定时器
        return () -> clearTimeout(timer);
    });
    
    // UI
    return Column(() -> {
        // 搜索框
        Row(() -> {
            TextInput(query, "Search...");
            RenderNode.showWhen(isLoading::get, loadingSpinner);
        });
        
        Spacer(8);
        
        // 错误显示
        RenderNode.showWhen(
            hasError::get,
            Render.text(() -> "Error: " + error.get(), 
                Style.builder().color(0xFF0000).build())
        );
        
        // 结果列表
        RenderNode.showWhen(
            hasResults::get,
            RenderNode.forEach(
                items::get,
                Item::id,
                item -> itemCard(item)
            )
        );
        
        // 空状态
        RenderNode.showWhen(
            () -> !query.get().isEmpty() && 
                  !isLoading.get() && 
                  !hasResults.get() && 
                  !hasError.get(),
            Render.text("No results found")
        );
    });
});

// 项目卡片组件
static Component itemCard(Item item) {
    return Component.stateless(ctx -> 
        Box(
            Style.builder()
                .padding(8)
                .margin(4)
                .background(0xF5F5F5)
                .borderRadius(4)
                .build(),
            Column(() -> {
                Text(item.title());
                Text(item.description(), 
                    Style.builder().color(0x666666).fontSize(12).build());
            })
        )
    );
}
```

---

## 常见问题与解答

### Q1: provide() 找不到依赖会怎样？

**A**: 会抛出异常。可以提供默认值：

```java
// 可选依赖
var service = ctx.provideOptional(MyService.class)
    .orElse(new DefaultService());

// 或者在根组件总是提供默认值
Component app = Component.stateful(ctx -> {
    ctx.inject(MyService.class, new DefaultMyService());
    return mainContent;
});
```

### Q2: 可以在 effect 中调用 set 吗？

**A**: 可以，但要小心避免无限循环：

```java
// ⚠️ 危险：可能无限循环
ctx.effect(() -> {
    var value = signal.get();
    signal.set(value + 1);  // 这会触发 effect 再次执行！
    return null;
});

// ✅ 安全：使用 peek 或条件判断
ctx.effect(() -> {
    var value = signal.get();
    if (value < 10) {
        signal.set(value + 1);
    }
    return null;
});
```

### Q3: inject 的值可以是响应式的吗？

**A**: 可以！这是推荐的做法：

```java
// 注入响应式状态
Component app = Component.stateful(ctx -> {
    var theme = ctx.signal(Theme.LIGHT);
    ctx.inject(ThemeSignal.class, theme);
    return mainContent;
});

// 消费响应式状态
Component button = Component.stateless(ctx -> {
    var theme = ctx.provide(ThemeSignal.class);
    return Render.button("Click", onClick,
        Style.builder()
            .background(theme.get().primaryColor())
            .build());
});
```

### Q4: onMount 和 effect 有什么区别？

**A**:
- `onMount`：只执行一次（挂载时）
- `effect`：可能执行多次（依赖变化时）

```java
Component demo = Component.stateful(ctx -> {
    var count = ctx.signal(0);
    
    ctx.onMount(() -> {
        System.out.println("Mounted once!");
    });
    
    ctx.effect(() -> {
        System.out.println("Count: " + count.get());  // 每次 count 变化都执行
        return null;
    });
    
    return Render.button("+", () -> count.update(n -> n + 1));
});
```

---

## 下一步

现在你已经理解了 ComponentContext 的 Hook API，接下来学习如何使用 DSL 构建 UI：

➡️ **[06-render-dsl.md](./06-render-dsl.md)** - 渲染 DSL 设计

