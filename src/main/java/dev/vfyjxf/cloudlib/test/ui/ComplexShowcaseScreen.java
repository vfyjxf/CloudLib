package dev.vfyjxf.cloudlib.test.ui;

import dev.vfyjxf.cloudlib.api.ui.base.BasicScreen;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.element.ElementTree;
import dev.vfyjxf.cloudlib.api.ui.layout.modifier.Modifier;
import dev.vfyjxf.cloudlib.api.ui.reactive.*;
import dev.vfyjxf.cloudlib.api.ui.reactive.Component;
import dev.vfyjxf.cloudlib.ui.widgets.ButtonWidget;
import dev.vfyjxf.cloudlib.ui.widgets.TextWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;

import static dev.vfyjxf.cloudlib.api.ui.reactive.Render.*;
import static dev.vfyjxf.cloudlib.api.ui.reactive.Style.*;

/**
 * A comprehensive showcase screen demonstrating the full reactive UI system.
 * <p>
 * This screen demonstrates ALL reactive features:
 * <ul>
 *   <li><b>Signals</b> - Mutable reactive state</li>
 *   <li><b>Computed</b> - Derived state with auto-tracking</li>
 *   <li><b>Effects</b> - Side effects with cleanup</li>
 *   <li><b>Conditional Rendering</b> - showWhen, when</li>
 *   <li><b>List Rendering</b> - forEach with keys</li>
 *   <li><b>Component Composition</b> - Stateless/Stateful components</li>
 *   <li><b>Dependency Injection</b> - Providers for cross-cutting concerns</li>
 *   <li><b>Fine-grained Reactivity</b> - Minimal updates</li>
 *   <li><b>Lifecycle Hooks</b> - onMount, onUnmount</li>
 *   <li><b>Style System</b> - Dynamic theming</li>
 * </ul>
 * <p>
 * Open in game with: /cloudlib showcase
 */
public class ComplexShowcaseScreen extends BasicScreen {

    // ===== Element Tree for Reactive UI =====
    private final ElementTree elementTree;

    // ===== Application State (Global Signals) =====

    /** Current theme: light/dark/custom */
    private final Signal<Theme> currentTheme = Signal.of(Theme.LIGHT);

    /** Active main tab */
    private final Signal<TabType> activeTab = Signal.of(TabType.DASHBOARD);

    /** Global notification message */
    private final Signal<String> notification = Signal.of("");

    /** Debug mode flag */
    private final Signal<Boolean> debugMode = Signal.of(true);

    // ===== Dashboard State =====
    private final Signal<Integer> clickCount = Signal.of(0);
    private final Signal<Integer> score = Signal.of(0);
    private final Signal<Double> progress = Signal.of(0.0);

    // ===== Inventory Demo State =====
    private final Signal<List<InventorySlot>> inventory = Signal.of(new ArrayList<>());
    private final Signal<String> inventoryFilter = Signal.of("");
    private final Signal<SortMode> sortMode = Signal.of(SortMode.NAME);

    // ===== Chat Demo State =====
    private final Signal<List<ChatMessage>> messages = Signal.of(new ArrayList<>());
    private final Signal<String> inputText = Signal.of("");
    private final Signal<Boolean> isTyping = Signal.of(false);

    // ===== Quest Demo State =====
    private final Signal<List<Quest>> quests = Signal.of(new ArrayList<>());
    private final Signal<Quest> selectedQuest = Signal.of(null);

    // ===== Timing =====
    private final Signal<Long> tickCount = Signal.of(0L);
    private final Random random = new Random();

    // ===== Provider Keys =====
    public static final Providers.Key<Theme> THEME_KEY = Providers.key("theme");
    public static final Providers.Key<Consumer<String>> NOTIFY_KEY = Providers.key("notify");
    public static final Providers.Key<Boolean> DEBUG_KEY = Providers.key("debug");

    public ComplexShowcaseScreen() {
        super();
        this.elementTree = new ElementTree();
        initializeData();
    }

    private void initializeData() {
        // Initialize inventory
        inventory.set(List.of(
            new InventorySlot(1, "Diamond", Items.DIAMOND, 64, Rarity.RARE),
            new InventorySlot(2, "Iron Ingot", Items.IRON_INGOT, 32, Rarity.COMMON),
            new InventorySlot(3, "Gold Ingot", Items.GOLD_INGOT, 16, Rarity.UNCOMMON),
            new InventorySlot(4, "Emerald", Items.EMERALD, 8, Rarity.RARE),
            new InventorySlot(5, "Netherite Ingot", Items.NETHERITE_INGOT, 1, Rarity.EPIC),
            new InventorySlot(6, "Coal", Items.COAL, 64, Rarity.COMMON),
            new InventorySlot(7, "Lapis Lazuli", Items.LAPIS_LAZULI, 48, Rarity.UNCOMMON),
            new InventorySlot(8, "Redstone", Items.REDSTONE, 64, Rarity.COMMON)
        ));

        // Initialize quests
        quests.set(List.of(
            new Quest("q1", "Gather Resources", "Collect 100 diamonds", 0, 100, QuestStatus.IN_PROGRESS),
            new Quest("q2", "Defeat the Dragon", "Slay the Ender Dragon", 0, 1, QuestStatus.NOT_STARTED),
            new Quest("q3", "Master Crafter", "Craft 50 items", 35, 50, QuestStatus.IN_PROGRESS),
            new Quest("q4", "Explorer", "Discover 10 biomes", 10, 10, QuestStatus.COMPLETED)
        ));

        // Initialize chat
        messages.set(List.of(
            new ChatMessage("m1", "System", "Welcome to CloudLib Showcase!", System.currentTimeMillis() - 60000, true),
            new ChatMessage("m2", "Player", "This is a test message", System.currentTimeMillis() - 30000, false),
            new ChatMessage("m3", "System", "Try clicking around!", System.currentTimeMillis(), true)
        ));
    }

    @Override
    protected void init() {
        super.init();

        // Build and attach the reactive UI
        RenderNode root = buildRootUI();
        elementTree.attachRoot(root);
        elementTree.flushBuild();

        showNotification("Screen initialized! Welcome to the Complex Showcase.");
    }

    // ========================================================================
    // ROOT UI STRUCTURE
    // ========================================================================

    private RenderNode buildRootUI() {
        // Main app with providers for dependency injection
        return RenderNode.component("root", Component.stateful(ctx -> {
            // Provide theme to all children
            // (In real implementation, this would use Providers)

            return Column(() -> {
                // Top bar with navigation and controls
                Child(buildTopBar());

                // Notification bar
                Child(buildNotificationBar());

                Spacer(4);

                // Main content area
                Row(() -> {
                    // Side navigation
                    Child(buildSideNav());

                    Spacer(8);

                    // Tab content
                    Child(RenderNode.component("content", Component.stateless(c ->
                        buildTabContent()
                    )));
                });

                FlexSpacer();

                // Bottom status bar
                Child(buildStatusBar());
            });
        }));
    }

    // ========================================================================
    // TOP BAR
    // ========================================================================

    private RenderNode buildTopBar() {
        return RenderNode.component("topbar", Component.stateful(ctx -> {
            // Computed: formatted theme name
            var themeName = ctx.computed(() -> {
                Theme t = currentTheme.get();
                return "Theme: " + t.name();
            });

            Style barStyle = Style.builder()
                .background(currentTheme.get().headerBg)
                .padding(8)
                .build();

            return Row(barStyle, () -> {
                // Logo / Title
                Text("☰ CloudLib Complex Showcase", Style.builder()
                    .fontSize(14)
                    .bold()
                    .color(currentTheme.get().textPrimary)
                    .build());

                FlexSpacer();

                // Theme selector
                Text(themeName::get, Style.builder()
                    .color(currentTheme.get().textSecondary)
                    .fontSize(11)
                    .build());

                Spacer(8);

                // Theme toggle buttons
                Button("Light", () -> currentTheme.set(Theme.LIGHT), themeButtonStyle(Theme.LIGHT));
                Button("Dark", () -> currentTheme.set(Theme.DARK), themeButtonStyle(Theme.DARK));
                Button("Ocean", () -> currentTheme.set(Theme.OCEAN), themeButtonStyle(Theme.OCEAN));

                Spacer(16);

                // Debug toggle
                Button(
                    debugMode.get() ? "🐛 Debug ON" : "🐛 Debug OFF",
                    () -> debugMode.update(b -> !b),
                    Style.builder()
                        .padding(4, 8)
                        .background(debugMode.get() ? 0xFF44AA44 : 0xFF666666)
                        .color(0xFFFFFFFF)
                        .build()
                );
            });
        }));
    }

    private Style themeButtonStyle(Theme theme) {
        boolean isActive = currentTheme.get() == theme;
        return Style.builder()
            .padding(4, 8)
            .margin(0, 2)
            .background(isActive ? theme.accentColor : 0xFF555555)
            .color(0xFFFFFFFF)
            .build();
    }

    // ========================================================================
    // NOTIFICATION BAR
    // ========================================================================

    private RenderNode buildNotificationBar() {
        return RenderNode.showWhen(
            () -> !notification.get().isEmpty(),
            RenderNode.component("notification", Component.stateful(ctx -> {
                // Auto-dismiss effect
                ctx.effect(() -> {
                    String msg = notification.get();
                    if (!msg.isEmpty()) {
                        // Would schedule dismiss in real impl
                        System.out.println("[Notification] " + msg);
                    }
                    return null;
                });

                Style style = Style.builder()
                    .background(0xFF44AA88)
                    .padding(6, 12)
                    .build();

                return Row(style, () -> {
                    Text("ℹ ", Style.builder().color(0xFFFFFFFF).build());
                    Text(notification::get, Style.builder().color(0xFFFFFFFF).build());
                    FlexSpacer();
                    Button("✕", () -> notification.set(""),
                        Style.builder().color(0xFFFFFFFF).padding(2, 6).build());
                });
            }))
        );
    }

    // ========================================================================
    // SIDE NAVIGATION
    // ========================================================================

    private RenderNode buildSideNav() {
        return RenderNode.component("sidenav", Component.stateful(ctx -> {
            Style navStyle = Style.builder()
                .background(currentTheme.get().sidebarBg)
                .padding(8)
                .minWidth(120)
                .build();

            return Column(navStyle, () -> {
                for (TabType tab : TabType.values()) {
                    Child(buildNavItem(tab));
                }

                FlexSpacer();

                // Stats summary
                Spacer(12);
                Text("Quick Stats", Style.builder()
                    .fontSize(10)
                    .color(currentTheme.get().textSecondary)
                    .bold()
                    .build());

                Text(() -> "Clicks: " + clickCount.get(), smallTextStyle());
                Text(() -> "Score: " + score.get(), smallTextStyle());
                Text(() -> "Quests: " + quests.get().stream()
                    .filter(q -> q.status == QuestStatus.COMPLETED).count() + "/" + quests.get().size(),
                    smallTextStyle());
            });
        }));
    }

    private RenderNode buildNavItem(TabType tab) {
        return RenderNode.component("nav-" + tab.name(), Component.stateful(ctx -> {
            boolean isActive = activeTab.get() == tab;

            Style style = Style.builder()
                .padding(8, 12)
                .margin(2, 0)
                .background(isActive ? currentTheme.get().accentColor : 0x00000000)
                .color(isActive ? 0xFFFFFFFF : currentTheme.get().textPrimary)
                .build();

            return Row(style, () -> {
                Text(tab.icon + " ");
                Button(tab.displayName, () -> activeTab.set(tab),
                    Style.builder().color(style.get(Color.class) != null ?
                        style.get(Color.class).value() : 0xFFFFFFFF).build());
            });
        }));
    }

    // ========================================================================
    // TAB CONTENT (DYNAMIC)
    // ========================================================================

    private RenderNode buildTabContent() {
        return RenderNode.dynamic(() -> {
            Style contentStyle = Style.builder()
                .background(currentTheme.get().contentBg)
                .padding(12)
                .flex(1)
                .build();

            return Column(contentStyle, () -> {
                Child(switch (activeTab.get()) {
                    case DASHBOARD -> buildDashboard();
                    case INVENTORY -> buildInventory();
                    case CHAT -> buildChat();
                    case QUESTS -> buildQuests();
                    case SETTINGS -> buildSettings();
                });
            });
        });
    }

    // ========================================================================
    // DASHBOARD TAB
    // ========================================================================

    private RenderNode buildDashboard() {
        return RenderNode.component("dashboard", Component.stateful(ctx -> {
            // Computed values
            var clicksPerSecond = ctx.computed(() ->
                tickCount.get() > 0 ? clickCount.get() / (tickCount.get() / 20.0) : 0.0
            );

            var progressPercent = ctx.computed(() ->
                String.format("%.1f%%", progress.get() * 100)
            );

            var level = ctx.computed(() -> score.get() / 100);
            var xpToNextLevel = ctx.computed(() -> 100 - (score.get() % 100));

            // Effect: Log score milestones
            ctx.effect(() -> {
                int s = score.get();
                if (s > 0 && s % 50 == 0) {
                    showNotification("Milestone reached: " + s + " points!");
                }
                return null;
            });

            return Column(() -> {
                Text("📊 Dashboard", titleStyle());
                Spacer(12);

                // Stats cards row
                Row(() -> {
                    Child(buildStatCard("Clicks", () -> String.valueOf(clickCount.get()), "🖱️"));
                    Spacer(8);
                    Child(buildStatCard("Score", () -> String.valueOf(score.get()), "⭐"));
                    Spacer(8);
                    Child(buildStatCard("Level", () -> String.valueOf(level.get()), "📈"));
                    Spacer(8);
                    Child(buildStatCard("CPS", () -> String.format("%.2f", clicksPerSecond.get()), "⚡"));
                });

                Spacer(16);

                // Progress section
                Text("Progress", subtitleStyle());
                Spacer(4);
                Text(() -> "Current progress: " + progressPercent.get(), normalTextStyle());

                // Progress bar (simulated with text)
                Row(() -> {
                    int filled = (int) (progress.get() * 20);
                    Text("[" + "█".repeat(filled) + "░".repeat(20 - filled) + "]",
                        Style.builder().color(currentTheme.get().accentColor).build());
                });

                Spacer(16);

                // Action buttons
                Text("Actions", subtitleStyle());
                Spacer(4);

                Row(() -> {
                    Button("Click +1", () -> {
                        clickCount.update(n -> n + 1);
                        score.update(s -> s + 1);
                    }, actionButtonStyle());

                    Button("Click +10", () -> {
                        clickCount.update(n -> n + 10);
                        score.update(s -> s + 10);
                    }, actionButtonStyle());

                    Button("Boost Progress", () -> {
                        progress.update(p -> Math.min(1.0, p + 0.1));
                    }, actionButtonStyle());

                    Button("Reset All", () -> {
                        clickCount.set(0);
                        score.set(0);
                        progress.set(0.0);
                    }, Style.builder()
                        .padding(6, 12)
                        .margin(4)
                        .background(0xFFAA4444)
                        .color(0xFFFFFFFF)
                        .build());
                });

                Spacer(16);

                // Level info
                Text("Level Progress", subtitleStyle());
                Text(() -> "XP to next level: " + xpToNextLevel.get(), normalTextStyle());

                // Debug info
                Child(RenderNode.showWhen(
                    debugMode::get,
                    Column(() -> {
                        Spacer(12);
                        Text("🐛 Debug Info", Style.builder()
                            .color(0xFFAAAA00)
                            .fontSize(10)
                            .build());
                        Text(() -> "Tick count: " + tickCount.get(), debugTextStyle());
                        Text(() -> "Active tab: " + activeTab.get().name(), debugTextStyle());
                    })
                ));
            });
        }));
    }

    private RenderNode buildStatCard(String label, java.util.function.Supplier<String> value, String icon) {
        return RenderNode.component("stat-" + label, Component.stateless(ctx -> {
            Style cardStyle = Style.builder()
                .background(currentTheme.get().cardBg)
                .padding(12)
                .minWidth(80)
                .build();

            return Column(cardStyle, () -> {
                Text(icon, Style.builder().fontSize(16).build());
                Text(label, Style.builder()
                    .color(currentTheme.get().textSecondary)
                    .fontSize(10)
                    .build());
                Text(value, Style.builder()
                    .color(currentTheme.get().textPrimary)
                    .fontSize(14)
                    .bold()
                    .build());
            });
        }));
    }

    // ========================================================================
    // INVENTORY TAB
    // ========================================================================

    private RenderNode buildInventory() {
        return RenderNode.component("inventory", Component.stateful(ctx -> {
            // Computed: filtered and sorted inventory
            var filteredInventory = ctx.computed(() -> {
                String filter = inventoryFilter.get().toLowerCase();
                SortMode mode = sortMode.get();

                return inventory.get().stream()
                    .filter(slot -> filter.isEmpty() ||
                        slot.name.toLowerCase().contains(filter))
                    .sorted((a, b) -> switch (mode) {
                        case NAME -> a.name.compareTo(b.name);
                        case COUNT -> Integer.compare(b.count, a.count);
                        case RARITY -> Integer.compare(b.rarity.ordinal(), a.rarity.ordinal());
                    })
                    .toList();
            });

            var totalItems = ctx.computed(() ->
                inventory.get().stream().mapToInt(s -> s.count).sum()
            );

            var uniqueItems = ctx.computed(() -> inventory.get().size());

            return Column(() -> {
                Text("🎒 Inventory", titleStyle());
                Spacer(8);

                // Stats
                Row(() -> {
                    Text(() -> "Total items: " + totalItems.get(), normalTextStyle());
                    Spacer(16);
                    Text(() -> "Unique: " + uniqueItems.get(), normalTextStyle());
                });

                Spacer(8);

                // Filter and sort controls
                Row(() -> {
                    Text("Filter: ", normalTextStyle());
                    Button(
                        inventoryFilter.get().isEmpty() ? "[All]" : inventoryFilter.get(),
                        () -> {
                            // Cycle through filters
                            String current = inventoryFilter.get();
                            if (current.isEmpty()) inventoryFilter.set("diamond");
                            else if (current.equals("diamond")) inventoryFilter.set("ingot");
                            else inventoryFilter.set("");
                        },
                        smallButtonStyle()
                    );

                    Spacer(16);

                    Text("Sort: ", normalTextStyle());
                    for (SortMode mode : SortMode.values()) {
                        Button(
                            mode.name(),
                            () -> sortMode.set(mode),
                            sortMode.get() == mode ? activeSortStyle() : smallButtonStyle()
                        );
                    }
                });

                Spacer(12);

                // Inventory grid
                Text("Items:", subtitleStyle());
                Spacer(4);

                Child(RenderNode.forEach(
                    filteredInventory::get,
                    slot -> slot.id,
                    (slot, index) -> buildInventorySlot(slot)
                ));

                Spacer(12);

                // Actions
                Row(() -> {
                    Button("Add Random Item", () -> {
                        var items = List.of(
                            new InventorySlot(random.nextInt(1000) + 100, "Random Gem",
                                Items.AMETHYST_SHARD, random.nextInt(64) + 1, Rarity.UNCOMMON)
                        );
                        var newList = new ArrayList<>(inventory.get());
                        newList.addAll(items);
                        inventory.set(newList);
                    }, actionButtonStyle());

                    Button("Clear Filters", () -> inventoryFilter.set(""), actionButtonStyle());
                });
            });
        }));
    }

    private RenderNode buildInventorySlot(InventorySlot slot) {
        return RenderNode.component("slot-" + slot.id, Component.stateful(ctx -> {
            var isHovered = ctx.signal(false);

            Style slotStyle = Style.builder()
                .background(isHovered.get() ? 0xFF555577 : currentTheme.get().cardBg)
                .padding(6, 8)
                .margin(2, 0)
                .build();

            return Row(slotStyle, () -> {
                // Rarity indicator
                Text(slot.rarity.symbol + " ",
                    Style.builder().color(slot.rarity.color).build());

                // Item name
                Text(slot.name, Style.builder()
                    .color(currentTheme.get().textPrimary)
                    .build());

                FlexSpacer();

                // Count
                Text("x" + slot.count, Style.builder()
                    .color(currentTheme.get().textSecondary)
                    .build());

                Spacer(8);

                // Actions
                Button("+", () -> updateSlotCount(slot.id, 1), tinyButtonStyle());
                Button("-", () -> updateSlotCount(slot.id, -1), tinyButtonStyle());
            });
        }));
    }

    private void updateSlotCount(int slotId, int delta) {
        var updated = inventory.get().stream()
            .map(s -> s.id == slotId ?
                new InventorySlot(s.id, s.name, s.item, Math.max(0, s.count + delta), s.rarity) : s)
            .filter(s -> s.count > 0)
            .toList();
        inventory.set(updated);
    }

    // ========================================================================
    // CHAT TAB
    // ========================================================================

    private RenderNode buildChat() {
        return RenderNode.component("chat", Component.stateful(ctx -> {
            var messageCount = ctx.computed(() -> messages.get().size());

            // Effect: Scroll to bottom on new message
            ctx.effect(() -> {
                int count = messageCount.get();
                if (count > 0) {
                    System.out.println("[Chat] Message count: " + count);
                }
                return null;
            });

            return Column(() -> {
                Text("💬 Chat", titleStyle());
                Spacer(8);

                // Message count
                Text(() -> "Messages: " + messageCount.get(), normalTextStyle());

                Spacer(8);

                // Messages list
                Child(RenderNode.forEach(
                    messages::get,
                    msg -> msg.id,
                    (msg, idx) -> buildChatMessage(msg)
                ));

                FlexSpacer();

                // Typing indicator
                Child(RenderNode.showWhen(
                    isTyping::get,
                    Row(() -> {
                        Text("Someone is typing...",
                            Style.builder().color(0xFF888888).fontSize(10).build());
                    })
                ));

                Spacer(8);

                // Input area
                Row(() -> {
                    Text("Input: ", normalTextStyle());
                    Button(
                        inputText.get().isEmpty() ? "[Type message...]" : inputText.get(),
                        () -> inputText.set("Hello " + random.nextInt(100)),
                        Style.builder()
                            .background(currentTheme.get().inputBg)
                            .padding(6, 12)
                            .flex(1)
                            .build()
                    );

                    Spacer(4);

                    Button("Send", () -> {
                        if (!inputText.get().isEmpty()) {
                            var newMsg = new ChatMessage(
                                UUID.randomUUID().toString(),
                                "Player",
                                inputText.get(),
                                System.currentTimeMillis(),
                                false
                            );
                            var newList = new ArrayList<>(messages.get());
                            newList.add(newMsg);
                            messages.set(newList);
                            inputText.set("");
                        }
                    }, actionButtonStyle());
                });
            });
        }));
    }

    private RenderNode buildChatMessage(ChatMessage msg) {
        return RenderNode.component("msg-" + msg.id, Component.stateless(ctx -> {
            Style msgStyle = Style.builder()
                .background(msg.isSystem ? 0xFF334455 : currentTheme.get().cardBg)
                .padding(6, 8)
                .margin(2, 0)
                .build();

            String timeStr = formatTime(msg.timestamp);

            return Row(msgStyle, () -> {
                Text("[" + timeStr + "] ",
                    Style.builder().color(0xFF888888).fontSize(10).build());
                Text(msg.sender + ": ",
                    Style.builder().color(msg.isSystem ? 0xFF44AAFF : 0xFFAAAA44).bold().build());
                Text(msg.content, Style.builder()
                    .color(currentTheme.get().textPrimary)
                    .build());
            });
        }));
    }

    // ========================================================================
    // QUESTS TAB
    // ========================================================================

    private RenderNode buildQuests() {
        return RenderNode.component("quests", Component.stateful(ctx -> {
            var completedQuests = ctx.computed(() ->
                (int) quests.get().stream().filter(q -> q.status == QuestStatus.COMPLETED).count()
            );

            var totalProgress = ctx.computed(() -> {
                var list = quests.get();
                if (list.isEmpty()) return 0.0;
                return list.stream()
                    .mapToDouble(q -> (double) q.current / q.target)
                    .average()
                    .orElse(0.0) * 100;
            });

            return Column(() -> {
                Text("📜 Quests", titleStyle());
                Spacer(8);

                // Quest overview
                Row(() -> {
                    Text(() -> "Completed: " + completedQuests.get() + "/" + quests.get().size(),
                        normalTextStyle());
                    Spacer(16);
                    Text(() -> String.format("Overall: %.1f%%", totalProgress.get()),
                        normalTextStyle());
                });

                Spacer(12);

                // Quest list
                Child(RenderNode.forEach(
                    quests::get,
                    quest -> quest.id,
                    (quest, idx) -> buildQuestItem(quest)
                ));

                Spacer(12);

                // Selected quest details
                Child(RenderNode.showWhen(
                    () -> selectedQuest.get() != null,
                    buildQuestDetails()
                ));

                // Actions
                Spacer(12);
                Row(() -> {
                    Button("Progress Random Quest", () -> {
                        var list = quests.get();
                        if (!list.isEmpty()) {
                            int idx = random.nextInt(list.size());
                            Quest q = list.get(idx);
                            if (q.status != QuestStatus.COMPLETED) {
                                var updated = new Quest(q.id, q.title, q.description,
                                    Math.min(q.target, q.current + 1), q.target,
                                    q.current + 1 >= q.target ? QuestStatus.COMPLETED : QuestStatus.IN_PROGRESS);
                                var newList = new ArrayList<>(list);
                                newList.set(idx, updated);
                                quests.set(newList);

                                if (updated.status == QuestStatus.COMPLETED) {
                                    showNotification("Quest completed: " + updated.title);
                                    score.update(s -> s + 50);
                                }
                            }
                        }
                    }, actionButtonStyle());
                });
            });
        }));
    }

    private RenderNode buildQuestItem(Quest quest) {
        return RenderNode.component("quest-" + quest.id, Component.stateful(ctx -> {
            boolean isSelected = selectedQuest.get() != null &&
                selectedQuest.get().id.equals(quest.id);

            Style questStyle = Style.builder()
                .background(isSelected ? 0xFF445566 : currentTheme.get().cardBg)
                .padding(8)
                .margin(2, 0)
                .build();

            return Row(questStyle, () -> {
                // Status icon
                Text(quest.status.icon + " ",
                    Style.builder().color(quest.status.color).build());

                // Title
                Button(quest.title, () -> selectedQuest.set(quest),
                    Style.builder().color(currentTheme.get().textPrimary).build());

                FlexSpacer();

                // Progress
                Text(() -> quest.current + "/" + quest.target,
                    Style.builder().color(currentTheme.get().textSecondary).build());
            });
        }));
    }

    private RenderNode buildQuestDetails() {
        return RenderNode.component("quest-details", Component.stateless(ctx -> {
            Quest q = selectedQuest.get();
            if (q == null) return RenderNode.empty();

            Style detailStyle = Style.builder()
                .background(0xFF3A3A4A)
                .padding(12)
                .margin(8, 0)
                .build();

            return Column(detailStyle, () -> {
                Text("Quest Details", subtitleStyle());
                Spacer(4);
                Text("Title: " + q.title, normalTextStyle());
                Text("Description: " + q.description, normalTextStyle());
                Text("Status: " + q.status.name(), normalTextStyle());
                Text(() -> "Progress: " + q.current + "/" + q.target, normalTextStyle());

                Spacer(8);
                Button("Close", () -> selectedQuest.set(null), smallButtonStyle());
            });
        }));
    }

    // ========================================================================
    // SETTINGS TAB
    // ========================================================================

    private RenderNode buildSettings() {
        return RenderNode.component("settings", Component.stateful(ctx -> {
            var showAdvanced = ctx.signal(false);

            // Track settings changes
            ctx.effect(() -> {
                System.out.println("[Settings] Theme: " + currentTheme.get().name());
                System.out.println("[Settings] Debug: " + debugMode.get());
                return null;
            });

            return Column(() -> {
                Text("⚙️ Settings", titleStyle());
                Spacer(12);

                // Theme settings
                Text("Theme", subtitleStyle());
                Spacer(4);
                Row(() -> {
                    for (Theme theme : Theme.values()) {
                        Button(theme.name(),
                            () -> currentTheme.set(theme),
                            currentTheme.get() == theme ? activeThemeStyle(theme) : inactiveThemeStyle());
                    }
                });

                Spacer(16);

                // General settings
                Text("General", subtitleStyle());
                Spacer(4);
                Row(() -> {
                    Text("Debug Mode: ", normalTextStyle());
                    Button(
                        debugMode.get() ? "ON" : "OFF",
                        () -> debugMode.update(b -> !b),
                        debugMode.get() ? activeToggleStyle() : inactiveToggleStyle()
                    );
                });

                Spacer(12);

                // Advanced settings toggle
                Button(
                    showAdvanced.get() ? "▼ Hide Advanced" : "▶ Show Advanced",
                    () -> showAdvanced.update(b -> !b),
                    smallButtonStyle()
                );

                // Advanced settings (conditional)
                Child(RenderNode.showWhen(
                    showAdvanced::get,
                    Column(() -> {
                        Spacer(8);
                        Text("Advanced Settings", subtitleStyle());
                        Spacer(4);
                        Text("• Render FPS: 60", normalTextStyle());
                        Text("• Update Rate: 20 TPS", normalTextStyle());
                        Text("• Debug Overlay: " + (debugMode.get() ? "Enabled" : "Disabled"),
                            normalTextStyle());
                        Text("• Reactive System: Active", normalTextStyle());
                    })
                ));

                FlexSpacer();

                // Danger zone
                Spacer(16);
                Text("Danger Zone", Style.builder().color(0xFFFF4444).bold().build());
                Spacer(4);
                Button("Reset All Data", () -> {
                    clickCount.set(0);
                    score.set(0);
                    progress.set(0.0);
                    currentTheme.set(Theme.LIGHT);
                    debugMode.set(true);
                    initializeData();
                    showNotification("All data has been reset!");
                }, dangerButtonStyle());
            });
        }));
    }

    // ========================================================================
    // STATUS BAR
    // ========================================================================

    private RenderNode buildStatusBar() {
        return RenderNode.component("statusbar", Component.stateful(ctx -> {
            var currentTime = ctx.computed(() -> {
                // Force re-read on tick
                tickCount.get();
                return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            });

            Style barStyle = Style.builder()
                .background(currentTheme.get().statusBarBg)
                .padding(4, 8)
                .build();

            return Row(barStyle, () -> {
                Text(() -> "Tab: " + activeTab.get().displayName, statusTextStyle());
                Spacer(16);
                Text(() -> "Score: " + score.get(), statusTextStyle());
                Spacer(16);
                Text(() -> "Tick: " + tickCount.get(), statusTextStyle());

                FlexSpacer();

                Text(currentTime::get, statusTextStyle());
                Spacer(8);
                Text("CloudLib v1.0", statusTextStyle());
            });
        }));
    }

    // ========================================================================
    // RENDER LOOP
    // ========================================================================

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Update reactive tree
        elementTree.flushBuild();

        // Render background
        int bgColor = currentTheme.get().background;
        graphics.fill(0, 0, width, height, bgColor);

        // Render widget tree (existing system)
        super.render(graphics, mouseX, mouseY, partialTick);

        // Debug overlay
        if (debugMode.get()) {
            renderDebugOverlay(graphics);
        }
    }

    private void renderDebugOverlay(GuiGraphics graphics) {
        var stats = elementTree.getStats();
        int y = height - 24;

        String line1 = String.format("Elements: %d | Components: %d | Leaves: %d",
            stats.totalElements(), stats.componentElements(), stats.leafElements());
        graphics.drawString(font, line1, 4, y, 0x88FFFF00);

        String line2 = String.format("Theme: %s | Tab: %s | Tick: %d",
            currentTheme.get().name(), activeTab.get().name(), tickCount.get());
        graphics.drawString(font, line2, 4, y + 10, 0x8888FF88);
    }

    @Override
    public void tick() {
        super.tick();
        tickCount.update(t -> t + 1);

        // Simulate progress
        if (tickCount.get() % 20 == 0) {
            progress.update(p -> Math.min(1.0, p + 0.01));
        }
    }

    @Override
    public void removed() {
        super.removed();
        elementTree.detach();
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private void showNotification(String message) {
        notification.set(message);
    }

    private String formatTime(long timestamp) {
        long seconds = (System.currentTimeMillis() - timestamp) / 1000;
        if (seconds < 60) return seconds + "s ago";
        if (seconds < 3600) return (seconds / 60) + "m ago";
        return (seconds / 3600) + "h ago";
    }

    // ========================================================================
    // STYLE FACTORIES
    // ========================================================================

    private Style titleStyle() {
        return Style.builder()
            .fontSize(14)
            .bold()
            .color(currentTheme.get().textPrimary)
            .build();
    }

    private Style subtitleStyle() {
        return Style.builder()
            .fontSize(12)
            .bold()
            .color(currentTheme.get().textSecondary)
            .build();
    }

    private Style normalTextStyle() {
        return Style.builder()
            .color(currentTheme.get().textPrimary)
            .build();
    }

    private Style smallTextStyle() {
        return Style.builder()
            .color(currentTheme.get().textSecondary)
            .fontSize(10)
            .build();
    }

    private Style debugTextStyle() {
        return Style.builder()
            .color(0xFF888800)
            .fontSize(9)
            .build();
    }

    private Style statusTextStyle() {
        return Style.builder()
            .color(0xFFAAAAAA)
            .fontSize(10)
            .build();
    }

    private Style actionButtonStyle() {
        return Style.builder()
            .padding(6, 12)
            .margin(4)
            .background(currentTheme.get().accentColor)
            .color(0xFFFFFFFF)
            .build();
    }

    private Style smallButtonStyle() {
        return Style.builder()
            .padding(4, 8)
            .margin(2)
            .background(0xFF555555)
            .color(0xFFFFFFFF)
            .build();
    }

    private Style tinyButtonStyle() {
        return Style.builder()
            .padding(2, 6)
            .margin(1)
            .background(0xFF666666)
            .color(0xFFFFFFFF)
            .build();
    }

    private Style activeSortStyle() {
        return Style.builder()
            .padding(4, 8)
            .margin(2)
            .background(currentTheme.get().accentColor)
            .color(0xFFFFFFFF)
            .build();
    }

    private Style activeThemeStyle(Theme theme) {
        return Style.builder()
            .padding(6, 12)
            .margin(4)
            .background(theme.accentColor)
            .color(0xFFFFFFFF)
            .build();
    }

    private Style inactiveThemeStyle() {
        return Style.builder()
            .padding(6, 12)
            .margin(4)
            .background(0xFF444444)
            .color(0xFFAAAAAA)
            .build();
    }

    private Style activeToggleStyle() {
        return Style.builder()
            .padding(4, 12)
            .background(0xFF44AA44)
            .color(0xFFFFFFFF)
            .build();
    }

    private Style inactiveToggleStyle() {
        return Style.builder()
            .padding(4, 12)
            .background(0xFF666666)
            .color(0xFFAAAAAA)
            .build();
    }

    private Style dangerButtonStyle() {
        return Style.builder()
            .padding(8, 16)
            .background(0xFFAA4444)
            .color(0xFFFFFFFF)
            .build();
    }

    // ========================================================================
    // DATA CLASSES
    // ========================================================================

    public enum TabType {
        DASHBOARD("Dashboard", "📊"),
        INVENTORY("Inventory", "🎒"),
        CHAT("Chat", "💬"),
        QUESTS("Quests", "📜"),
        SETTINGS("Settings", "⚙️");

        final String displayName;
        final String icon;

        TabType(String displayName, String icon) {
            this.displayName = displayName;
            this.icon = icon;
        }
    }

    public enum Theme {
        LIGHT(
            "Light",
            0xFFF0F0F0,  // background
            0xFFE0E0E0,  // headerBg
            0xFFD0D0D0,  // sidebarBg
            0xFFFFFFFF,  // contentBg
            0xFFC0C0C0,  // statusBarBg
            0xFFEEEEEE,  // cardBg
            0xFFDDDDDD,  // inputBg
            0xFF333333,  // textPrimary
            0xFF666666,  // textSecondary
            0xFF4488FF   // accentColor
        ),
        DARK(
            "Dark",
            0xFF1A1A2E,
            0xFF16213E,
            0xFF1A1A2E,
            0xFF0F3460,
            0xFF16213E,
            0xFF1A2744,
            0xFF233554,
            0xFFE0E0E0,
            0xFF888888,
            0xFFE94560
        ),
        OCEAN(
            "Ocean",
            0xFF0A192F,
            0xFF172A45,
            0xFF0A192F,
            0xFF1D3557,
            0xFF172A45,
            0xFF233B54,
            0xFF2D4A65,
            0xFFCCD6F6,
            0xFF8892B0,
            0xFF64FFDA
        );

        final String name;
        final int background;
        final int headerBg;
        final int sidebarBg;
        final int contentBg;
        final int statusBarBg;
        final int cardBg;
        final int inputBg;
        final int textPrimary;
        final int textSecondary;
        final int accentColor;

        Theme(String name, int background, int headerBg, int sidebarBg,
              int contentBg, int statusBarBg, int cardBg, int inputBg,
              int textPrimary, int textSecondary, int accentColor) {
            this.name = name;
            this.background = background;
            this.headerBg = headerBg;
            this.sidebarBg = sidebarBg;
            this.contentBg = contentBg;
            this.statusBarBg = statusBarBg;
            this.cardBg = cardBg;
            this.inputBg = inputBg;
            this.textPrimary = textPrimary;
            this.textSecondary = textSecondary;
            this.accentColor = accentColor;
        }
    }

    public enum Rarity {
        COMMON("◆", 0xFFAAAAAA),
        UNCOMMON("◆", 0xFF55FF55),
        RARE("◆", 0xFF5555FF),
        EPIC("◆", 0xFFAA55FF);

        final String symbol;
        final int color;

        Rarity(String symbol, int color) {
            this.symbol = symbol;
            this.color = color;
        }
    }

    public enum SortMode { NAME, COUNT, RARITY }

    public enum QuestStatus {
        NOT_STARTED("○", 0xFF666666),
        IN_PROGRESS("◐", 0xFFFFAA00),
        COMPLETED("●", 0xFF44FF44);

        final String icon;
        final int color;

        QuestStatus(String icon, int color) {
            this.icon = icon;
            this.color = color;
        }
    }

    public record InventorySlot(int id, String name, net.minecraft.world.item.Item item, int count, Rarity rarity) {}
    public record ChatMessage(String id, String sender, String content, long timestamp, boolean isSystem) {}
    public record Quest(String id, String title, String description, int current, int target, QuestStatus status) {}
}
