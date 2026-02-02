package dev.vfyjxf.cloudlib.api.ui.texture;

import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import net.minecraft.resources.ResourceLocation;

/**
 * A texture that supports batch rendering via {@link SceneCanvas}.
 * <p>
 * Uses a "push" model where the texture emits rendering primitives into a {@link VertexEmitter}.
 * The emitter provides only two fundamental operations: {@code textured()} and {@code colored()}.
 * Complex effects (gradients, borders, shadows, etc.) should be built by texture implementations
 * using these primitives.
 *
 * <h3>Built-in Implementations</h3>
 * <ul>
 *   <li>{@link ImageTexture} - Single textured quad</li>
 *   <li>{@link SpriteTexture} - Atlas sprite quad</li>
 *   <li>{@link NineSliceTexture} - 9 textured quads</li>
 *   <li>{@link ColorTexture} - Single colored quad</li>
 * </ul>
 *
 */
public interface BatchableTexture extends VisualTexture {

    /**
     * Emits rendering primitives into the emitter for batch rendering.
     * <p>
     * Implementations should call appropriate methods on {@link VertexEmitter}
     * one or more times to emit primitives. For example:
     * <ul>
     *   <li>Simple textures emit 1 textured quad</li>
     *   <li>Nine-slice textures emit up to 9 textured quads</li>
     *   <li>Color textures emit 1 colored quad</li>
     *   <li>Gradient textures emit 1 gradient quad</li>
     *   <li>Border textures emit up to 4 border primitives</li>
     *   <li>Composite textures may emit mixed primitives</li>
     * </ul>
     *
     * @param emitter the vertex emitter to push primitives into
     * @param x       screen X position
     * @param y       screen Y position
     * @param width   render width
     * @param height  render height
     * @param color   tint color (ARGB format, 0xFFFFFFFF = no tint)
     */
    void emit(VertexEmitter emitter, float x, float y, float width, float height, int color);

    /**
     * Returns whether this texture supports batching.
     * <p>
     * Override to return false for dynamic textures that cannot be batched
     * (e.g., textures with per-frame animation state).
     */
    default boolean supportsBatching() {
        return true;
    }

    /**
     * Emitter interface for receiving rendering primitives.
     * <p>
     * Implemented by the rendering system (SceneCanvas) to collect primitive data for batched rendering.
     * Provides only the essential primitives - complex effects should be built by textures using these.
     *
     * <h3>Design Principle</h3>
     * Keep the emitter simple with only fundamental operations. Complex rendering like
     * gradients, borders, shadows should be implemented by texture classes using these primitives.
     */
    interface VertexEmitter {

        /**
         * Emits a textured quad with UV coordinates.
         *
         * @param texture the texture ResourceLocation
         * @param x       screen X position
         * @param y       screen Y position
         * @param width   quad width
         * @param height  quad height
         * @param u0      left UV coordinate (0.0 to 1.0)
         * @param v0      top UV coordinate (0.0 to 1.0)
         * @param u1      right UV coordinate (0.0 to 1.0)
         * @param v1      bottom UV coordinate (0.0 to 1.0)
         * @param color   tint color (ARGB), 0xFFFFFFFF means no tint
         */
        void textured(ResourceLocation texture, float x, float y, float width, float height,
                      float u0, float v0, float u1, float v1, int color);

        /**
         * Emits a solid-color filled quad.
         *
         * @param x      screen X position
         * @param y      screen Y position
         * @param width  quad width
         * @param height quad height
         * @param color  fill color (ARGB)
         */
        void colored(float x, float y, float width, float height, int color);
    }
}
