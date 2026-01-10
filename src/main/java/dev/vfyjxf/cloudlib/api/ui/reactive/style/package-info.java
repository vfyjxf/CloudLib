/**
 * Composable style system for UI elements.
 * <p>
 * This package provides a CSS-like styling API that allows declarative,
 * composable styling of UI components.
 * <p>
 * <h2>Quick Start</h2>
 * <pre>{@code
 * import static dev.vfyjxf.cloudlib.api.ui.reactive.style.Styles.*;
 *
 * // Create styles
 * var cardStyle = Style.of(
 *     padding(12),
 *     background(0xFFFFFFFF),
 *     border(1, 0xFF666666),
 *     rounded(4)
 * );
 *
 * var buttonStyle = Style.of(
 *     padding(8, 16),
 *     background(0xFF0066CC),
 *     rounded(4),
 *     cursor(Cursor.HAND)
 * );
 *
 * // Chain-style composition
 * var hoveredButton = buttonStyle.with(
 *     background(0xFF0088FF)
 * );
 *
 * // Merge styles (later properties override)
 * var combined = cardStyle.merge(buttonStyle);
 * }</pre>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.style.Style} - The main style container</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.style.Styles} - Static DSL methods</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.style.StyleProperty} - Base interface for properties</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.style.StyleContext} - Context for applying styles</li>
 * </ul>
 *
 * <h2>Extensibility</h2>
 * <p>
 * External code can implement {@link dev.vfyjxf.cloudlib.api.ui.reactive.style.StyleProperty}
 * to create custom style properties:
 * <pre>{@code
 * public class ShadowProperty implements StyleProperty {
 *     private final double blur;
 *     private final int color;
 *
 *     @Override
 *     public void apply(StyleContext context) {
 *         context.setCustom("shadowBlur", blur);
 *         context.setCustom("shadowColor", color);
 *     }
 *
 *     @Override
 *     public String name() {
 *         return "shadow";
 *     }
 * }
 * }</pre>
 *
 * @see dev.vfyjxf.cloudlib.api.ui.reactive.style.Style
 * @see dev.vfyjxf.cloudlib.api.ui.reactive.style.Styles
 */
@ApiStatus.Experimental
package dev.vfyjxf.cloudlib.api.ui.reactive.style;

import org.jetbrains.annotations.ApiStatus;
