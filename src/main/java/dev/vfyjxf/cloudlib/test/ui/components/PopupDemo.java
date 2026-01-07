package dev.vfyjxf.cloudlib.test.ui.components;

import dev.vfyjxf.cloudlib.api.ui.reactive.Component;
import dev.vfyjxf.cloudlib.api.ui.reactive.Key;
import dev.vfyjxf.cloudlib.api.ui.reactive.RenderNode;
import dev.vfyjxf.cloudlib.api.ui.reactive.Signal;
import dev.vfyjxf.cloudlib.api.ui.reactive.Style;
import dev.vfyjxf.cloudlib.api.ui.reactive.widget.RPopup;
import dev.vfyjxf.cloudlib.api.ui.reactive.widget.layer.LayoutMode;

import java.util.function.Supplier;

import static dev.vfyjxf.cloudlib.api.ui.reactive.Render.*;
import static dev.vfyjxf.cloudlib.api.ui.reactive.Style.*;

/**
 * Self-contained popup components using {@link Component#stateful} for state management.
 * 
 * <p><b>No field declaration needed in parent!</b> Just embed directly in UI code:</p>
 * <pre>{@code
 * // In buildWidgetsTab() - using typed Key
 * Embed(Key.of("widgets", "popup"), PopupDemo.PopupHolder(this::buttonStyle));
 * 
 * // Or with simple string (still works)
 * Embed("widgets.popup", PopupDemo.PopupHolder(this::buttonStyle));
 * }</pre>
 * 
 * <p>State is managed by {@link dev.vfyjxf.cloudlib.api.ui.reactive.ComponentContext ComponentContext}
 * and persists across rebuilds automatically.</p>
 * 
 * <h2>Layer System</h2>
 * <p>Popups use the new layer system:</p>
 * <ul>
 *   <li>{@link LayoutMode#FLOATING} - Popup floats within parent, doesn't affect sibling layout</li>
 *   <li>{@link LayoutMode#OVERLAY} - Popup renders in separate layer above all content</li>
 * </ul>
 */
public final class PopupDemo {
    
    private PopupDemo() {} // No instantiation
    
    /**
     * A self-contained popup section using the new layer system.
     * 
     * <p>This popup uses {@link LayoutMode#FLOATING} - it floats within its parent
     * but doesn't affect sibling layout calculations.</p>
     * 
     * <h3>Usage:</h3>
     * <pre>{@code
     * // Directly in UI building code - no setup needed!
     * Embed(Key.of("widgets", "popup"), PopupDemo.PopupHolder(this::buttonStyle));
     * }</pre>
     * 
     * @param buttonStyle button style supplier
     * @return the stateful component
     */
    public static Component PopupHolder(Supplier<Style> buttonStyle) {
        return Component.stateful(ctx -> {
            // Use ComponentContext hooks for state - like React/Compose hooks
            Signal<Boolean> showPopup = ctx.signal(false);
            RPopup.PopupState popupState = ctx.memo(
                () -> new RPopup.PopupState().setPosition(100, 80));
            RenderNode.ScrollState scrollState = ctx.memo(
                () -> new RenderNode.ScrollState().setDraggable(true));
            
            // Build UI using current state
            // Using Column instead of Stack - popup is rendered in overlay layer,
            // so it won't interfere with column layout
            return Column(() -> {
                // Control section
                Text("Draggable Popup (Layer System)", Style.of(bold(), color(0xFF88AAFF)));
                Spacer(4);
                
                Row(() -> {
                    Button(showPopup.get() ? "Hide Popup" : "Show Popup",
                        () -> showPopup.update(b -> !b),
                        buttonStyle.get());
                    
                    if (showPopup.get()) {
                        Spacer(8);
                        Text("(Drag title bar to move)", Style.of(color(0xFF888888)));
                    }
                });
                
                Spacer(8);
                Text("State: " + (showPopup.get() ? "VISIBLE" : "HIDDEN"),
                    Style.of(color(0xFF666666)));
                
                Spacer(4);
                Text("Mode: Overlay Layer (LayerPriority.POPUP)", 
                    Style.of(color(0xFF888888), fontSize(9)));
                
                // Popup is added to popup layer by ReactiveRenderer
                // Not rendered inline - doesn't affect this Column's layout!
                // Popup auto-adds to scope, no need for Child()
                if (showPopup.get()) {
                    buildPopupNode(popupState, scrollState, showPopup);
                }
            });
        });
    }
    
    /**
     * Simplified toggle button with internal state.
     * Demonstrates the simplest possible stateful component.
     * 
     * <pre>{@code
     * // No setup needed!
     * Embed("myToggle", PopupDemo.ToggleButton("Dark Mode", Style.of(...)));
     * }</pre>
     */
    public static Component ToggleButton(String label, Style style) {
        return Component.stateful(ctx -> {
            Signal<Boolean> on = ctx.signal(false);
            
            return Row(() -> {
                Button(on.get() ? "✓ " + label : "○ " + label,
                    () -> on.update(b -> !b),
                    style);
                Spacer(8);
                Text("(" + (on.get() ? "ON" : "OFF") + ")", Style.of(color(0xFF888888)));
            });
        });
    }
    
    /**
     * A counter component with internal state.
     */
    public static Component Counter(Style buttonStyle) {
        return Component.stateful(ctx -> {
            Signal<Integer> count = ctx.signal(0);
            
            return Row(() -> {
                Button("-", () -> count.update(n -> n - 1), buttonStyle);
                Spacer(4);
                Text(String.valueOf(count.get()), Style.of(color(0xFFFFFFFF)));
                Spacer(4);
                Button("+", () -> count.update(n -> n + 1), buttonStyle);
            });
        });
    }
    
    /**
     * Demonstrates using the Floating DSL directly for in-container floating panels.
     * 
     * <p>Unlike popups (which render in a separate overlay layer), floating widgets
     * stay within their parent container but don't affect sibling layout.</p>
     * 
     * <p>Use cases:</p>
     * <ul>
     *   <li>Draggable tool panels</li>
     *   <li>Floating info boxes</li>
     *   <li>Mini-maps or status overlays</li>
     * </ul>
     * 
     * @param buttonStyle button style supplier
     * @return the stateful component
     */
    public static Component FloatingPanelDemo(Supplier<Style> buttonStyle) {
        return Component.stateful(ctx -> {
            Signal<Boolean> showPanel = ctx.signal(false);
            
            return Column(() -> {
                Text("Floating Panel (In-Container)", Style.of(bold(), color(0xFFFFCC88)));
                Spacer(4);
                
                Row(() -> {
                    Button(showPanel.get() ? "Hide Panel" : "Show Panel",
                        () -> showPanel.update(b -> !b),
                        buttonStyle.get());
                    
                    if (showPanel.get()) {
                        Spacer(8);
                        Text("(Draggable within container)", Style.of(color(0xFF888888)));
                    }
                });
                
                // The floating panel - doesn't affect the layout of items below
                if (showPanel.get()) {
                    Floating(20, 60, true, () -> {
                        // Panel content - Floating already creates a Column internally
                        Text("📌 Floating Panel", Style.of(bold(), color(0xFFFFFFFF)));
                        Spacer(4);
                        Text("Drag me!", Style.of(color(0xFFCCCCCC)));
                        Text("I don't affect layout", Style.of(color(0xFF999999), fontSize(8)));
                    });
                }
                
                // This text is NOT pushed down by the floating panel
                Spacer(8);
                Text("↑ Notice: Content above doesn't move", Style.of(color(0xFF88FF88)));
                Text("  when floating panel is shown", Style.of(color(0xFF88FF88)));
            });
        });
    }
    
    // ===== Private Helpers =====
    
    /**
     * Builds the popup render node.
     * 
     * <p>This popup is handled specially by ReactiveRenderer - it gets added to
     * the popup layer (LayerPriority.POPUP) instead of being rendered inline.
     * This means it appears above all normal content without affecting layout.</p>
     */
    private static RenderNode buildPopupNode(
            RPopup.PopupState popupState,
            RenderNode.ScrollState scrollState,
            Signal<Boolean> showPopup) {
        
        return Popup("Layer-Based Popup", 220, 180, popupState,
            popup -> {
                popup.onCloseRequest().register(ctx -> {
                    System.out.println("[PopupHolder] Close requested");
                    showPopup.set(false);
                });
                popup.events().attach().register(ctx ->
                    System.out.println("[PopupHolder] Popup attached (Layer System)"));
                popup.events().detach().register(ctx ->
                    System.out.println("[PopupHolder] Popup detached"));
            },
            () -> {
                // ScrollArea auto-adds to parent scope, no need for Child()
                // ScrollArea internally creates a Column, so content goes directly
                ScrollArea(208, 140, scrollState, () -> {
                    Text("Layer System Active!", Style.of(bold(), color(0xFF88FF88)));
                    Text("Rendered in popup layer", Style.of(color(0xFFFFFFFF)));
                    Spacer(8);
                    
                    Text("Layer Features:", Style.of(color(0xFFCCCCCC)));
                    Spacer(4);
                    
                    for (String feature : new String[]{
                        "✓ Automatic z-ordering",
                        "✓ Doesn't affect parent layout",
                        "✓ Input handled front-to-back",
                        "✓ Modal support available",
                        "✓ Cached across frames"
                    }) {
                        Text(feature, Style.of(color(0xFFAADDFF)));
                        Spacer(2);
                    }
                    
                    Spacer(8);
                    for (int i = 1; i <= 10; i++) {
                        Text("📌 Item #" + i, Style.of(color(0xFFDDDDFF)));
                        Spacer(4);
                    }
                });
            }
        );
    }
}
