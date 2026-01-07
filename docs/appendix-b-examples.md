# Appendix B: Examples - 完整示例代码

## 目录

- [基础示例](#基础示例)
- [表单示例](#表单示例)
- [列表示例](#列表示例)
- [主题切换示例](#主题切换示例)
- [标签页示例](#标签页示例)
- [数据加载示例](#数据加载示例)
- [自定义 Hook 示例](#自定义-hook-示例)
- [组合模式示例](#组合模式示例)

---

## 基础示例

### 计数器

最简单的有状态组件示例。

```java
public class CounterExample {
    
    public static Component counter() {
        return Component.stateful(ctx -> {
            // 创建状态
            var count = ctx.signal(0);
            
            // 操作
            Runnable increment = () -> count.update(n -> n + 1);
            Runnable decrement = () -> count.update(n -> n - 1);
            Runnable reset = () -> count.set(0);
            
            // 渲染
            return Column(() -> {
                // 动态文本
                Text(() -> "Count: " + count.get());
                
                // 按钮行
                Row(() -> {
                    Button("-", decrement);
                    Button("+", increment);
                    Button("Reset", reset);
                });
            });
        });
    }
    
    // 带样式的版本
    public static Component styledCounter() {
        return Component.stateful(ctx -> {
            var count = ctx.signal(0);
            
            // 样式
            Style containerStyle = Style.builder()
                .padding(16)
                .background(0xF0F0F0)
                .borderRadius(8)
                .build();
            
            Style countStyle = Style.builder()
                .fontSize(24)
                .fontColor(0x333333)
                .build();
            
            Style buttonStyle = Style.builder()
                .width(40)
                .height(40)
                .background(0x4488FF)
                .fontColor(0xFFFFFF)
                .borderRadius(4)
                .build();
            
            return Box(containerStyle, () -> {
                Column(() -> {
                    Text(() -> String.valueOf(count.get()), countStyle);
                    
                    Row(() -> {
                        Button("-", buttonStyle, () -> count.update(n -> n - 1));
                        Button("+", buttonStyle, () -> count.update(n -> n + 1));
                    });
                });
            });
        });
    }
}
```

### Hello World

```java
public class HelloWorldExample {
    
    // 最简单的无状态组件
    public static Component helloWorld() {
        return Component.stateless(ctx -> 
            Render.text("Hello, World!")
        );
    }
    
    // 带动态绑定
    public static Component greeting(Signal<String> name) {
        return Component.stateless(ctx -> 
            Render.text(() -> "Hello, " + name.get() + "!")
        );
    }
    
    // 输入姓名
    public static Component interactiveGreeting() {
        return Component.stateful(ctx -> {
            var name = ctx.signal("World");
            
            return Column(() -> {
                Input(name::get, name::set);
                Text(() -> "Hello, " + name.get() + "!");
            });
        });
    }
}
```

---

## 表单示例

### 登录表单

```java
public class LoginFormExample {
    
    record LoginData(String username, String password) {}
    
    public static Component loginForm(Consumer<LoginData> onSubmit) {
        return Component.stateful(ctx -> {
            // 状态
            var username = ctx.signal("");
            var password = ctx.signal("");
            var isSubmitting = ctx.signal(false);
            var error = ctx.signal("");
            
            // 派生状态
            var isValid = ctx.computed(() -> 
                !username.get().isBlank() && 
                password.get().length() >= 6
            );
            
            var buttonText = ctx.computed(() -> 
                isSubmitting.get() ? "Logging in..." : "Login"
            );
            
            // 操作
            Runnable handleSubmit = () -> {
                if (!isValid.get()) {
                    error.set("Please fill in all fields correctly");
                    return;
                }
                
                error.set("");
                isSubmitting.set(true);
                
                // 模拟异步操作
                onSubmit.accept(new LoginData(username.get(), password.get()));
            };
            
            // 样式
            Style formStyle = Style.builder()
                .width(300)
                .padding(24)
                .background(0xFFFFFF)
                .borderRadius(8)
                .build();
            
            Style inputStyle = Style.builder()
                .width(252)
                .height(36)
                .padding(8)
                .borderColor(0xCCCCCC)
                .borderWidth(1)
                .borderRadius(4)
                .build();
            
            Style errorStyle = Style.builder()
                .fontColor(0xFF0000)
                .fontSize(12)
                .build();
            
            // 渲染
            return Box(formStyle, () -> {
                Column(() -> {
                    Text("Login", Style.builder().fontSize(20).build());
                    
                    // 用户名
                    Text("Username");
                    Input(inputStyle, username::get, username::set);
                    
                    // 密码
                    Text("Password");
                    PasswordInput(inputStyle, password::get, password::set);
                    
                    // 错误信息
                    ShowWhen(
                        () -> !error.get().isEmpty(),
                        Render.text(error::get, errorStyle)
                    );
                    
                    // 提交按钮
                    Button(buttonText::get, handleSubmit);
                });
            });
        });
    }
}
```

### 注册表单（带验证）

```java
public class RegistrationFormExample {
    
    public static Component registrationForm() {
        return Component.stateful(ctx -> {
            // 字段
            var email = ctx.signal("");
            var password = ctx.signal("");
            var confirmPassword = ctx.signal("");
            
            // 验证状态
            var emailError = ctx.computed(() -> {
                String e = email.get();
                if (e.isEmpty()) return "";
                if (!e.contains("@")) return "Invalid email format";
                return "";
            });
            
            var passwordError = ctx.computed(() -> {
                String p = password.get();
                if (p.isEmpty()) return "";
                if (p.length() < 8) return "Password must be at least 8 characters";
                if (!p.matches(".*[A-Z].*")) return "Password must contain uppercase";
                if (!p.matches(".*[0-9].*")) return "Password must contain a number";
                return "";
            });
            
            var confirmError = ctx.computed(() -> {
                if (confirmPassword.get().isEmpty()) return "";
                if (!confirmPassword.get().equals(password.get())) {
                    return "Passwords do not match";
                }
                return "";
            });
            
            var isValid = ctx.computed(() -> 
                !email.get().isEmpty() &&
                emailError.get().isEmpty() &&
                !password.get().isEmpty() &&
                passwordError.get().isEmpty() &&
                confirmError.get().isEmpty()
            );
            
            return Column(() -> {
                Text("Create Account", Style.builder().fontSize(20).build());
                
                // Email
                fieldWithValidation("Email", email, emailError);
                
                // Password
                fieldWithValidation("Password", password, passwordError);
                
                // Confirm Password
                fieldWithValidation("Confirm Password", confirmPassword, confirmError);
                
                // Submit
                Button("Register", () -> {
                    if (isValid.get()) {
                        System.out.println("Registering...");
                    }
                });
            });
        });
    }
    
    private static void fieldWithValidation(
        String label, 
        Signal<String> value, 
        Computed<String> error
    ) {
        Column(() -> {
            Text(label);
            Input(value::get, value::set);
            ShowWhen(
                () -> !error.get().isEmpty(),
                Render.text(error::get, Style.builder().fontColor(0xFF0000).build())
            );
        });
    }
}
```

---

## 列表示例

### Todo 列表

```java
public class TodoListExample {
    
    record Todo(String id, String text, boolean completed) {
        Todo toggle() {
            return new Todo(id, text, !completed);
        }
    }
    
    public static Component todoApp() {
        return Component.stateful(ctx -> {
            // 状态
            var todos = ctx.signal(List.<Todo>of());
            var inputText = ctx.signal("");
            var filter = ctx.signal("all"); // all, active, completed
            
            // 派生状态
            var filteredTodos = ctx.computed(() -> {
                String f = filter.get();
                return todos.get().stream()
                    .filter(todo -> switch(f) {
                        case "active" -> !todo.completed();
                        case "completed" -> todo.completed();
                        default -> true;
                    })
                    .toList();
            });
            
            var activeCount = ctx.computed(() -> 
                (int) todos.get().stream().filter(t -> !t.completed()).count()
            );
            
            // 操作
            Runnable addTodo = () -> {
                String text = inputText.get().trim();
                if (!text.isEmpty()) {
                    var newTodo = new Todo(UUID.randomUUID().toString(), text, false);
                    todos.update(list -> {
                        var newList = new ArrayList<>(list);
                        newList.add(newTodo);
                        return newList;
                    });
                    inputText.set("");
                }
            };
            
            Consumer<String> toggleTodo = (id) -> todos.update(list -> 
                list.stream()
                    .map(t -> t.id().equals(id) ? t.toggle() : t)
                    .toList()
            );
            
            Consumer<String> removeTodo = (id) -> todos.update(list -> 
                list.stream()
                    .filter(t -> !t.id().equals(id))
                    .toList()
            );
            
            Runnable clearCompleted = () -> todos.update(list -> 
                list.stream()
                    .filter(t -> !t.completed())
                    .toList()
            );
            
            // 渲染
            return Column(() -> {
                Text("Todo App", Style.builder().fontSize(24).build());
                
                // 输入区
                Row(() -> {
                    Input(inputText::get, inputText::set);
                    Button("Add", addTodo);
                });
                
                // 过滤器
                Row(() -> {
                    filterButton("All", "all", filter);
                    filterButton("Active", "active", filter);
                    filterButton("Completed", "completed", filter);
                });
                
                // 列表
                ForEach(
                    filteredTodos::get,
                    Todo::id,
                    todo -> todoItem(todo, toggleTodo, removeTodo)
                );
                
                // 状态栏
                Row(() -> {
                    Text(() -> activeCount.get() + " items left");
                    Button("Clear Completed", clearCompleted);
                });
            });
        });
    }
    
    private static Component todoItem(
        Todo todo, 
        Consumer<String> onToggle, 
        Consumer<String> onRemove
    ) {
        Style style = Style.builder()
            .padding(8)
            .background(todo.completed() ? 0xF0F0F0 : 0xFFFFFF)
            .build();
        
        Style textStyle = Style.builder()
            .fontColor(todo.completed() ? 0x999999 : 0x333333)
            .build();
        
        return Component.stateless(ctx -> 
            Row(style, () -> {
                Checkbox(todo.completed(), () -> onToggle.accept(todo.id()));
                Text(todo.text(), textStyle);
                Button("×", () -> onRemove.accept(todo.id()));
            })
        );
    }
    
    private static void filterButton(String label, String value, Signal<String> current) {
        Style style = Style.builder()
            .padding(4, 8, 4, 8)
            .background(current.peek().equals(value) ? 0x4488FF : 0xE0E0E0)
            .fontColor(current.peek().equals(value) ? 0xFFFFFF : 0x333333)
            .borderRadius(4)
            .build();
        
        Button(label, style, () -> current.set(value));
    }
}
```

### 虚拟列表

```java
public class VirtualListExample {
    
    record Item(int id, String name) {}
    
    public static Component virtualList(
        Signal<List<Item>> items,
        int itemHeight,
        int visibleHeight
    ) {
        return Component.stateful(ctx -> {
            var scrollOffset = ctx.signal(0);
            
            // 计算可见项
            var visibleRange = ctx.computed(() -> {
                int offset = scrollOffset.get();
                int startIndex = offset / itemHeight;
                int endIndex = startIndex + (visibleHeight / itemHeight) + 1;
                return new int[]{startIndex, endIndex};
            });
            
            var visibleItems = ctx.computed(() -> {
                int[] range = visibleRange.get();
                List<Item> all = items.get();
                return all.subList(
                    Math.max(0, range[0]),
                    Math.min(all.size(), range[1])
                );
            });
            
            var totalHeight = ctx.computed(() -> items.get().size() * itemHeight);
            var offsetY = ctx.computed(() -> (visibleRange.get()[0]) * itemHeight);
            
            Style containerStyle = Style.builder()
                .height(visibleHeight)
                .build();
            
            Style innerStyle = Style.builder()
                .height(totalHeight.get())
                .build();
            
            return ScrollView(
                containerStyle,
                onScroll(scrollOffset::set),
                Box(innerStyle, () -> {
                    Box(Style.builder().marginTop(offsetY.get()).build(), () -> {
                        ForEach(
                            visibleItems::get,
                            Item::id,
                            item -> itemRow(item, itemHeight)
                        );
                    });
                })
            );
        });
    }
    
    private static Component itemRow(Item item, int height) {
        Style style = Style.builder()
            .height(height)
            .padding(8)
            .background(item.id() % 2 == 0 ? 0xF8F8F8 : 0xFFFFFF)
            .build();
        
        return Component.stateless(ctx -> 
            Row(style, () -> {
                Text(String.valueOf(item.id()));
                Text(item.name());
            })
        );
    }
}
```

---

## 主题切换示例

```java
public class ThemingExample {
    
    record Theme(
        int background,
        int text,
        int primary,
        int secondary,
        int accent
    ) {}
    
    static final Key<Theme> THEME_KEY = Key.create("theme");
    
    static final Theme LIGHT_THEME = new Theme(
        0xFFFFFF, 0x333333, 0x4488FF, 0x6699CC, 0xFF8844
    );
    
    static final Theme DARK_THEME = new Theme(
        0x1E1E1E, 0xE0E0E0, 0x66AAFF, 0x4477AA, 0xFFAA66
    );
    
    public static Component themedApp() {
        return Component.stateful(ctx -> {
            var isDark = ctx.signal(false);
            var theme = ctx.computed(() -> isDark.get() ? DARK_THEME : LIGHT_THEME);
            
            // 提供主题
            ctx.effect(() -> {
                ctx.provide(THEME_KEY, theme.get());
                return null;
            });
            
            Style containerStyle = Style.builder()
                .background(theme.get().background())
                .padding(16)
                .build();
            
            return Box(containerStyle, () -> {
                Column(() -> {
                    // 主题切换按钮
                    themeToggle(isDark);
                    
                    // 主题化内容
                    Child(themedContent());
                });
            });
        });
    }
    
    private static void themeToggle(Signal<Boolean> isDark) {
        Row(() -> {
            Text(() -> isDark.get() ? "Dark Mode" : "Light Mode");
            Switch(isDark::get, isDark::set);
        });
    }
    
    private static Component themedContent() {
        return Component.stateless(ctx -> {
            var theme = ctx.inject(THEME_KEY, LIGHT_THEME);
            
            Style cardStyle = Style.builder()
                .background(theme.secondary())
                .padding(16)
                .borderRadius(8)
                .build();
            
            Style textStyle = Style.builder()
                .fontColor(theme.text())
                .build();
            
            Style buttonStyle = Style.builder()
                .background(theme.primary())
                .fontColor(0xFFFFFF)
                .padding(8, 16, 8, 16)
                .borderRadius(4)
                .build();
            
            return Box(cardStyle, () -> {
                Column(() -> {
                    Text("Themed Card", textStyle);
                    Text("This content respects the theme", textStyle);
                    Button("Action", buttonStyle, () -> {});
                });
            });
        });
    }
}
```

---

## 标签页示例

```java
public class TabsExample {
    
    record Tab(String id, String label, Component content) {}
    
    public static Component tabs(List<Tab> tabs) {
        return Component.stateful(ctx -> {
            var activeTab = ctx.signal(tabs.get(0).id());
            
            Style tabBarStyle = Style.builder()
                .background(0xF0F0F0)
                .build();
            
            return Column(() -> {
                // 标签栏
                Row(tabBarStyle, () -> {
                    for (Tab tab : tabs) {
                        tabButton(tab, activeTab);
                    }
                });
                
                // 内容区
                Box(() -> {
                    ForEach(
                        () -> tabs,
                        Tab::id,
                        tab -> tabContent(tab, activeTab)
                    );
                });
            });
        });
    }
    
    private static void tabButton(Tab tab, Signal<String> activeTab) {
        var isActive = activeTab.peek().equals(tab.id());
        
        Style style = Style.builder()
            .padding(8, 16, 8, 16)
            .background(isActive ? 0xFFFFFF : 0xF0F0F0)
            .fontColor(isActive ? 0x4488FF : 0x666666)
            .build();
        
        Button(tab.label(), style, () -> activeTab.set(tab.id()));
    }
    
    private static Component tabContent(Tab tab, Signal<String> activeTab) {
        return Component.stateless(ctx -> 
            ShowWhen(
                () -> activeTab.get().equals(tab.id()),
                RenderNode.of(tab.content())
            )
        );
    }
    
    // 使用示例
    public static Component example() {
        return tabs(List.of(
            new Tab("home", "Home", homeContent()),
            new Tab("profile", "Profile", profileContent()),
            new Tab("settings", "Settings", settingsContent())
        ));
    }
    
    private static Component homeContent() {
        return Component.stateless(ctx -> Render.text("Home Content"));
    }
    
    private static Component profileContent() {
        return Component.stateless(ctx -> Render.text("Profile Content"));
    }
    
    private static Component settingsContent() {
        return Component.stateless(ctx -> Render.text("Settings Content"));
    }
}
```

---

## 数据加载示例

```java
public class DataLoadingExample {
    
    enum LoadState { IDLE, LOADING, SUCCESS, ERROR }
    
    record AsyncData<T>(LoadState state, T data, String error) {
        static <T> AsyncData<T> idle() { return new AsyncData<>(LoadState.IDLE, null, null); }
        static <T> AsyncData<T> loading() { return new AsyncData<>(LoadState.LOADING, null, null); }
        static <T> AsyncData<T> success(T data) { return new AsyncData<>(LoadState.SUCCESS, data, null); }
        static <T> AsyncData<T> error(String msg) { return new AsyncData<>(LoadState.ERROR, null, msg); }
    }
    
    public static Component userProfile(String userId) {
        return Component.stateful(ctx -> {
            var userData = ctx.signal(AsyncData.<User>idle());
            
            // 加载数据
            Runnable loadUser = () -> {
                userData.set(AsyncData.loading());
                
                // 模拟异步加载
                CompletableFuture
                    .supplyAsync(() -> fetchUser(userId))
                    .thenAccept(user -> userData.set(AsyncData.success(user)))
                    .exceptionally(e -> {
                        userData.set(AsyncData.error(e.getMessage()));
                        return null;
                    });
            };
            
            // 挂载时加载
            ctx.onMount(loadUser);
            
            // 渲染
            return Column(() -> {
                Text("User Profile");
                
                // 根据状态渲染不同内容
                Child(switch (userData.get().state()) {
                    case IDLE -> Component.stateless(c -> Render.empty());
                    case LOADING -> loadingSpinner();
                    case SUCCESS -> userCard(userData.get().data());
                    case ERROR -> errorMessage(userData.get().error(), loadUser);
                });
            });
        });
    }
    
    private static Component loadingSpinner() {
        return Component.stateless(ctx -> 
            Box(Style.builder().padding(20).build(), () -> {
                Text("Loading...");
            })
        );
    }
    
    private static Component userCard(User user) {
        return Component.stateless(ctx -> {
            Style style = Style.builder()
                .padding(16)
                .background(0xF8F8F8)
                .borderRadius(8)
                .build();
            
            return Box(style, () -> {
                Column(() -> {
                    Text(user.name());
                    Text(user.email());
                });
            });
        });
    }
    
    private static Component errorMessage(String error, Runnable onRetry) {
        return Component.stateless(ctx -> {
            Style style = Style.builder()
                .padding(16)
                .background(0xFFEEEE)
                .borderRadius(8)
                .build();
            
            return Box(style, () -> {
                Column(() -> {
                    Text("Error: " + error, Style.builder().fontColor(0xFF0000).build());
                    Button("Retry", onRetry);
                });
            });
        });
    }
    
    private static User fetchUser(String id) {
        // 模拟 API 调用
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        return new User("John Doe", "john@example.com");
    }
    
    record User(String name, String email) {}
}
```

---

## 自定义 Hook 示例

```java
public class CustomHooksExample {
    
    // useDebounce - 防抖
    public static Signal<String> useDebounce(
        ComponentContext ctx, 
        Signal<String> value, 
        int delayMs
    ) {
        var debouncedValue = ctx.signal(value.get());
        var timerRef = ctx.<ScheduledFuture<?>>ref();
        
        ctx.effect(() -> {
            String current = value.get();
            
            // 取消之前的定时器
            if (timerRef.get() != null) {
                timerRef.get().cancel(false);
            }
            
            // 设置新定时器
            timerRef.set(scheduler.schedule(
                () -> debouncedValue.set(current),
                delayMs, TimeUnit.MILLISECONDS
            ));
            
            return () -> {
                if (timerRef.get() != null) {
                    timerRef.get().cancel(false);
                }
            };
        });
        
        return debouncedValue;
    }
    
    // useLocalStorage - 本地存储
    public static Signal<String> useLocalStorage(
        ComponentContext ctx, 
        String key, 
        String defaultValue
    ) {
        // 从存储读取初始值
        String initial = localStorage.get(key, defaultValue);
        var value = ctx.signal(initial);
        
        // 值变化时保存到存储
        ctx.effect(() -> {
            String current = value.get();
            localStorage.set(key, current);
            return null;
        });
        
        return value;
    }
    
    // useWindowSize - 窗口尺寸
    public static Computed<Size> useWindowSize(ComponentContext ctx) {
        var width = ctx.signal(800);
        var height = ctx.signal(600);
        
        ctx.effect(() -> {
            var listener = new WindowResizeListener() {
                public void onResize(int w, int h) {
                    width.set(w);
                    height.set(h);
                }
            };
            window.addResizeListener(listener);
            return () -> window.removeResizeListener(listener);
        });
        
        return ctx.computed(() -> new Size(width.get(), height.get()));
    }
    
    record Size(int width, int height) {}
    
    // 使用自定义 Hook
    public static Component searchBox() {
        return Component.stateful(ctx -> {
            var searchText = ctx.signal("");
            var debouncedSearch = useDebounce(ctx, searchText, 300);
            
            // 只在防抖后的值变化时搜索
            ctx.effect(() -> {
                String query = debouncedSearch.get();
                if (!query.isEmpty()) {
                    performSearch(query);
                }
                return null;
            });
            
            return Input(searchText::get, searchText::set);
        });
    }
}
```

---

## 组合模式示例

### 复合组件模式

```java
public class CompoundComponentExample {
    
    // Card 复合组件
    public static class Card {
        public static Component container(Component... children) {
            Style style = Style.builder()
                .background(0xFFFFFF)
                .borderRadius(8)
                .padding(16)
                .build();
            
            return Component.stateless(ctx -> 
                Box(style, () -> {
                    Column(() -> {
                        for (Component child : children) {
                            Child(child);
                        }
                    });
                })
            );
        }
        
        public static Component header(String title) {
            Style style = Style.builder()
                .fontSize(18)
                .fontColor(0x333333)
                .build();
            
            return Component.stateless(ctx -> Render.text(title, style));
        }
        
        public static Component body(Component content) {
            return Component.stateless(ctx -> 
                Box(Style.builder().padding(8, 0, 8, 0).build(), () -> {
                    Child(content);
                })
            );
        }
        
        public static Component footer(Component... actions) {
            return Component.stateless(ctx -> 
                Row(() -> {
                    for (Component action : actions) {
                        Child(action);
                    }
                })
            );
        }
    }
    
    // 使用
    public static Component userCard(User user) {
        return Card.container(
            Card.header(user.name()),
            Card.body(Component.stateless(ctx -> 
                Column(() -> {
                    Text(user.email());
                    Text(user.bio());
                })
            )),
            Card.footer(
                Button("Edit", () -> {}),
                Button("Delete", () -> {})
            )
        );
    }
}
```

### Render Props 模式

```java
public class RenderPropsExample {
    
    // 可拖拽组件
    public static Component draggable(
        Function<DragState, Component> render
    ) {
        return Component.stateful(ctx -> {
            var isDragging = ctx.signal(false);
            var position = ctx.signal(new Point(0, 0));
            
            var dragState = ctx.computed(() -> 
                new DragState(isDragging.get(), position.get())
            );
            
            // 拖拽逻辑
            ctx.effect(() -> {
                if (isDragging.get()) {
                    var listener = new MouseMoveListener() {
                        public void onMove(int x, int y) {
                            position.set(new Point(x, y));
                        }
                    };
                    window.addMouseMoveListener(listener);
                    return () -> window.removeMouseMoveListener(listener);
                }
                return null;
            });
            
            return Box(
                onMouseDown(() -> isDragging.set(true)),
                onMouseUp(() -> isDragging.set(false)),
                Child(render.apply(dragState.get()))
            );
        });
    }
    
    record DragState(boolean isDragging, Point position) {}
    record Point(int x, int y) {}
    
    // 使用
    public static Component draggableBox() {
        return draggable(state -> 
            Component.stateless(ctx -> {
                Style style = Style.builder()
                    .width(100)
                    .height(100)
                    .background(state.isDragging() ? 0x4488FF : 0x888888)
                    .build();
                
                return Box(style, () -> {
                    Text("Drag me!");
                    Text("Position: " + state.position().x() + ", " + state.position().y());
                });
            })
        );
    }
}
```

---

## 参见

- **[04. Component](./04-component.md)** - 组件系统详解
- **[05. ComponentContext](./05-component-context.md)** - Hook API 详解
- **[12. Best Practices](./12-best-practices.md)** - 最佳实践

