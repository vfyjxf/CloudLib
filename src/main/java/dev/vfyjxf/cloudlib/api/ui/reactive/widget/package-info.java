/**
 * Reactive Widget System for CloudLib.
 * <p>
 * This package contains a lightweight widget system specifically designed
 * for the reactive rendering pipeline. Unlike the main Widget system, RWidget
 * (Reactive Widget) is optimized for:
 * <ul>
 *   <li><b>Fine-grained updates</b> - Only dirty widgets re-render</li>
 *   <li><b>Direct rendering</b> - Minimal abstraction over GuiGraphics</li>
 *   <li><b>ElementTree integration</b> - Works with the reactive Element tree</li>
 *   <li><b>Lightweight</b> - Simple, focused API</li>
 * </ul>
 * 
 * <h2>Architecture</h2>
 * <pre>
 * RenderNode (DSL output)
 *      │
 *      ▼ ReactiveRenderer.buildWidget()
 * RWidget tree
 *      │
 *      ▼ RWidget.render()
 * GuiGraphics
 * </pre>
 * 
 * <h2>Available Widgets</h2>
 * <ul>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.widget.RWidget} - Base class</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.widget.RText} - Text display</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.widget.RButton} - Clickable button</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.widget.RGroup} - Layout container (Column/Row)</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.widget.RScrollArea} - Scrollable container</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.widget.RPanel} - Bordered panel with title</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.widget.RCheckbox} - Checkbox toggle</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.widget.RSpacer} - Layout spacer</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // In a Screen class
 * private ElementTree elementTree;
 * private ReactiveRenderer renderer;
 * 
 * @Override
 * protected void init() {
 *     elementTree = new ElementTree();
 *     renderer = new ReactiveRenderer(font);
 *     renderer.attachElementTree(elementTree, this::buildUI);
 * }
 * 
 * private RenderNode buildUI() {
 *     return Column(() -> {
 *         Text("Hello World");
 *         Button("Click Me", () -> System.out.println("Clicked!"));
 *     });
 * }
 * 
 * @Override
 * public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
 *     elementTree.flushBuild();
 *     renderer.render(graphics, 20, 20, mouseX, mouseY, delta);
 * }
 * }</pre>
 * 
 * @see dev.vfyjxf.cloudlib.api.ui.reactive.widget.ReactiveRenderer
 * @see dev.vfyjxf.cloudlib.api.ui.element.ElementTree
 */
@javax.annotation.ParametersAreNonnullByDefault
@net.minecraft.MethodsReturnNonnullByDefault
package dev.vfyjxf.cloudlib.api.ui.reactive.widget;
