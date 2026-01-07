package dev.vfyjxf.cloudlib.test.ui;

import dev.vfyjxf.cloudlib.api.ui.base.BasicScreen;
import dev.vfyjxf.cloudlib.api.ui.element.ElementTree;
import dev.vfyjxf.cloudlib.api.ui.reactive.*;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

import static dev.vfyjxf.cloudlib.api.ui.reactive.Render.*;
import static dev.vfyjxf.cloudlib.api.ui.reactive.Style.*;

/**
 * A showcase screen demonstrating the reactive UI system.
 * <p>
 * This screen demonstrates:
 * <ul>
 *   <li>Stateful components with signals</li>
 *   <li>Computed values for derived state</li>
 *   <li>Conditional rendering</li>
 *   <li>Dynamic list rendering</li>
 *   <li>Fine-grained reactivity</li>
 *   <li>Component composition</li>
 *   <li>Effect lifecycle</li>
 * </ul>
 */
public class ReactiveShowcaseScreen extends BasicScreen {
    
    private final ElementTree elementTree;
    
    // ===== Application State =====
    
    /** Main counter signal */
    private final Signal<Integer> count = Signal.of(0);
    
    /** User name signal */
    private final Signal<String> userName = Signal.of("Player");
    
    /** Theme toggle */
    private final Signal<Boolean> darkMode = Signal.of(false);
    
    /** Todo list items */
    private final Signal<List<TodoItem>> todos = Signal.of(new ArrayList<>());
    
    /** Active tab */
    private final Signal<String> activeTab = Signal.of("counter");
    
    /** Search query */
    private final Signal<String> searchQuery = Signal.of("");
    
    public ReactiveShowcaseScreen() {
        super();
        this.elementTree = new ElementTree();
        
        // Initialize with some todos
        todos.set(List.of(
            new TodoItem(1, "Learn Reactive UI", false),
            new TodoItem(2, "Build awesome GUIs", false),
            new TodoItem(3, "Have fun coding", true)
        ));
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Build the reactive UI tree
        RenderNode root = buildUI();
        elementTree.attachRoot(root);
        elementTree.flushBuild();
    }
    
    /**
     * Builds the main UI tree.
     */
    private RenderNode buildUI() {
        return Column(() -> {
            // Header with theme toggle
            Child(buildHeader());
            
            Spacer(16);
            
            // Tab navigation
            Child(buildTabBar());
            
            Spacer(8);
            
            // Tab content (reactive based on activeTab)
            Child(buildTabContent());
            
            FlexSpacer();
            
            // Footer with stats
            Child(buildFooter());
        });
    }
    
    // ===== Header Component =====
    
    private RenderNode buildHeader() {
        return RenderNode.component("header", Component.stateful(ctx -> {
            // Computed greeting
            var greeting = ctx.computed(() -> {
                String name = userName.get();
                int c = count.get();
                return String.format("Welcome, %s! (Count: %d)", name, c);
            });
            
            return Row(() -> {
                // Greeting text (reactive)
                Text(greeting::get, Style.builder()
                    .color(darkMode.get() ? 0xFFFFFF : 0x333333)
                    .fontSize(18)
                    .build());
                
                FlexSpacer();
                
                // Theme toggle button
                Button(
                    darkMode.get() ? "☀ Light" : "🌙 Dark",
                    () -> darkMode.update(b -> !b),
                    Style.of(padding(4, 8))
                );
            });
        }));
    }
    
    // ===== Tab Bar Component =====
    
    private RenderNode buildTabBar() {
        return RenderNode.component("tabs", Component.stateful(ctx -> {
            return Row(() -> {
                // Tab buttons
                for (String tab : List.of("counter", "todos", "profile", "settings")) {
                    final String tabName = tab;
                    boolean isActive = activeTab.get().equals(tabName);
                    
                    Button(
                        capitalize(tabName),
                        () -> activeTab.set(tabName),
                        Style.builder()
                            .background(isActive ? 0x4488FF : 0x666666)
                            .color(0xFFFFFF)
                            .padding(8, 16)
                            .margin(0, 4)
                            .build()
                    );
                }
            });
        }));
    }
    
    // ===== Tab Content =====
    
    private RenderNode buildTabContent() {
        // Dynamic content based on active tab
        return RenderNode.dynamic(() -> {
            return switch (activeTab.get()) {
                case "counter" -> buildCounterTab();
                case "todos" -> buildTodosTab();
                case "profile" -> buildProfileTab();
                case "settings" -> buildSettingsTab();
                default -> text("Unknown tab");
            };
        });
    }
    
    // ===== Counter Tab =====
    
    private RenderNode buildCounterTab() {
        return RenderNode.component("counter-tab", Component.stateful(ctx -> {
            // Computed values for display
            var doubled = ctx.computed(() -> count.get() * 2);
            var squared = ctx.computed(() -> count.get() * count.get());
            var isEven = ctx.computed(() -> count.get() % 2 == 0);
            
            // Track build count for demo
            var buildCount = ctx.signal(0);
            buildCount.update(n -> n + 1);
            
            // Effect example
            ctx.effect(() -> {
                System.out.println("[Effect] Count changed to: " + count.get());
                return () -> System.out.println("[Cleanup] Previous count effect");
            });
            
            return Column(() -> {
                Text("Counter Demo", Style.builder().fontSize(16).bold().build());
                Spacer(12);
                
                // Current count display
                Row(() -> {
                    Text("Count: ");
                    Text(() -> String.valueOf(count.get()), 
                        Style.builder().fontSize(24).color(0x4488FF).build());
                });
                
                Spacer(8);
                
                // Derived values
                Column(() -> {
                    Text(() -> "Doubled: " + doubled.get());
                    Text(() -> "Squared: " + squared.get());
                    Text(() -> "Is Even: " + isEven.get());
                });
                
                Spacer(12);
                
                // Control buttons
                Row(() -> {
                    Button("-10", () -> count.update(n -> n - 10), buttonStyle());
                    Button("-1", () -> count.update(n -> n - 1), buttonStyle());
                    Button("Reset", () -> count.set(0), buttonStyle());
                    Button("+1", () -> count.update(n -> n + 1), buttonStyle());
                    Button("+10", () -> count.update(n -> n + 10), buttonStyle());
                });
                
                Spacer(8);
                
                // Build count indicator (shows fine-grained updates)
                Text(() -> "Tab rebuilds: " + buildCount.get(),
                    Style.builder().color(0x888888).fontSize(10).build());
            });
        }));
    }
    
    // ===== Todos Tab =====
    
    private RenderNode buildTodosTab() {
        return RenderNode.component("todos-tab", Component.stateful(ctx -> {
            var newTodoText = ctx.signal("");
            var nextId = ctx.signal(100);
            
            // Computed filtered list
            var filteredTodos = ctx.computed(() -> {
                String query = searchQuery.get().toLowerCase();
                return todos.get().stream()
                    .filter(t -> query.isEmpty() || t.text().toLowerCase().contains(query))
                    .toList();
            });
            
            // Computed statistics
            var totalCount = ctx.computed(() -> todos.get().size());
            var completedCount = ctx.computed(() -> 
                (int) todos.get().stream().filter(TodoItem::completed).count());
            var pendingCount = ctx.computed(() -> totalCount.get() - completedCount.get());
            
            return Column(() -> {
                Text("Todo List", Style.builder().fontSize(16).bold().build());
                Spacer(12);
                
                // Stats row
                Row(() -> {
                    Text(() -> String.format("Total: %d | Done: %d | Pending: %d",
                        totalCount.get(), completedCount.get(), pendingCount.get()),
                        Style.of(color(0x666666)));
                });
                
                Spacer(8);
                
                // Search input placeholder (simulated with button)
                Row(() -> {
                    Text("Search: ");
                    Button(
                        searchQuery.get().isEmpty() ? "[Click to search]" : searchQuery.get(),
                        () -> {
                            // Toggle between some example searches
                            String current = searchQuery.get();
                            if (current.isEmpty()) searchQuery.set("Learn");
                            else if (current.equals("Learn")) searchQuery.set("Build");
                            else searchQuery.set("");
                        },
                        Style.builder().padding(4, 8).background(0xEEEEEE).build()
                    );
                });
                
                Spacer(8);
                
                // Todo list
                Child(RenderNode.forEach(
                    filteredTodos::get,
                    TodoItem::id,
                    (todo, index) -> buildTodoItem(todo)
                ));
                
                Spacer(12);
                
                // Add todo button
                Row(() -> {
                    Button("+ Add Random Todo", () -> {
                        int id = nextId.get();
                        nextId.update(n -> n + 1);
                        
                        List<TodoItem> current = new ArrayList<>(todos.get());
                        current.add(new TodoItem(id, "New Task #" + id, false));
                        todos.set(current);
                    }, buttonStyle());
                    
                    Spacer(8);
                    
                    Button("Clear Completed", () -> {
                        List<TodoItem> remaining = todos.get().stream()
                            .filter(t -> !t.completed())
                            .toList();
                        todos.set(remaining);
                    }, buttonStyle());
                });
            });
        }));
    }
    
    private RenderNode buildTodoItem(TodoItem todo) {
        return RenderNode.component("todo-" + todo.id(), Component.stateful(ctx -> {
            return Row(() -> {
                // Checkbox (simulated)
                Button(
                    todo.completed() ? "☑" : "☐",
                    () -> toggleTodo(todo.id()),
                    Style.of(padding(4))
                );
                
                Spacer(8);
                
                // Todo text
                Text(todo.text(), Style.builder()
                    .color(todo.completed() ? 0x888888 : 0x333333)
                    .strikethrough(todo.completed())
                    .build());
                
                FlexSpacer();
                
                // Delete button
                Button("✕", () -> deleteTodo(todo.id()),
                    Style.builder().color(0xFF4444).padding(4).build());
            });
        }));
    }
    
    private void toggleTodo(int id) {
        List<TodoItem> updated = todos.get().stream()
            .map(t -> t.id() == id ? new TodoItem(t.id(), t.text(), !t.completed()) : t)
            .toList();
        todos.set(updated);
    }
    
    private void deleteTodo(int id) {
        List<TodoItem> remaining = todos.get().stream()
            .filter(t -> t.id() != id)
            .toList();
        todos.set(remaining);
    }
    
    // ===== Profile Tab =====
    
    private RenderNode buildProfileTab() {
        return RenderNode.component("profile-tab", Component.stateful(ctx -> {
            var isEditing = ctx.signal(false);
            
            return Column(() -> {
                Text("Profile", Style.builder().fontSize(16).bold().build());
                Spacer(12);
                
                Row(() -> {
                    Text("Username: ");
                    Text(userName::get, Style.builder().fontSize(14).color(0x4488FF).build());
                    
                    Spacer(8);
                    
                    Button(
                        isEditing.get() ? "Save" : "Edit",
                        () -> {
                            if (isEditing.get()) {
                                // Simulate name change
                                String[] names = {"Player", "Hero", "Legend", "Champion"};
                                int idx = (int) (Math.random() * names.length);
                                userName.set(names[idx]);
                            }
                            isEditing.update(b -> !b);
                        },
                        buttonStyle()
                    );
                });
                
                Spacer(16);
                
                // Show edit form when editing
                Child(RenderNode.showWhen(
                    isEditing::get,
                    Column(() -> {
                        Text("Click Save to randomly change name", 
                            Style.builder().color(0x666666).fontSize(12).build());
                    })
                ));
                
                Spacer(16);
                
                // User statistics
                Column(() -> {
                    Text("Statistics:", Style.builder().bold().build());
                    Spacer(4);
                    Text(() -> "Counter clicks: " + count.get());
                    Text(() -> "Todos completed: " + todos.get().stream()
                        .filter(TodoItem::completed).count());
                });
            });
        }));
    }
    
    // ===== Settings Tab =====
    
    private RenderNode buildSettingsTab() {
        return RenderNode.component("settings-tab", Component.stateful(ctx -> {
            var showAdvanced = ctx.signal(false);
            
            // Effect to log settings changes
            ctx.effect(() -> {
                System.out.println("[Settings] Dark mode: " + darkMode.get());
                return null;
            }, darkMode.get());
            
            return Column(() -> {
                Text("Settings", Style.builder().fontSize(16).bold().build());
                Spacer(12);
                
                // Theme setting
                Row(() -> {
                    Text("Theme: ");
                    Button(
                        darkMode.get() ? "Dark Mode ✓" : "Light Mode ✓",
                        () -> darkMode.update(b -> !b),
                        buttonStyle()
                    );
                });
                
                Spacer(12);
                
                // Advanced settings toggle
                Button(
                    showAdvanced.get() ? "Hide Advanced" : "Show Advanced",
                    () -> showAdvanced.update(b -> !b),
                    buttonStyle()
                );
                
                Spacer(8);
                
                // Advanced settings (conditional)
                Child(RenderNode.showWhen(
                    showAdvanced::get,
                    Column(() -> {
                        Spacer(8);
                        Text("Advanced Settings", Style.builder().bold().build());
                        Spacer(4);
                        Text("• Option 1: Enabled");
                        Text("• Option 2: Disabled");
                        Text("• Debug Mode: Off");
                    })
                ));
                
                FlexSpacer();
                
                // Reset all button
                Button("Reset All Data", () -> {
                    count.set(0);
                    userName.set("Player");
                    darkMode.set(false);
                    searchQuery.set("");
                    todos.set(List.of(
                        new TodoItem(1, "Learn Reactive UI", false),
                        new TodoItem(2, "Build awesome GUIs", false),
                        new TodoItem(3, "Have fun coding", true)
                    ));
                }, Style.builder()
                    .background(0xFF4444)
                    .color(0xFFFFFF)
                    .padding(8, 16)
                    .build());
            });
        }));
    }
    
    // ===== Footer Component =====
    
    private RenderNode buildFooter() {
        return RenderNode.component("footer", Component.stateless(ctx -> {
            return Row(() -> {
                Text("Reactive UI Demo", Style.builder().color(0x888888).fontSize(10).build());
                FlexSpacer();
                Text("CloudLib Framework", Style.builder().color(0x888888).fontSize(10).build());
            });
        }));
    }
    
    // ===== Render Loop Integration =====
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Flush any pending reactive updates
        elementTree.flushBuild();
        
        // Render background based on theme
        int bgColor = darkMode.get() ? 0xFF1A1A2E : 0xFFF0F0F0;
        graphics.fill(0, 0, width, height, bgColor);
        
        // Render the widget tree (existing system)
        super.render(graphics, mouseX, mouseY, partialTick);
        
        // Debug: show tree stats
        if (isDebugEnabled()) {
            var stats = elementTree.getStats();
            String debugInfo = String.format("Elements: %d | Components: %d | Leaves: %d",
                stats.totalElements(), stats.componentElements(), stats.leafElements());
            graphics.drawString(font, debugInfo, 4, height - 12, 0x88888888);
        }
    }
    
    @Override
    public void tick() {
        super.tick();
        // Could add periodic updates here
    }
    
    @Override
    public void removed() {
        super.removed();
        elementTree.detach();
    }
    
    // ===== Helper Methods =====
    
    private Style buttonStyle() {
        return Style.builder()
            .background(darkMode.get() ? 0x444466 : 0x4488FF)
            .color(0xFFFFFF)
            .padding(6, 12)
            .margin(0, 4)
            .build();
    }
    
    private String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
    
    private boolean isDebugEnabled() {
        return true; // Could be a setting
    }
    
    // ===== Data Classes =====
    
    public record TodoItem(int id, String text, boolean completed) {}
}
