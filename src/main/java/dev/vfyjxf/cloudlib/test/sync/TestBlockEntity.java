package dev.vfyjxf.cloudlib.test.sync;

import dev.vfyjxf.cloudlib.api.data.CheckStrategy;
import dev.vfyjxf.cloudlib.api.data.snapshot.DiffObservable;
import dev.vfyjxf.cloudlib.api.network.UnaryFlowHandler;
import dev.vfyjxf.cloudlib.api.network.expose.DiffLayerExpose;
import dev.vfyjxf.cloudlib.api.network.expose.Expose;
import dev.vfyjxf.cloudlib.api.network.expose.ReversedOnly;
import dev.vfyjxf.cloudlib.api.ui.sync.menu.BasicMenu;
import dev.vfyjxf.cloudlib.api.ui.sync.menu.MenuInfo;
import dev.vfyjxf.cloudlib.test.TestRegistry;
import dev.vfyjxf.cloudlib.utils.Locations;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static dev.vfyjxf.cloudlib.api.data.CheckStrategy.primitive;
import static dev.vfyjxf.cloudlib.api.data.CheckStrategy.sameItemStack;
import static dev.vfyjxf.cloudlib.api.data.snapshot.Snapshot.immutableRefOf;
import static dev.vfyjxf.cloudlib.api.data.snapshot.Snapshot.mutableRefOf;

public class TestBlockEntity extends BlockEntity {

    private static final ItemStack[] ITEM_STACKS = new ItemStack[]{
            new ItemStack(Items.ACACIA_WOOD, 10),
            new ItemStack(Items.WHITE_WOOL, 20),
            new ItemStack(Items.RED_WOOL, 30),
            new ItemStack(Items.GREEN_WOOL, 40),
            new ItemStack(Items.BLUE_WOOL, 50),
            new ItemStack(Items.YELLOW_WOOL, 60),
    };

    private ItemStack selected;
    private int basic;
    private String reference = "";
    private ItemStack registerEntry = ItemStack.EMPTY;
    public final ObservableItemHandler transform = new ObservableItemHandler(9);
    private List<ItemStack> list;
    private long currentTick;

    public TestBlockEntity(BlockPos pos, BlockState blockState) {
        super(TestRegistry.testBlockEntity.get(), pos, blockState);
    }

    public static BlockEntityTicker<TestBlockEntity> ticker() {
        return (level1, pos, state, blockEntity) -> {
            if (level1.isClientSide) return;
            blockEntity.currentTick++;
            ThreadLocalRandom random = ThreadLocalRandom.current();
            if (blockEntity.currentTick % random.nextInt(1, 20) == 0) {
                blockEntity.basic = random.nextInt(100);
                blockEntity.reference = "test" + blockEntity.basic;
                blockEntity.registerEntry = ITEM_STACKS[random.nextInt(ITEM_STACKS.length)].copy();
            }
            if (blockEntity.currentTick % 40 == 0) {
                for (int i = 0; i < 9; i++) {
                    int index = random.nextInt(ITEM_STACKS.length);
                    ItemStack stack = ITEM_STACKS[index].copyWithCount(random.nextInt(1, 10));
                    blockEntity.transform.setStackInSlot(i, stack);
                }
            }
        };
    }

    public static class Menu extends BasicMenu<TestBlockEntity> {

//        public final Expose<@NotNull Integer> basic = expose(
//                "basic",
//                mutableRefOf(primitive()),
//                o -> o.basic,
//                UnaryFlowHandler.codecOf(ByteBufCodecs.INT)
//        );
//
//        public final Expose<@NotNull String> reference = expose(
//                "reference",
//                mutableRefOf(CheckStrategy.equals()),
//                o -> o.reference,
//                UnaryFlowHandler.codecOf(ByteBufCodecs.STRING_UTF8)
//        );
//
//        public final Expose<@NotNull ItemStack> registerEntry = expose(
//                "registerEntry",
//                mutableRefOf(sameItemStack),
//                o -> o.registerEntry,
//                UnaryFlowHandler.codecOf(ItemStack.STREAM_CODEC)
//        );
//
//        public final ReversedOnly<@NotNull ItemStack, @NotNull ItemStack> selected = reversedOnly(
//                "selected",
//                ItemStack.STREAM_CODEC::encode,
//                ItemStack.STREAM_CODEC::decode
//        ).whenReceiveFromClient(stack -> {
//            provider.selected = stack;
//            System.out.println("Selected: " + stack);
//        });

//        public final LayerExpose<@NotNull List<ItemStack>> layerExpose = layerExpose(
//                "transform",
//                immutableRefOf(provider.transform),
//                o -> o.transform,
//                (byteBuf, element) ->
//                {
//                    IItemHandler inner = element.inner;
//                    int size = inner.getSlots();
//                    byteBuf.writeInt(size);
//                    for (int i = 0; i < size; i++) {
//                        ItemStack stack = inner.getStackInSlot(i);
//                        ItemStack.OPTIONAL_STREAM_CODEC.encode(byteBuf, stack);
//                    }
//                },
//                (byteBuf) ->
//                {
//                    int size = byteBuf.readInt();
//                    List<ItemStack> stacks = new ArrayList<>();
//                    for (int i = 0; i < size; i++) {
//                        ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(byteBuf);
//                        stacks.add(stack);
//                    }
//                    return stacks;
//                }
//        );

        public final DiffLayerExpose<List<ItemStack>, Set<IntObjectPair<ItemStack>>> diffable = diffLayerExpose(
                "diffable",
                immutableRefOf(provider.transform),
                o -> o.transform,
                (byteBuf, element) ->
                {
                    List<ItemStack> list = IntStream.range(0, element.getSlots())
                            .mapToObj(element::getStackInSlot)
                            .toList();
                    ItemStack.OPTIONAL_LIST_STREAM_CODEC.encode(byteBuf, list);
                },
                ItemStack.OPTIONAL_LIST_STREAM_CODEC::decode,

                (byteBuf, element) -> {
                    byteBuf.writeInt(element.size());
                    for (IntObjectPair<ItemStack> stack : element) {
                        byteBuf.writeInt(stack.leftInt());
                        ItemStack.OPTIONAL_STREAM_CODEC.encode(byteBuf, stack.right());
                    }
                },
                (byteBuf -> {
                    int size = byteBuf.readInt();
                    Set<IntObjectPair<ItemStack>> stacks = new HashSet<>();
                    for (int i = 0; i < size; i++) {
                        int index = byteBuf.readInt();
                        ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(byteBuf);
                        stacks.add(IntObjectPair.of(index, stack));
                    }
                    return stacks;
                })

        );

        public static final MenuInfo<Menu, TestBlockEntity> INFO = MenuInfo.create(
                Locations.of("test_block_entity"),
                Menu::new,
                () -> TestBlockEntityScreen::new
        );

        public Menu(MenuType<Menu> menuType, int containerId, Inventory inventory, TestBlockEntity holder) {
            super(menuType, containerId, holder, inventory);
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    public static class ObservableItemHandler extends ItemStackHandler implements DiffObservable<Set<IntObjectPair<ItemStack>>> {

        private final IntSet changedSlots = new IntArraySet();


        public ObservableItemHandler(NonNullList<ItemStack> stacks) {
            super(stacks);
        }

        public ObservableItemHandler(int size) {
            super(size);
        }

        @Override
        protected void onContentsChanged(int slot) {
            changedSlots.add(slot);
        }

        @Override
        public boolean changed() {
            return changedSlots.isEmpty();
        }

        @Override
        public Set<IntObjectPair<ItemStack>> difference() {
            var difference = changedSlots.intStream()
                    .mapToObj(slot -> IntObjectPair.of(slot, this.getStackInSlot(slot)))
                    .filter(pair -> !pair.right().isEmpty())
                    .collect(Collectors.toSet());
            changedSlots.clear();
            return difference;
        }
    }


}
