package dev.vfyjxf.nimbusprojection.demo;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Demo block with no block entity: the in-world provider anchors a long-range
 * floating waypoint marker to every placed waypoint block.
 */
public class WaypointBlock extends Block {

    public WaypointBlock() {
        super(BlockBehaviour.Properties.of());
    }
}
