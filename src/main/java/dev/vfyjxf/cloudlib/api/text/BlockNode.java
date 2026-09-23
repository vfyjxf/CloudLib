package dev.vfyjxf.cloudlib.api.text;

import net.minecraft.world.level.block.state.BlockState;

/**
 * An inline isometric block render ({@code BlockRenderDispatcher.renderSingleBlock}).
 *
 * @param state the block state to render
 * @param size  edge length of the reserved square box in pixels
 */
public record BlockNode(BlockState state, int size) implements RichNode {

    public BlockNode {
        if (state == null) throw new NullPointerException("state");
    }

    public BlockNode(BlockState state) {
        this(state, 16);
    }
}
