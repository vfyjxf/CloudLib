# 08. Element Tree - 元素树与协调算法

## 目录

- [概述](#概述)
- [Element 的角色](#element-的角色)
- [Element 类型层次](#element-类型层次)
- [ElementTree 管理](#elementtree-管理)
- [协调算法（Reconciliation）](#协调算法reconciliation)
- [生命周期管理](#生命周期管理)
- [Key 与身份](#key-与身份)
- [性能优化](#性能优化)
- [完整案例](#完整案例)
- [常见问题与解答](#常见问题与解答)

---

## 概述

**Element** 是 UI 的实际实例，是 RenderNode 的"实现"。如果 RenderNode 是蓝图，那 Element 就是根据蓝图建造的实际建筑。

```
┌─────────────────────────────────────────────────────────┐
│                 RenderNode → Element                    │
│                                                         │
│  RenderNode (What)          Element (How)               │
│  ┌──────────────────┐       ┌──────────────────┐       │
│  │ "我想要一个按钮  │       │ 实际的按钮实例    │       │
│  │  文字是 Click"   │──────▶│ 可以接收点击     │       │
│  │                  │       │ 有渲染状态       │       │
│  └──────────────────┘       └──────────────────┘       │
│                                                         │
│  特点对比：                                             │
│  • 不可变             • 可变                           │
│  • 轻量描述           • 完整实例                       │
│  • 每帧重建           • 跨帧复用                       │
│  • 无状态             • 有状态                         │
└─────────────────────────────────────────────────────────┘
```

---

## Element 的角色

### Element 职责

```
┌─────────────────────────────────────────────────────────┐
│                    Element 职责                         │
│                                                         │
│  1. 持有状态                                            │
│     • 组件状态（Signal, Computed）                     │
│     • 渲染状态（是否脏、是否挂载）                     │
│     • 子元素引用                                       │
│                                                         │
│  2. 管理生命周期                                        │
│     • mount() - 挂载                                   │
│     • update() - 更新                                  │
│     • unmount() - 卸载                                 │
│                                                         │
│  3. 执行渲染                                            │
│     • 将 RenderNode 转换为实际 UI                      │
│     • 处理布局计算                                     │
│     • 执行绘制                                         │
│                                                         │
│  4. 响应变化                                            │
│     • 接收 RenderNode 更新                             │
│     • 协调子元素                                       │
│     • 触发重绘                                         │
└─────────────────────────────────────────────────────────┘
```

### Element 与 RenderNode 的映射

```java
// 一个 RenderNode 类型对应一个 Element 类型
TextNode      → TextElement
ColumnNode    → ColumnElement
ComponentRef  → ComponentElement
ConditionalNode → ConditionalElement
ForEachNode   → ForEachElement
```

---

## Element 类型层次

```
Element (abstract)
│
├── LeafElement (abstract)
│   ├── TextElement
│   ├── ImageElement
│   └── ButtonElement
│
├── ContainerElement (abstract)
│   ├── ColumnElement
│   ├── RowElement
│   ├── BoxElement
│   └── ScrollElement
│
├── ComponentElement
│
└── StructuralElement (abstract)
    ├── ConditionalElement
    └── ForEachElement
```

### 基类 Element

```java
public abstract class Element {
    protected Element parent;
    protected RenderNode renderNode;
    protected boolean mounted = false;
    protected boolean dirty = false;
    
    // 生命周期
    public abstract void mount();
    public abstract void update(RenderNode newNode);
    public abstract void unmount();
    
    // 渲染
    public abstract void render(RenderContext ctx);
    
    // 标记需要更新
    public void markDirty() {
        this.dirty = true;
        if (parent != null) {
            parent.markDirty();
        }
    }
}
```

### ComponentElement - 组件元素

```java
public class ComponentElement extends Element {
    private final Component component;
    private final ComponentContext context;
    
    // 组件状态存储
    private final List<Signal<?>> signals = new ArrayList<>();
    private final List<Computed<?>> computeds = new ArrayList<>();
    private final List<EffectHandle> effects = new ArrayList<>();
    
    // 子元素
    private Element child;
    
    @Override
    public void mount() {
        // 1. 创建 context
        context = new ComponentContextImpl(this);
        
        // 2. 执行 render，获取 RenderNode
        RenderNode node = component.render(context);
        
        // 3. 为 RenderNode 创建子 Element
        child = createElement(node);
        child.parent = this;
        child.mount();
        
        // 4. 执行 effects
        for (EffectHandle effect : effects) {
            effect.run();
        }
        
        // 5. 执行 onMount 回调
        context.triggerMount();
        
        mounted = true;
    }
    
    @Override
    public void update(RenderNode newNode) {
        // ComponentRef 更新时，重新渲染
        ComponentRef ref = (ComponentRef) newNode;
        
        if (ref.component() == component) {
            // 同一个组件，协调子元素
            RenderNode childNode = component.render(context);
            reconcile(child, childNode);
        } else {
            // 不同组件，完全重建
            unmount();
            mount();
        }
    }
    
    @Override
    public void unmount() {
        // 1. 执行 effect cleanup
        for (EffectHandle effect : effects) {
            effect.cleanup();
        }
        
        // 2. 执行 onUnmount 回调
        context.triggerUnmount();
        
        // 3. 卸载子元素
        child.unmount();
        
        // 4. 清理状态
        signals.clear();
        computeds.clear();
        effects.clear();
        
        mounted = false;
    }
}
```

### ContainerElement - 容器元素

```java
public abstract class ContainerElement extends Element {
    protected List<Element> children = new ArrayList<>();
    
    @Override
    public void mount() {
        ContainerNode node = (ContainerNode) renderNode;
        
        for (RenderNode childNode : node.children()) {
            Element child = createElement(childNode);
            child.parent = this;
            children.add(child);
            child.mount();
        }
        
        mounted = true;
    }
    
    @Override
    public void update(RenderNode newNode) {
        ContainerNode newContainerNode = (ContainerNode) newNode;
        List<RenderNode> newChildren = newContainerNode.children();
        
        // 协调子元素
        reconcileChildren(children, newChildren);
        
        // 更新自身属性
        this.renderNode = newNode;
    }
    
    @Override
    public void unmount() {
        for (Element child : children) {
            child.unmount();
        }
        children.clear();
        mounted = false;
    }
}
```

---

## ElementTree 管理

### ElementTree 类

```java
public class ElementTree {
    private Element root;
    private final Screen screen;
    
    public ElementTree(Screen screen) {
        this.screen = screen;
    }
    
    // 初始化树
    public void mount(Component rootComponent) {
        RenderNode node = new ComponentRef(rootComponent, null);
        root = createElement(node);
        root.mount();
    }
    
    // 更新树
    public void update() {
        if (root.dirty) {
            root.update(root.renderNode);
            root.dirty = false;
        }
    }
    
    // 渲染树
    public void render(RenderContext ctx) {
        root.render(ctx);
    }
    
    // 销毁树
    public void unmount() {
        root.unmount();
        root = null;
    }
}
```

### 创建 Element

```java
public static Element createElement(RenderNode node) {
    return switch (node) {
        case EmptyNode e -> new EmptyElement();
        case TextNode t -> new TextElement(t);
        case ImageNode i -> new ImageElement(i);
        case ButtonNode b -> new ButtonElement(b);
        case ColumnNode c -> new ColumnElement(c);
        case RowNode r -> new RowElement(r);
        case BoxNode b -> new BoxElement(b);
        case ScrollNode s -> new ScrollElement(s);
        case ComponentRef ref -> new ComponentElement(ref);
        case ConditionalNode cond -> new ConditionalElement(cond);
        case ForEachNode<?> each -> new ForEachElement(each);
        default -> throw new IllegalArgumentException("Unknown node type");
    };
}
```

---

## 协调算法（Reconciliation）

协调是对比新旧 RenderNode，决定如何更新 Element 的过程。

### 协调策略

```
┌─────────────────────────────────────────────────────────┐
│                     协调策略                            │
│                                                         │
│  规则 1: 类型相同 → 更新                               │
│  ┌─────────┐      ┌─────────┐                          │
│  │ TextNode│  →   │ TextNode│   = 更新 TextElement    │
│  │ "Hello" │      │ "World" │     的文本              │
│  └─────────┘      └─────────┘                          │
│                                                         │
│  规则 2: 类型不同 → 替换                               │
│  ┌─────────┐      ┌─────────┐                          │
│  │ TextNode│  →   │ImageNode│   = 销毁 TextElement    │
│  │ "Hello" │      │ icon.png│     创建 ImageElement   │
│  └─────────┘      └─────────┘                          │
│                                                         │
│  规则 3: 有 Key → 按 Key 匹配                          │
│  [A, B, C] → [B, C, D]                                 │
│  Key 匹配: B→B, C→C                                    │
│  Key 新增: D                                           │
│  Key 删除: A                                           │
└─────────────────────────────────────────────────────────┘
```

### 基础协调

```java
void reconcile(Element element, RenderNode newNode) {
    RenderNode oldNode = element.renderNode;
    
    // 类型相同，更新
    if (sameType(oldNode, newNode)) {
        element.update(newNode);
    } else {
        // 类型不同，替换
        Element parent = element.parent;
        int index = parent.indexOfChild(element);
        
        element.unmount();
        Element newElement = createElement(newNode);
        parent.replaceChild(index, newElement);
        newElement.mount();
    }
}

boolean sameType(RenderNode a, RenderNode b) {
    return a.getClass() == b.getClass();
}
```

### 子元素列表协调

```java
void reconcileChildren(
    List<Element> oldChildren, 
    List<RenderNode> newChildren
) {
    int oldSize = oldChildren.size();
    int newSize = newChildren.size();
    
    // 更新共有部分
    int minSize = Math.min(oldSize, newSize);
    for (int i = 0; i < minSize; i++) {
        reconcile(oldChildren.get(i), newChildren.get(i));
    }
    
    // 移除多余的旧元素
    for (int i = newSize; i < oldSize; i++) {
        oldChildren.get(i).unmount();
    }
    if (newSize < oldSize) {
        oldChildren.subList(newSize, oldSize).clear();
    }
    
    // 添加新元素
    for (int i = oldSize; i < newSize; i++) {
        Element newChild = createElement(newChildren.get(i));
        newChild.parent = this;
        oldChildren.add(newChild);
        newChild.mount();
    }
}
```

### 带 Key 的协调

```java
void reconcileWithKeys(
    List<Element> oldChildren,
    List<RenderNode> newChildren,
    Function<RenderNode, Object> keyExtractor
) {
    // 构建旧元素的 key → element 映射
    Map<Object, Element> keyToElement = new HashMap<>();
    for (Element child : oldChildren) {
        Object key = keyExtractor.apply(child.renderNode);
        if (key != null) {
            keyToElement.put(key, child);
        }
    }
    
    List<Element> newElementList = new ArrayList<>();
    Set<Object> usedKeys = new HashSet<>();
    
    // 遍历新节点
    for (RenderNode newNode : newChildren) {
        Object key = keyExtractor.apply(newNode);
        Element element;
        
        if (key != null && keyToElement.containsKey(key)) {
            // 找到匹配的旧元素，复用
            element = keyToElement.get(key);
            element.update(newNode);
            usedKeys.add(key);
        } else {
            // 没有匹配，创建新元素
            element = createElement(newNode);
            element.mount();
        }
        
        element.parent = this;
        newElementList.add(element);
    }
    
    // 卸载未使用的旧元素
    for (var entry : keyToElement.entrySet()) {
        if (!usedKeys.contains(entry.getKey())) {
            entry.getValue().unmount();
        }
    }
    
    // 替换子元素列表
    oldChildren.clear();
    oldChildren.addAll(newElementList);
}
```

---

## 生命周期管理

### 生命周期状态图

```
┌─────────────────────────────────────────────────────────┐
│                    Element 生命周期                     │
│                                                         │
│  ┌─────────────┐                                       │
│  │  Created    │  ← createElement()                    │
│  └──────┬──────┘                                       │
│         │ mount()                                       │
│         ▼                                               │
│  ┌─────────────┐                                       │
│  │   Mounted   │  ← 可以接收更新和渲染                 │
│  └──────┬──────┘                                       │
│         │                                               │
│         │ ◀──── update() ◀──── 状态变化               │
│         │         │                                     │
│         │         ▼                                     │
│         │  ┌─────────────┐                             │
│         │  │   Updated   │                             │
│         │  └──────┬──────┘                             │
│         │         │                                     │
│         │         └──────▶ 继续 Mounted 状态           │
│         │                                               │
│         │ unmount()                                     │
│         ▼                                               │
│  ┌─────────────┐                                       │
│  │  Unmounted  │  ← 资源已释放                         │
│  └─────────────┘                                       │
└─────────────────────────────────────────────────────────┘
```

### 挂载过程

```java
// ComponentElement.mount() 详细流程
public void mount() {
    // Phase 1: 初始化
    mounted = false;
    context = new ComponentContextImpl(this);
    
    // Phase 2: 首次渲染
    context.setPhase(Phase.RENDER);
    RenderNode node = component.render(context);
    
    // Phase 3: 创建子树
    child = createElement(node);
    child.parent = this;
    child.mount();  // 递归挂载
    
    // Phase 4: 执行 Effects
    context.setPhase(Phase.EFFECT);
    for (EffectHandle effect : effects) {
        effect.run();
    }
    
    // Phase 5: 触发 onMount
    context.setPhase(Phase.CALLBACK);
    for (Runnable callback : onMountCallbacks) {
        callback.run();
    }
    
    mounted = true;
}
```

### 卸载过程

```java
// ComponentElement.unmount() 详细流程
public void unmount() {
    // Phase 1: 标记卸载中
    unmounting = true;
    
    // Phase 2: 执行 Effect Cleanup
    for (EffectHandle effect : effects) {
        Runnable cleanup = effect.getCleanup();
        if (cleanup != null) {
            cleanup.run();
        }
    }
    
    // Phase 3: 触发 onUnmount
    for (Runnable callback : onUnmountCallbacks) {
        callback.run();
    }
    
    // Phase 4: 卸载子树
    if (child != null) {
        child.unmount();
        child = null;
    }
    
    // Phase 5: 清理状态
    signals.clear();
    computeds.clear();
    effects.clear();
    
    // Phase 6: 断开响应式订阅
    for (Signal<?> signal : signals) {
        signal.clearSubscribers();
    }
    
    mounted = false;
    unmounting = false;
}
```

---

## Key 与身份

### Key 的作用

```
场景：待办事项列表

items = [
    { id: 1, text: "Buy milk" },
    { id: 2, text: "Walk dog" },
    { id: 3, text: "Do laundry" }
]

用户删除第一项后：
items = [
    { id: 2, text: "Walk dog" },
    { id: 3, text: "Do laundry" }
]

无 Key 协调：
  位置 0: item1 → item2  (更新文本: "Buy milk" → "Walk dog")
  位置 1: item2 → item3  (更新文本: "Walk dog" → "Do laundry")
  位置 2: item3 → null   (销毁 Element)
  
  问题：如果 item1 有输入框，用户正在输入，焦点会丢失！

有 Key 协调 (key = id):
  key=1: 存在 → 不存在  (销毁 Element)
  key=2: 位置 0 → 位置 0  (保持不变)
  key=3: 位置 1 → 位置 1  (保持不变)
  
  优势：item2 和 item3 的 Element 被复用，状态保持
```

### Key 的最佳实践

```java
// ✅ 好的 Key：稳定、唯一
RenderNode.forEach(
    items::get,
    Item::id,  // 使用数据库 ID
    item -> itemCard(item)
);

// ✅ 好的 Key：组合唯一
RenderNode.forEach(
    items::get,
    item -> item.category() + "-" + item.id(),
    item -> itemCard(item)
);

// ❌ 坏的 Key：索引（列表重排时无效）
int[] index = {0};
RenderNode.forEach(
    items::get,
    item -> index[0]++,  // 索引作为 key
    item -> itemCard(item)
);

// ❌ 坏的 Key：随机值（每次渲染都不同）
RenderNode.forEach(
    items::get,
    item -> Math.random(),  // 每次都不同！
    item -> itemCard(item)
);
```

---

## 性能优化

### 优化 1：跳过未变化的子树

```java
public void update(RenderNode newNode) {
    // 如果 RenderNode 引用相同，跳过更新
    if (renderNode == newNode) {
        return;
    }
    
    // 如果节点内容相等，跳过更新
    if (renderNode.equals(newNode)) {
        return;
    }
    
    // 执行实际更新
    doUpdate(newNode);
}
```

### 优化 2：批量更新

```java
public class UpdateBatcher {
    private final Set<Element> pendingUpdates = new LinkedHashSet<>();
    private boolean isFlushing = false;
    
    public void scheduleUpdate(Element element) {
        pendingUpdates.add(element);
        
        if (!isFlushing) {
            // 下一帧执行
            Platform.runLater(this::flush);
        }
    }
    
    private void flush() {
        isFlushing = true;
        
        try {
            for (Element element : pendingUpdates) {
                element.update(element.renderNode);
            }
        } finally {
            pendingUpdates.clear();
            isFlushing = false;
        }
    }
}
```

### 优化 3：细粒度更新

```java
// 不更新整个 ComponentElement，只更新需要变化的部分
public class TextElement extends Element {
    private String currentText;
    
    public void updateText(String newText) {
        if (!currentText.equals(newText)) {
            currentText = newText;
            // 只重绘文本，不重建 Element
            invalidateRender();
        }
    }
}

// 在渲染时
TextNode textNode = (TextNode) renderNode;
String text = textNode.textSupplier().get();  // 响应式获取
if (!text.equals(currentText)) {
    updateText(text);
}
```

---

## 完整案例

### 案例：动态列表

```java
// 数据模型
record Task(String id, String title, boolean done) {}

// 组件
Component taskList = Component.stateful(ctx -> {
    var tasks = ctx.signal(List.of(
        new Task("1", "Learn Java", false),
        new Task("2", "Build UI", false),
        new Task("3", "Write docs", true)
    ));
    
    Runnable addTask = () -> {
        String id = UUID.randomUUID().toString();
        tasks.update(list -> {
            var newList = new ArrayList<>(list);
            newList.add(new Task(id, "New Task", false));
            return newList;
        });
    };
    
    Consumer<String> toggleTask = (taskId) -> {
        tasks.update(list -> list.stream()
            .map(t -> t.id().equals(taskId) 
                ? new Task(t.id(), t.title(), !t.done()) 
                : t)
            .toList());
    };
    
    Consumer<String> removeTask = (taskId) -> {
        tasks.update(list -> list.stream()
            .filter(t -> !t.id().equals(taskId))
            .toList());
    };
    
    return Column(() -> {
        // 添加按钮
        Button("Add Task", addTask);
        
        Spacer(8);
        
        // 任务列表 - 使用 forEach 带 key
        RenderNode.forEach(
            tasks::get,
            Task::id,  // key
            task -> taskItem(task, toggleTask, removeTask)
        );
    });
});

// 单个任务项
static Component taskItem(
    Task task,
    Consumer<String> onToggle,
    Consumer<String> onRemove
) {
    return Component.stateful(ctx -> {
        // 本地状态：编辑模式
        var isEditing = ctx.signal(false);
        var editText = ctx.signal(task.title());
        
        // 这个 Component 的 Element 会通过 key 被复用
        // 即使列表重新排序，状态也会保持
        
        return Row(8, () -> {
            // 复选框
            Checkbox(task.done(), () -> onToggle.accept(task.id()));
            
            // 标题（可编辑）
            RenderNode.conditional(
                isEditing::get,
                // 编辑模式
                Row(() -> {
                    TextInput(editText);
                    Button("Save", () -> isEditing.set(false));
                }),
                // 显示模式
                Box(
                    Style.builder()
                        .textDecoration(task.done() ? "line-through" : "none")
                        .build(),
                    Text(task.title()),
                    onClick(() -> isEditing.set(true))
                )
            );
            
            Expanded();
            
            // 删除按钮
            Button("×", () -> onRemove.accept(task.id()));
        });
    });
}
```

### Element 树变化过程

```
初始状态:
ComponentElement (taskList)
└── ColumnElement
    ├── ButtonElement "Add Task"
    ├── SpacerElement
    └── ForEachElement
        ├── ComponentElement (key="1")
        │   └── RowElement ...
        ├── ComponentElement (key="2")
        │   └── RowElement ...
        └── ComponentElement (key="3")
            └── RowElement ...

删除 task "2" 后:
ComponentElement (taskList)
└── ColumnElement
    ├── ButtonElement "Add Task"     ← 复用
    ├── SpacerElement                ← 复用
    └── ForEachElement
        ├── ComponentElement (key="1")  ← 复用，key 匹配
        │   └── RowElement ...
        └── ComponentElement (key="3")  ← 复用，key 匹配
            └── RowElement ...

key="2" 的 ComponentElement 被卸载
key="1" 和 key="3" 的 ComponentElement 保持所有状态
```

---

## 常见问题与解答

### Q1: 为什么需要 Element，不能直接用 RenderNode？

**A**: RenderNode 是不可变的描述，不能：
- 持有可变状态
- 管理生命周期
- 执行副作用

Element 是实际实例，可以做这些事情。

### Q2: 协调算法的时间复杂度是多少？

**A**:
- 相同位置比较：O(n)
- 带 Key 比较：O(n)（使用 HashMap）
- 无 Key 时位置变化：O(n²)（最坏情况）

Key 可以将列表重排从 O(n²) 优化到 O(n)。

### Q3: Element 什么时候会被重建？

**A**: 只有当 RenderNode 类型变化时：

```java
// 类型相同，更新
TextNode("Hello") → TextNode("World")  // 更新 TextElement

// 类型不同，重建
TextNode("Hello") → ImageNode(...)    // 销毁 TextElement，创建 ImageElement
```

### Q4: 条件渲染时，非活跃分支的 Element 会保持吗？

**A**: 不会。条件变化时：

```java
conditional(
    () -> isActive.get(),
    componentA,  // isActive=true 时挂载
    componentB   // isActive=false 时挂载
)

// isActive: true → false
// componentA 的 Element 被卸载
// componentB 的 Element 被创建
```

如果需要保持状态，可以用 `showWhen` + CSS 隐藏：

```java
Column(() -> {
    showWhen(() -> tab == 0, content0);  // tab=0 时显示
    showWhen(() -> tab == 1, content1);  // tab=1 时显示
    // 两个都存在，只是显示/隐藏
});
```

---

## 下一步

现在你已经理解了 Element 树的工作原理，接下来深入了解细粒度响应式更新：

➡️ **[09-fine-grained-reactivity.md](./09-fine-grained-reactivity.md)** - 细粒度响应式更新

