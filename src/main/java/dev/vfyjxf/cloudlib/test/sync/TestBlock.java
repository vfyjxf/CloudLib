package dev.vfyjxf.cloudlib.test.sync;

import dev.vfyjxf.cloudlib.api.block.BasicEntityBlock;

public class TestBlock extends BasicEntityBlock<TestBlockEntity> {

    public TestBlock(Properties properties) {
        super(TestBlockEntities.testBlockEntity, TestBlockEntity.Menu.INFO, properties);
        this.stateDefinition.getPossibleStates();
    }
}
