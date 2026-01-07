package dev.vfyjxf.cloudlib.test.ui;

import dev.vfyjxf.cloudlib.api.ui.reactive.Computed;
import dev.vfyjxf.cloudlib.api.ui.reactive.Signal;
import dev.vfyjxf.cloudlib.api.ui.reactive.Tracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * A working demo screen that actually renders to screen.
 * <p>
 * This demonstrates the reactive system with real rendering.
 * <p>
 * Press ` (backtick) in game to open.
 */
public class WorkingShowcaseScreen extends Screen {

    // ===== Reactive State =====
    private final Signal<Integer> count = Signal.of(0);
    private final Signal<Integer> score = Signal.of(0);
    private final Signal<String> activeTab = Signal.of("counter");
    private final Signal<Boolean> darkMode = Signal.of(false);
    private final Signal<List<String>> todos = Signal.of(new ArrayList<>(List.of(
        "Learn Reactive UI",
        "Build awesome GUIs", 
        "Have fun coding"
    )));
    
    // ===== Computed Values =====
    private final Computed<Integer> doubled;
    private final Computed<Integer> squared;
    private final Computed<String> countText;
    private final Computed<Integer> todoCount;
    
    // ===== UI State =====
    private int selectedButton = -1;
    
    public WorkingShowcaseScreen() {
        super(Component.literal("Reactive UI Demo"));
        
        // Initialize computed values
        this.doubled = Computed.of(() -> count.get() * 2);
        this.squared = Computed.of(() -> count.get() * count.get());
        this.countText = Computed.of(() -> "Count: " + count.get());
        this.todoCount = Computed.of(() -> todos.get().size());
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Background
        int bgColor = darkMode.get() ? 0xFF1A1A2E : 0xFFF0F0F0;
        graphics.fill(0, 0, width, height, bgColor);
        
        int textColor = darkMode.get() ? 0xFFE0E0E0 : 0xFF333333;
        int accentColor = darkMode.get() ? 0xFFE94560 : 0xFF4488FF;
        
        int y = 10;
        int x = 20;
        
        // ===== Header =====
        graphics.drawString(font, "☰ CloudLib Reactive UI Demo", x, y, accentColor);
        y += 20;
        
        // Theme toggle
        String themeText = darkMode.get() ? "[Dark Mode]" : "[Light Mode]";
        int themeX = width - 100;
        graphics.drawString(font, themeText, themeX, 10, textColor);
        
        // ===== Tab Bar =====
        y += 5;
        String[] tabs = {"counter", "todos", "about"};
        int tabX = x;
        for (String tab : tabs) {
            boolean isActive = activeTab.get().equals(tab);
            int tabColor = isActive ? accentColor : 0xFF666666;
            int tabBg = isActive ? (darkMode.get() ? 0xFF2A2A4E : 0xFFDDDDFF) : 0x00000000;
            
            String tabLabel = "[" + capitalize(tab) + "]";
            int tabWidth = font.width(tabLabel) + 8;
            
            if (tabBg != 0) {
                graphics.fill(tabX - 2, y - 2, tabX + tabWidth, y + 12, tabBg);
            }
            graphics.drawString(font, tabLabel, tabX, y, tabColor);
            tabX += tabWidth + 10;
        }
        y += 25;
        
        // ===== Separator =====
        graphics.fill(x, y, width - x, y + 1, 0xFF444444);
        y += 10;
        
        // ===== Tab Content =====
        switch (activeTab.get()) {
            case "counter" -> renderCounterTab(graphics, x, y, textColor, accentColor);
            case "todos" -> renderTodosTab(graphics, x, y, textColor, accentColor);
            case "about" -> renderAboutTab(graphics, x, y, textColor, accentColor);
        }
        
        // ===== Footer =====
        int footerY = height - 20;
        graphics.fill(x, footerY - 5, width - x, footerY - 4, 0xFF444444);
        graphics.drawString(font, "Score: " + score.get(), x, footerY, 0xFF888888);
        graphics.drawString(font, "Press ESC to close", width - 120, footerY, 0xFF888888);
        
        // ===== Debug Info =====
        graphics.drawString(font, "Tab: " + activeTab.get() + " | Count: " + count.get(), 
            x, height - 35, 0xFF666666);
    }
    
    private void renderCounterTab(GuiGraphics graphics, int x, int y, int textColor, int accentColor) {
        graphics.drawString(font, "📊 Counter Demo", x, y, accentColor);
        y += 20;
        
        // Main counter display
        graphics.drawString(font, countText.get(), x, y, textColor);
        y += 15;
        
        // Derived values (auto-computed)
        graphics.drawString(font, "Doubled: " + doubled.get(), x, y, textColor);
        y += 12;
        graphics.drawString(font, "Squared: " + squared.get(), x, y, textColor);
        y += 12;
        graphics.drawString(font, "Is Even: " + (count.get() % 2 == 0), x, y, textColor);
        y += 25;
        
        // Buttons
        graphics.drawString(font, "Controls:", x, y, 0xFF888888);
        y += 15;
        
        String[] buttons = {"[-10]", "[-1]", "[Reset]", "[+1]", "[+10]"};
        int btnX = x;
        for (int i = 0; i < buttons.length; i++) {
            int btnColor = (selectedButton == i) ? 0xFFFFFF00 : accentColor;
            graphics.drawString(font, buttons[i], btnX, y, btnColor);
            btnX += font.width(buttons[i]) + 10;
        }
        y += 25;
        
        // Score buttons
        graphics.drawString(font, "Score:", x, y, 0xFF888888);
        y += 15;
        graphics.drawString(font, "[+10 Score]  [+50 Score]  [Reset Score]", x, y, accentColor);
        
        y += 30;
        graphics.drawString(font, "💡 Click buttons above or use keyboard:", x, y, 0xFF888888);
        y += 12;
        graphics.drawString(font, "   ← → to change count, ↑ ↓ for score", x, y, 0xFF888888);
    }
    
    private void renderTodosTab(GuiGraphics graphics, int x, int y, int textColor, int accentColor) {
        graphics.drawString(font, "📝 Todo List", x, y, accentColor);
        y += 15;
        graphics.drawString(font, "Total: " + todoCount.get() + " items", x, y, 0xFF888888);
        y += 20;
        
        List<String> items = todos.get();
        for (int i = 0; i < items.size(); i++) {
            String item = items.get(i);
            graphics.drawString(font, "• " + item, x + 10, y, textColor);
            graphics.drawString(font, "[X]", x + 200, y, 0xFFAA4444);
            y += 14;
        }
        
        y += 15;
        graphics.drawString(font, "[+ Add Todo]  [Clear All]", x, y, accentColor);
        
        y += 25;
        graphics.drawString(font, "💡 Press 'A' to add, 'C' to clear", x, y, 0xFF888888);
    }
    
    private void renderAboutTab(GuiGraphics graphics, int x, int y, int textColor, int accentColor) {
        graphics.drawString(font, "ℹ️ About Reactive UI", x, y, accentColor);
        y += 20;
        
        String[] lines = {
            "This demo shows CloudLib's reactive UI system:",
            "",
            "• Signal - Mutable reactive state",
            "• Computed - Auto-derived values",
            "• Fine-grained updates - Only changed parts update",
            "",
            "When you change 'count', only the count-related",
            "text updates. The rest of the UI stays unchanged.",
            "",
            "This is different from React where the entire",
            "component would re-render on state change."
        };
        
        for (String line : lines) {
            graphics.drawString(font, line, x, y, line.isEmpty() ? textColor : 
                (line.startsWith("•") ? accentColor : textColor));
            y += 12;
        }
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case 263 -> { // Left arrow
                count.update(n -> n - 1);
                return true;
            }
            case 262 -> { // Right arrow
                count.update(n -> n + 1);
                return true;
            }
            case 265 -> { // Up arrow
                score.update(s -> s + 10);
                return true;
            }
            case 264 -> { // Down arrow
                score.update(s -> Math.max(0, s - 10));
                return true;
            }
            case 82 -> { // R key - reset
                count.set(0);
                return true;
            }
            case 84 -> { // T key - toggle theme
                darkMode.update(b -> !b);
                return true;
            }
            case 65 -> { // A key - add todo
                if (activeTab.get().equals("todos")) {
                    List<String> current = new ArrayList<>(todos.get());
                    current.add("New Task #" + (current.size() + 1));
                    todos.set(current);
                }
                return true;
            }
            case 67 -> { // C key - clear todos
                if (activeTab.get().equals("todos")) {
                    todos.set(new ArrayList<>());
                }
                return true;
            }
            case 49 -> { // 1 key
                activeTab.set("counter");
                return true;
            }
            case 50 -> { // 2 key
                activeTab.set("todos");
                return true;
            }
            case 51 -> { // 3 key
                activeTab.set("about");
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = 20;
        int y = 40; // Tab bar Y position
        
        // Tab clicks
        String[] tabs = {"counter", "todos", "about"};
        int tabX = x;
        for (String tab : tabs) {
            String tabLabel = "[" + capitalize(tab) + "]";
            int tabWidth = font.width(tabLabel) + 8;
            
            if (mouseX >= tabX - 2 && mouseX <= tabX + tabWidth &&
                mouseY >= y - 2 && mouseY <= y + 12) {
                activeTab.set(tab);
                return true;
            }
            tabX += tabWidth + 10;
        }
        
        // Theme toggle click
        int themeX = width - 100;
        if (mouseX >= themeX && mouseX <= themeX + 80 && mouseY >= 8 && mouseY <= 22) {
            darkMode.update(b -> !b);
            return true;
        }
        
        // Counter tab button clicks
        if (activeTab.get().equals("counter")) {
            int btnY = 130;
            int btnX = x;
            String[] buttons = {"[-10]", "[-1]", "[Reset]", "[+1]", "[+10]"};
            int[] deltas = {-10, -1, 0, 1, 10};
            
            for (int i = 0; i < buttons.length; i++) {
                int btnWidth = font.width(buttons[i]);
                if (mouseX >= btnX && mouseX <= btnX + btnWidth &&
                    mouseY >= btnY && mouseY <= btnY + 12) {
                    final int delta = deltas[i];
                    if (delta == 0) {
                        count.set(0);
                    } else {
                        count.update(n -> n + delta);
                    }
                    return true;
                }
                btnX += btnWidth + 10;
            }
            
            // Score buttons
            btnY = 170;
            if (mouseY >= btnY && mouseY <= btnY + 12) {
                if (mouseX >= x && mouseX <= x + 80) {
                    score.update(s -> s + 10);
                    return true;
                } else if (mouseX >= x + 90 && mouseX <= x + 170) {
                    score.update(s -> s + 50);
                    return true;
                } else if (mouseX >= x + 180 && mouseX <= x + 280) {
                    score.set(0);
                    return true;
                }
            }
        }
        
        // Todo tab interactions
        if (activeTab.get().equals("todos")) {
            // Delete todo clicks
            List<String> items = todos.get();
            int itemY = 95;
            for (int i = 0; i < items.size(); i++) {
                if (mouseX >= x + 200 && mouseX <= x + 220 &&
                    mouseY >= itemY && mouseY <= itemY + 12) {
                    List<String> newList = new ArrayList<>(items);
                    newList.remove(i);
                    todos.set(newList);
                    return true;
                }
                itemY += 14;
            }
            
            // Add/Clear buttons
            int btnY = itemY + 15;
            if (mouseY >= btnY && mouseY <= btnY + 12) {
                if (mouseX >= x && mouseX <= x + 80) {
                    List<String> current = new ArrayList<>(todos.get());
                    current.add("New Task #" + (current.size() + 1));
                    todos.set(current);
                    return true;
                } else if (mouseX >= x + 100 && mouseX <= x + 180) {
                    todos.set(new ArrayList<>());
                    return true;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    private String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
