# CloudLib UI 测试框架使用指南

## 概述

CloudLib UI 测试框架提供了一套完整的、不依赖 Minecraft 客户端的纯 JUnit 测试工具，
用于对 Widget UI 系统进行单元测试和集成测试。

### 框架架构

```
TestScene (主入口 Facade)
├── TestSceneHost         — 轻量级 SceneHost，无需 Minecraft 运行时
├── WidgetFinder          — Widget 定位谓词（按 key、类型、文本、路径）
├── WidgetAssert          — 可链式 Widget 断言
├── WidgetInspector       — Widget 属性提取
├── InputSequence         — 输入操作序列构建器
├── EventRecorder         — 事件录制与断言
├── TreeSnapshot          — 树状态快照
├── TreeDiffAssert        — 快照差异断言
├── TreeStructureAssert   — 树结构声明式断言
└── PathFinder            — 路径表达式解析
```

### 快速入门

```java
import dev.vfyjxf.cloudlib.api.ui.test.*;
import dev.vfyjxf.cloudlib.ui.widget.*;

class MyWidgetTest {

    @Test
    void basicExample() {
        // 1. 创建场景
        var scene = TestScene.create(800, 600);

        // 2. 添加 widget（key + 位置 + 大小）
        var btn = ButtonWidget.of("Click Me");
        scene.add(btn, "myBtn", 10, 10, 100, 30);

        // 3. 初始化场景（init → mount → layout）
        scene.setup();

        // 4. 断言状态
        scene.assertExists("myBtn");
        scene.assertThat("myBtn").isVisible().hasLabel("Click Me");

        // 5. 模拟交互
        scene.tap("myBtn");

        // 6. 验证结果
        scene.assertThat("myBtn").isVisible();
    }
}
```

---

## 1. TestScene — 场景 Facade

`TestScene` 是整个测试框架的核心入口。它封装了 `Scene`、`TestSceneHost`，
并提供了添加 widget、生命周期管理、输入模拟和断言的统一 API。

### 1.1 创建与生命周期

```java
// 创建指定大小的测试场景
var scene = TestScene.create(800, 600);

// 添加 widget（多种重载）
scene.add(widget);                              // 仅添加
scene.add(widget, "key");                       // 附带 key
scene.add(widget, "key", x, y, width, height);  // 附带 key + 位置大小

// 初始化场景：执行 init → mount → layout
scene.setup();

// 场景初始化后动态添加 widget
scene.add(newWidget, "late", 0, 0, 80, 30);
scene.rebuild();  // 仅初始化/挂载新增的未挂载 widget

// 推进时间
scene.tick();       // 推进 1 tick
scene.tick(10);     // 推进 10 ticks

// 手动触发布局
scene.layout();

// 销毁场景
scene.destroy();
```

### 1.2 嵌套组件

```java
// 创建 WidgetGroup 并以 builder 方式添加子组件
scene.addGroup("panel", 0, 0, 400, 300, panel -> {
    // addInto: 将子 widget 添加到指定容器中，自动追踪 bounds
    scene.addInto(panel, ButtonWidget.of("OK"), "ok", 10, 10, 80, 30);
    scene.addInto(panel, ButtonWidget.of("Cancel"), "cancel", 100, 10, 80, 30);
});

// 手动设置 bounds（通过 Taffy 样式系统的绝对定位）
scene.setTrackedBound(widget, x, y, w, h);
```

> **注意**：嵌套 widget 的 bounds 由 Taffy 布局引擎管理。`setTrackedBound()` 和
> `addInto()` 使用 `positionAbsolute()` + `insetLeft/Top()` + `sizeOf()` 样式
> 而非直接设置属性，确保 Taffy 正确计算位置。

### 1.3 访问底层对象

```java
scene.scene();   // 获取底层 Scene 对象
scene.root();    // 获取根 WidgetGroup<Widget>
```

---

## 2. Widget 属性配置

`Widget.setPos()`、`setSize()`、`setBound()`、`setFocusable()` 为 `protected` 方法，
不应从测试代码直接调用。使用以下替代方式：

```java
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.base.FocusNode;
import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.*;

// 设置 widget 的 key（public 方法，可直接调用）
widget.setKey("myKey");

// 设置位置和大小（通过 Taffy 样式系统）
widget.useStyle(UIStyle.of(
    positionAbsolute(),
    insetLeft(x), insetTop(y),
    sizeOf(width, height)
));

// 设置可聚焦状态
widget.setFocusNode(new FocusNode());
```

> **何时使用**：大多数情况下使用 `TestScene.add()` 或 `TestScene.addInto()` 即可
> 自动处理 key 和位置。当需要手动配置 widget 时用 `setKey()` + `useStyle()`。

---

## 3. WidgetFinder — 查找 Widget

`WidgetFinder` 提供声明式的 Widget 定位能力。大多数 TestScene 方法既接受
`Object key` 也接受 `WidgetFinder`。

### 3.1 基础查找

```java
// 按 key 查找
WidgetFinder.byKey("ok")

// 按类型查找
WidgetFinder.byType(ButtonWidget.class)

// 按类型 + key
WidgetFinder.byType(ButtonWidget.class, "ok")

// 按显示文本查找（支持 ButtonWidget, LabelWidget, TextWidget, TextFieldWidget）
WidgetFinder.byText("Click Me")

// 自定义谓词
WidgetFinder.where(w -> w.visible() && w.width() > 100)
```

### 3.2 组合查找

```java
// 交集：同时满足两个条件
WidgetFinder.byType(ButtonWidget.class).and(WidgetFinder.byKey("ok"))

// 并集：满足任一条件
WidgetFinder.byKey("a").or(WidgetFinder.byKey("b"))

// 作用域限定：在某个容器内搜索
WidgetFinder.byKey("btn").within(WidgetFinder.byKey("toolbar"))

// 索引：取第 N 个匹配结果（0-based）
WidgetFinder.byType(ButtonWidget.class).at(0)
WidgetFinder.byType(ButtonWidget.class).first()
```

### 3.3 路径表达式

```java
// 使用路径表达式导航 widget 树
WidgetFinder.path("WidgetGroup[key=panel]/ButtonWidget[key=ok]")

// 路径语法：
//   TypeName          — 按类型名匹配
//   TypeName[key=x]   — 按类型 + key
//   [key=x]           — 任何类型，指定 key
//   [N]               — 按子节点索引（0-based）
//   *                 — 匹配任意单个 widget
//   **                — 匹配任意深度（零或多层）

// 示例
WidgetFinder.path("**/ButtonWidget")           // 任意深度下的 ButtonWidget
WidgetFinder.path("WidgetGroup/[0]")           // 第一个 WidgetGroup 的第一个子节点
WidgetFinder.path("**/[key=toolbar]/*/ButtonWidget")  // toolbar 内任意直接子容器下的按钮
```

### 3.4 在 TestScene 上使用

```java
// 查找方法
Widget w = scene.find("key");                       // 按 key（快捷方式）
Widget w = scene.find(WidgetFinder.byText("OK"));   // 按 finder
ButtonWidget b = scene.find("key", ButtonWidget.class);  // 按 key + 类型
List<Widget> all = scene.findAll(WidgetFinder.byType(ButtonWidget.class));

// 存在性
boolean exists = scene.exists("key");
boolean exists = scene.exists(WidgetFinder.byText("OK"));

// 断言
scene.assertExists("key");
scene.assertNotExists("deleted");
scene.assertCount(WidgetFinder.byType(ButtonWidget.class), 3);
```

---

## 4. WidgetAssert — 链式断言

通过 `scene.assertThat()` 获取 `WidgetAssert` 实例，支持流畅的链式调用。

### 4.1 基本用法

```java
// 按 key 创建断言
scene.assertThat("myBtn")
     .isVisible()
     .hasLabel("OK")
     .isEnabled();

// 按 WidgetFinder 创建断言
scene.assertThat(WidgetFinder.byText("Status"))
     .hasText("Ready")
     .isVisible();

// 使用回调式断言
scene.assertWidget(WidgetFinder.byKey("btn"), wa -> {
    wa.isVisible().hasLabel("OK");
});
```

### 4.2 可见性与状态

```java
.isVisible()          .isNotVisible()
.isFocused()          .isNotFocused()
.isActive()           .isNotActive()
.isInteractive()      .isNotInteractive()
.isFocusable()        .isNotFocusable()
.isEnabled()          .isDisabled()          // ButtonWidget
.isEditable()         .isNotEditable()       // TextFieldWidget
.isToggled()          .isNotToggled()        // ToggleWidget
.isMounted()
.hasLifecycle(Lifecycle.MOUNTED)
```

### 4.3 位置与布局

```java
.hasPos(10, 20)            // 相对父容器位置
.hasAbsolutePos(60, 70)    // 绝对屏幕位置
.hasSize(100, 30)
.hasWidth(100)
.hasHeight(30)
.containsPoint(50, 15)     // 坐标是否在 bounds 内
.hasBoundsWithin(0, 0, 800, 600)  // bounds 是否在指定区域内
```

### 4.4 Widget 类型特定断言

```java
// ButtonWidget
.hasLabel("OK")
.hasLabel(Component.literal("OK"))
.isEnabled()   .isDisabled()

// LabelWidget / TextWidget
.hasText("Status: Ready")
.hasText(Component.literal("Status: Ready"))

// TextFieldWidget
.hasValue("input text")
.isEditable()  .isNotEditable()

// ToggleWidget
.isToggled()   .isNotToggled()

// SliderWidget
.hasSliderValue(75.0)
.hasSliderValue(75.0, 0.01)  // 带容差
```

### 4.5 树结构断言

```java
.hasKey("myWidget")
.isOfType(ButtonWidget.class)
.hasChildCount(3)
.hasNoChildren()
.hasChild(WidgetFinder.byKey("child"))
.hasParentKey("parent")
.hasParentOf(WidgetGroup.class)
```

### 4.6 相对位置断言

```java
var btnA = scene.find("a");
var btnB = scene.find("b");
var panel = scene.find("panel");

scene.assertThat("a").isLeftOf(btnB);
scene.assertThat("a").isAbove(btnB);
scene.assertThat("a").isWithin(panel);
```

### 4.7 自定义断言

```java
// 通用自定义断言
scene.assertThat("btn").satisfies(w -> {
    assertTrue(w.visible());
    assertEquals("myKey", w.key());
});

// 类型安全的自定义断言
scene.assertThat("slider").satisfies(SliderWidget.class, slider -> {
    assertTrue(slider.value() >= 0);
    assertTrue(slider.value() <= 100);
});
```

---

## 5. 输入模拟

### 5.1 快捷方法

TestScene 提供了常用交互的快捷方法：

```java
// 鼠标点击
scene.tap("btn");              // 左键点击（按 key）
scene.tap(finder);             // 左键点击（按 finder）
scene.tapAt(100, 200);         // 在绝对坐标点击
scene.tapAt("btn", 5, 5);     // 相对于 widget 的偏移点击
scene.doubleTap("btn");        // 双击
scene.rightTap("btn");         // 右键点击

// 鼠标悬停
scene.hover("btn");

// 滚动
scene.scroll("list", 0, 120);  // scrollX, scrollY

// 拖拽
scene.drag("source", "target");

// 键盘
scene.pressKey(GLFW.GLFW_KEY_A);
scene.pressEnter();
scene.pressEscape();
scene.pressTab();

// 文本输入
scene.typeText("textField", "Hello World");
```

### 5.2 InputSequence — 复杂输入序列

对于多步骤、带修饰键的复杂交互，使用 `InputSequence`：

```java
// Lambda 方式（推荐）
scene.perform(seq -> {
    seq.moveTo(WidgetFinder.byKey("source"))
       .mouseDown(0)
       .dragTo(WidgetFinder.byKey("target"))
       .mouseUp(0);
});

// 也可以传入预构建的序列
var seq = InputSequence.begin()
    .moveTo(WidgetFinder.byKey("btn"))
    .click()
    .moveTo(100, 200)
    .rightClick();
scene.perform(seq);
```

### 5.3 InputSequence 完整 API

**鼠标操作**：
```java
.moveTo(WidgetFinder.byKey("w"))  // 移动到 widget 中心
.moveTo(100, 200)                 // 移动到坐标
.mouseDown()    .mouseDown(button) // 按下鼠标（默认左键=0）
.mouseUp()      .mouseUp(button)   // 释放鼠标
.click()        .click(button)     // 点击 = mouseDown + mouseUp
.doubleClick()                     // 双击
.rightClick()                      // 右键点击
.dragTo(WidgetFinder.byKey("t"))   // 拖拽到 widget
.dragTo(300, 400)                  // 拖拽到坐标
.dragBy(50, 0)                     // 按增量拖拽
.scroll(0, 120)                    // 滚动
```

**键盘操作**：
```java
.keyDown(GLFW.GLFW_KEY_A)   // 按下键
.keyUp(GLFW.GLFW_KEY_A)     // 释放键
.keyPress(GLFW.GLFW_KEY_A)  // 按压 = keyDown + keyUp
.typeChar('a')               // 输入单个字符
.typeText("Hello")           // 输入文本字符串
```

**修饰键**：
```java
.holdCtrl()    .releaseCtrl()
.holdShift()   .releaseShift()
.holdAlt()     .releaseAlt()

// 快捷组合
.ctrlClick(WidgetFinder.byKey("item"))  // Ctrl+左键点击
.shiftClick(WidgetFinder.byKey("item")) // Shift+左键点击
.ctrlA()       // Ctrl+A（全选）
.ctrlC()       // Ctrl+C（复制）
.ctrlV()       // Ctrl+V（粘贴）
.ctrlZ()       // Ctrl+Z（撤销）
.ctrlShift(GLFW.GLFW_KEY_Z)  // Ctrl+Shift 组合键
```

**时间控制**：
```java
.tick()       // 推进 1 tick
.tick(10)     // 推进 10 ticks
```

**中间断言**（在序列执行过程中插入验证）：
```java
scene.perform(seq -> {
    seq.moveTo(WidgetFinder.byKey("btn"))
       .click()
       .then(s -> {
           // 点击后验证状态
           s.assertThat("btn").isVisible();
       })
       .thenAssert("label", wa -> {
           wa.hasText("Clicked!");
       })
       .click();  // 继续后续操作
});
```

---

## 6. EventRecorder — 事件录制

`EventRecorder` 用于记录 Widget 收到的事件，并提供丰富的断言能力。

### 6.1 创建与附加

```java
var btn = scene.add(ButtonWidget.of("OK"), "ok", 10, 10, 80, 30);

// 方式一：通过 EventRecorder 静态方法
var rec = EventRecorder.on(btn);          // 非消费型（事件继续传播）
var rec = EventRecorder.consuming(btn);   // 消费型（事件停止传播）

// 方式二：通过 TestScene 快捷方法
var rec = scene.record(btn);              // = EventRecorder.on(btn)
var rec = scene.recordConsuming(btn);     // = EventRecorder.consuming(btn)
```

> **重要**：`EventRecorder` 必须在 `scene.setup()` **之前**创建，
> 因为它需要注册事件监听器。

### 6.2 支持录制的事件

| 事件名            | 触发时机              |
| ----------------- | --------------------- |
| `mouseClicked`    | 鼠标按下              |
| `mouseReleased`   | 鼠标释放              |
| `click`           | 逻辑点击（按下+释放） |
| `mouseDragged`    | 鼠标拖拽              |
| `mouseScrolled`   | 鼠标滚轮              |
| `keyPressed`      | 键盘按下              |
| `keyReleased`     | 键盘释放              |
| `charTyped`       | 字符输入              |

### 6.3 断言 API

```java
// 基本断言
rec.assertFired("click");                  // 事件被触发过
rec.assertNotFired("keyPressed");          // 事件未被触发
rec.assertFiredTimes("click", 3);          // 精确触发次数
rec.assertFiredAtLeast("click", 1);        // 至少触发次数

// 事件顺序
rec.assertOrder("mouseClicked", "mouseReleased", "click");

// 排他断言
rec.assertNothingFired();                  // 无任何事件
rec.assertOnlyFired("click", "mouseClicked", "mouseReleased");  // 仅这些事件

// 条件断言
rec.assertFiredWith("mouseClicked", entry ->
    entry.data().containsKey("button")
);

// 查询
rec.wasFired("click");        // boolean
rec.count("click");           // int
rec.totalCount();             // int
rec.eventNames();             // List<String> 按触发顺序

// 获取记录条目
rec.firstEntry();
rec.lastEntry();
rec.lastEntryOf("click");
rec.entries();                // 所有条目
rec.entriesOf("click");      // 特定事件的条目

// 清除记录（重新开始）
rec.clear();

// 调试输出
System.out.println(rec.summary());
```

### 6.4 使用多个 EventRecorder 验证事件传播

```java
var parent = scene.addGroup("panel", 0, 0, 400, 300, panel -> {
    scene.addInto(panel, ButtonWidget.of("Child"), "child", 10, 10, 80, 30);
});
var parentRec = scene.record(parent);
var childRec = scene.record(scene.find("child"));
scene.setup();

scene.tap("child");

// 子 widget 先收到事件
childRec.assertFired("mouseClicked");
// 事件冒泡到父容器
parentRec.assertFired("mouseClicked");

// 使用消费型录制器阻止冒泡
var consumingRec = EventRecorder.consuming(scene.find("child"));
// 之后 parent 将不再收到来自 child 的事件
```

---

## 7. 焦点系统测试

### 7.1 设置可聚焦

```java
var btn = ButtonWidget.of("Focusable");
btn.setFocusNode(new FocusNode());  // 设置可聚焦
scene.add(btn, "btn", 10, 10, 100, 30);
scene.setup();
```

> **默认行为**：Widget 默认 **不可聚焦**。必须通过 `widget.setFocusNode(new FocusNode())`
> 设置焦点能力。

### 7.2 焦点操作与断言

```java
// 通过点击获取焦点
scene.tap("btn");
scene.assertFocused("btn");

// 编程式焦点操作
scene.requestFocus("btn");
scene.clearFocus();

// 焦点断言
scene.assertFocused("btn");           // 指定 widget 持有焦点
scene.assertNoFocus();                // 无 widget 持有焦点
scene.assertThat("btn").isFocused();  // 链式断言
scene.assertThat("btn").isNotFocused();

// 获取当前焦点 widget
Widget focused = scene.focusedWidget();
```

### 7.3 焦点行为要点

- **点击可聚焦 widget** → 获得焦点，前一个失去焦点
- **点击不可聚焦 widget** → 焦点转移到最近的可聚焦祖先（通常是根 FocusScopeNode），
  原来的焦点 widget 失去焦点
- **焦点事件**：`onFocus` / `onFocusLost`（Widget 自身），
  `onFocusIn` / `onFocusOut`（含冒泡上下文）

```java
var events = new ArrayList<String>();
btn.onFocus(ctx -> events.add("focus"));
btn.onFocusLost(ctx -> events.add("focusLost"));
```

---

## 8. Hit Testing

### 8.1 基本用法

```java
// 在指定坐标执行 hit test
Widget hit = scene.hitTestAt(100, 50);
// 返回 null 表示仅命中根（或无命中）

// 断言命中目标
scene.assertHitTarget(100, 50, "btn");     // 指定坐标应命中 key="btn"
scene.assertNoHitAt(700, 500);             // 指定坐标不应命中任何 widget（根除外）
```

### 8.2 行为要点

| 场景                       | hitTestAt 返回值                        |
| -------------------------- | --------------------------------------- |
| 命中具名 widget            | 该 widget                               |
| 坐标在空白区域             | 根 WidgetGroup（key=null）或 null        |
| Widget `visible=false`     | 跳过，返回下方 widget 或根              |
| Widget `interactive=false` | 跳过，返回下方 widget 或根              |
| 重叠 widget                | 后添加的在上方（最后添加优先）          |
| 边界坐标                   | **包含性**：`x=width-1` 仍在 widget 内 |

> **关键**：Widget 的边界是 **包含** 的。一个位于 `(100, 100)` 大小 `50×50` 的 widget，
> 有效命中范围是 `x ∈ [100, 149]`, `y ∈ [100, 149]`。`x=150` 已在 widget 外。

> **根 WidgetGroup**：根始终覆盖整个场景区域且 key 为 null。
> 当使用 `hitTestAt()` 时，结果可能是根而非 null。
> `assertNoHitAt()` 方法自动排除根。手动检查时使用：
> ```java
> var hit = scene.hitTestAt(x, y);
> assertTrue(hit == null || !"myKey".equals(hit.key()));
> ```

---

## 9. TreeSnapshot & TreeDiffAssert — 快照与差异

### 9.1 创建快照

```java
// 在某一时刻捕获树状态
var before = scene.snapshot();

// 执行操作...
scene.tap("toggle");
slider.setValue(75);

// 再次快照
var after = scene.snapshot();
```

### 9.2 快照内容

`TreeSnapshot` 递归遍历 widget 树，为每个 widget 记录：

- `type` — 类简名（如 `"ButtonWidget"`）
- `key` — widget key
- `identity` — `System.identityHashCode()`（用于判断是否同一实例）
- `label` — 显示文本（如有）
- `properties` — `{visible, x, y, width, height}`
- `children` — 子节点列表

```java
// 按 key 查找快照节点
TreeSnapshot.Node node = snapshot.findByKey("btn");

// 按类型查找
List<TreeSnapshot.Node> buttons = snapshot.findByType("ButtonWidget");

// 打印人类可读的树表示
System.out.println(snapshot.print());
```

### 9.3 差异断言

```java
var diff = TreeDiffAssert.create(before, after);

// 按 key 断言变化类型
diff.created("newWidget");    // 新增 widget
diff.removed("oldWidget");    // 删除 widget
diff.updated("label");        // 属性变化（同一实例，属性不同）
diff.reused("toggle");        // 未变化（同一实例，属性相同）
diff.replaced("btn");         // 替换（不同实例，相同 key）

// 批量断言
diff.noRemovals();            // 无删除
diff.noCreations();           // 无新增
diff.unchanged();             // 无任何变化

// 计数断言
diff.createdCount(2);
diff.removedCount(1);
diff.updatedCount(3);
diff.reusedCount(5);

// 调试：打印详细报告
System.out.println(diff.report());

// 快捷方式（通过 TestScene）
var diff = scene.assertDiff(before);
// 等价于 TreeDiffAssert.create(before, scene.snapshot())
```

### 9.4 差异状态说明

| DiffStatus | 含义                                             |
| ---------- | ------------------------------------------------ |
| `REUSED`   | 同一实例，属性未变（identityHashCode 相同，属性相同） |
| `UPDATED`  | 同一实例，属性已变（identityHashCode 相同，属性不同） |
| `CREATED`  | before 中不存在，after 中存在                     |
| `REMOVED`  | before 中存在，after 中不存在                     |
| `REPLACED` | key 相同但实例不同（identityHashCode 不同）       |

> **注意**：TreeSnapshot 按 **对象身份**（identityHashCode）跟踪 widget，
> 而非内部状态。修改 `ToggleWidget.toggle()` 或 `SliderWidget.setValue()` 
> 不会改变 widget 的身份，因此快照认为它们是 **REUSED**，而非 UPDATED。
> 只有其 **可见属性**（visible、位置、大小、label 文本）发生变化时才记为 UPDATED。

---

## 10. TreeStructureAssert — 树结构声明

用于声明式地断言整个 widget 树的结构：

```java
scene.assertTree(root -> {
    root.group(WidgetGroup.class, "panel", panel -> {
        panel.widget(ButtonWidget.class, "ok").withLabel("OK").visible();
        panel.widget(ButtonWidget.class, "cancel").withLabel("Cancel");
    });
    root.widget(LabelWidget.class, "status");
});

// 也可以用 any/anyGroup 做宽松匹配
scene.assertTree(root -> {
    root.anyGroup(container -> {
        container.any();             // 任意 widget
        container.widget(ButtonWidget.class);  // 类型匹配
    });
});

// 可见性约束
scene.assertTree(root -> {
    root.widget(ButtonWidget.class, "vis").visible();
    root.widget(ButtonWidget.class, "hid").hidden();
});
```

---

## 11. WidgetInspector — 属性提取

轻量级工具，用于提取 Widget 的显示文本：

```java
// 提取 widget 的主要显示文本（String 或 null）
String text = WidgetInspector.textOf(widget);

// 支持的 widget 类型：
//   ButtonWidget  → label().getString()
//   LabelWidget   → text().getString()
//   TextWidget    → text().getString()
//   TextFieldWidget → value()

// Component → String 转换
String str = WidgetInspector.componentToString(component);
```

> **注意**：`TextWidget.text()` 和 `LabelWidget.text()` 返回 `Component` 对象，
> 而不是 `String`。直接 `toString()` 会得到 `"literal{Hello}"`。
> 正确获取字符串值应使用 `.getString()` 或 `WidgetInspector.textOf()`。

---

## 12. 调试工具

```java
// 打印整个 widget 树（缩进表示层级）
System.out.println(scene.printTree());

// 列出所有已注册的 key
List<Object> keys = scene.allKeys();

// 打印快照
System.out.println(scene.snapshot().print());

// 打印差异报告
System.out.println(diff.report());

// 事件录制摘要
System.out.println(rec.summary());
```

---

## 13. 常见模式与最佳实践

### 13.1 测试组织

```java
class MyFeatureTest {

    // 共享场景构建方法
    private TestScene createScene() {
        var scene = TestScene.create(800, 600);
        scene.add(ButtonWidget.of("OK"), "ok", 10, 10, 80, 30);
        scene.add(ToggleWidget.create(false), "toggle", 10, 50, 40, 20);
        return scene;
    }

    @Nested
    class ClickTest {
        @Test
        void clickButtonFiresEvent() {
            var scene = createScene();
            var rec = scene.record(scene.find("ok"));
            scene.setup();

            scene.tap("ok");
            rec.assertFired("click");
        }
    }

    @Nested
    class ToggleTest {
        @Test
        void toggleChangesState() {
            var scene = createScene();
            scene.setup();

            scene.assertThat("toggle").isNotToggled();
            scene.tap("toggle");
            scene.assertThat("toggle").isToggled();
        }
    }
}
```

### 13.2 回调与联动测试

```java
@Test
void toggleControlsButtonEnabled() {
    var scene = TestScene.create(800, 600);
    var toggle = ToggleWidget.create(false);
    var btn = ButtonWidget.of("Submit");

    toggle.onToggle(on -> btn.setEnabled(on));
    btn.setEnabled(false);  // 初始状态同步

    scene.add(toggle, "toggle", 10, 10, 40, 20);
    scene.add(btn, "btn", 60, 10, 80, 30);
    scene.setup();

    scene.assertThat("btn").isDisabled();
    scene.tap("toggle");
    scene.assertThat("btn").isEnabled();
}
```

### 13.3 动态增删 Widget

```java
@Test
void dynamicAddAndRemove() {
    var scene = TestScene.create(800, 600);
    scene.add(ButtonWidget.of("A"), "a", 10, 10, 80, 30);
    scene.setup();

    // 动态添加
    scene.add(ButtonWidget.of("B"), "b", 100, 10, 80, 30);
    scene.rebuild();
    scene.assertExists("b");

    // 动态移除
    scene.remove("b");
    assertFalse(scene.exists("b"));
}
```

### 13.4 多阶段带断言的输入序列

```java
@Test
void wizardNavigation() {
    // ...setup wizard steps...
    scene.perform(seq -> {
        seq.moveTo(WidgetFinder.byKey("next"))
           .click()
           .then(s -> s.assertThat("step2").isVisible())
           .moveTo(WidgetFinder.byKey("next"))
           .click()
           .then(s -> s.assertThat("step3").isVisible())
           .moveTo(WidgetFinder.byKey("prev"))
           .click()
           .then(s -> s.assertThat("step2").isVisible());
    });
}
```

---

## 14. 已知限制与注意事项

### 14.1 TestFont — 测试用 Font

测试框架内置了 `TestFont`，可在不启动 Minecraft 客户端的情况下提供 Font 实例。
`TestSceneHost` 在未显式提供 Font 时会自动懒创建一个固定宽度的 `TestFont`。

> **版本依赖**：`TestFont` 基于 **NeoForge 21.1.x (Minecraft 1.21.1)** 的
> `net.minecraft.client.gui.Font` 内部结构实现。它通过反射替换 Font 内部的
> `StringSplitter`，使字体宽度计算在无渲染环境下正常工作。
> 如果 Font 类的内部结构在未来版本中发生变化（字段重命名、构造器签名变更等），
> `TestFont` 需要相应更新。

```java
// 默认固定宽度：每个字符 6px（Minecraft 默认字体近似值）
Font font = TestFont.create();
font.width("Hello");    // → ceil(5 × 6.0) = 30
font.lineHeight;        // → 9 (Minecraft 固定值)

// 自定义固定宽度
Font font = TestFont.create(8.0f);
font.width("AB");       // → ceil(2 × 8.0) = 16

// 自定义逐字符宽度
Font font = TestFont.create((codePoint, style) -> {
    if (codePoint == ' ') return 4.0f;
    return 6.0f;
});

// 显式传入 TestSceneHost（通常不需要，自动懒创建）
var host = new TestSceneHost(800, 600, TestFont.create());
```

**自动集成**：`TestSceneHost.font()` 在首次调用时自动创建 `TestFont.create()`，
因此 `TestScene.create(800, 600)` 创建的场景开箱即可使用所有依赖 Font 的 widget
（如 `TextFieldWidget`、`ButtonWidget` 等的 MeasureFunc 计算）。

**支持的 Font 方法**：

| 方法                | 功能                  | 测试环境下的行为                 |
| ------------------- | --------------------- | -------------------------------- |
| `width(String)`     | 字符串像素宽度        | `ceil(charWidth × length)`       |
| `width(FormattedText)` | 富文本宽度         | 通过 StringSplitter              |
| `lineHeight`        | 行高                  | 始终为 9                         |
| `getSplitter()`     | 获取 StringSplitter   | 返回自定义固定宽度 splitter      |
| `plainSubstrByWidth` | 截取文本到指定宽度   | 正常工作                         |
| `wordWrapHeight`    | 自动换行高度          | 正常工作                         |
| `drawInBatch`       | 渲染文本              | ❌ 抛出 NPE（无 FontSet 渲染管线） |

### 14.2 TestSceneHost 限制

| 限制                   | 影响                                          | 解决方案                           |
| ---------------------- | --------------------------------------------- | ---------------------------------- |
| **无渲染**             | 不执行 `render()`，无法测试视觉输出           | 仅测试状态和逻辑                   |
| **Font 仅支持测量**    | `drawInBatch` 等渲染方法会 NPE                | 仅依赖 `width()` / `lineHeight`   |
| **Taffy 布局覆盖 bounds** | 有 Font 后 layout 成功运行，会覆盖手动设置的 bounds | 使用 `setTrackedBound()` 手动设置  |

### 14.2 常见陷阱

#### `text()` 返回 Component 而非 String
```java
// ❌ 错误
assertEquals("Hello", label.text());           // Component ≠ String
assertEquals("Hello", label.text().toString()); // toString → "literal{Hello}"

// ✅ 正确
assertEquals("Hello", label.text().getString());
scene.assertThat("label").hasText("Hello");    // WidgetAssert 内部处理
```

#### `defer()` vs `nextTick()` 时机
```java
// defer() → 在渲染帧开始时执行（drainDeferred），tick() 不触发
// nextTick() → 在 tick() 中执行

// ❌ 测试中无法通过 tick() 触发 defer
scene.scene().defer(() -> flag.set(true));
scene.tick();  // 不会执行 defer 任务

// ✅ 使用 nextTick
scene.scene().nextTick(() -> flag.set(true));
scene.tick();  // 会执行
```

#### hitTestAt 返回根
```java
// ❌ 可能 NPE（根的 key 为 null）
var hit = scene.hitTestAt(x, y);
hit.key().equals("btn");  // NullPointerException!

// ✅ 安全判断
assertTrue(hit == null || !"btn".equals(hit.key()));
// 或使用框架方法
scene.assertHitTarget(x, y, "btn");  // 安全
scene.assertNoHitAt(x, y);           // 自动排除根
```

#### Widget 边界是包含的
```java
// Widget at (100, 100), size 50×50
// 有效范围: x ∈ [100, 149], y ∈ [100, 149]

scene.assertHitTarget(100, 100, "box");  // ✅ 左上角
scene.assertHitTarget(149, 149, "box");  // ✅ 右下角
scene.assertNoHitAt(150, 125);           // ✅ 刚好在外面
```

#### ButtonWidget.enabled() 默认为 true
```java
// ❌ 假设新创建的按钮是禁用的
var btn = ButtonWidget.of("Submit");
assertFalse(btn.enabled());  // 失败！默认为 true

// ✅ 显式设置初始状态
btn.setEnabled(false);
assertFalse(btn.enabled());
```

#### 点击不可聚焦 widget 会影响焦点
```java
// 点击不可聚焦 widget 时，框架会向上查找最近的可聚焦祖先（根 FocusScopeNode）
// 这会导致之前聚焦的 widget 失去焦点！

var btn = ButtonWidget.of("A");
btn.setFocusNode(new FocusNode());
scene.add(btn, "a", ...);
scene.add(ButtonWidget.of("B"), "b", ...);  // b 不可聚焦
scene.setup();

scene.tap("a");   // a 获得焦点
scene.tap("b");   // ❌ a 并不保持焦点！焦点转移到根 scope

scene.assertThat("a").isNotFocused();  // 实际行为
```

#### EventRecorder 必须在 setup() 前创建
```java
// ❌ setup 后创建，会错过初始化事件
scene.setup();
var rec = EventRecorder.on(scene.find("btn"));

// ✅ setup 前创建
var btn = scene.add(ButtonWidget.of("OK"), "ok", ...);
var rec = EventRecorder.on(btn);  // 或 scene.record(btn)
scene.setup();
```

#### TreeSnapshot 按身份跟踪
```java
// 修改 widget 内部值不影响快照差异
toggle.toggle();    // 快照仍认为 REUSED
slider.setValue(75); // 快照仍认为 REUSED

// 只有可见属性变化才记为 UPDATED
label.setText("new text");  // label 文本变化 → UPDATED
widget.setVisible(false);   // visible 变化 → UPDATED
```

---

## 15. 完整示例：下拉菜单

```java
@Test
void dropdownMenuOpensAndClosesCorrectly() {
    var scene = TestScene.create(800, 600);

    // 触发按钮
    var trigger = ButtonWidget.of("Menu ▾");
    scene.add(trigger, "trigger", 10, 10, 100, 30);

    // 菜单面板（初始隐藏）
    var menu = new WidgetGroup<>();
    menu.setVisible(false);
    scene.add(menu, "menu", 10, 45, 120, 90);

    // 空白区域用于点击关闭
    scene.add(ButtonWidget.of(""), "empty", 400, 400, 50, 50);

    // 使用 click group 实现"点击外部关闭"
    trigger.setClickGroup("dropdown");
    menu.setClickGroup("dropdown");
    menu.onClickOutside(ctx -> menu.setVisible(false));

    // 点击触发按钮打开菜单
    trigger.onClick(() -> menu.setVisible(!menu.visible()));

    scene.setup();

    // 初始状态
    scene.assertThat("menu").isNotVisible();

    // 点击触发按钮 → 菜单打开
    scene.tap("trigger");
    scene.assertThat("menu").isVisible();

    // 点击菜单外部 → 菜单关闭
    scene.tap("empty");
    scene.assertThat("menu").isNotVisible();
}
```
