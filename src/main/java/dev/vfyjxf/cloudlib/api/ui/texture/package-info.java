/**
 * CloudLib Texture API - A flexible, composable GUI texture system.
 *
 * <h2>Design Philosophy</h2>
 * <p>
 * This system follows the <strong>composition over inheritance</strong> principle,
 * providing minimal core interfaces with complex functionality achieved through
 * composition and decorator patterns.
 *
 * <h2>Core Interfaces</h2>
 *
 * <h3>{@link dev.vfyjxf.cloudlib.api.ui.texture.UITexture}</h3>
 * <p>
 * Functional interface defining a single render method:
 * <pre>{@code
 * @FunctionalInterface
 * public interface Texture {
 *     void render(GuiGraphics graphics, int x, int y, int width, int height);
 * }
 * }</pre>
 *
 * <h3>{@link dev.vfyjxf.cloudlib.api.ui.texture.SizedTexture}</h3>
 * <p>
 * Extends Texture with intrinsic dimensions:
 * <pre>{@code
 * public interface SizedTexture extends Texture {
 *     int width();
 *     int height();
 * }
 * }</pre>
 *
 * <h2>Animation System</h2>
 *
 * <h3>{@link dev.vfyjxf.cloudlib.api.ui.texture.Animation}</h3>
 * <p>
 * Generic interface for animations, independent of Texture:
 * <pre>{@code
 * public interface Animation<T> {
 *     float progress();
 *     void setProgress(float progress);
 *     T value();
 *     T value(float partialTick);
 * }
 * }</pre>
 *
 * <h3>{@link dev.vfyjxf.cloudlib.api.ui.texture.Playable}</h3>
 * <p>
 * Extends Animation with time-driven playback control.
 *
 * <h2>Texture Implementations</h2>
 * <ul>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.texture.ImageTexture} - Image textures</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture} - Solid color textures</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.texture.NineSliceTexture} - Nine-slice scalable textures</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.texture.SpriteTexture} - Texture atlas sprites</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.texture.ProgressTexture} - Progress bars</li>
 * </ul>
 *
 * <h2>Animation Implementations</h2>
 * <ul>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.texture.FrameAnimation} - Frame-based animations</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.texture.TweenAnimation} - Tween animations with easing</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.texture.AnimationSequence} - Sequential animations</li>
 * </ul>
 *
 * <h2>Utilities</h2>
 * <ul>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.texture.Textures} - Composition and transformation utilities</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.texture.TextureBatch} - Batch rendering for performance</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.texture.Easing} - Easing functions</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.texture.Interpolator} - Value interpolators</li>
 * </ul>
 *
 * @see dev.vfyjxf.cloudlib.api.ui.texture.UITexture
 * @see dev.vfyjxf.cloudlib.api.ui.texture.Animation
 * @see dev.vfyjxf.cloudlib.api.ui.texture.Textures
 */
package dev.vfyjxf.cloudlib.api.ui.texture;
