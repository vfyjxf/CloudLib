package dev.vfyjxf.cloudlib.test.ui;

import dev.vfyjxf.cloudlib.api.ui.element.ElementTree;
import dev.vfyjxf.cloudlib.api.ui.reactive.Computed;
import dev.vfyjxf.cloudlib.api.ui.reactive.Key;
import dev.vfyjxf.cloudlib.api.ui.reactive.Render;
import dev.vfyjxf.cloudlib.api.ui.reactive.RenderNode;
import dev.vfyjxf.cloudlib.api.ui.reactive.Signal;
import dev.vfyjxf.cloudlib.api.ui.reactive.Style;
import dev.vfyjxf.cloudlib.api.ui.reactive.Theme;
import dev.vfyjxf.cloudlib.api.ui.reactive.widget.RBackground;
import dev.vfyjxf.cloudlib.api.ui.reactive.widget.RSlider;
import dev.vfyjxf.cloudlib.api.ui.reactive.widget.RTreeView;
import dev.vfyjxf.cloudlib.api.ui.reactive.widget.ReactiveRenderer;
import dev.vfyjxf.cloudlib.test.ui.components.PopupDemo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

import static dev.vfyjxf.cloudlib.api.ui.reactive.Render.Button;
import static dev.vfyjxf.cloudlib.api.ui.reactive.Render.Child;
import static dev.vfyjxf.cloudlib.api.ui.reactive.Render.Column;
import static dev.vfyjxf.cloudlib.api.ui.reactive.Render.Embed;
import static dev.vfyjxf.cloudlib.api.ui.reactive.Render.FlexSpacer;
import static dev.vfyjxf.cloudlib.api.ui.reactive.Render.Row;
import static dev.vfyjxf.cloudlib.api.ui.reactive.Render.ScrollArea;
import static dev.vfyjxf.cloudlib.api.ui.reactive.Render.Spacer;
import static dev.vfyjxf.cloudlib.api.ui.reactive.Render.Text;
import static dev.vfyjxf.cloudlib.api.ui.reactive.Style.bold;
import static dev.vfyjxf.cloudlib.api.ui.reactive.Style.color;
import static dev.vfyjxf.cloudlib.api.ui.reactive.Style.fontSize;
import static dev.vfyjxf.cloudlib.api.ui.reactive.Style.margin;
import static dev.vfyjxf.cloudlib.api.ui.reactive.Style.padding;
import static dev.vfyjxf.cloudlib.api.ui.reactive.Style.strikethrough;

/**
 * Showcase screen using the full reactive DSL with Signal tracking.
 * <p>
 * This demonstrates:
 * <ul>
 *   <li>Signal - mutable reactive state</li>
 *   <li>Computed - derived reactive state</li>
 *   <li>DSL - Column/Row/Text/Button</li>
 *   <li>Conditional rendering - showWhen</li>
 *   <li>List rendering - forEach</li>
 *   <li>Fine-grained updates</li>
 *   <li>Theme system for dark/light mode</li>
 * </ul>
 * <p>
 * Open with ` key in game.
 */
public class DSLShowcaseScreen extends Screen {

    // ===== Reactive State (Signals) =====

    /**
     * Counter value
     */
    private final Signal<Integer> count = Signal.of(0);

    /**
     * User's name
     */
    private final Signal<String> userName = Signal.of("Player");

    /**
     * Dark mode toggle
     */
    private final Signal<Boolean> darkMode = Signal.of(true);

    /**
     * Active tab
     */
    private final Signal<String> activeTab = Signal.of("counter");

    /**
     * Todo items
     */
    private final Signal<List<TodoItem>> todos = Signal.of(new ArrayList<>());

    /**
     * Show advanced options
     */
    private final Signal<Boolean> showAdvanced = Signal.of(false);

    // ===== Theme System =====

    /**
     * Reactive theme that responds to dark mode changes
     */
    private final Theme theme = Theme.reactive(darkMode::get);

    /**
     * Background widget
     */
    private RBackground background;

    // ===== Computed Values =====

    private final Computed<Integer> doubledCount;
    private final Computed<String> greeting;
    private final Computed<Integer> completedTodos;

    // ===== Rendering System =====

    /**
     * The element tree for reactive updates
     */
    private ElementTree elementTree;

    /**
     * The reactive renderer (RenderNode -> RWidget)
     */
    private ReactiveRenderer renderer;

    /**
     * Scroll state for main content
     */
    private final RenderNode.ScrollState mainScrollState = new RenderNode.ScrollState().setDraggable(true);

    /**
     * Scroll state for nested scroll demo
     */
    private final RenderNode.ScrollState nestedScrollState = new RenderNode.ScrollState().setDraggable(true);

    // ===== Advanced Component State =====

    /**
     * Slider state
     */
    private final RSlider.SliderState sliderState = new RSlider.SliderState(0.5f);

    /**
     * Progress value
     */
    private final Signal<Float> progressValue = Signal.of(0.0f);

    /**
     * Tree view node data
     */
    private RTreeView.TreeNode treeRoot;

    private static final int SCROLL_SPEED = 12;

    public DSLShowcaseScreen() {
        super(Component.literal("DSL Showcase"));

        // Initialize computed values
        this.doubledCount = Computed.of(() -> count.get() * 2);
        this.greeting = Computed.of(() -> "Hello, " + userName.get() + "!");
        this.completedTodos = Computed.of(() ->
            (int) todos.get().stream().filter(TodoItem::completed).count()
        );

        // Initialize todos
        todos.set(List.of(
            new TodoItem("1", "Learn Signals", false),
            new TodoItem("2", "Build with DSL", false),
            new TodoItem("3", "Master Reactivity", true)
        ));

        // Initialize tree view data
        initTreeData();
    }

    private void initTreeData() {
        treeRoot = new RTreeView.TreeNode("📁", "CloudLib").setExpanded(true);

        var apiNode = new RTreeView.TreeNode("📁", "api").setExpanded(true);
        apiNode.addChild(new RTreeView.TreeNode("📁", "ui").setExpanded(true)
                                                          .addChild(new RTreeView.TreeNode("📄", "Widget.java"))
                                                          .addChild(new RTreeView.TreeNode("📄", "Element.java"))
                                                          .addChild(new RTreeView.TreeNode("📁", "reactive")
                                                              .addChild(new RTreeView.TreeNode("📄", "Signal.java"))
                                                              .addChild(new RTreeView.TreeNode("📄", "Computed.java"))
                                                              .addChild(new RTreeView.TreeNode("📄", "Render.java"))));
        apiNode.addChild(new RTreeView.TreeNode("📄", "Registry.java"));

        var implNode = new RTreeView.TreeNode("📁", "impl");
        implNode.addChild(new RTreeView.TreeNode("📄", "WidgetImpl.java"));
        implNode.addChild(new RTreeView.TreeNode("📄", "RegistryImpl.java"));

        treeRoot.addChild(apiNode);
        treeRoot.addChild(implNode);
        treeRoot.addChild(new RTreeView.TreeNode("📄", "CloudLib.java"));
    }

    @Override
    protected void init() {
        super.init();

        // Create reactive background
        background = RBackground.fromTheme(theme);
        background.setBounds(0, 0, width, height);

        // Create the element tree for reactive tracking
        elementTree = new ElementTree();

        // Create the renderer with font
        renderer = new ReactiveRenderer(font);

        // Attach the UI to the element tree
        // This enables Signal tracking and fine-grained updates
        renderer.attachElementTree(elementTree, this::buildRootUI);
    }

    @Override
    public void removed() {
        super.removed();
        // Clean up element tree
        if (elementTree != null) {
            elementTree.detach();
        }
    }

    private RenderNode buildRootUI() {
        // Wrap entire UI in a scroll area that fills the screen (minus margins)
        // Leave 10px margins on each side, and 30px at bottom for debug text
        int scrollWidth = Math.max(100, width - 20);
        int scrollHeight = Math.max(100, height - 40);

        // Simple scroll area - popup is now handled by PopupDemo component internally
        return ScrollArea(scrollWidth, scrollHeight, mainScrollState, () -> {
            Column(() -> {
                // ===== Header =====
                Spacer(10);
                Text("CloudLib Reactive DSL Showcase", Style.of(
                    fontSize(16),
                    bold(),
                    color(0xFF4488FF)
                ));

                Spacer(12);

                // ===== Greeting (Computed) - reactive! =====
                Text(greeting::get, Style.of(color(0xFF888888)));

                Spacer(16);

                // ===== Tab Navigation (dynamic styling) =====
                Child(RenderNode.dynamic(() -> Row(() -> {
                    TabButton("counter", "Counter");
                    TabButton("todos", "Todos");
                    TabButton("widgets", "Widgets");
                    TabButton("settings", "Settings");
                })));

                Spacer(12);

                // ===== Tab Content (Dynamic) =====
                Child(RenderNode.dynamic(this::buildTabContent));
            });
        });
    }

    /**
     * Creates a tab button.
     */
    private void TabButton(String tabId, String label) {
        boolean isActive = activeTab.get().equals(tabId);
        Button(label, () -> activeTab.set(tabId), theme.tabButton(isActive));
    }

    /**
     * Builds the content for the active tab.
     */
    private RenderNode buildTabContent() {
        return switch (activeTab.get()) {
            case "counter" -> buildCounterTab();
            case "todos" -> buildTodosTab();
            case "widgets" -> buildWidgetsTab();
            case "settings" -> buildSettingsTab();
            default -> Render.text("Unknown tab");
        };
    }

    // ========================================
    // COUNTER TAB
    // ========================================

    private RenderNode buildCounterTab() {
        return Column(() -> {
            Text("Counter Demo", Style.of(fontSize(14), bold()));

            Spacer(8);

            // Current count (reactive via Supplier)
            Text(() -> "Count: " + count.get(), Style.of(
                fontSize(20),
                color(0xFF44AA44)
            ));

            // Doubled count (computed, reactive)
            Text(() -> "Doubled: " + doubledCount.get(), Style.of(
                color(0xFF888888)
            ));

            Spacer(12);

            // Control buttons - no buildUI needed!
            Row(() -> {
                Button("-10", () -> count.update(n -> n - 10), buttonStyle());
                Button("-1", () -> count.update(n -> n - 1), buttonStyle());
                Button("Reset", () -> count.set(0), buttonStyle());
                Button("+1", () -> count.update(n -> n + 1), buttonStyle());
                Button("+10", () -> count.update(n -> n + 10), buttonStyle());
            });

            Spacer(16);

            // Conditional messages (dynamic)
            Child(RenderNode.dynamic(() -> {
                if (count.get() > 10) {
                    return Render.text("Count is greater than 10!", Style.of(color(0xFFFFAA00)));
                } else if (count.get() < 0) {
                    return Render.text("Count is negative!", Style.of(color(0xFFFF4444)));
                }
                return RenderNode.empty();
            }));
        });
    }

    // ========================================
    // TODOS TAB
    // ========================================

    private RenderNode buildTodosTab() {
        return Column(() -> {
            Text("Todo List", Style.of(fontSize(14), bold()));

            Spacer(8);

            // Stats (reactive)
            Text(() -> String.format("Completed: %d / %d", completedTodos.get(), todos.get().size()),
                Style.of(color(0xFF888888))
            );

            Spacer(8);

            // Todo list (dynamic - rebuilds when todos change)
            Child(RenderNode.dynamic(() -> Column(() -> {
                for (TodoItem todo : todos.get()) {
                    // buildTodoItem returns Row() which auto-adds to parent scope
                    buildTodoItem(todo);
                }
            })));

            Spacer(12);

            // Add todo button - no buildUI needed!
            Row(() -> {
                Button("+ Add Todo", () -> {
                    String id = String.valueOf(System.currentTimeMillis());
                    var newTodo = new TodoItem(id, "New Task #" + (todos.get().size() + 1), false);
                    var newList = new ArrayList<>(todos.get());
                    newList.add(newTodo);
                    todos.set(newList);
                }, buttonStyle());

                Spacer(8);

                Button("Clear Completed", () -> {
                    var remaining = todos.get().stream()
                                         .filter(t -> !t.completed())
                                         .toList();
                    todos.set(remaining);
                }, buttonStyle());
            });
        });
    }

    private RenderNode buildTodoItem(TodoItem todo) {
        return Row(() -> {
            // Checkbox - no buildUI needed!
            Button(todo.completed() ? "[x]" : "[ ]", () -> toggleTodo(todo.id()),
                Style.of(padding(4), margin(0, 4)));

            // Text
            Text(todo.text(), Style.of(
                color(todo.completed() ? 0xFF888888 : 0xFFFFFFFF),
                strikethrough(todo.completed())
            ));

            FlexSpacer();

            // Delete button - no buildUI needed!
            Button("X", () -> deleteTodo(todo.id()),
                Style.of(color(0xFFFF4444), padding(4)));
        });
    }

    private void toggleTodo(String id) {
        var updated = todos.get().stream()
                           .map(t -> t.id().equals(id) ? new TodoItem(t.id(), t.text(), !t.completed()) : t)
                           .toList();
        todos.set(updated);
    }

    private void deleteTodo(String id) {
        var remaining = todos.get().stream()
                             .filter(t -> !t.id().equals(id))
                             .toList();
        todos.set(remaining);
    }

    // ========================================
    // WIDGETS TAB - Advanced Components Demo
    // ========================================

    private RenderNode buildWidgetsTab() {
        return Column(() -> {
            Text("Advanced Widgets Demo", Style.of(fontSize(14), bold()));

            Spacer(12);

            // ===== Progress Bar Section =====
            Text("Progress Bar", Style.of(bold(), color(0xFF88AAFF)));
            Spacer(4);

            // Animated progress bar - uses tick event for auto-animation
            Embed(Key.of("progress.animated"), animatedProgressBar());

            Spacer(8);

            Row(() -> {
                Button("Start", () -> progressValue.set(0.0f), smallButtonStyle());
                Button("+10%", () -> progressValue.update(v -> Math.min(1.0f, v + 0.1f)), smallButtonStyle());
                Button("Complete", () -> progressValue.set(1.0f), smallButtonStyle());
            });

            Spacer(16);

            // ===== Slider Section =====
            Text("Slider Control", Style.of(bold(), color(0xFF88AAFF)));
            Spacer(4);

            Child(slider(120, 16, sliderState, "Value: %v"));

            Spacer(16);

            // ===== Draggable Popup Section =====
            // Just embed the component - no field declaration needed!
            // State is managed internally via ComponentContext hooks
            Embed(Key.of("widgets.popup"), PopupDemo.PopupHolder(this::buttonStyle));

            Spacer(16);

            // ===== Tree View Section =====
            Text("Tree View (File Browser)", Style.of(bold(), color(0xFF88AAFF)));
            Spacer(4);

            Child(treeView(200, 150, treeRoot));

            Spacer(16);

            // ===== Nested Scroll Areas =====
            Text("Nested Scroll Area", Style.of(bold(), color(0xFF88AAFF)));
            Spacer(4);

            nestedScrollDemo();
        });
    }

    /**
     * Creates an animated progress bar component.
     * Uses ctx.onTick() to auto-increment progress every 20 ticks.
     */
    private dev.vfyjxf.cloudlib.api.ui.reactive.Component animatedProgressBar() {
        return dev.vfyjxf.cloudlib.api.ui.reactive.Component.stateful(ctx -> {
            // Use ref for tick counter (doesn't cause rebuilds)
            var tickRef = ctx.<Integer>ref("animationTick");
            if (tickRef.get() == null) {
                tickRef.set(0);
            }

            // Register tick callback for auto-animation
            ctx.onTick(() -> {
                int ticks = tickRef.get() + 1;
                tickRef.set(ticks);

                // Auto-increment progress every 20 ticks (1 second)
                if (ticks % 20 == 0 && progressValue.get() < 1.0f) {
                    progressValue.update(v -> Math.min(1.0f, v + 0.05f));
                }
            });

            // Return dynamic node that re-renders when progressValue changes
            return RenderNode.dynamic(() -> {
                return progressBar(180, 16, progressValue.get(), "%p");
            });
        });
    }

    /**
     * Creates a progress bar render node.
     */
    private RenderNode progressBar(int width, int height, float progress, String label) {
        return RenderNode.leaf("progress_bar", new ProgressBarProps(width, height, progress, label));
    }

    /**
     * Creates a slider render node.
     */
    private RenderNode slider(int width, int height, RSlider.SliderState state, String label) {
        return RenderNode.leaf("slider", new SliderProps(width, height, state, label));
    }

    /**
     * Creates a tree view render node.
     */
    private RenderNode treeView(int width, int height, RTreeView.TreeNode root) {
        return RenderNode.leaf("tree_view", new TreeViewProps(width, height, root));
    }

    /**
     * Creates a nested scroll demo with clearly scrollable content.
     * The content is much taller than the viewport to ensure scrolling works.
     */
    private RenderNode nestedScrollDemo() {
        return Column(() -> {
            // Description - shows current scroll position
            Text(() -> "Scroll area (scrollY=" + nestedScrollState.getScrollY() + "):",
                Style.of(color(0xFFCCCCCC)));
            Spacer(4);

            // The scroll area - 100px tall viewport with ~300px content
            // Uses class member nestedScrollState for state persistence
            // Note: ScrollArea auto-adds to parent scope, no need for Child()
            // Note: ScrollArea internally creates a Column, so content goes directly in the block
            ScrollArea(180, 100, nestedScrollState, () -> {
                // Add enough items to clearly exceed the viewport
                for (int i = 1; i <= 20; i++) {
                    int itemColor = (i % 2 == 0) ? 0xFFAAFFAA : 0xFF88DD88;
                    Text("Scroll Item #" + i, Style.of(color(itemColor)));
                    Spacer(2);
                }
                // Add a marker at the bottom
                Spacer(8);
                Text("=== END ===", Style.of(bold(), color(0xFFFFAAAA)));
            });
        });
    }

    // Props records for custom widgets
    public record ProgressBarProps(int width, int height, float progress, String label) {}

    public record SliderProps(int width, int height, RSlider.SliderState state, String label) {}

    public record TreeViewProps(int width, int height, RTreeView.TreeNode root) {}

    // ========================================
    // SETTINGS TAB
    // ========================================

    private RenderNode buildSettingsTab() {
        return Column(() -> {
            Text("Settings", Style.of(fontSize(14), bold()));

            Spacer(12);

            // Username setting (dynamic row for reactive text)
            Child(RenderNode.dynamic(() -> Row(() -> {
                Text("Username: ", theme.textSecondary());
                Text(userName.get(), Style.of(color(theme.primary())));

                Spacer(8);

                Button("Change", () -> {
                    String[] names = {"Player", "Hero", "Champion", "Legend"};
                    int idx = (int) (Math.random() * names.length);
                    userName.set(names[idx]);
                }, theme.smallButton());
            })));

            Spacer(8);

            // Dark mode toggle (dynamic for button state)
            Child(RenderNode.dynamic(() -> Row(() -> {
                Text("Dark Mode: ", theme.textSecondary());
                Button(
                    darkMode.get() ? "ON" : "OFF",
                    () -> darkMode.update(b -> !b),
                    theme.toggleButton(darkMode.get())
                );
            })));

            Spacer(12);

            // Advanced settings toggle (dynamic)
            Child(RenderNode.dynamic(() -> Column(() -> {
                Button(
                    showAdvanced.get() ? "v Hide Advanced" : "> Show Advanced",
                    () -> showAdvanced.update(b -> !b),
                    theme.smallButton()
                );

                // Conditional: advanced settings
                if (showAdvanced.get()) {
                    Spacer(8);
                    Text("Advanced Settings", theme.subtitle());
                    Text("  Debug Mode: Off", theme.textMuted());
                    Text("  Render FPS: 60", theme.textMuted());
                    Text("  Signal Count: 6", theme.textMuted());
                }
            })));

            FlexSpacer();

            // Reset button
            Button("Reset All", () -> {
                count.set(0);
                userName.set("Player");
                darkMode.set(false);
                showAdvanced.set(false);
                activeTab.set("counter");
            }, theme.dangerButton());
        });
    }

    // ========================================
    // STYLES (using Theme)
    // ========================================

    private Style buttonStyle() {
        return theme.primaryButton();
    }

    private Style smallButtonStyle() {
        return theme.smallButton();
    }

    // ========================================
    // RENDER
    // ========================================

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 不调用 super，禁用模糊效果
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Render reactive background (responds to dark mode changes)
        background.setSize(width, height);
        background.render(graphics, font, mouseX, mouseY, partialTick);

        // Flush pending builds from element tree
        if (elementTree != null) {
            elementTree.flushBuild();
        }

        // Render UI tree using ReactiveRenderer
        if (renderer != null) {
            renderer.render(graphics, 10, 10, mouseX, mouseY, partialTick);
        }

        // Debug info - shows widget count
        // Note: Popup state is now internal to PopupDemo component (not exposed here)
        String debug = String.format("Tab: %s | Count: %d | Todos: %d | Dark: %s",
            activeTab.get(), count.get(), todos.get().size(),
            darkMode.get() ? "ON" : "OFF");
        if (renderer != null) {
            debug += String.format(" | Widgets: %d", renderer.countWidgets());
        }
        graphics.drawString(font, debug, 4, height - 12, theme.textMutedColor());

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    // ========================================
    // INTERACTION
    // ========================================

    // Note: All widgets (including popup) are managed by ReactiveRenderer.
    // PopupDemo component manages its own state internally.

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (renderer != null && renderer.mouseScrolled((int) mouseX, (int) mouseY, scrollX, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (renderer != null && renderer.mouseClicked((int) mouseX, (int) mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (renderer != null && renderer.mouseDragged((int) mouseX, (int) mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (renderer != null) {
            renderer.mouseReleased((int) mouseX, (int) mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void tick() {
        super.tick();
        // Delegate tick to renderer for widget event system
        if (renderer != null) {
            renderer.tick();
        }
    }

    // ========================================
    // DATA CLASSES & HELPERS
    // ========================================

    public record TodoItem(String id, String text, boolean completed) {}
}
