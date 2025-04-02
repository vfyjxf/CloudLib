package dev.vfyjxf.cloudlib.test.sync;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.vfyjxf.cloudlib.api.data.EqualsChecker;
import dev.vfyjxf.cloudlib.api.snapshot.DiffObservable;
import dev.vfyjxf.cloudlib.api.ui.sync.accessor.ValueAccessor;
import dev.vfyjxf.cloudlib.api.ui.sync.menu.BasicMenu;
import dev.vfyjxf.cloudlib.api.ui.sync.menu.MenuInfo;
import dev.vfyjxf.cloudlib.utils.Locations;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;
import java.util.stream.Stream;

public class TestBlockEntity extends BlockEntity {


    public static final Codec<TestBlockEntity> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.fieldOf("basic").forGetter(o -> o.basic),
            Codec.STRING.fieldOf("reference").forGetter(o -> o.reference),
            ItemStack.CODEC.fieldOf("registerEntry").forGetter(o -> o.registerEntry),
            ItemStack.CODEC.listOf().fieldOf("list").forGetter(o -> o.list)
    ).apply(ins, TestBlockEntity::new));


    private int basic;
    private String reference;
    private ItemStack registerEntry;
    public ObservableItemHandler transform;
    private List<ItemStack> list;

    private TestBlockEntity(int basic, String reference, ItemStack registerEntry, List<ItemStack> list) {
        super(null, null, null);
        this.basic = basic;
        this.reference = reference;
        this.registerEntry = registerEntry;
        this.list = list;
    }

    public TestBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static class Menu extends BasicMenu<TestBlockEntity> {

        public final ValueAccessor<Integer> basic = defineAccessor(
                ByteBufCodecs.VAR_INT,
                EqualsChecker.primitive(),
                "basic", o -> o.basic
        );

        public final ValueAccessor<String> reference = defineAccessor(
                ByteBufCodecs.STRING_UTF8,
                EqualsChecker.equals(),
                "reference", o -> o.reference
        );

        public final ValueAccessor<ItemStack> registerEntry = defineAccessor(
                ItemStack.STREAM_CODEC,
                EqualsChecker.sameItemStack,
                "registerEntry", o -> o.registerEntry
        );

        public static final MenuInfo<Menu, TestBlockEntity> INFO = MenuInfo.create(
                Locations.of("test_block_entity"),
                Menu::new,
                () -> TestBlockEntityScreen::new
        );

        public Menu(MenuType<?> menuType, int containerId, Inventory playerInventory, TestBlockEntity holder) {
            super(menuType, containerId, holder);
            basic.onUpdate((old, next) -> {

            });
        }

    }

    public static class ObservableItemHandler implements DiffObservable<Stream<ItemStack>> {

        public final IItemHandler inner;

        public ObservableItemHandler(IItemHandler inner) {
            this.inner = inner;
        }


        @Override
        public boolean changed() {
            return false;
        }

        @Override
        public Stream<ItemStack> changes() {
            return Stream.empty();
        }
    }


}
