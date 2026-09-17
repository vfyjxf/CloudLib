package dev.vfyjxf.cloudlib.internal.ui.inworld;

import dev.vfyjxf.cloudlib.api.ui.inworld.OcclusionProbe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LevelOcclusionProbeTest {

    /** A minimal block level: stone where listed, air everywhere else. */
    private static final class FakeLevel implements BlockGetter {

        private final Set<BlockPos> solid = new HashSet<>();

        @Override
        public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return solid.contains(pos) ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return Fluids.EMPTY.defaultFluidState();
        }

        @Override
        public int getHeight() {
            return 384;
        }

        @Override
        public int getMinBuildHeight() {
            return -64;
        }
    }

    @Test
    void clearSegmentPassesThroughEmptySpace() {
        LevelOcclusionProbe probe = new LevelOcclusionProbe(new FakeLevel());

        assertTrue(probe.segmentClear(new Vec3(0.5, 1.5, 8.5), new Vec3(0.5, 1.5, -8.5)));
    }

    @Test
    void segmentThroughASolidBlockIsBlocked() {
        FakeLevel level = new FakeLevel();
        level.solid.add(new BlockPos(0, 1, 0));
        LevelOcclusionProbe probe = new LevelOcclusionProbe(level);

        assertFalse(probe.segmentClear(new Vec3(0.5, 1.5, 8.5), new Vec3(0.5, 1.5, -8.5)));
        assertTrue(probe.segmentClear(new Vec3(2.5, 1.5, 8.5), new Vec3(2.5, 1.5, -8.5)));
    }

    @Test
    void visibilityCountsTheBlockedSightLines() {
        FakeLevel level = new FakeLevel();
        level.solid.add(new BlockPos(0, 1, 4));
        LevelOcclusionProbe probe = new LevelOcclusionProbe(level);

        // a wide quad centered straight ahead of the eye: only the center
        // sight line funnels through the stone column, the four corner lines
        // pass outside it
        double visibility = probe.visibility(
                new Vec3(0.5, 1.5, 8.5), new Vec3(0.5, 1.5, 0.0), new Vec3(0.02, 0, 0), new Vec3(0, 0.02, 0), 200, 200);
        assertEquals(0.8, visibility, 1.0e-9);
    }

    @Test
    void implementsTheOcclusionProbeContract() {
        OcclusionProbe probe = new LevelOcclusionProbe(new FakeLevel());
        Vec3 eye = new Vec3(0, 65, 0);
        Vec3 center = new Vec3(4, 65, 0);

        assertEquals(1.0, probe.visibility(eye, center, new Vec3(0.02, 0, 0), new Vec3(0, 0.02, 0), 50, 50), 0.0);
    }
}
